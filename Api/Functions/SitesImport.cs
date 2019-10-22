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

namespace Api.Functions
{
    public static class SitesImport
    {
        static HttpClient client = new HttpClient();
        static string sensorsApiUrl = "https://data.melbourne.vic.gov.au/resource/vh2v-4nfs.json?%24limit=10000";
        static string restrictionsApiUrl = "https://data.melbourne.vic.gov.au/resource/ntht-5rk7.json?%24limit=10000";
        static string baysApiUrl = "https://data.melbourne.vic.gov.au/resource/wuf8-susg.json?%24limit=100000";

        // Sync all the sites using the city of Melbourne API
        [FunctionName("SitesImport")]
        public static async Task<IActionResult> Run(
            [HttpTrigger(AuthorizationLevel.Anonymous, "post", Route = "sites/import")] HttpRequest req,
            [CosmosDB(
                databaseName: "parkingdb",
                collectionName: "sites",
                ConnectionStringSetting = "CosmosDBConnectionString")]IAsyncCollector<dynamic> documents,
            ILogger log)
            
        {
            log.LogInformation($"C# Timer trigger function executed at: {DateTime.Now}");

            // Retrieve the restrictions
            Dictionary<string, dynamic> dict_res = new Dictionary<string, dynamic>();
            using (HttpResponseMessage res = await client.GetAsync(restrictionsApiUrl))
            {
                 using (HttpContent content_res = res.Content)
                 {
                     dynamic result_res = JsonConvert.DeserializeObject(await content_res.ReadAsStringAsync());
                     foreach (var item_res in result_res)
                     {
                        var key_dict_res = item_res.bayid.ToString();
                        dict_res.Add(key_dict_res, item_res);
                     }
                 }
            }

            log.LogInformation($"Restrictions downloaded at: {DateTime.Now}");

            // Retrieve the bays
            Dictionary<string, dynamic> dict_bay = new Dictionary<string, dynamic>();
            using (HttpResponseMessage bay = await client.GetAsync(baysApiUrl))
            {
                using (HttpContent content_bay = bay.Content)
                {
                    dynamic result_bay = JsonConvert.DeserializeObject(await content_bay.ReadAsStringAsync());
                    foreach (var item_bay in result_bay)
                    {
                        if (item_bay.marker_id != null && !dict_bay.ContainsKey(item_bay.marker_id.ToString()))
                        {
                            var key_dict_bay = item_bay.marker_id.ToString();
                            dict_bay.Add(key_dict_bay, item_bay);
                        }
                    }
                }
            }

            log.LogInformation($"Bays downloaded at: {DateTime.Now}");

            // Retrieve the sensors and compose the object to store
            using (HttpResponseMessage sens = await client.GetAsync(sensorsApiUrl))
            {
                log.LogInformation($"Sensors downloaded at: {DateTime.Now}");
                using (HttpContent content_sens = sens.Content)
                {
                    //get the results of the query
                    dynamic result_sens = JsonConvert.DeserializeObject(await content_sens.ReadAsStringAsync());

                    foreach (var item_sens in result_sens)
                    {
                        //get the corresponding bays and restrictions
                        dynamic item_res = null;
                        dict_res.TryGetValue(item_sens.bay_id.ToString(), out item_res);
                        dynamic item_bay = null;
                        dict_bay.TryGetValue(item_sens.st_marker_id.ToString(), out item_bay);

                        // compose the restrictions
                        List<dynamic> restrictions = new List<dynamic>();
                        for (int i = 1; i < 7; i++)
                        {
                            if (item_res?["description" + i] != null)
                            {
                                restrictions.Add(
                                    new {
                                        description = item_res?["description" + i], 
                                        disabilityext = item_res?["disabilityext" + i], 
                                        duration = item_res?["duration" + i], 
                                        effectiveonph = item_res?["effectiveonph" + i], 
                                        endtime  = item_res?["endtime" + i], 
                                        fromday = item_res?["fromday" + i], 
                                        starttime = item_res?["starttime" + i], 
                                        today = item_res?["today" + i], 
                                        typedesc = item_res?["typedesc" + i], 
                                    }
                                );
                            }  
                        }

                        // Compose the new object
                        var newObject = new {
                            id = item_sens.bay_id.ToString(),
                            sourceReferences = new {
                                bayId = item_sens.bay_id.ToString(),
                                markerId = item_sens.st_marker_id.ToString(),
                            },
                            description = item_bay?.rd_seg_dsc,
                            polygon = ConvertGeometry(item_bay?.the_geom),
                            restrictions = restrictions,
                            location = new Point(
                                (double)item_sens.location?.longitude,
                                (double)item_sens.location?.latitude
                            )
                        };

                        if (newObject.polygon == null)
                        {
                            log.LogInformation($"This bay does not have a related polygon {newObject.id}");
                        }
                        // Add the object to the database
                        await documents.AddAsync(newObject);
                    }
                    log.LogInformation($"Total parkings processed {result_sens.Count}");
                }
                return new NoContentResult();
            }
        }

        private static dynamic ConvertGeometry(dynamic the_geom)
        {
            if (the_geom == null || the_geom.type != "MultiPolygon"
                || the_geom.coordinates.Count != 1 || the_geom.coordinates[0].Count != 1)
            {
                return null;
            }
            else
            {
                return the_geom.coordinates[0][0];
            }
        }
    }
}
