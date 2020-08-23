#!/bin/bash

curl --location --request GET 'localhost:9200/companies/_search' \
--header 'Content-Type: application/json' \
--data-raw '{
    "size": 0,
    "query": {
        "match_all": {
            "boost": 1.0
        }
    },
    "aggregations": {
        "group_by_sector": {
            "terms": {
                "field": "Sector.keyword",
                "size": 15,
                "min_doc_count": 1
            },
            "aggregations": {
                "total_EBITDA": {
                    "sum": {
                        "field": "EBITDA"
                    }
                },
                "PEGreater": {
                    "filter": {
                        "range": {
                            "Price/Earnings": {
                                "from": 20,
                                "to": null,
                                "include_lower": false,
                                "include_upper": true,
                                "boost": 1.0
                            }
                        }
                    },
                    "aggregations": {
                        "total_EBITDA": {
                            "sum": {
                                "field": "EBITDA"
                            }
                        }
                    }
                },
                "EBITDA_percentage": {
                    "bucket_script": {
                        "buckets_path": {
                            "filteredEBITDA": "PEGreater>total_EBITDA",
                            "totalEBITDA": "total_EBITDA"
                        },
                        "script": {
                            "source": "(params.filteredEBITDA / params.totalEBITDA) * 100",
                            "lang": "painless"
                        },
                        "gap_policy": "skip"
                    }
                }
            }
        }
    }
}'
