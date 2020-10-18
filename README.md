<h1>
  Introduction
</h1>

<p>
In this short article, I would like to write about aggregations in Elasticsearch.
  In particular, I want to focus on different types of aggregations that are available in Elasticsearch. 
  Next, I will show a few examples of usage in Java.
  Elasticsearch has pretty neat query language for expressing different types of aggregations, and what is also significant, such queries can be efficient for analyzing big data sets.
  This search engine is sufficient for many analytical cases and sometimes easier to configure and manage in comparison to other technologies. 
  An additional advantage of Elasticsearch is the possibility of using Kibana. This UI is a powerful tool for creating all kinds of visualizations based on the data in our indexes.
</p>

<p>
  All code used in this text can be found on my <a href="https://github.com/djurasze/elasticsearch-examples">GitHub</a>.
</p>
<p>
  Additionally, <a href="https://github.com/djurasze/elasticsearch-examples/blob/master/src/main/resources/data/elastic.postman_collection.json">here</a> you can find Postman project with all queries which we will be using.    Just import it to the client, and you are ready to go.
</p>

<h2>
Loading testing data
</h2>

<p>
  For this article, we will need an index with sample data to analyze.
  I have prepared a simple index with a decent number of data that we can use to test our aggregations.
</p>

<p>
  Data set is available <a href="https://pkgstore.datahub.io/core/s-and-p-500-companies-financials/constituents-financials_json/data/ddf1c04b0ad45e44f976c1f32774ed9a/constituents-financials_json.json">here</a>.
  To load this into the Elasticsearch index, we must reformat this a little.
I have prepared simple script which can do it <a href="https://github.com/djurasze/elasticsearch-examples/blob/master/src/main/resources/data/bulk_parser.sh">here</a>. Just save it on your disk and run this command:
  </p>

<pre class="listing">
/.bulk_parser.sh data_to_parse.txt
</pre>

<p>
  Or you can just download prepared data from <a href="https://github.com/djurasze/elasticsearch-examples/blob/master/src/main/resources/data/data.txt">here</a>.
</p>
  
<p>
  Next, we will load this data to Elasticsearch using bulk insert:
</p>

<pre class="listing">
curl --location --request POST 'localhost:9200/companies/_bulk' \
--header 'Content-Type: application/x-ndjson' \
--data-binary @ready_data.txt
</pre>

<p>
Like previously you can find necessary script <a href="https://github.com/djurasze/elasticsearch-examples/blob/master/src/main/resources/data/insert.sh">here</a>.
</p>

<h2>
Short data description
</h2>

<p>
  As you can see, we will be analyzing the basic financial data of companies in the S&P500 index.
</p>

<p>
We have 505 records, where each record contains, among others given fields:
</p>

<ol>
  <li> Name – name of the company</li>
  <li> Sector – sector to which a given company belongs</li>
  <li> Market Cap – total dollar market value of a company's shares </li>
  <li> Eearnings/share – company's profit divided by the number of shares </li>
  <li> Price/Earnings – ratio of a company's share price to the company's earnings per share. </li>
  <li> Dividend Yield – the amount of money a company pays shareholders (over the course of a year) for owning a share of its stock divided by its current stock price </li>
  <li> EBITDA – earnings before interest, taxes, depreciation, and amortization, is a measure of a company's overall financial performance  </li>
</ol>

<h1>
  Aggregation types
</h1>

<p>
  When we already have data to analyze, let's start with a quick introduction to the available types of aggregation in Elasticsearch. 
  It is crucial to understand when we can use each of them and what are the differences between them. 
  When we work with big data sets, it is significant to know which operations cannot be easily horizontally scaled, so as a result, they can dramatically decrease the performance of our queries.
  In this article, I will show a few examples of aggregations from three basic families.  
</p>

<h2> Bucket aggregation </h2>

<p>
 This type is responsible for building groups (buckets) of documents in which every document matches the given criteria. 
  To some extent it can be compared to <i>group by</i> clause in SQL. So basically, using this aggregation, we can split our documents to separate buckets based on the given criteria. 
  Elasticsearch gives us many options for specifying these criteria, starting with matching based on a single term (e.g. group by sector name) and ending with handy date histogram aggregation, which quickly divides our data based on given intervals.
</p>

