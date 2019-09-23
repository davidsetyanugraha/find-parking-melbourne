using System;
using Microsoft.Azure.Documents.Spatial;
using Newtonsoft.Json;

namespace Api.Models
{
    public class SiteState
    {
        public string Id { get; set; }
        public string Status { get; set; }
        //https://stackoverflow.com/questions/44950597/cosmos-db-not-respecting-json-net-camelcasenamingstrategy-in-query
        //https://stackoverflow.com/questions/37489768/how-to-tell-documentdb-sdk-to-use-camelcase-during-linq-query/37490316#37490316
        //https://github.com/Azure/azure-cosmos-dotnet-v2/issues/317
        //https://github.com/Azure/azure-cosmos-dotnet-v2/issues/286
        [JsonProperty("location")]
        public Point Location { get; set; }
        public DateTime LastUpdate { get; set; }
        public string Zone { get; set; }
    }
}