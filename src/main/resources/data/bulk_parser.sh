#!/bin/bash

echo 'preparing data for bulk insert...'
awk '!previous_empty && // {print "{\"index\":{}}"}
     {previous_empty = $0 == ""; print}' $1 > ready_data.txt

echo 'success'