<p>
 But we have to be cautious. If we generate too many buckets, then the reduction phase can be overwhelming for the coordinating node, which is responsible for reducing buckets from worker nodes.
 That cannot be horizontally scaled and may be a bottleneck for our query. 
 In case this is acceptable in our scenario, we can limit the number of buckets per shard. Then, only the top buckets will be sent to the coordinating node. 
 Doing this, we can unload the coordinating server, but our results could not be exact anymore. If this is not possible, partitioning could be the solution.
</p>

<p> Image below illustrates the problem with too many buckets: </p>

<div class="separator" style="clear: both;"><a href="https://1.bp.blogspot.com/-aE_rWOLyah0/X0q5xGZdjwI/AAAAAAAADfk/1VnFs9_D_A4kbJdIXF4u6O43Ah46szBVQCPcBGAYYCw/s0/query-for-terms-agg.png" style="display: block; padding: 1em 0; text-align: center;"><img alt="" border="0" data-original-height="350" data-original-width="941" src="https://1.bp.blogspot.com/-aE_rWOLyah0/X0q5xGZdjwI/AAAAAAAADfk/1VnFs9_D_A4kbJdIXF4u6O43Ah46szBVQCPcBGAYYCw/s0/query-for-terms-agg.png"/></a>
<small>Source: <a href="https://qbox.io/blog/how-to-download-all-unique-terms-field-elasticsearch"> https://qbox.io/</a></small>
</div>

<h2> Metric aggregation </h2>

<p>
 These aggregations are responsible for gathering/calculating metric values in our buckets (they can also be used without buckets, then we are calculating metric aggregation for entire data).
 Like in bucket family Elasticsearch gives us here many predefined options to use. For example, we can calculate the max/min/avg value of a given attribute per bucket. 
 If we need more advanced calculations, we can write our custom script aggregation to do this.
</p>

<h2> Pipeline aggregation</h2>

<p>
 These aggregations are responsible for gathering/calculating metric values in our buckets (they can also be used without buckets, then we will be calculating metric aggregation for entire data).
 Like in bucket family, Elasticsearch gives us here many predefined options to use. For example, we can calculate the max/min/avg value of a given attribute per bucket. 
 If we need more advanced calculations, we can write our custom script aggregation to do this.
</p>

<h1> Examples </h1>
  
  <p> 
    Ok, now, when we are after the short introduction, we can start writing our aggregations. <br/>
    All DSL search queries we can execute using the given command:
  </p>

<pre class="listing">
curl --location --request GET 'localhost:9200/companies/_search' \
--header 'Content-Type: application/json' \
--data-raw '[JSON_DSL_QUERY]'
</pre>


<h2> Single metric aggregation </h2>

<p>
  We will start with something simple. We want to calculate the total market capacity of the 500 largest companies in the US.
</p>

<p>
  Using Elasticserach query DSL, we can achieve this with:
</p>

<pre class="listing">
{
    "query": {
        "match_all": {}
    },
    "size":0,
    "aggs" : {
        "total_market_capacity" : { "sum" : { "field" : "Market Cap" } }
    }
}
</pre>

<p>
That is a JSON based language provided by Elasticsearch. We can, of course, use this query directly in our application through  HTTP client or using some low-level library like 

  <a href="https://www.elastic.co/guide/en/elasticsearch/client/java-rest/master/java-rest-low.html">this</a>.
  But when we have, for example, a Java application and we want something more high level with better fault tolerance and clarity, we can use Java high-level REST-client which is available 
  <a href="https://www.elastic.co/guide/en/elasticsearch/client/java-rest/7.0/java-rest-high.html">here</a>. Of course, there are plenty more clients available to use in other languages.
  Additionally, the high-level client can be easily integrated with our application through a spring data module.
 
</p>

<p>
   So instead of using a hardcoded JSON query, we can write our aggregation like this:
</p>

<pre class="listing">
// building the search (query  + aggregations)
SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

// setting the query which is used for the aggregation
searchSourceBuilder.query(QueryBuilders.matchAllQuery()).size(0);

// build the aggregation
SumAggregationBuilder sumAggregation = AggregationBuilders.sum("total_market_capacity").field("Market Cap");
searchSourceBuilder.aggregation(sumAggregation);
</pre>

<p>
Size 0 is added because, without it, Elasticserach additionally returns single records that were found for the given match_all query. 
  In our case, only the aggregation result is relevant.

</p>

