using System;
using System.IO;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Http;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.Logging;
using Newtonsoft.Json;
using Microsoft.Azure.WebJobs.Extensions.SignalRService;

namespace Api.Functions
{
    public static class SitesStateConnect
    {
        [FunctionName("SitesStateConnect")]
        public static IActionResult Run(
            [HttpTrigger(AuthorizationLevel.Anonymous, "get", "post", Route = "sites/state/connection/negotiate")] HttpRequest req,
            [SignalRConnectionInfo(HubName = "SitesState")]SignalRConnectionInfo connectionInfo,
            ILogger log)
        {
            log.LogInformation("Connection created for client.");

            return connectionInfo != null
                ? (ActionResult)new OkObjectResult(connectionInfo)
                : new NotFoundObjectResult("Failed to connect to the notification service.");
        }
    }
}
