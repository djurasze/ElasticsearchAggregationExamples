#!/bin/bash

curl --location --request GET 'localhost:9200/companies/_search' \
--header 'Content-Type: application/json' \
--data-raw '{
    "query": {
        "match_all": {}
    },
    "size":0,
    "aggs" : {
        "total_market_capacity" : { "sum" : { "field" : "Market Cap" } }
    }
}'