<p>
  If we are using Spring Boot framework, we can turn on additional DSL query logging adding this config to <i>application.properties</i>:
</p>

<pre class="listing">
logging.level.tracer=TRACE
</pre>

<p>
  That will log not only our request but also response:
</p>

<pre class="listing">
2020-08-29 23:42:00.107 TRACE 10508 --- [/O dispatcher 1] tracer: curl -iX POST 'http://localhost:9200/_search?rest_total_hits_as_int=true&typed_keys=true&ignore_unavailable=false&expand_wildcards=open&allow_no_indices=true&ignore_throttled=true&search_type=query_then_fetch&batched_reduce_size=512' -d '{"size":0,"query":{"match_all":{"boost":1.0}},"aggregations":{"total_market_capacity":{"sum":{"field":"Market Cap"}}}}'
# HTTP/1.1 200 OK
# content-type: application/json; charset=UTF-8
# content-length: 211
#
# {"took":29,"timed_out":false,"_shards":{"total":9,"successful":9,"skipped":0,"failed":0},"hits":{"total":531,"max_score":null,"hits":[]},"aggregations":{"sum#total_market_capacity":{"value":2.4865915532544E13}}}

</pre>

<p>
  We can see that consequently, our Java code will fire practically the same query. 
</p>

<p>
  And how can we visualize this aggregation in Kibana? 
  In order to achieve that we can create a horizontal bar visualization and select from a metrics tab <i>sum aggregation</i> on field <i>Market Cap.</i> For this simple case this is all what we need to do.
</p>


<div class="separator" style="clear: both;"><a href="https://1.bp.blogspot.com/-sNkStiqsddY/X0rN6N2_LVI/AAAAAAAADf0/9rDcYck5aR8EXxIr9Us4VT4Sk2rjy9PgQCNcBGAsYHQ/s0/total_market_capacity.PNG" style="display: block; padding: 1em 0; text-align: center;"><img alt="" border="0" data-original-height="696" data-original-width="1855" src="https://1.bp.blogspot.com/-sNkStiqsddY/X0rN6N2_LVI/AAAAAAAADf0/9rDcYck5aR8EXxIr9Us4VT4Sk2rjy9PgQCNcBGAsYHQ/s0/total_market_capacity.PNG"/></a></div>

<h2> Bucket aggregation with count  </h2>

<p>
	As a next task, let's find out how many companies are in each sector.
 
</p>

<pre class="listing">
{
    "query": {
        "match_all": {}
    },
    "size":0,
    "aggs": {
    "group_by_sector": {
      "terms": {
        "field": "Sector.keyword",
        "size": 15,
        "shard_size": 15,
        "show_term_doc_count_error": true
      }
    }
    }
}
</pre>


<p>
  
  This query is shorter because we want only the <i>count</i> metric, which is a default metric returned during bucket aggregation (Count metric aggregation is implicit). 
  
</p>
<p>
  To get all sectors in a single query, we have to increase the <i>size</i> of returned buckets per each shard. This parameter is also important in terms of accuracy because if the number of unique buckets calculated in the shard is greater than this parameter, the returned data can be slightly not accurate. But a value too big will cost us also performance. 
  The higher the requested size is, the more expensive it will be to compute the final results (both due to more calculations on each shard and due to larger data transfers between the nodes and the client). 
  If we want to get higher precision but not necessarily increase the number of buckets in our result to the client, we can use the <i>shard_size</i> parameter. 
  It specifies how many buckets will be gathered from each shard and also specify that the coordinator node will return only the <i>size</i> amount of buckets to the client. By default, we will get only ten sectors with the highest number of companies. 
    
</p>
<p>
  Additionally, it can be useful to use attribute <i>"show_term_doc_count_error": true</i> to measure the error chance of our aggregation. More about it <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket-terms-aggregation.html#_per_bucket_document_count_error ">here</a>.
 
  </p>

<p>
   Similarly, in Java:
</p>

<pre class="listing">
// building the search (query  + aggregations)
SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

// setting the query which is used for the aggregation
searchSourceBuilder.query(QueryBuilders.matchAllQuery()).size(0);

// build the aggregation
TermsAggregationBuilder groupBySector = AggregationBuilders.terms("group_by_sector").field("Sector.keyword")
        .size(15);
searchSourceBuilder.aggregation(groupBySector);
</pre>

