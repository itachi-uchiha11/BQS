import org.apache.lucene.queryparser.xml.ParserException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;

import java.sql.Array;
import java.util.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Main {
    public static void main(String[] args) throws ParserException, IOException {
        BoolQueryBuilder unopt = new BoolQueryBuilder();
        BoolQueryBuilder temp = new BoolQueryBuilder();
        MatchQueryBuilder temp2 = new MatchQueryBuilder("a","India");
        MatchQueryBuilder temp3 = new MatchQueryBuilder("b",1);
        MatchQueryBuilder temp4 = new MatchQueryBuilder("b",2);
        MatchQueryBuilder temp5 = new MatchQueryBuilder("b",3);
        unopt.should(new BoolQueryBuilder().must(temp2).must(temp3));
        unopt.should(new BoolQueryBuilder().must(temp2).must(temp4));
        unopt.should(new BoolQueryBuilder().must(temp2).must(temp5));
        String x = unopt.toString();
        System.out.println(x);
        QueryBuilder opt = BQS.optimize_boolquerybuilder(unopt);
        String x2 = opt.toString();
        System.out.println(x2);
    }
}
