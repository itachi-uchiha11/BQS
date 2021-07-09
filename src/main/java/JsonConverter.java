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
        Object[] s2 = jsonObject1.keySet().toArray();
        String x2 = (String) s2[0];
        Object x3 = jsonObject1.get(x2);
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
            System.out.println(obj+" : "+obj.getClass()+"is not supported\n");
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
        QueryBuilder queryBuilder = BoolQuerySimplifier.optimizeBoolQueryBuilder(boolQueryBuilder);
//        System.out.println(queryBuilder);
        return getJsonObject(queryBuilder);
    }
    private static JSONObject converter2(JSONObject obj){
        JSONObject jsonObject = new JSONObject();
        Set<String> set = obj.keySet();
        for(String x :set){
//            System.out.println(x);
            if(x.equals("bool")){
//                System.out.println("in bool");
                jsonObject.put(x, optimizedObj((JSONObject) obj.get(x)).get(x));
            }
            else{
                jsonObject.put(x,obj.get(x));
            }
        }
        return jsonObject;
    }
    private static JSONArray converter2(JSONArray obj){
        JSONArray jsonArray = new JSONArray();
        for(Object x:obj){
            jsonArray.add(converter2((JSONObject) x));
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
            throw new UnsupportedOperationException(obj.getClass() + " is not supported");
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

        FileReader fileReader2 = new FileReader("src/main/java/json1");
        Object obj2;
        obj2 = jsonParser.parse(fileReader2);
        obj2 = (((JSONObject) ((JSONObject) ((JSONObject)obj2).get("query")).get("constant_score")).get("filter"));
        JSONObject jsonObject = (JSONObject) converter(obj2);
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
