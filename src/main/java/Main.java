//import org.apache.http.HttpHost;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
//import org.apache.lucene.queryparser.xml.ParserException;
//import org.elasticsearch.client.RestClient;
//import org.elasticsearch.client.RestHighLevelClient;
//import org.elasticsearch.client.rest.RestClient;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.get.GetIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.rest.RestClient;
import org.elasticsearch.common.jackson.dataformat.yaml.snakeyaml.parser.ParserException;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.logicng.transformations.simplification.DefaultRatingFunction;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
//        BoolQueryBuilder unopt = new BoolQueryBuilder();
//        BoolQueryBuilder temp = new BoolQueryBuilder();
//        MatchQueryBuilder temp2 = new MatchQueryBuilder("a","India");
//        MatchQueryBuilder temp3 = new MatchQueryBuilder("b",1);
//        MatchQueryBuilder temp4 = new MatchQueryBuilder("b",2);
//        MatchQueryBuilder temp5 = new MatchQueryBuilder("b",3);
//        unopt.should(new BoolQueryBuilder().mustNot(temp2).mustNot(temp3));
//        unopt.should(new BoolQueryBuilder().mustNot(temp2).mustNot(temp4));
//        unopt.should(new BoolQueryBuilder().mustNot(temp2).mustNot(temp5));
//        String x = unopt.toString();
//        System.out.println(x);
//        QueryBuilder opt = BoolQuerySimplifier.optimizeBoolQueryBuilder(unopt);
//        String x2 = opt.toString();
//        System.out.println(x2);

        String string = "localhost";
        RestClient builder = RestClient.builder(string).setMaxRetryTimeout(TimeValue.timeValueMillis(60000))
                .setSocketTimeout(TimeValue.timeValueMillis(60000))
                .setConnectTimeout(TimeValue.timeValueMillis(60000))
                .setConnectionRequestTimeout(TimeValue.timeValueMillis(60000))
                .setMaxResponseSize(new ByteSizeValue(1, ByteSizeUnit.GB))
                .setMaxConnectionsPerRoute(20000).setMaxConnectionsTotal(30000)
                .connectionLiveTimeInPool(TimeValue.timeValueMillis(60000)).build();
//        String clusterName = builder.getClusterName();
//        IndicesAdminClient indices = builder.admin().indices();
//        GetIndexRequestBuilder getIndexRequestBuilder = indices.prepareGetIndex().addIndices("*");
//        ActionFuture<GetIndexResponse> index = indices.getIndex(getIndexRequestBuilder.request());
//        String[] indices1 = index.get().indices();
//        for(String x :indices1){System.out.println(x);}
//        System.out.println(clusterName);
        System.out.println(builder.prepareSearch("mynetflix").execute().actionGet());
//        System.out.println(searchResponse);
        builder.close();
    }
}
