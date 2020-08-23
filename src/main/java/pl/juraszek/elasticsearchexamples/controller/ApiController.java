package pl.juraszek.elasticsearchexamples.controller;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.avg.AvgAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.avg.ParsedAvg;
import org.elasticsearch.search.aggregations.metrics.sum.ParsedSum;
import org.elasticsearch.search.aggregations.metrics.sum.SumAggregationBuilder;
import org.elasticsearch.search.aggregations.pipeline.ParsedSimpleValue;
import org.elasticsearch.search.aggregations.pipeline.PipelineAggregatorBuilders;
import org.elasticsearch.search.aggregations.pipeline.bucketscript.BucketScriptPipelineAggregationBuilder;
import org.elasticsearch.search.aggregations.pipeline.bucketsort.BucketSortPipelineAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.util.Pair;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("aggregations")
public class ApiController {

    private final RestHighLevelClient client;

    public ApiController(RestHighLevelClient client) {
        this.client = client;
    }

    @GetMapping("/totalMarketCapacity")
    public Map<String, Double> getTotalMarketCapacity() throws IOException {

        // building the search (query  + aggregations)
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // setting the query which is used for the aggregation
        searchSourceBuilder.query(QueryBuilders.matchAllQuery()).size(0);

        // build the aggregation
        SumAggregationBuilder sumAggregation = AggregationBuilders.sum("total_market_capacity").field("Market Cap");
        searchSourceBuilder.aggregation(sumAggregation);

        // create the request
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(searchSourceBuilder);

        // get aggregation:
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

        // get sum aggregation:
        Aggregations aggregation = response.getAggregations();

        double totalMarketCapacity = ((ParsedSum) aggregation.get("total_market_capacity")).getValue();

        return Collections.singletonMap("total_market_capacity", totalMarketCapacity);
    }

    @GetMapping("/companiesCountBySector")
    public Map<String, Map<Object, Long>> getCompaniesCountBySector() throws IOException {

        // building the search (query  + aggregations)
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // setting the query which is used for the aggregation
        searchSourceBuilder.query(QueryBuilders.matchAllQuery()).size(0);

        // build the aggregation
        TermsAggregationBuilder groupBySector = AggregationBuilders.terms("group_by_sector").field("Sector.keyword")
                .size(15);
        searchSourceBuilder.aggregation(groupBySector);

        // create the request
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(searchSourceBuilder);

        // get aggregation:
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

        // get sum aggregation:
        Aggregations aggregation = response.getAggregations();

        Map<Object, Long> countsPerSector = ((ParsedStringTerms) aggregation.get("group_by_sector")).getBuckets().stream()
                .collect(Collectors.toMap(Terms.Bucket::getKey, Terms.Bucket::getDocCount));

        return Collections.singletonMap("counts_by_sector", countsPerSector);
    }

    @GetMapping("/avgPEAndEarningsPerShareBySectorWithOrder")
    public Map<String, Object> getAvgPEAndEarningsPerShareBySectorWithOrder() throws IOException {

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


        // create the request
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(searchSourceBuilder);

        // get aggregation:
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

        // get sum aggregation:
        Aggregations aggregation = response.getAggregations();

        List<Pair<Object, Map<String, Double>>> avgResults = ((ParsedStringTerms) aggregation.get("group_by_sector")).getBuckets().stream()
                .map(bucket -> Pair.of(bucket.getKey(), Map
                        .of("avg_pe", ((ParsedAvg) bucket.getAggregations().get("avg_pe")).getValue(),
                                "avg_earnings_per_share",
                                ((ParsedAvg) bucket.getAggregations().get("avg_earnings_per_share")).getValue()))).collect(Collectors.toList());

        return Collections.singletonMap("averages_by_sector", avgResults);
    }

    @GetMapping("/avgPEAndEarningsPerShareBySectorWithSort")
    public Map<String, Object> getAvgPEAndEarningsPerShareBySectorWithSort() throws IOException {

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

        // create the request
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(searchSourceBuilder);

        // get aggregation:
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

        // get sum aggregation:
        Aggregations aggregation = response.getAggregations();

        List<Pair<Object, Map<String, Double>>> avgResults = ((ParsedStringTerms) aggregation.get("group_by_sector")).getBuckets().stream()
                .map(bucket -> Pair.of(bucket.getKey(), Map
                        .of("avg_pe", ((ParsedAvg) bucket.getAggregations().get("avg_pe")).getValue(),
                                "avg_earnings_per_share",
                                ((ParsedAvg) bucket.getAggregations().get("avg_earnings_per_share")).getValue()))).collect(Collectors.toList());

        return Collections.singletonMap("averages_by_sector", avgResults);
    }

    @GetMapping("/ratioPercentageOfMarketCapWhenPEGreaterThan20BySector")
    public Map<String, Object> getRatioPercentageOfMarketCapWhenPEGreaterThan20BySector() throws IOException {

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

        // create the request
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(searchSourceBuilder);

        // get aggregation:
        SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

        // get sum aggregation:
        Aggregations aggregation = response.getAggregations();

        List<Pair<Object, Map<String, String>>> avgResults = ((ParsedStringTerms) aggregation.get("group_by_sector")).getBuckets().stream()
                .map(bucket -> Pair.of(bucket.getKey(), Map
                        .of("EBITDA_ratio_percentage", ((ParsedSimpleValue) bucket.getAggregations().get("EBITDA_percentage")).getValueAsString())))
                .collect(Collectors.toList());

        return Collections.singletonMap("EBITDA_ratio_percentage_by_sector", avgResults);
    }
}
