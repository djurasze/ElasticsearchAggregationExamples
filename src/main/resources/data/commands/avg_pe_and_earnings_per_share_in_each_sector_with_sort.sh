#!/bin/bash

curl --location --request GET 'localhost:9200/companies/_search' \
--header 'Content-Type: application/json' \
--data-raw '{
    "query": {
        "match_all": {}
    },
    "size":0,
    "aggs": {
    "group_by_sector": {
      "terms": {
        "field": "Sector.keyword",
        "size": 15
      },
         "aggs" : {
        "avg_pe" : { "avg" : { "field" : "Price/Earnings" } },
        "avg_earnings_per_share" : { "avg" : { "field" : "Earnings/Share" } },
        "bucket_sort_by_pe": {
                    "bucket_sort": {
                        "sort": [
                          {"avg_pe": {"order": "desc"}}
                        ],
                        "size": 15
                    }
                }

    	}
    }
    }
}'
