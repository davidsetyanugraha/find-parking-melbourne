using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Http;
using Microsoft.Extensions.Logging;
using System.Net.Http;
using System.Collections.Generic;

namespace Api.Functions
{
    public static class SitesGet
    {
        [FunctionName("SitesGet")]
        public static HttpResponseMessage Run(
            [HttpTrigger(AuthorizationLevel.Function, "get", Route = "sites/import")] HttpRequestMessage req,
            [CosmosDB(
                databaseName: "parkingdb",
                collectionName: "sites",
                ConnectionStringSetting = "CosmosDBConnectionString", SqlQuery = "SELECT * FROM c")] IEnumerable<object> documents,
            ILogger log)
        {
            return req.CreateResponse(documents);
        }
    }
}
