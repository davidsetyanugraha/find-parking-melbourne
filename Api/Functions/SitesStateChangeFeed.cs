using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Microsoft.Azure.Documents;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Host;
using Microsoft.Azure.WebJobs.Extensions.SignalRService;
using Microsoft.Extensions.Logging;
using Api.Models;

namespace Api.Functions
{
    // Flights example https://davetheunissen.io/real-time-flight-map-w-azure-functions-cosmosdb-signalr/
    //https://anthonychu.ca/post/cosmosdb-real-time-azure-functions-signalr-service/
    //https://www.serverless360.com/blog/build-real-time-serverless-application-with-azure-functions-and-signalr
    //http://dontcodetired.com/blog/post/Using-the-Azure-SignalR-Service-Bindings-in-Azure-Functions-to-Create-Real-time-Serverless-Applications
    //https://docs.microsoft.com/en-us/azure/azure-signalr/signalr-quickstart-azure-functions-csharp
    public static class SitesStateChangeFeed
    {
        // Excecutes every time there is a change in the db
        [FunctionName("SitesStateChangeFeed")]
        public static void Run(
            [CosmosDBTrigger(
                databaseName: "parkingdb",
                collectionName: "sitesstate",
                ConnectionStringSetting = "CosmosDBConnectionString",
                LeaseCollectionName = "leases",
                //FeedPollDelay = 1000,
                CreateLeaseCollectionIfNotExists = true)]IReadOnlyList<Document> input,
            [SignalR(HubName = "SitesState")]IAsyncCollector<SignalRMessage> signalRMessages,
            ILogger log)
        {
            if (input != null && input.Count > 0)
            {
                log.LogInformation("Documents modified " + input.Count);
            }

            foreach (var document in input)
            {
                //If the bay was updated
                if (document.GetPropertyValue<string>("recordState") !=
                    SiteState.EntityState.Created.ToString())
                {
                    //Signal the connected client
                    signalRMessages.AddAsync(
                    new SignalRMessage 
                    {
                        GroupName = document.Id,
                        Target = "SitesState",
                        Arguments = new [] { document } 
                    });
                }                
            }
        }
    }
}
