using System;
using System.Net.Http;
using System.Threading.Tasks;
using Microsoft.Azure.Documents.Spatial;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Host;
using Microsoft.Extensions.Logging;
using Newtonsoft.Json;

namespace Parking.Sites
{
    public static class SitesStateImport
    {
        static HttpClient client = new HttpClient();
        static string parkingApiUrl = "https://data.melbourne.vic.gov.au/resource/vh2v-4nfs.json?%24limit=10000";
        
        [FunctionName("SitesStateImport")]
        public static async Task RunAsync(
            //[TimerTrigger("0 */2 * * * *")]TimerInfo myTimer,
            [HttpTrigger(Microsoft.Azure.WebJobs.Extensions.Http.AuthorizationLevel.Anonymous, "post", Route = "sites/stateupdate")] Microsoft.AspNetCore.Http.HttpRequest req,
            [CosmosDB(
                databaseName: "parkingdb",
                collectionName: "sitesstate",
                ConnectionStringSetting = "CosmosDBConnectionString")]IAsyncCollector<dynamic> documents,
            ILogger log)
        {
            log.LogInformation($"Sites State import executed at: {DateTime.Now}");

            using (HttpResponseMessage res = await client.GetAsync(parkingApiUrl))
            {
                using (HttpContent content = res.Content)
                {
                    var lastUpdate = DateTime.Now;
                    dynamic result = JsonConvert.DeserializeObject(await content.ReadAsStringAsync());
                    foreach (var item in result) {
                        var newObject = new {
                            id = item.bay_id.ToString(),
                            status = item.status,
                            location = new Point(
                                (double)item.location.longitude,
                                (double)item.location.latitude
                            ),
                            lastUpdate = lastUpdate
                        };
                        await documents.AddAsync(newObject);
                    }

                    log.LogInformation($"Total sites processed {result.Count}");
                }
            }
        }
    }
}