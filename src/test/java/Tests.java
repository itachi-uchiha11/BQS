import org.elasticsearch.index.query.*;
import org.junit.jupiter.api.*;
import org.elasticsearch.index.query.QueryBuilder;

public class Tests {
    @Test
    void Test1() {
        BoolQueryBuilder unopt = new BoolQueryBuilder();
        MatchQueryBuilder temp2 = new MatchQueryBuilder("a","India");
        MatchQueryBuilder temp3 = new MatchQueryBuilder("b",1);
        MatchQueryBuilder temp4 = new MatchQueryBuilder("b",2);
        MatchQueryBuilder temp5 = new MatchQueryBuilder("b",3);
        unopt.should(new BoolQueryBuilder().must(temp2).must(temp3));
        unopt.should(new BoolQueryBuilder().must(temp2).must(temp4));
        unopt.should(new BoolQueryBuilder().must(temp2).must(temp5));
        BoolQueryBuilder target = new BoolQueryBuilder();
        target.must(temp2);
        BoolQueryBuilder temp = new BoolQueryBuilder();
        temp.should(temp3);
        temp.should(temp4);
        temp.should(temp5);
        target.must(temp);
        QueryBuilder opt = BoolQuerySimplifier.optimizeBoolQueryBuilder(unopt);
        String x1 = unopt.toString();
        String x2 = opt.toString();
        String x3 = target.toString();
        System.out.println(x1+x3);
        Assertions.assertEquals(x2,x3);
    }

    @Test
    void Test2() {
        BoolQueryBuilder unopt = new BoolQueryBuilder();
        MatchAllQueryBuilder target = new MatchAllQueryBuilder();
        unopt.must(new MatchAllQueryBuilder());
        QueryBuilder opt = BoolQuerySimplifier.optimizeBoolQueryBuilder(unopt);
        String x1 = unopt.toString();
        String x2 = opt.toString();
        String x3 = target.toString();
        System.out.println(x1+x3);
        Assertions.assertEquals(x2,x3);
    }

    @Test
    void Test3(){
        BoolQueryBuilder unopt = new BoolQueryBuilder();
        MatchQueryBuilder temp2 = new MatchQueryBuilder("a","India");
        MatchQueryBuilder temp3 = new MatchQueryBuilder("b",1);
        MatchQueryBuilder temp4 = new MatchQueryBuilder("b",2);
        MatchQueryBuilder temp5 = new MatchQueryBuilder("b",3);
        unopt.should(new MatchQueryBuilder("a","China"));
        unopt.should(new BoolQueryBuilder().must(temp2).must(temp3));
        unopt.should(new BoolQueryBuilder().must(temp2).must(temp4));
        unopt.should(new BoolQueryBuilder().must(temp2).must(temp5));
        BoolQueryBuilder target = new BoolQueryBuilder();
        BoolQueryBuilder target2 = new BoolQueryBuilder();
        target2.must(temp2);
        BoolQueryBuilder temp = new BoolQueryBuilder();
        temp.should(temp3);
        temp.should(temp4);
        temp.should(temp5);
        target2.must(temp);
        target.should(new MatchQueryBuilder("a","China"));
        target.should(target2);
        QueryBuilder opt = BoolQuerySimplifier.optimizeBoolQueryBuilder(unopt);
        String x1 = unopt.toString();
        String x2 = opt.toString();
        String x3 = target.toString();
        System.out.println(x1+x3);
        Assertions.assertEquals(x2,x3);
    }
    @Test
    void Test4(){
        BoolQueryBuilder unopt = new BoolQueryBuilder();
        MatchQueryBuilder temp2 = new MatchQueryBuilder("a","India");
        MatchQueryBuilder temp3 = new MatchQueryBuilder("b",1);
        MatchQueryBuilder temp4 = new MatchQueryBuilder("b",2);
        MatchQueryBuilder temp5 = new MatchQueryBuilder("b",3);
        unopt.should(new BoolQueryBuilder().mustNot(temp2).mustNot(temp3));
        unopt.should(new BoolQueryBuilder().mustNot(temp2).mustNot(temp4));
        unopt.should(new BoolQueryBuilder().mustNot(temp2).mustNot(temp5));
        BoolQueryBuilder target = new BoolQueryBuilder();
        BoolQueryBuilder target2 = new BoolQueryBuilder();
        BoolQueryBuilder temp = new BoolQueryBuilder();
        temp.must(temp3);
        temp.must(temp4);
        temp.must(temp5);
        target2.should(temp2);
        target2.should(temp);
        target.mustNot(target2);
        QueryBuilder opt = BoolQuerySimplifier.optimizeBoolQueryBuilder(unopt);
        String x1 = unopt.toString();
        String x2 = opt.toString();
        String x3 = target.toString();
        System.out.println(x1+x3);
        Assertions.assertEquals(x2,x3);
    }
    @Test
    void Test5(){
        BoolQueryBuilder unopt = new BoolQueryBuilder().must(new BoolQueryBuilder().must(new MatchQueryBuilder("a","India")).mustNot(new MatchQueryBuilder("a","India")));
        QueryBuilder target = new BoolQueryBuilder().mustNot(new MatchAllQueryBuilder());
        QueryBuilder opt = BoolQuerySimplifier.optimizeBoolQueryBuilder(unopt);
        String x1 = unopt.toString();
        String x2 = opt.toString();
        String x3 = target.toString();
        System.out.println(x1+x3);
        Assertions.assertEquals(x2,x3);
    }

}
