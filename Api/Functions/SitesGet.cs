using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Http;
using Microsoft.Extensions.Logging;
using System.Net.Http;
using System.Collections.Generic;

namespace Api.Functions
{
    public static class SitesGet
    {
        //Retrieves all the parking bays
        [FunctionName("SitesGet")]
        public static HttpResponseMessage Run(
            [HttpTrigger(AuthorizationLevel.Anonymous, "get", "post", Route = "sites/")] HttpRequestMessage req,
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