<p>
  To visualize this aggregation, we have to select from the bucket tab term aggregation and next select Sector.keyword field. Similarly, as previously, we have to increase the buckets amount.
</p>

<div class="separator" style="clear: both;"><a href="https://1.bp.blogspot.com/-ruak2V4sMHE/X4ykKXgK_dI/AAAAAAAADhc/A_eDjYF8jVQluBJAekqyflzq7mSp7PP2QCPcBGAYYCw/s0/companies_count_in_each_sector.PNG" style="display: block; padding: 1em 0; text-align: center; "><img alt="" border="0" data-original-height="654" data-original-width="1830" src="https://1.bp.blogspot.com/-ruak2V4sMHE/X4ykKXgK_dI/AAAAAAAADhc/A_eDjYF8jVQluBJAekqyflzq7mSp7PP2QCPcBGAYYCw/s0/companies_count_in_each_sector.PNG"/></a></div>

<h2> Bucket and metric aggregations  </h2>

<p>
In the previous example, we did not use metric aggregation explicitly. If we want to calculate some other metrics different than count, we have to specify that. 
</p>
<p>
Let's see which sectors are valued the most concerning their earnings.
To do that, we will calculate the average p/e ratio and earnings/share in every sector. So in this example, we will use one bucket aggregation, and for these buckets, at the same level, two metric aggregations.
</p>

<pre class="listing">
{
    "query": {
        "match_all": {}
    },
    "size": 0,
    "aggs": {
        "group_by_sector": {
            "terms": {
                "field": "Sector.keyword",
                "size": 15
            },
            "aggs": {
                "avg_pe": {
                    "avg": {
                        "field": "Price/Earnings"
                    }
                },
                "avg_earnings_per_share": {
                    "avg": {
                        "field": "Earnings/Share"
                    }
                }
            }
        }
    }
}
</pre>

<p>
  As you can see, our results are sorted by default using the number of elements in each bucket. If we want to sort by our averages, we must specify sorting. We can achieve that by adding this pipeline to our query:

</p>

<pre class="listing">
{
    ...
    "aggs": {
        "group_by_sector": {
            ...
            "aggs": {
                ...
                "bucket_sort_by_pe": {
                    "bucket_sort": {
                        "sort": [
                            {
                                "avg_pe": {
                                    "order": "desc"
                                }
                            }
                        ],
                        "size": 15
                    }
                }
            }
        }
    }
}
</pre>

<pre class="listing">
// building the search (query  + aggregations)
SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

// setting the query which is used for the aggregation
searchSourceBuilder.query(QueryBuilders.matchAllQuery()).size(0);

// create sort pipeline aggregation
List<FieldSortBuilder> sorts = new ArrayList<>();
String sortField = "avg_pe";
SortOrder sortOrder = SortOrder.DESC;
FieldSortBuilder fieldSortBuilder = new FieldSortBuilder(sortField);
fieldSortBuilder.order(sortOrder);
sorts.add(fieldSortBuilder);
BucketSortPipelineAggregationBuilder sortAggregation = new BucketSortPipelineAggregationBuilder("bucket_sort_by_pe", sorts);


// build the aggregation
TermsAggregationBuilder groupBySector = AggregationBuilders.terms("group_by_sector").field("Sector.keyword")
        .size(15);
AvgAggregationBuilder averagePRRatio = AggregationBuilders.avg("avg_pe").field("Price/Earnings");
AvgAggregationBuilder averageEarningsPerShare = AggregationBuilders.avg("avg_earnings_per_share")
        .field("Earnings/Share");
searchSourceBuilder
        .aggregation(groupBySector.subAggregation(averagePRRatio).subAggregation(averageEarningsPerShare).subAggregation(sortAggregation));


</pre>

<p>
  You may ask why we could not use a simple order parameter like this:

</p>

<pre class="listing">
{
    ...
    "aggs": {
        "group_by_sector": {
            "terms": {
                "field": "Sector.keyword",
                "size": 15,
                "order": [
                    {
                        "avg_pe": "desc"
                    },
                    {
                        "_key": "asc"
                    }
                ]
            },
            ...
        }
    }
}
</pre>

<pre class="listing">
// building the search (query  + aggregations)
SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

// setting the query which is used for the aggregation
searchSourceBuilder.query(QueryBuilders.matchAllQuery()).size(0);

