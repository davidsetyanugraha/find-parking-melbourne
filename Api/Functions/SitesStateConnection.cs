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

        [FunctionName("SitesStateConnectionNegotiate")]
        public static IActionResult Negotiate(
            [HttpTrigger(AuthorizationLevel.Anonymous, "post", Route = "sites/state/connection/negotiate")] HttpRequest req,
            [SignalRConnectionInfo(HubName = "SitesState")]SignalRConnectionInfo connectionInfo,
            ILogger log)
        {
            log.LogInformation("Connection created for client.");

            return connectionInfo != null
                ? (ActionResult)new OkObjectResult(connectionInfo)
                : new NotFoundObjectResult("Failed to connect to the notification service.");
        }

        [FunctionName("SitesStateConnectionFollow")]
        public static async Task<IActionResult> Follow(
            [HttpTrigger(AuthorizationLevel.Anonymous, "post", Route = "sites/state/connection/follow")] Command command,
            [SignalR(HubName = "SitesState")] IAsyncCollector<SignalRGroupAction> signalRGroupActions,
            ILogger log)
        {
            // string requestBody = await new StreamReader(req.Body).ReadToEndAsync();
            // var command = JsonConvert.DeserializeObject<Command>(requestBody);
            
            var validationResult = new CommandValidator().Validate(command);

            if (!validationResult.IsValid)
            {
                return new BadRequestObjectResult(validationResult.Errors.Select(e => new {
                    Field = e.PropertyName,
                    Error = e.ErrorMessage
                }));
            }

            log.LogInformation($"Client registering to follow bay {command.ConnectionId}.");
            
            // var userIdClaim = claimsPrincipal.FindFirst(ClaimTypes.NameIdentifier);
            await signalRGroupActions.AddAsync(
                new SignalRGroupAction
                {
                    ConnectionId = command.ConnectionId,
                    GroupName = command.ParkingBayId,
                    Action = GroupAction.Add
                });

            return (ActionResult)new OkResult();
        }

        [FunctionName("SitesStateConnectionUnfollow")]
        public static async Task<IActionResult> Unfollow(
            [HttpTrigger(AuthorizationLevel.Anonymous, "post", Route = "sites/state/connection/unfollow")] Command command,
            [SignalR(HubName = "SitesState")] IAsyncCollector<SignalRGroupAction> signalRGroupActions,
            ILogger log)
        {
            var validationResult = new CommandValidator().Validate(command);

            if (!validationResult.IsValid)
            {
                return new BadRequestObjectResult(validationResult.Errors.Select(e => new {
                    Field = e.PropertyName,
                    Error = e.ErrorMessage
                }));
            }

            log.LogInformation($"Client unregistering from following bay {command.ConnectionId}.");

            //var userIdClaim = claimsPrincipal.FindFirst(ClaimTypes.NameIdentifier);
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
