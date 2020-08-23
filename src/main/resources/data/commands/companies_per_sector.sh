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
        "size": 1,
        "shard_size": 1,
        "show_term_doc_count_error": true
      }
    }
    }
}'
