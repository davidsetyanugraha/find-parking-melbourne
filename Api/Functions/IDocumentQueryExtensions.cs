using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.Azure.Documents.Linq;

namespace Api.Functions
{
    //Utility classes for transforming between collections
    public static class IDocumentQueryExtensions
    {
        // Sample from https://stackoverflow.com/questions/39338131/documentclient-createdocumentquery-async
        public static async Task<List<T>> ToListAsync<T>(this IDocumentQuery<T> queryable)
        {
            var list = new List<T>();
            while (queryable.HasMoreResults)
            {   //Note that ExecuteNextAsync can return many records in each call
                var response = await queryable.ExecuteNextAsync<T>();
                list.AddRange(response);
            }
            return list;
        }
        public static async Task<List<T>> ToListAsync<T>(this IQueryable<T> query)
        {
            return await query.AsDocumentQuery().ToListAsync();
        }

        public static async Task<Dictionary<TKey, TSource>> ToDictionary<TSource, TKey>(this IDocumentQuery<TSource> queryable, Func<TSource, TKey> keySelector)
        {
            var dictionary = new Dictionary<TKey, TSource>();
            while (queryable.HasMoreResults)
            {   //Note that ExecuteNextAsync can return many records in each call
                var response = await queryable.ExecuteNextAsync<TSource>();
                foreach (var item in response)
                {
                    dictionary.Add(keySelector(item), item);
                }
            }
            return dictionary;
        }
    }
}