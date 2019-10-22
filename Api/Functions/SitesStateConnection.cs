using System;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Http;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.Logging;
using Newtonsoft.Json;
using Microsoft.Azure.WebJobs.Extensions.SignalRService;
using FluentValidation;
using Api.Models;
using Microsoft.Azure.Documents.Client;
using Microsoft.Azure.Documents;

namespace Api.Functions
{
    public static class SitesStateConnection
    {
        public class Command
        {
            public string ConnectionId { get; set; }
            public string ParkingBayId { get; set; }
        }

        public class CommandValidator : AbstractValidator<Command>
        {
            public CommandValidator()
            {
                RuleFor(x => x.ConnectionId)
                    .NotEmpty();
                RuleFor(x => x.ParkingBayId)
                    .NotEmpty();
            }
        }

        //Connects the client to the SignalR Service
        [FunctionName("SitesStateConnectionNegotiate")]
        public static IActionResult Negotiate(
            [HttpTrigger(AuthorizationLevel.Anonymous, "post", Route = "sites/state/connection/negotiate")] HttpRequest req,
            [SignalRConnectionInfo(HubName = "SitesState")]SignalRConnectionInfo connectionInfo,
            ILogger log)
        {
            log.LogInformation($"Connection created for client {connectionInfo.Url} {connectionInfo.AccessToken}.");

            return connectionInfo != null
                ? (ActionResult)new OkObjectResult(connectionInfo)
                : new NotFoundObjectResult("Failed to connect to the notification service.");
        }

        // Allows the client to follow the updates of a parking bay
        [FunctionName("SitesStateConnectionFollow")]
        public static async Task<IActionResult> Follow(
            [HttpTrigger(AuthorizationLevel.Anonymous, "post", Route = "sites/state/connection/follow")] Command command,
            [CosmosDB(ConnectionStringSetting = "CosmosDBConnectionString")] DocumentClient client,
            [SignalR(HubName = "SitesState")] IAsyncCollector<SignalRGroupAction> signalRGroupActions,
            ILogger log)
        {
            //Validate the input parameters
            var validationResult = new CommandValidator().Validate(command);
            if (!validationResult.IsValid)
            {
                return new BadRequestObjectResult(validationResult.Errors.Select(e => new {
                    Field = e.PropertyName,
                    Error = e.ErrorMessage
                }));
            }

            //Check if the bay is valid, otherwise return not found
            DocumentResponse<SiteState> documentResponse;
            try
            {
                documentResponse = await client.ReadDocumentAsync<SiteState>(
                    UriFactory.CreateDocumentUri("parkingdb", "sitesstate", command.ParkingBayId),
                    new RequestOptions() { PartitionKey = new PartitionKey(null) });
            }
            catch (DocumentClientException e) {
                if (e.StatusCode == System.Net.HttpStatusCode.NotFound) return new NotFoundResult();
                else throw;
            }
            
            var siteState = documentResponse.Document;

            if (siteState == null) {
                return new NotFoundResult();
            }

            log.LogInformation($"Client {command.ConnectionId} registering to follow bay {command.ParkingBayId}.");
            
            // Add the connection to the group
            await signalRGroupActions.AddAsync(
                new SignalRGroupAction
                {
                    ConnectionId = command.ConnectionId,
                    GroupName = command.ParkingBayId,
                    Action = GroupAction.Add
                });

            return (ActionResult)new OkObjectResult(siteState);
        }

        // Allows the client to stop following the updates of a parking bay
        [FunctionName("SitesStateConnectionUnfollow")]
        public static async Task<IActionResult> Unfollow(
            [HttpTrigger(AuthorizationLevel.Anonymous, "post", Route = "sites/state/connection/unfollow")] Command command,
            [SignalR(HubName = "SitesState")] IAsyncCollector<SignalRGroupAction> signalRGroupActions,
            ILogger log)
        {
            //Validate the input parameters
            var validationResult = new CommandValidator().Validate(command);
            if (!validationResult.IsValid)
            {
                return new BadRequestObjectResult(validationResult.Errors.Select(e => new {
                    Field = e.PropertyName,
                    Error = e.ErrorMessage
                }));
            }

            log.LogInformation($"Client {command.ConnectionId} unregistering from following bay {command.ParkingBayId}.");

            // Remove the connection from the group
            await signalRGroupActions.AddAsync(
                new SignalRGroupAction
                {
                    ConnectionId = command.ConnectionId,
                    GroupName = command.ParkingBayId,
                    Action = GroupAction.Remove
                });

            return (ActionResult)new OkResult();
        }
    }
}
