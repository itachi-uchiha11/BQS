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
import org.elasticsearch.index.query.*;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.transformations.simplification.DefaultRatingFunction;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        BoolQueryBuilder a = new BoolQueryBuilder();

//        a.should(new BoolQueryBuilder().must(new MatchQueryBuilder("countries","India")).must(new TermQueryBuilder("years",2000)));
//        a.should(new BoolQueryBuilder().must(new MatchQueryBuilder("countries","India")).must(new TermQueryBuilder("years",2001)));
        a.should(QueryBuilders.prefixQuery("countries","USA"));
        a.should(QueryBuilders.prefixQuery("countries","USA1"));
        System.out.println(a);
        QueryBuilder o = BoolQuerySimplifier.optimizeBoolQueryBuilder(a);
        System.out.println(o);

        String x="";

        FileWriter fw = new FileWriter("9");
        fw.write(x);
        fw.close();



//        BoolQueryBuilder b = new BoolQueryBuilder();
//        for(int i=1900;i<=2020;i++){
//            BoolQueryBuilder b1 = new BoolQueryBuilder();
//            if(i%4==0){
//                b1.must(new MatchQueryBuilder("actors","Elle Fanning"));
//            }
//            else if(i%4==1){
//                b1.must(new MatchQueryBuilder("actors","Elle Fanning1"));
//            }
//            else if(i%4==2){
//                b1.must(new MatchQueryBuilder("actors","Elle Fanning2"));
//            }
//            else{
//                b1.must(new MatchQueryBuilder("countries","USA"));
//            }
//            b1.must(new TermQueryBuilder("year",i));
//            b.should(b1);
//        }

//        a.must(new PrefixQueryBuilder("imdb_url","https://www.imdb.com/title/tt"));
//        a.must(new PrefixQueryBuilder("img_url","https://m.media-amazon.com/images/"));

//        a=b;
//        System.out.println(a.toString());
//        QueryBuilder opt_a = BoolQuerySimplifier.optimizeBoolQueryBuilder(a);
//        System.out.println(opt_a.toString());
//        FileWriter fw = new FileWriter("test11");
//        fw.write(a.toString());
//        fw.close();
//        FileWriter fw2 = new FileWriter("test22");
//        fw2.write(opt_a.toString());
//        fw2.close();
//
//        BoolQueryBuilder a1 = new BoolQueryBuilder();
//        a1.mustNot(QueryBuilders.prefixQuery("a","substring"));
//        a1.mustNot(QueryBuilders.prefixQuery("a","subway"));
//        a1.mustNot(QueryBuilders.prefixQuery("a","subss"));
//        a1.mustNot(QueryBuilders.prefixQuery("b","substrig"));
//        a1.mustNot(QueryBuilders.prefixQuery("b","substri"));
//        QueryBuilder a1_opt = BoolQuerySimplifier.optimizeBoolQueryBuilder(a1);
//        System.out.println(a1);
//        System.out.println(a1_opt);

//        String string = "localhost";
//        RestClient builder = RestClient.builder(string).setMaxRetryTimeout(TimeValue.timeValueMillis(60000))
//                .setSocketTimeout(TimeValue.timeValueMillis(60000))
//                .setConnectTimeout(TimeValue.timeValueMillis(60000))
//                .setConnectionRequestTimeout(TimeValue.timeValueMillis(60000))
//                .setMaxResponseSize(new ByteSizeValue(1, ByteSizeUnit.GB))
//                .setMaxConnectionsPerRoute(20000).setMaxConnectionsTotal(30000)
//                .connectionLiveTimeInPool(TimeValue.timeValueMillis(60000)).build();
//
//        String clusterName = builder.getClusterName();
//        IndicesAdminClient indices = builder.admin().indices();
//        GetIndexRequestBuilder getIndexRequestBuilder = indices.prepareGetIndex().addIndices("*");
//        ActionFuture<GetIndexResponse> index = indices.getIndex(getIndexRequestBuilder.request());
//        String[] indices1 = index.get().indices();
//        for(String x :indices1){System.out.println(x);}
//        System.out.println(clusterName);
//        System.out.println(builder.prepareSearch("mynetflix").execute().actionGet());
//
//        builder.close();
    }
}
