import org.elasticsearch.common.geo.builders.ShapeBuilder;
import org.elasticsearch.index.query.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class JsonConverter {

    static JSONObject jobj = new JSONObject();
    static JSONArray jarray = new JSONArray();
    static public JSONParser jsonParser = new JSONParser();
    static public int unoptLength = 0;
    static public int optLength = 0;
    static public List<Double> maxPercent = new ArrayList<>();
    static public List<Integer> unoptSize = new ArrayList<>();
    static public List<Integer> optSize = new ArrayList<>();
    static public long optTime = 0L;
    static public boolean toBeAnalysed = true;
    static public int varno = 0;
    static public int extra_size = 0;
    static public boolean returnUnoptimized = false;
    static public boolean addTime = true;
    private final String[] queries = {"prefix","range","term","terms","wildcard","exists","spr_query_string","query_string","match_all","match","geo_shape"};

    private GeoShapeQueryBuilder getGeoShapeBuilder(Object[] s, JSONObject jsonObject) {
        for (Object xx : s) {
            String x = (String) xx;
            if (x.equals("_name")) {

            } else {
                JSONObject jsonObject1 = (JSONObject) jsonObject.get(x);
                String rad = (String) ((JSONObject) (jsonObject1).get("shape")).get("radius");
                JSONArray coord = (JSONArray) ((JSONObject) (jsonObject1).get("shape")).get("coordinates");
                double lat = (double) coord.get(0);
                double lon = (double) coord.get(1);
                GeoShapeQueryBuilder geoShapeQueryBuilder = new GeoShapeQueryBuilder(x, ShapeBuilder.newCircleBuilder().center(lat, lon).radius(rad)).queryName(null);
                return geoShapeQueryBuilder;
            }
        }
        return null;
    }

    // Query Builder corresponding to Term Level Queries
    private QueryBuilder getSingleQueryBuilder(JSONObject jsonObject) {
        Object[] s = jsonObject.keySet().toArray();
        String x = (String) s[0];
        JSONObject jsonObject1 = (JSONObject) jsonObject.get(x);
        if (x.equals("bool")) {
            return getBoolQueryBuilder(jsonObject1);
        }
        Object[] s2 = null;
        String x2 = "";
        Object x3 = null;
        if (x.equals("match_all")) {
        } else {
            s2 = jsonObject1.keySet().toArray();
            x2 = (String) s2[0];
            x3 = jsonObject1.get(x2);
        }
        switch (x) {
            case "prefix": {
                QueryBuilder queryBuilder = QueryBuilders.prefixQuery(x2, (String) x3);
                return queryBuilder;
            }
            case "term": {
                return QueryBuilders.termQuery(x2, x3);
            }
            case "terms": {
                if (x3 instanceof JSONArray) {
                    return QueryBuilders.termsQuery(x2, (JSONArray) x3);
                }
                return QueryBuilders.termsQuery("miscellaneous_" + varno++, jsonObject1);
            }
            case "range": {
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(x2);
                for (Object y : ((JSONObject) x3).keySet()) {
                    switch ((String) y) {
                        case "from": {
                            rangeQueryBuilder.from(((JSONObject) x3).get(y));
                            break;
                        }
                        case "to": {
                            rangeQueryBuilder.to(((JSONObject) x3).get(y));
                            break;
                        }
                        case "include_lower": {
                            rangeQueryBuilder.includeLower((Boolean) ((JSONObject) x3).get(y));
                            break;
                        }
                        case "include_upper": {
                            rangeQueryBuilder.includeUpper((Boolean) ((JSONObject) x3).get(y));
                            break;
                        }
                    }
                }
                return rangeQueryBuilder;
            }
            case "exists": {
                return QueryBuilders.wildcardQuery((String) x3, "*");
            }
            case "spr_query_string": {
                QueryStringQueryBuilder queryStringQueryBuilder = QueryBuilders.queryString((String) jsonObject1.get("query"));
                for (Object z : (JSONArray) jsonObject1.get("fields")) {
                    queryStringQueryBuilder.field((String) z);
                }
                queryStringQueryBuilder.useDisMax((Boolean) jsonObject1.get("use_dis_max"));
                return queryStringQueryBuilder;
            }
            case "match_all": {
                return QueryBuilders.matchAllQuery();
            }
            case "match": {
                return QueryBuilders.matchQuery(x2, x3);
            }
            case "geo_shape": {
                return getGeoShapeBuilder(s2, jsonObject1);
            }
        }
        throw new UnsupportedOperationException(x + " is not supported in QueryBuilders");
    }

    // returns List of QueryBuilder objects corresponding to its member clauses
    private ArrayList<QueryBuilder> getQueryBuilders(Object obj, String tt) {
        ArrayList<QueryBuilder> lst = new ArrayList<>();
        if (obj.getClass() == jobj.getClass()) {
            lst.add(getSingleQueryBuilder((JSONObject) obj));
        } else if (obj.getClass() == jarray.getClass()) {
            for (Object x : (JSONArray) obj) {
                lst.add(getSingleQueryBuilder((JSONObject) x));
            }
        } else {
            extra_size += (3 + tt.length());
            extra_size += obj.toString().length();

            return null;
        }
        return lst;
    }

    private BoolQueryBuilder getBoolQueryBuilder(JSONObject jsonObject) {
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        for (Object x : jsonObject.keySet()) {
            ArrayList<QueryBuilder> lst = getQueryBuilders(jsonObject.get(x), (String) x);
            switch ((String) x) {
                case "filter": {
                }
                case "must": {
                    for (QueryBuilder y : lst) {
                        boolQueryBuilder.must(y);
                    }
                    break;
                }
                case "must_not": {
                    for (QueryBuilder y : lst) {
                        boolQueryBuilder.mustNot(y);
                    }
                    break;
                }
                case "should": {
                    for (QueryBuilder y : lst) {
                        boolQueryBuilder.should(y);
                    }
                    break;
                }
                default: {
                    break;
                }
            }
        }
        return boolQueryBuilder;
    }

    // resolves Placeholder QueryBuilders into
    // their respective Json format Query Syntax
    private JSONObject resolveJSONObject(JSONObject j) {
        JSONObject newj = new JSONObject();
        String string = "miscellaneous_";
        for (Object e : j.keySet().toArray()) {
            String s = (String) e;
            if (s.equals("prefix")) {
                JSONObject jsonObject = new JSONObject();
                String k = (String) (((JSONObject) j.get(s)).keySet().toArray()[0]);
                jsonObject.put(k, ((JSONObject) ((JSONObject) j.get(s)).get(k)).get("prefix"));
                newj.put(s, jsonObject);
            } else if (s.equals("query_string")) {
                newj.put("spr_query_string", j.get(s));
            } else if (s.equals("wildcard")) {
                JSONObject tempjson = new JSONObject();
                tempjson.put("field", ((JSONObject) j.get(s)).keySet().toArray()[0]);
                newj.put("exists", tempjson);
            } else if (s.equals("terms")) {
                String k = (String) (((JSONObject) j.get(s)).keySet().toArray()[0]);
                if (k.length() >= string.length() && string.equals(k.substring(0, 14))) {
                    JSONObject jsonObject = (JSONObject) ((JSONArray) ((JSONObject) j.get(s)).get(k)).get(0);
                    newj.put("terms", jsonObject);
                } else {
                    Object value = j.get(s);
                    if (value instanceof JSONObject) {
                        newj.put(s, resolveJSONObject((JSONObject) value));
                    } else if (value instanceof JSONArray) {
                        JSONArray jsonArray = new JSONArray();
                        for (Object a : (JSONArray) value) {
                            if (a instanceof JSONObject) {
                                jsonArray.add(resolveJSONObject((JSONObject) a));
                            } else {
                                jsonArray.add(a);
                            }
                        }
                        newj.put(s, jsonArray);
                    } else {
                        newj.put(s, value);
                    }
                }
            } else {
                Object value = j.get(s);
                if (value instanceof JSONObject) {
                    newj.put(s, resolveJSONObject((JSONObject) value));
                } else if (value instanceof JSONArray) {
                    JSONArray jsonArray = new JSONArray();
                    for (Object a : (JSONArray) value) {
                        if (a instanceof JSONObject) {
                            jsonArray.add(resolveJSONObject((JSONObject) a));
                        } else {
                            jsonArray.add(a);
                        }
                    }
                    newj.put(s, jsonArray);
                } else {
                    newj.put(s, value);
                }
            }
        }
        return newj;
    }

    // Converts QueryBuilder object to Json format
    private JSONObject getJsonObject(QueryBuilder queryBuilder) {
        String x = queryBuilder.toString();
        try {
            JSONObject jsonobj = (JSONObject) jsonParser.parse(x);
            jsonobj = resolveJSONObject(jsonobj);
            return jsonobj;
        } catch (ParseException e) {
            System.out.println("Incorrect JSON formation");
            e.printStackTrace();
            return null;
        }
    }

    private JSONObject optimizedObj(JSONObject jsonObject) {
        BoolQueryBuilder boolQueryBuilder = getBoolQueryBuilder(jsonObject);
        long l1 = getJsonObject(boolQueryBuilder).toString().length();
        QueryBuilder queryBuilder;
        // returns uncached-unoptimized query
        if (returnUnoptimized) {
            queryBuilder = boolQueryBuilder;
        }
        // returns optimized query
        else {
            long start = System.nanoTime();
            queryBuilder = BoolQuerySimplifier.optimizeBoolQueryBuilder(boolQueryBuilder);
            long end = System.nanoTime();
            if (addTime) optTime += (end - start) / (1000 * 1000);
        }
        long l2 = getJsonObject(queryBuilder).toString().length();
        maxPercent.add(((double) (l1 - l2)) / (double) l1);
        unoptSize.add((int) l1);
        optSize.add((int) l2);
        return getJsonObject(queryBuilder);
    }

    // main converter function
    // Unoptimized Json -> Unoptimized-uncached/Optimized Json
    // depending on value of unoptimized flag
    private JSONObject converter2(JSONObject obj) {
        JSONObject jsonObject = new JSONObject();
        Set<String> set = obj.keySet();
        for (String x : set) {
            if (x.equals("bool")) {
                JSONObject jobj = new JSONObject();
                jobj.put(x, obj.get(x));
                if (toBeAnalysed) {
                    System.out.println("Start Analysis\nUnoptimized:");
                    analyser(jobj, "");
                }
                JSONObject optimizedObj = optimizedObj((JSONObject) obj.get(x));
                JSONObject jobj2 = new JSONObject();
                if (optimizedObj.get(x) != null) {
                    jobj2.put(x, optimizedObj.get(x));
                    if (toBeAnalysed) {
                        System.out.println("Optimized:");
                        analyser(jobj2, "");
                    }
                } else {
                    jobj2 = optimizedObj;
                    if (toBeAnalysed) System.out.println(optimizedObj);
                    x = (String) jobj2.keySet().toArray()[0];
                }
                if (toBeAnalysed) {
                    System.out.println("End Analysis");
                }
                jsonObject.put(x, jobj2.get(x));
            } else {
                jsonObject.put(x, converter(obj.get(x)));
            }
        }
        return jsonObject;
    }

    private JSONArray converter2(JSONArray obj) {
        JSONArray jsonArray = new JSONArray();
        for (Object x : obj) {
            if (x instanceof String) {
                return obj;
            } else {
                jsonArray.add(converter2((JSONObject) x));
            }
        }
        return jsonArray;
    }

    public Object converter(Object obj) {
        if (obj.getClass() == jobj.getClass()) {
            return converter2((JSONObject) obj);
        } else if (obj.getClass() == jarray.getClass()) {
            return converter2((JSONArray) obj);
        } else {
            return obj;
        }
    }

    private void print(String a, String o) {
        System.out.print(o);
        System.out.println(a + " : ");
    }

    private void analyser2(Object o, String offset) {
        Map<String,Integer> queryCountMap = new HashMap<>();
        for(String x:queries){
            queryCountMap.put(x,0);
        }
        if (o instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) o;
            String x = (String) jsonObject.keySet().toArray()[0];
            if (x.equals("bool")) {
                analyser(jsonObject, offset);
            } else {
                Integer count = queryCountMap.get(x);
                if(count==null){
                    System.out.println(x + " is not suppported");
                    throw new UnsupportedOperationException(x.getClass() + " is not supported");
                }
                else{
                    queryCountMap.put(x,count+1);
                }
            }
        } else {
            JSONArray jsonArray = (JSONArray) o;
            for (Object x : jsonArray) {
                JSONObject jsonObject = (JSONObject) x;
                String y = (String) jsonObject.keySet().toArray()[0];
                if (y.equals("bool")) {
                    analyser(jsonObject, offset);
                } else {
                    Integer count = queryCountMap.get(y);
                    if(count==null){
                        System.out.println(x + " is not suppported");
                        throw new UnsupportedOperationException(x.getClass() + " is not supported");
                    }
                    else{
                        queryCountMap.put(y,count+1);
                    }
                }
            }
        }
        for(Map.Entry<String,Integer>entry : queryCountMap.entrySet()){
            if(entry.getValue()!=0){
                System.out.println(offset+entry.getKey()+" : "+entry.getValue());
            }
        }
    }

    // Prints Tree structure of Bool Query
    // Useful for debugging and visualizing optimization
    private void analyser(JSONObject jsonObject, String offset) {
        for (Object objj : jsonObject.keySet().toArray()) {
            String obj = (String) objj;
            switch (obj) {
                case "bool": {
                    print("bool", offset);
                    JSONObject child = ((JSONObject) jsonObject.get("bool"));
                    for (Object xx : (child.keySet().toArray())) {
                        String x = (String) xx;
                        JSONObject j = new JSONObject();
                        j.put(x, child.get(x));
                        analyser(j, offset + "    ");
                    }
                    break;
                }
                case "must_not": {
                }
                case "should": {
                }
                case "must": {
                    print(obj, offset);
                    Object child = (jsonObject.get(obj));
                    analyser2(child, offset + "    ");
                    break;
                }
                default: {
                }
            }
        }
    }

    // Optimized and Unoptimized-uncached queries are written into Files Directory
    // Optimization Stats are writtten in OptimizeQueryLog in Files Directory
    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException, ParseException {
        String[] unOptQueries = {"demo"};
        FileWriter fw22 = null;
        fw22 = new FileWriter("src/main/Files/" + "OptimizedQueryLog");
        Map<String, Long> optTimeMap = new HashMap<>();
        for (String x : unOptQueries) {
            FileReader fileReader2 = new FileReader("src/main/Files/" + x);
            Object obj2;
            System.out.println("json query : " + x);
            obj2 = jsonParser.parse(fileReader2);
            unoptLength = ((JSONObject) obj2).toString().length();
            extra_size = 0;
            optLength = 0;
            optTime = 0L;
            maxPercent = new ArrayList<>();
            optSize = new ArrayList<>();
            unoptSize = new ArrayList<>();
            JsonConverter jsonConverter = new JsonConverter();
            JSONObject jsonObject = (JSONObject) jsonConverter.converter(obj2);
            optTimeMap.put(x, optTime);
            optLength = (jsonObject).toString().length();
            FileWriter fw2 = null;
            try {
                fw2 = new FileWriter("src/main/Files/" + x + "-2");
                fw2.write(jsonObject.toString());
                fw2.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fw22.append("Unoptimimized Query : " + x + "\nUnoptimized Query(without caching) : " + x + "-1\nOptimized Query : " + x + "-2\nUnoptimized Query Size : " + unoptLength + "\nUnoptimized Query(without caching) Size:" + (unoptLength - extra_size) + "\nOptimized Query Size :" + (optLength) + "\n_Cache_Key Size : " + extra_size + "\n");
                fw22.append("Size Reductions : " + (unoptLength - optLength - extra_size) + "\nBool Query Optimization % : \n");
                fw22.append("Percentage Optimization in Size : " + (unoptLength - optLength - extra_size)*(100) / (unoptLength - extra_size) + "%\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fw2 = new FileWriter("src/main/Files/" + x + "-1");
                returnUnoptimized = true;
                addTime = false;
                JSONObject jsonObject1 = (JSONObject) jsonConverter.converter(obj2);
                returnUnoptimized = false;
                addTime = true;
                fw2.write(jsonObject1.toString());
                fw2.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        fw22.close();
        for(Map.Entry<String,Long>e : optTimeMap.entrySet()){
            System.out.println(e);
        }
    }
}
