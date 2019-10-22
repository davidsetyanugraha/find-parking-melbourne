using System;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Azure.Documents.Spatial;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Http;
using Microsoft.Extensions.Logging;
using FluentValidation;
using Microsoft.Azure.Documents.Client;
using Microsoft.Azure.Documents.Linq;
using Api.Models;
using Microsoft.Azure.Documents;

namespace Api.Functions
{
    public static class SitesStateGet
    {
        public class Query
        {
            public double? Latitude { get; set; }
            public double? Longitude { get; set; }
            public double? Distance { get; set; }
        }

        // Sample from https://www.tomfaltesek.com/azure-functions-input-validation/
        public class QueryValidator : AbstractValidator<Query>
        {
            public QueryValidator()
            {
                RuleFor(x => x.Latitude)
                    .NotEmpty()
                    .GreaterThanOrEqualTo(-90)
                    .LessThanOrEqualTo(90);
                RuleFor(x => x.Longitude)
                    .NotEmpty()
                    .GreaterThanOrEqualTo(-180)
                    .LessThanOrEqualTo(180);
                RuleFor(x => x.Distance)
                    .GreaterThan(0)
                    .LessThanOrEqualTo(3000);
            }
        }

        // Get the state of the parking bays giving a point and a distance
        [FunctionName("SitesStateGet")]
        public static async Task<IActionResult> Run(
            [HttpTrigger(AuthorizationLevel.Anonymous, "get", "post", Route = "sites/state")] Query message,
            [CosmosDB(ConnectionStringSetting = "CosmosDBConnectionString")] DocumentClient client,
            ILogger log)
        {
            // Validate input
            var validationResult = new QueryValidator().Validate(message);
            if (!validationResult.IsValid)
            {
                return new BadRequestObjectResult(validationResult.Errors.Select(e => new {
                    Field = e.PropertyName,
                    Error = e.ErrorMessage
                }));
            }
            
            log.LogInformation("Finding points at the requested distance.");

            //Query the database to find the parking bays
            Uri collectionUri = UriFactory.CreateDocumentCollectionUri("parkingdb", "sitesstate");
            // The distance is in meters according to https://stackoverflow.com/questions/54453190/what-units-does-st-distance-return
            var distance = message.Distance ?? 1000;
            // From http://dontcodetired.com/blog/post/Reading-Azure-Cosmos-DB-Data-In-Azure-Functions and
            // https://docs.microsoft.com/en-us/sandbox/functions-recipes/cosmos-db?tabs=csharp
            IDocumentQuery<SiteState> query = client.CreateDocumentQuery<SiteState>(collectionUri,
                // new FeedOptions() { EnableCrossPartitionQuery = true })
                //https://dontpaniclabs.com/blog/post/2017/09/07/getting-started-azure-cosmos-db-part-4-partition-keys/
                //https://www.lytzen.name/2016/12/06/find-docs-with-no-partitionkey-in-azure.html
                new FeedOptions() { PartitionKey = new PartitionKey(null) })//Undefined.Value) })
                .Where(siteState => siteState.RecordState != SiteState.EntityState.Deleted
                    && siteState.Location.Distance(new Point(message.Longitude.Value, message.Latitude.Value)) < distance)
                .AsDocumentQuery();

            // More of the SDK https://joonasw.net/view/exploring-cosmos-db-sdk-v3

            var result = await query.ToListAsync();
            return new OkObjectResult(result);
        }
    }
}
