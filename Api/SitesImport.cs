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
        static string baysApiUrl = "https://data.melbourne.vic.gov.au/resource/wuf8-susg.json?%24limit=100000";

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
            using (HttpResponseMessage bay = await client.GetAsync(baysApiUrl))
                using (HttpContent content_sens = sens.Content)
                using (HttpContent content_res = res.Content)
                using (HttpContent content_bay = bay.Content)

                {
                    dynamic result_sens = JsonConvert.DeserializeObject(await content_sens.ReadAsStringAsync());
                    dynamic result_res = JsonConvert.DeserializeObject(await content_res.ReadAsStringAsync());
                    dynamic result_bay = JsonConvert.DeserializeObject(await content_bay.ReadAsStringAsync());
                    Dictionary<string, dynamic> dict_res = new Dictionary<string, dynamic>();
                    Dictionary<string, dynamic> dict_bay = new Dictionary<string, dynamic>();

                    foreach (var item_res in result_res) {
                        var key_dict_res = item_res.bayid.ToString();
                        dict_res.Add(key_dict_res, item_res);

                        foreach (var item_bay in result_bay) {
                            // don't know how to state different from
                            if (item_bay.marker_id == null){
                                // didn't know an alternative, since break and continue aren't
                                var dummy = 0;
                            }
                            // for some reason the function would not work along with a OR statement above. 
                            // Included condition because marker_id 799W is duplicated. It seems to be the only case.
                            else if(dict_bay.ContainsKey(item_bay.marker_id.ToString())){
                                var dummy = 0;
                            }
                            else {
                                var key_dict_bay = item_bay.marker_id.ToString();
                                dict_bay.Add(key_dict_bay, item_bay);
                            }

                            foreach (var item_sens in result_sens) {

                                if (key_dict_res == item_sens.bay_id.ToString()){
                                    // wouldn't allow me to use the key variable here...
                                    if (dict_bay.ContainsKey(item_sens.st_marker_id.ToString())){
                                        var newObject = new {
                                            id = item_sens.bay_id.ToString(),
                                            sourceReferences = new {
                                                bayId = item_sens.bay_id.ToString(),
                                                markerId = item_sens.st_marker_id.ToString(),
                                            },
                                            description = item_bay.rd_seg_dsc,
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
                        }
                    }

                    log.LogInformation($"Total parkings processed {result_sens.Count}");
                }
                return new NoContentResult();
        }
    }
}
