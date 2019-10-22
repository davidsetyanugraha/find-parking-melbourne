using System;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Http;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.Logging;
using Newtonsoft.Json;
using FluentValidation;
using Api.Models;
using Microsoft.Azure.Documents.Client;
using Microsoft.Azure.Documents;

namespace Api.Functions
{
    public static class SitesStateUpdate
    {
        public class Command
        {
            public string Id { get; set; }
            public string Status { get; set; }
        }

        public class CommandValidator : AbstractValidator<Command>
        {
            public CommandValidator()
            {
                RuleFor(x => x.Id)
                    .NotEmpty();
                RuleFor(x => x.Status)
                    .NotNull();
            }
        }

        // Update the parking bay status without relying on the update from the api
        //https://markheath.net/post/azure-functions-rest-csharp-bindings
        //http://dontcodetired.com/blog/post/Different-Ways-to-Parse-Http-Request-Data-in-Http-triggered-Azure-Functions
        [FunctionName("SitesStateUpdate")]
        public static async Task<IActionResult> Run(
            [HttpTrigger(AuthorizationLevel.Anonymous, "put", Route = "sites/state/{id}")] Command command,
            [CosmosDB(ConnectionStringSetting = "CosmosDBConnectionString")] DocumentClient client,
            ILogger log)
        {
            Uri collectionUri = UriFactory.CreateDocumentCollectionUri("parkingdb", "sitesstate");

            var document = client.CreateDocumentQuery(collectionUri,
                new FeedOptions() { PartitionKey = new PartitionKey(null) })
                .Where(x => x.Id == command.Id)
                .AsEnumerable().FirstOrDefault();
            if (document == null)
            {
                return new NotFoundResult();
            }
            
            document.SetPropertyValue("status", command.Status);
            document.SetPropertyValue("recordState", SiteState.EntityState.Updated.ToString());
            document.SetPropertyValue("ttl", -1);
            await client.ReplaceDocumentAsync(document);

            return new OkObjectResult(document);
        }
    }
}
