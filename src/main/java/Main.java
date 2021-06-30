import java.sql.Array;
import java.util.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Main {
    static Integer MOD = (int) 1e9 + 7;
    static FastReader sc = new FastReader();
    static Print p = new Print();

    public static String addmatch(String a,String b){
        String ans="{\"match\":{";
        ans+=a;
        ans+=":";
        ans+=b;
        ans+="}}";
        return ans;
    }
    public static String queryy(int i){
        String ans="{\"bool\":{\"must\":[";
        ans+=addmatch("\"country\"","\"India\"");
        ans+=",";
        ans+=addmatch("\"release_year\"","\""+Integer.toString(i)+"\"");

        ans+="]}}";
        return ans;

    }
    public static void B(){
        int a=2010;
        int b=2022;
        String ans = "\"query\": {\"bool\": {\"should\": [";
        for(int i=2010;i<5023;i++){
            ans+=queryy(i);
            if(i!=5022){
                ans+=",";
            }
        }
        ans+="]}}";
        System.out.println(ans);
    }
    public static void A(){
        int a=2010;
        int b=2022;
        String ans = "\"query\": {\"bool\": {\"must\": [";
        ans+=addmatch("\"country\"","\"India\"");
        ans+=",";
        ans+="{\"bool\": {\"should\": [";
        for(int i=2010;i<5023;i++) {
            ans += addmatch("\"release_year\"","\""+Integer.toString(i)+"\"");
            if (i != 5022) {
                ans += ",";
            }
        }
        ans+="]}}";
        ans+="]}}";
        System.out.println(ans);
    }

    public static void C(){
        int n= sc.nextInt();
        int m= sc.nextInt();
        String x = sc.nextLine();
        int[] a= new int[n];
        int[] b= new int[n];
        int j=0;
        for(int i=0;i<x.length();i++){
            char c = x.charAt(i);
            if(c=='1'){b[j++]=i;}
        }
        j=0;
        for(int i=0;i<n;i++){
            while(i<b[j]){

            }

        }
    }
    public static void solve() {
//        B();
//        System.out.println("\n");
//        A();
        C();
    }

    public static void main(String[] args) {
        int t = 1;
        t = sc.nextInt();
        while (t-- != 0) {
            solve();
        }
        p.print();
    }

    static class Functions {

        static void sort(int[] a) {
            ArrayList<Integer> l = new ArrayList<>();
            for (int i : a) l.add(i);
            Collections.sort(l);
            for (int i = 0; i < a.length; i++) a[i] = l.get(i);
        }

        public static long mod_add(long a, long b) {
            return (a % MOD + b % MOD + MOD) % MOD;
        }

        public static long pow(long a, long b) {
            long res = 1;
            while (b > 0) {
                if ((b & 1) != 0)
                    res = mod_mul(res, a);
                a = mod_mul(a, a);
                b >>= 1;
            }
            return res;
        }

        public static long mod_mul(long a, long b) {
            long res = 0;
            a %= MOD;
            while (b > 0) {
                if ((b & 1) > 0) {
                    res = mod_add(res, a);
                }
                a = (2 * a) % MOD;
                b >>= 1;
            }
            return res;
        }

        public static long gcd(long a, long b) {
            if (a == 0) return b;
            return gcd(b % a, a);
        }

        public static long factorial(long n) {
            long res = 1;
            for (int i = 1; i <= n; i++) {
                res = (i % MOD * res % MOD) % MOD;
            }
            return res;
        }

        public static int count(int[] arr, int x) {
            int count = 0;
            for (int val : arr) if (val == x) count++;
            return count;
        }

    }

    static class Print {
        StringBuffer strb = new StringBuffer();

        public void write(Object str) {
            strb.append(str);
        }

        public void writes(Object str) {
            strb.append(str).append(" ");
        }

        public void writeln(Object str) {
            strb.append(str).append("\n");
        }

        public void writeln() {
            strb.append('\n');
        }

        public void writes(int[] arr) {
            for (int val : arr) {
                write(val);
                write(' ');
            }
        }

        public void writeln(int[] arr) {
            for (int val : arr) {
                writeln(val);
            }
        }

        public void print() {
            System.out.print(strb);
        }

        public void println() {
            System.out.println(strb);
        }
    }

    static class FastReader {
        BufferedReader br;
        StringTokenizer st;

        public FastReader() {
            br = new BufferedReader(new InputStreamReader(System.in));
        }

        String next() {
            while (st == null || !st.hasMoreElements()) {
                try {
                    st = new StringTokenizer(br.readLine());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return st.nextToken();
        }

        int nextInt() {
            return Integer.parseInt(next());
        }

        long nextLong() {
            return Long.parseLong(next());
        }

        double nextDouble() {
            return Double.parseDouble(next());
        }

        int[] readArray(int n) {
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = nextInt();
            return a;
        }

        double[] readArrayDouble(int n) {
            double[] a = new double[n];
            for (int i = 0; i < n; i++) a[i] = nextInt();
            return a;
        }

        String nextLine() {
            String str = "";
            try {
                str = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return str;
        }
    }
}
