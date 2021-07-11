import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class JsonConverter {

    static JSONObject jobj = new JSONObject();
    static JSONArray jarray = new JSONArray();
    static public JSONParser jsonParser = new JSONParser();

    private static QueryBuilder getSingleQueryBuilder(JSONObject jsonObject){
        Object[] s = jsonObject.keySet().toArray();
        String x = (String) s[0];
        JSONObject jsonObject1 = (JSONObject) jsonObject.get(x);
        if(x.equals("bool")){
            return getBoolQueryBuilder(jsonObject1);
        }
        Object[] s2=null;
        String x2="";
        Object x3=null;
        if(x.equals("match_all")){}
        else {
            s2 = jsonObject1.keySet().toArray();
            x2 = (String) s2[0];
            x3 = jsonObject1.get(x2);
        }
        switch(x){
            case "prefix":{
                return QueryBuilders.prefixQuery(x2, (String) x3);
            }
            case "term":{
                return QueryBuilders.termQuery(x2,x3);
            }
            case "terms":{
                return QueryBuilders.termsQuery(x2,(JSONArray) x3);
            }
            case "range":{
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(x2);
                for(Object y:((JSONObject)x3).keySet()){
                    switch((String) y){
                        case "from":{rangeQueryBuilder.from(((JSONObject)x3).get(y));break;}
                        case "to":{rangeQueryBuilder.to(((JSONObject)x3).get(y));break;}
                        case "include_lower":{rangeQueryBuilder.includeLower((Boolean) ((JSONObject)x3).get(y));break;}
                        case "include_upper":{rangeQueryBuilder.includeUpper((Boolean) ((JSONObject)x3).get(y));break;}
                    }
                }
                return rangeQueryBuilder;
            }
            case "exists":{
                return QueryBuilders.wildcardQuery((String)x3,"*");
            }
            case "spr_query_string":{
                QueryStringQueryBuilder queryStringQueryBuilder = QueryBuilders.queryString((String) jsonObject1.get("query"));
                for(Object z:(JSONArray)jsonObject1.get("fields")){
                    queryStringQueryBuilder.field((String)z);
                }
                queryStringQueryBuilder.useDisMax((Boolean) jsonObject1.get("use_dis_max"));
                return queryStringQueryBuilder;
            }
            case "match_all":{
                return QueryBuilders.matchAllQuery();
            }
        }
        throw new UnsupportedOperationException(x + " is not supported in QueryBuilders");
    }
    private static ArrayList<QueryBuilder> getQueryBuilders(Object obj){
        ArrayList<QueryBuilder> lst = new ArrayList<>();
        if(obj.getClass()==jobj.getClass()){
            lst.add(getSingleQueryBuilder((JSONObject) obj));
        }
        else if(obj.getClass()==jarray.getClass()){
            for(Object x:(JSONArray) obj){
                lst.add(getSingleQueryBuilder((JSONObject) x));
            }
        }
        else{
            System.out.println(obj+" : "+obj.getClass()+" is not supported");
            return null;
//            throw new UnsupportedOperationException(obj.getClass() + " is not supported");
        }
        return lst;
    }

    private static BoolQueryBuilder getBoolQueryBuilder(JSONObject jsonObject){
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        for(Object x:jsonObject.keySet()){
            ArrayList<QueryBuilder> lst = getQueryBuilders(jsonObject.get(x));
            switch((String) x){
                case "filter":;
                case "must":{
                    for(QueryBuilder y:lst){
                        boolQueryBuilder.must(y);
                    }
                    break;
                }
                case "must_not":{
                    for(QueryBuilder y:lst){
                        boolQueryBuilder.mustNot(y);
                    }
                    break;
                }
                case "should":{
                    for(QueryBuilder y:lst){
                        boolQueryBuilder.should(y);
                    }
                    break;
                }
                default:{
                    break;
                }
            }
        }
        return boolQueryBuilder;
    }
    private static JSONObject getJsonObject(QueryBuilder queryBuilder){
        String x = queryBuilder.toString();
        try {
            return (JSONObject) jsonParser.parse(x);
        } catch (ParseException e) {
            System.out.println("Incorrect JSON formation");
            e.printStackTrace();
            return null;
        }
    }
    private static JSONObject optimizedObj(JSONObject jsonObject){
        BoolQueryBuilder boolQueryBuilder = getBoolQueryBuilder(jsonObject);
//        System.out.println(boolQueryBuilder);
        System.out.println("optimized1");
        System.out.println(boolQueryBuilder);
        QueryBuilder queryBuilder = BoolQuerySimplifier.optimizeBoolQueryBuilder(boolQueryBuilder);
        System.out.println(queryBuilder);
        System.out.println("optimized2");
        return getJsonObject(queryBuilder);
    }
    private static JSONObject converter2(JSONObject obj){
        JSONObject jsonObject = new JSONObject();
        Set<String> set = obj.keySet();
        for(String x :set){
//            System.out.println(x);
            if(x.equals("bool")){
//                System.out.println("in bool");
                JSONObject jobj = new JSONObject();
                jobj.put(x, obj.get(x));
                System.out.println(jobj);
                analyser(jobj,"");
                JSONObject optimizedObj = optimizedObj((JSONObject) obj.get(x));
                jsonObject.put(x, optimizedObj.get(x));
                JSONObject jobj2 = new JSONObject();
                jobj2.put(x, optimizedObj.get(x));
                analyser(jobj2,"");
            }
            else{
                jsonObject.put(x,converter(obj.get(x)));
            }
        }
        return jsonObject;
    }
    private static JSONArray converter2(JSONArray obj){
        JSONArray jsonArray = new JSONArray();
        for(Object x:obj){
            if(x instanceof String){
                return obj;
            }
            else {
                jsonArray.add(converter2((JSONObject) x));
            }
        }
        return jsonArray;
    }
    private static Object converter(Object obj){
        if(obj.getClass()==jobj.getClass()){
            return converter2((JSONObject) obj);
        }
        else if(obj.getClass()==jarray.getClass()){
            return converter2((JSONArray) obj);
        }
        else{
            System.out.println(obj.getClass() + " is not supported");
            return obj;
        }
    }
    private static void print(int a,int b,int c,int d,int e,int f,int g,String o){
        if(a!=0){
            System.out.print(o+"prefix : ");
            System.out.println(a);
        }
        if(b!=0){
            System.out.print(o+"range : ");
            System.out.println(b);
        }
        if(c!=0){
            System.out.print(o+"term : ");
            System.out.println(c);
        }
        if(d!=0){
            System.out.print(o+"terms : ");
            System.out.println(d);
        }
        if(e!=0){
            System.out.print(o+"exists/wildcard : ");
            System.out.println(e);
        }
        if(f!=0){
            System.out.print(o+"spr_query_string/queryString : ");
            System.out.println(f);
        }
        if(g!=0){
            System.out.print(o+"match_all : ");
            System.out.println(g);
        }
    }
    private static void print(String a,String o){
        System.out.print(o);
        System.out.println(a+" : ");
    }
    private static void analyser2(Object o,String offset){
        if(o instanceof JSONObject){
            JSONObject jsonObject = (JSONObject) o;
            String x = (String)jsonObject.keySet().toArray()[0];
            if(x.equals("bool")){
                analyser(jsonObject,offset);
            }
            else{
                int p=0,r=0,t=0,ts=0,ex=0,sq=0,ma=0;
                switch(x){
                    case "prefix":{
                        p++;
                        break;
                    }
                    case "range":{
                        r++;
                        break;
                    }
                    case "term":{
//                        System.out.println("term : "+jsonObject.get(x));
                        t++;
                        break;
                    }
                    case "terms":{
//                        System.out.println("terms : "+jsonObject.get(x));
                        ts++;
                        break;
                    }
                    case "wildcard":{}
                    case "exists":{
                        ex++;
                        break;
                    }
                    case "spr_query_string":{}
                    case "query_string":{
                        sq++;
                        break;
                    }
                    case "match_all":{
                        ma++;
                        break;
                    }
                    default:{
                        System.out.println(x+" is not suppported like term clause");
                        throw new UnsupportedOperationException(x.getClass() + " is not supported");

                    }
                }
                print(p,r,t,ts,ex,sq,ma,offset);
            }
        }
        else{
            JSONArray jsonArray = (JSONArray) o;
            int p=0,r=0,t=0,ts=0,ex=0,sq=0,ma=0;
            for(Object x :jsonArray){
                JSONObject jsonObject = (JSONObject) x;
                String y = (String)jsonObject.keySet().toArray()[0];
                if(y.equals("bool")){
                    analyser(jsonObject,offset);
                }
                else{
                    switch(y){
                        case "prefix":{
                            p++;
                            break;
                        }
                        case "range":{
                            r++;
                            break;
                        }
                        case "term":{
//                            System.out.println("term : "+jsonObject.get(y));
                            t++;
                            break;
                        }
                        case "terms":{
//                            System.out.println("terms : "+jsonObject.get(y));
                            ts++;
                            break;
                        }
                        case "wildcard":{}
                        case "exists":{
                            ex++;
                            break;
                        }
                        case "spr_query_string":{}
                        case "query_string":{
                            sq++;
                            break;
                        }
                        case "match_all":{
                            ma++;
                            break;
                        }
                        default:{
                            System.out.println(y+" is not suppported like term clause");
                            throw new UnsupportedOperationException(y.getClass() + " is not supported");

                        }
                    }
                }
            }
            print(p,r,t,ts,ex,sq,ma,offset);
        }
    }
    private static void analyser(JSONObject jsonObject,String offset){
        for(Object objj : jsonObject.keySet().toArray()){
            String obj = (String) objj;
            switch(obj){
                case "bool":{
                    print("bool",offset);
                    JSONObject child = ((JSONObject) jsonObject.get("bool"));
                    System.out.println(child);
                    System.out.println(jsonObject);
                    for(Object xx: (child.keySet().toArray())){
//                        System.out.println(xx);
                        String x = (String) xx;
                        JSONObject j = new JSONObject();
                        j.put(x,child.get(x));
                        analyser(j,offset+"    ");
                    }
                    break;
                }
                case "must_not":{}
                case "should":{}
                case "must":{
                    print(obj,offset);
                    Object child = (jsonObject.get(obj));
                    analyser2(child,offset+"    ");
                    break;
                }
            }
        }
    }

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException, ParseException {

//        FileReader fileReader = new FileReader("src/main/java/json1");
//        Object obj;
//        obj = jsonParser.parse(fileReader);
//        JSONObject jsonObject = (JSONObject) obj;
//        Set<String> set = jsonObject.keySet();
//        for(String x :set){
//            System.out.println(x);
//        }
//        Object query = ((JSONObject) ((JSONObject) ((JSONObject) ((JSONObject) jsonObject.get("query")).get("constant_score")).get("filter")).get("bool")).get("must");
//        System.out.println(query.getClass());
//        System.out.println(query);
//        System.out.println(converter(obj));

        FileReader fileReader2 = new FileReader("src/main/java/json2");
        Object obj2;
        obj2 = jsonParser.parse(fileReader2);
//        JSONObject j = ((JSONObject) ((JSONObject) ((JSONObject) ((JSONObject)obj2).get("query")).get("constant_score")).get("filter"));
//        analyser(j,"");


//        obj2 = (((JSONObject) ((JSONObject) ((JSONObject)obj2).get("query")).get("constant_score")).get("filter"));
        JSONObject jsonObject = (JSONObject) converter(obj2);
//        System.out.println(jsonObject);
//        analyser(jsonObject,"");
//
//
//
//
//        System.out.println(jsonObject);

//        Object set2 = jsonObject2.keySet().toArray()[0];
//        System.out.println(set2);
//        Object jsonObject = jsonObject2.get(set2);
//        System.out.println(jsonObject.getClass());
//        System.out.println(jsonObject);
//        System.out.println(((JSONArray) jsonObject).get(0).getClass());
//        TermsQueryBuilder termsQueryBuilder = QueryBuilders.termsQuery((String) set2,(JSONArray) jsonObject);
//        System.out.println(termsQueryBuilder);








    }
}
