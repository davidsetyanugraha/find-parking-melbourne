using System;
using System.Collections.Generic;
using Microsoft.Azure.Documents;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Host;
using Microsoft.Extensions.Logging;

namespace Api.Functions
{
    public static class SitesStateChangeFeed
    {
        static int count = 0;

        [FunctionName("SitesStateChangeFeed")]
        public static void Run([CosmosDBTrigger(
            databaseName: "parkingdb",
            collectionName: "sitesstate",
            ConnectionStringSetting = "CosmosDBConnectionString",
            LeaseCollectionName = "leases",
            CreateLeaseCollectionIfNotExists = true)]IReadOnlyList<Document> input, ILogger log)
        {
            if (input != null && input.Count > 0)
            {
                log.LogInformation("Documents modified " + input.Count);
                log.LogInformation("First document Id " + input[0].Id);
                count+=input.Count;
                log.LogInformation("Total Documents modified " + count);
            }
        }
    }
}