// build the aggregation
TermsAggregationBuilder groupBySector = AggregationBuilders.terms("group_by_sector").field("Sector.keyword")
        .size(15);
AvgAggregationBuilder averagePRRatio = AggregationBuilders.avg("avg_pe").field("Price/Earnings");
AvgAggregationBuilder averageEarningsPerShare = AggregationBuilders.avg("avg_earnings_per_share")
        .field("Earnings/Share");
searchSourceBuilder
        .aggregation(groupBySector.subAggregation(averagePRRatio).subAggregation(averageEarningsPerShare).order(
                BucketOrder.aggregation("avg_pe", false)));


</pre>

<p>
  Usage of pipeline aggregation is necessary because our average results are known only when all partial averages from shards are already reduced together. The order can be used when we query a single shard, sort by max/min metric or we do not need a precise result. More information can be found <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket-terms-aggregation.html#search-aggregations-bucket-terms-aggregation-order">here</a>. 
  

  </p>

<p>
  And again visualization:
</p>

<div class="separator" style="clear: both;"><a href="https://1.bp.blogspot.com/-F8-WQWjlZwg/X4ykKYU9OOI/AAAAAAAADho/z9nQ6onqCtcrB90wxGR0FPG30wFQrL_FQCPcBGAYYCw/s0/avg_pe_ratio_and_es_in_each_sector.PNG" style="display: block; padding: 1em 0; text-align: center; "><img alt="" border="0" data-original-height="658" data-original-width="1835" src="https://1.bp.blogspot.com/-F8-WQWjlZwg/X4ykKYU9OOI/AAAAAAAADho/z9nQ6onqCtcrB90wxGR0FPG30wFQrL_FQCPcBGAYYCw/s0/avg_pe_ratio_and_es_in_each_sector.PNG"/></a></div>


<h2> Pipeline aggregation  </h2>

<p>
The last example will show us the usage of script pipeline aggregation. We will try to calculate the EBITDA ratio percentage in each sector for companies with PE greater than 20. First, we will divide data into buckets per sector. For each bucket, we will calculate the sum of EBIDTA. Next, in each sector, we will filter companies with PE greater than 20. For these filtered companies, we will also calculate total EBIDTA. And now, our pipeline script aggregation comes into play. From given sums of EBITDA, we will calculate the percentage ratio in each sector using a simple formula: 

</p>

<pre class="listing">
SUM(EBIDTA Of companies with PE > 20) / SUM(EBIDTA OF all companies in sector) * 100
</pre>

<p>
And this is how it will look in DSL and Java
</p>

<pre class="listing">
{
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
}
</pre>


<pre class="listing">
// building the search (query  + aggregations)
SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

// setting the query which is used for the aggregation
searchSourceBuilder.query(QueryBuilders.matchAllQuery()).size(0);

// create script pipeline aggregation
Map<String, String> bucketsPathsMap = new HashMap<>();
bucketsPathsMap.put("totalEBITDA", "total_EBITDA");
bucketsPathsMap.put("filteredEBITDA", "PEGreater>total_EBITDA");
Script script = new Script("(params.filteredEBITDA / params.totalEBITDA) * 100");

BucketScriptPipelineAggregationBuilder scriptAggregation = PipelineAggregatorBuilders
        .bucketScript("EBITDA_percentage", bucketsPathsMap, script);


// build the aggregation
TermsAggregationBuilder groupBySector = AggregationBuilders.terms("group_by_sector").field("Sector.keyword")
        .size(15);
SumAggregationBuilder sumEBITDA = AggregationBuilders.sum("total_EBITDA").field("EBITDA");

FilterAggregationBuilder filterPeGreaterThan20 = AggregationBuilders.filter("PEGreater", QueryBuilders.rangeQuery("Price/Earnings").gt(20));

searchSourceBuilder
        .aggregation(groupBySector.subAggregation(sumEBITDA).subAggregation(filterPeGreaterThan20.subAggregation(sumEBITDA)).subAggregation(scriptAggregation));

</pre>

<h1>Summary</h1>
<p>
In this short article, I wanted to show you a few examples of different aggregations available in Elasticsearch. I tried to introduce some useful concepts with examples in Java Spring and Kibana. Remember also that all samples are available on my <a href="https://github.com/djurasze/elasticsearch-examples">GitHub</a>. I hope that it was somehow useful to you.

</p>