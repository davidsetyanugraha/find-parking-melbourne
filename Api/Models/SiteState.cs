using System;
using Microsoft.Azure.Documents.Spatial;
using Newtonsoft.Json;
using Newtonsoft.Json.Converters;

namespace Api.Models
{
    public class SiteState
    {
        public enum EntityState
        {
            Created,
            Updated,
            Deleted
        }

        [JsonProperty("id")]
        public string Id { get; set; }
        [JsonProperty("status")]
        public string Status { get; set; }
        [JsonProperty("location")]
        public Point Location { get; set; }
        // public DateTime LastUpdate { get; set; }
        [JsonProperty("zone")]
        public string Zone { get; set; }
        [JsonProperty(PropertyName = "ttl", NullValueHandling = NullValueHandling.Ignore)]
        public int? Ttl { get; set; }
        [JsonProperty(PropertyName = "recordState", NullValueHandling = NullValueHandling.Ignore)]
        [JsonConverter(typeof(StringEnumConverter))]
        public EntityState RecordState { get; set; }
    }
}
//https://stackoverflow.com/questions/44950597/cosmos-db-not-respecting-json-net-camelcasenamingstrategy-in-query
//https://stackoverflow.com/questions/37489768/how-to-tell-documentdb-sdk-to-use-camelcase-during-linq-query/37490316#37490316
//https://github.com/Azure/azure-cosmos-dotnet-v2/issues/317
//https://github.com/Azure/azure-cosmos-dotnet-v2/issues/286
