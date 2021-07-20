import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


public class GetExecTime {
    static public JSONParser jsonParser = new JSONParser();
    private static long getTimeJson(String line) throws ParseException {
        JSONObject jsonObject = (JSONObject) jsonParser.parse(line);
        return (long) jsonObject.get("total_exec_time");
    }
    private static void getTime(String file) throws IOException, ParseException {
        BufferedReader in = new BufferedReader(new FileReader("src/main/Curl Outputs/" + file));
        String line;
        long avgTime=0L;
        int counter=0;
        while ((line = in.readLine()) != null) {
            avgTime += getTimeJson(line);
            counter++;
        }
        avgTime = avgTime/counter;
        System.out.println(file+" Total_Exec_Time : "+avgTime);
    }
    public static void main(String[] args) throws IOException, ParseException {
        String[] arr = {"_","a","b"};
        for(int i=1;i<8;i++){
            for(String j:arr){
                getTime("f"+i+j+"-out.json");
            }
            System.out.println(" ");
        }
    }
}
