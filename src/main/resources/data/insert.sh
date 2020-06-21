#!/bin/bash

curl --location --request POST 'localhost:9200/companies/_bulk' \
--header 'Content-Type: application/x-ndjson' \
--data-binary @data.txt
