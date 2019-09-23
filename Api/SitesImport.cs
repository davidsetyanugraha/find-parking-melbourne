using System;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Http;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.Logging;
using Newtonsoft.Json;
using System.Net.Http;
using Microsoft.Azure.Documents.Spatial;
using System.Collections.Generic;

namespace Parking.Sites
{
    public static class SitesImport
    {
        static HttpClient client = new HttpClient();
        static string sensorsApiUrl = "https://data.melbourne.vic.gov.au/resource/vh2v-4nfs.json?%24limit=10000";
        static string restrictionsApiUrl = "https://data.melbourne.vic.gov.au/resource/ntht-5rk7.json?%24limit=10000";
        

        [FunctionName("SitesImport")]
        public static async Task<IActionResult> Run(
            [HttpTrigger(AuthorizationLevel.Function, "post", Route = "sites/import")] HttpRequest req,
            [CosmosDB(
                databaseName: "parkingdb",
                collectionName: "sites",
                ConnectionStringSetting = "CosmosDBConnectionString")]IAsyncCollector<dynamic> documents,
            ILogger log)
            
        {
            log.LogInformation($"C# Timer trigger function executed at: {DateTime.Now}");

            using (HttpResponseMessage sens = await client.GetAsync(sensorsApiUrl))
            using (HttpResponseMessage res = await client.GetAsync(restrictionsApiUrl))
                using (HttpContent content_sens = sens.Content)
                using (HttpContent content_res = res.Content)
                {
                    dynamic result_sens = JsonConvert.DeserializeObject(await content_sens.ReadAsStringAsync());
                    dynamic result_res = JsonConvert.DeserializeObject(await content_res.ReadAsStringAsync());
                    Dictionary<string, dynamic> dict = new Dictionary<string, dynamic>();

                    foreach (var item_sens in result_sens) {
                        var key_dict = item_sens.bay_id.ToString();
                        dict.Add(key_dict, item_sens);

                        foreach (var item_res in result_res) {

                            if (key_dict == item_res.bayid.ToString()){
                                var newObject = new {
                                    id = item_sens.bay_id.ToString(),
                                    sourceReferences = new {
                                        bayId = item_sens.bay_id.ToString(),
                                        markerId = item_sens.st_marker_id.ToString(),
                                    },
                                    status = item_sens.status,
                                    // description only available in the bays
                                    restrictions = new {
                                        description = item_res.description1,
			                            disabilityext = item_res.disabilityext1,
			                            duration = item_res.duration1,
			                            effectiveonph = item_res.effectiveonph1,
			                            endtime  = item_res.endtime1,
			                            fromday = item_res.fromday1,
			                            starttime = item_res.starttime1,
			                            today = item_res.today1,
			                            typedesc = item_res.typedesc1,
                                    },
                                    location = new Point(
                                        (double)item_sens.location.longitude,
                                        (double)item_sens.location.latitude
                                    )
                                };
                                await documents.AddAsync(newObject);
                            }
                        }
                    }

                    /*foreach (var key_dict in dict) {
                        var newObject = key_dict; {
                            id = item_sens.bay_id.ToString(),
                            status = item.status,
                            location = new Point(
                                (double)item.location.longitude,
                                (double)item.location.latitude
                            )
                        };
                        await documents.AddAsync(newObject);
                    }*/

                    log.LogInformation($"Total parkings processed {result_sens.Count}");
                }
                return new NoContentResult();
        }
    }
}
