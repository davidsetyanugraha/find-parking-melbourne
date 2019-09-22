using System;
using System.Net.Http;
using System.Threading.Tasks;
using Microsoft.Azure.Documents.Spatial;
using Microsoft.Azure.WebJobs;
// using Microsoft.Azure.WebJobs.Host;
using Microsoft.Extensions.Logging;
using Newtonsoft.Json;

namespace Unimelb.Parking
{
    public static class ParkingAvailabilitySample
    {
        static HttpClient client = new HttpClient();
        static string parkingApiUrl = "https://data.melbourne.vic.gov.au/resource/vh2v-4nfs.json?%24limit=10000";
        
        [FunctionName("ParkingAvailabilitySample")]
        public static async Task RunAsync(
            [TimerTrigger("0 */2 * * * *")]TimerInfo myTimer,
            [CosmosDB(
                databaseName: "parkingdb",
                collectionName: "spots",
                ConnectionStringSetting = "CosmosDBConnectionString")]IAsyncCollector<dynamic> documents,
            ILogger log)
        {
            log.LogInformation($"C# Timer trigger function executed at: {DateTime.Now}");

            using (HttpResponseMessage res = await client.GetAsync(parkingApiUrl))
                using (HttpContent content = res.Content)
                {
                    dynamic result = JsonConvert.DeserializeObject(await content.ReadAsStringAsync());
                    foreach (var item in result) {
                        var newObject = new {
                            id = item.bay_id.ToString(),
                            status = item.status,
                            location = new Point(
                                (double)item.location.longitude,
                                (double)item.location.latitude
                            )
                        };
                        await documents.AddAsync(newObject);
                    }

                    log.LogInformation($"Total parkings processed {result.Count}");
                }
        }
    }
}
