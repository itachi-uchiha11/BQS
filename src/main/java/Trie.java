import java.util.HashMap;
import java.util.Map;

class Trie {
    private boolean printPrefixesFound=true;
    public void setPrintPrefixesFound(boolean b){
        printPrefixesFound = b;
    }
    private class TrieNode
    {
        Map<Character,TrieNode> children = new HashMap<>();
        boolean isEndOfWord;
        TrieNode(){
            isEndOfWord = false;
        }
    }
    private TrieNode root = new TrieNode();

    //return true if string doesn't have prefix present
    public boolean insert(String key)
    {
        int level;
        int length = key.length();
        char currChar;

        TrieNode pCrawl = root;

        for (level = 0; level < length; level++)
        {
            // if prefix encountered : no addition necessary
            if(pCrawl.isEndOfWord){
                //debugging purposes
                if(printPrefixesFound)System.out.println("String : "+key+"\nPrefix : "+key.substring(0,level));

                return false;
            }
            currChar = key.charAt(level);
            TrieNode node = pCrawl.children.get(currChar);
            if(node==null){
                pCrawl.children.put(currChar,new TrieNode());
            }
            pCrawl = pCrawl.children.get(currChar);
        }

        // if prefix encountered : no addition necessary
        if(pCrawl.isEndOfWord){
            //debugging purposes
            if(printPrefixesFound)System.out.println("String : "+key+"\nPrefix : "+key);
            return false;
        }
        pCrawl.isEndOfWord = true;
        return true;
    }
//    public static void main(String[] args){
//        Trie trie = new Trie();
//        String[] strings = {"str1","str12","str3","str324","str2","str2","str2","str","China-中国","China-中国ppp"};
//        for(String x:strings){
//            System.out.println(trie.insert(x)+" ");
//        }
//    }
}