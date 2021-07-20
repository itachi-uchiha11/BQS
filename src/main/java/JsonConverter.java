import org.elasticsearch.common.geo.builders.ShapeBuilder;
import org.elasticsearch.index.query.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    static public boolean toBeAnalysed = false;
    static public int varno = 0;
    static public int extra_size = 0;
    static public boolean unoptimized = false;
    static public boolean addTime = true;


    private static QueryBuilder getSingleQueryBuilder(JSONObject jsonObject) {
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
//                System.out.println(x2+"\n"+x3);
                QueryBuilder queryBuilder = QueryBuilders.prefixQuery(x2, (String) x3);
//                System.out.println(queryBuilder);
                return queryBuilder;
            }
            case "term": {
                return QueryBuilders.termQuery(x2, x3);
            }
            case "terms": {
//                System.out.println(x3);
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
                return getShapeBuilder(s2, jsonObject1);
//                return QueryBuilders.geoShapeQuery("x2",getShapeBuilder(x3));
            }
        }
        throw new UnsupportedOperationException(x + " is not supported in QueryBuilders");
    }

    private static GeoShapeQueryBuilder getShapeBuilder(Object[] s, JSONObject jsonObject) {
        for (Object xx : s) {
            String x = (String) xx;
            if (x.equals("_name")) {

            } else {
//                System.out.println(x);
                JSONObject jsonObject1 = (JSONObject) jsonObject.get(x);
                String rad = (String) ((JSONObject) ((JSONObject) jsonObject.get(x)).get("shape")).get("radius");
                JSONArray coord = (JSONArray) ((JSONObject) ((JSONObject) jsonObject.get(x)).get("shape")).get("coordinates");
//                System.out.println(coord.getClass());
//                throw new UnsupportedOperationException(coord + " is not supported in GEOQueryBuilders");
                double lat = (double) coord.get(0);
                double lon = (double) coord.get(1);
                GeoShapeQueryBuilder geoShapeQueryBuilder = new GeoShapeQueryBuilder(x, ShapeBuilder.newCircleBuilder().center(lat, lon).radius(rad)).queryName(null);
//                System.out.println(geoShapeQueryBuilder);
                return geoShapeQueryBuilder;
            }
        }
        return null;
    }

    private static ArrayList<QueryBuilder> getQueryBuilders(Object obj, String tt) {
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
//            throw new UnsupportedOperationException(obj.getClass() + " is not supported");
        }
        return lst;
    }

    private static BoolQueryBuilder getBoolQueryBuilder(JSONObject jsonObject) {
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

    private static JSONObject resolveMiscellaneousAndPrefix(JSONObject j) {
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
//                System.out.println(k);
                if (k.length() >= string.length() && string.equals(k.substring(0, 14))) {
                    JSONObject jsonObject = (JSONObject) ((JSONArray) ((JSONObject) j.get(s)).get(k)).get(0);
                    newj.put("terms", jsonObject);
                } else {
                    Object value = j.get(s);
                    if (value instanceof JSONObject) {
                        newj.put(s, resolveMiscellaneousAndPrefix((JSONObject) value));
                    } else if (value instanceof JSONArray) {
                        JSONArray jsonArray = new JSONArray();
                        for (Object a : (JSONArray) value) {
                            if (a instanceof JSONObject) {
                                jsonArray.add(resolveMiscellaneousAndPrefix((JSONObject) a));
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
                    newj.put(s, resolveMiscellaneousAndPrefix((JSONObject) value));
                } else if (value instanceof JSONArray) {
                    JSONArray jsonArray = new JSONArray();
                    for (Object a : (JSONArray) value) {
                        if (a instanceof JSONObject) {
                            jsonArray.add(resolveMiscellaneousAndPrefix((JSONObject) a));
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

    private static JSONObject getJsonObject(QueryBuilder queryBuilder) {
        String x = queryBuilder.toString();
//        System.out.println(x);
        try {
            JSONObject jsonobj = (JSONObject) jsonParser.parse(x);
            jsonobj = resolveMiscellaneousAndPrefix(jsonobj);
//            System.out.println(jsonobj);
            return jsonobj;
        } catch (ParseException e) {
            System.out.println("Incorrect JSON formation");
            e.printStackTrace();
            return null;
        }
    }

    private static JSONObject optimizedObj(JSONObject jsonObject) {
        BoolQueryBuilder boolQueryBuilder = getBoolQueryBuilder(jsonObject);
        long l1 = getJsonObject(boolQueryBuilder).toString().length();
        QueryBuilder queryBuilder;
        if (unoptimized) {
            queryBuilder = boolQueryBuilder;
        } else {
            long start = System.nanoTime();
            queryBuilder = BoolQuerySimplifier.optimizeBoolQueryBuilder(boolQueryBuilder);
            long end = System.nanoTime();
            if(addTime)optTime+=(end-start)/(1000*1000);
        }
        long l2 = getJsonObject(queryBuilder).toString().length();
        maxPercent.add(((double) (l1 - l2)) / (double) l1);
        unoptSize.add((int) l1);
        optSize.add((int) l2);
        return getJsonObject(queryBuilder);
    }

    private static JSONObject converter2(JSONObject obj) {
        JSONObject jsonObject = new JSONObject();
        Set<String> set = obj.keySet();
        for (String x : set) {
            if (x.equals("bool")) {
                JSONObject jobj = new JSONObject();
                jobj.put(x, obj.get(x));
//                System.out.println(jobj);
                if (toBeAnalysed) {
                    System.out.println("XXXXXX\nUnoptimized:");
                    analyser(jobj, "");
                }
                JSONObject optimizedObj = optimizedObj((JSONObject) obj.get(x));
//                System.out.println(optimizedObj);
//                jsonObject.put(x, optimizedObj.get(x));
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
                    System.out.println("XXXXXX");
                }
                jsonObject.put(x, jobj2.get(x));
//                System.out.println(jsonObject);
            } else {
                jsonObject.put(x, converter(obj.get(x)));
            }
        }
        return jsonObject;
    }

    private static JSONArray converter2(JSONArray obj) {
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

    private static Object converter(Object obj) {
        if (obj.getClass() == jobj.getClass()) {
            return converter2((JSONObject) obj);
        } else if (obj.getClass() == jarray.getClass()) {
            return converter2((JSONArray) obj);
        } else {
            //System.out.println(obj.getClass() + " is not supported");
            return obj;
        }
    }

    private static void print(int a, int b, int c, int d, int e, int f, int g, int h, int i, String o) {
        if (a != 0) {
            System.out.print(o + "prefix : ");
            System.out.println(a);
        }
        if (b != 0) {
            System.out.print(o + "range : ");
            System.out.println(b);
        }
        if (c != 0) {
            System.out.print(o + "term : ");
            System.out.println(c);
        }
        if (d != 0) {
            System.out.print(o + "terms : ");
            System.out.println(d);
        }
        if (e != 0) {
            System.out.print(o + "exists/wildcard : ");
            System.out.println(e);
        }
        if (f != 0) {
            System.out.print(o + "spr_query_string/queryString : ");
            System.out.println(f);
        }
        if (g != 0) {
            System.out.print(o + "match_all : ");
            System.out.println(g);
        }
        if (h != 0) {
            System.out.print(o + "match : ");
            System.out.println(h);
        }
        if (i != 0) {
            System.out.print(o + "geo_shape : ");
            System.out.println(i);
        }
    }

    private static void print(String a, String o) {
        System.out.print(o);
        System.out.println(a + " : ");
    }

    private static void analyser2(Object o, String offset) {

        if (o instanceof JSONObject) {

            JSONObject jsonObject = (JSONObject) o;
//            System.out.println(jsonObject.toString().length());
            String x = (String) jsonObject.keySet().toArray()[0];
            if (x.equals("bool")) {
                analyser(jsonObject, offset);
            } else {
                int p = 0, r = 0, t = 0, ts = 0, ex = 0, sq = 0, ma = 0, mq = 0, gs = 0;
                switch (x) {
                    case "prefix": {
                        p++;
                        break;
                    }
                    case "range": {
                        r++;
                        break;
                    }
                    case "term": {
//                        System.out.println("term : "+jsonObject.get(x));
                        t++;
                        break;
                    }
                    case "terms": {
//                        System.out.println("terms : "+jsonObject.get(x));
                        ts++;
                        break;
                    }
                    case "wildcard": {
                    }
                    case "exists": {
                        ex++;
                        break;
                    }
                    case "spr_query_string": {
                    }
                    case "query_string": {
                        sq++;
                        break;
                    }
                    case "match_all": {
                        ma++;
                        break;
                    }
                    case "match": {
                        mq++;
                        break;
                    }
                    case "geo_shape": {
                        gs++;
                        break;
                    }
                    default: {
                        System.out.println(x + " is not suppported like term clause");
                        throw new UnsupportedOperationException(x.getClass() + " is not supported");

                    }
                }
                print(p, r, t, ts, ex, sq, ma, mq, gs, offset);
            }
        } else {
            JSONArray jsonArray = (JSONArray) o;
//            System.out.println(jsonArray.toString().length());
            int p = 0, r = 0, t = 0, ts = 0, ex = 0, sq = 0, ma = 0, mq = 0, gs = 0;
            for (Object x : jsonArray) {
                JSONObject jsonObject = (JSONObject) x;
                String y = (String) jsonObject.keySet().toArray()[0];
                if (y.equals("bool")) {
                    analyser(jsonObject, offset);
                } else {
                    switch (y) {
                        case "prefix": {
                            p++;
                            break;
                        }
                        case "range": {
                            r++;
                            break;
                        }
                        case "term": {
//                            System.out.println("term : "+jsonObject.get(y));
                            t++;
                            break;
                        }
                        case "terms": {
//                            System.out.println("terms : "+jsonObject.get(y));
                            ts++;
                            break;
                        }
                        case "wildcard": {
                        }
                        case "exists": {
                            ex++;
                            break;
                        }
                        case "spr_query_string": {
                        }
                        case "query_string": {
                            sq++;
                            break;
                        }
                        case "match_all": {
                            ma++;
                            break;
                        }
                        case "match": {
                            mq++;
                            break;
                        }
                        case "geo_shape": {
                            gs++;
                            break;
                        }
                        default: {
                            System.out.println(y + " is not suppported like term clause");
                            throw new UnsupportedOperationException(y.getClass() + " is not supported");

                        }
                    }
                }
            }
            print(p, r, t, ts, ex, sq, ma, mq, gs, offset);
        }
    }

    private static void analyser(JSONObject jsonObject, String offset) {
        for (Object objj : jsonObject.keySet().toArray()) {
            String obj = (String) objj;
            switch (obj) {
                case "bool": {
                    print("bool", offset);
                    JSONObject child = ((JSONObject) jsonObject.get("bool"));
//                    System.out.println(child);
//                    System.out.println(jsonObject);
                    for (Object xx : (child.keySet().toArray())) {
//                        System.out.println(xx);
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
//                    System.out.println(obj);
//                    System.out.println(jsonObject.get(obj));
                }
            }
        }
    }


    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException, ParseException {
        String[] unOptQueries = {
//                "json5",
//                "json6",
//                "json7", "json8",
//                "json13",
//                "json14",
//                "json15",
//                "json16",
//                "json8"
//                ,"json7","json14","json15","json16"
                "demo"
        };
        FileWriter fw22 = null;
        fw22 = new FileWriter("src/main/java/" + "OptimizedQueryLog");
        for (String x : unOptQueries) {
            FileReader fileReader2 = new FileReader("src/main/java/" + x);
            Object obj2;
            System.out.println("json query : "+x);
            obj2 = jsonParser.parse(fileReader2);
            unoptLength = ((JSONObject) obj2).toString().length();
            BufferedReader in = new BufferedReader(new FileReader("src/main/java/" + x));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }

//            System.out.println(sb.length());
//            System.out.println(obj2.toString().length());
            in.close();
            extra_size = 0;
            optLength = 0;
            optTime=0L;
            maxPercent = new ArrayList<>();
            optSize = new ArrayList<>();
            unoptSize = new ArrayList<>();
//            long start = System.nanoTime();
            JSONObject jsonObject = (JSONObject) converter(obj2);
//            long end = System.nanoTime();
            System.out.println("optimization time : " + optTime+"ms");
            optLength = (jsonObject).toString().length();
//            System.out.println("Extra Size : " + extra_size);
            FileWriter fw2 = null;
            try {
                fw2 = new FileWriter("src/main/java/" + x + "-2");
                fw2.write(jsonObject.toString());
                fw2.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fw22.append("Unoptimimized Query : " + x + "\nUnoptimized Query(without caching) : "+x+"-1\nOptimized Query : " + x + "-2\nUnoptimized Query Size : " + unoptLength + "\nUnoptimized Query(without caching) Size:"+(unoptLength-extra_size)+"\nOptimized Query Size :" + (optLength ) + "\n_Cache_Key Size : "+extra_size+"\n");
                fw22.append("Size Reductions : " + (unoptLength - optLength - extra_size) + "\nBool Query Optimization % : \n");
                int counter = 0;
//                for (Double d : maxPercent) {
//                    fw22.append((++counter) + ") " + d * 100 + "% \n");
//                }
//                fw22.append("\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fw2 = new FileWriter("src/main/java/" + x + "-1");
                unoptimized = true;
                addTime=false;
                JSONObject jsonObject1 = (JSONObject) converter(obj2);
                unoptimized = false;
                addTime=true;
                fw2.write(jsonObject1.toString());
                fw2.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        fw22.close();

    }
}
