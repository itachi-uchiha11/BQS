class Trie {
    static final int ALPHABET_SIZE = 260;
    private boolean printPrefixesFound=true;
    public void setPrintPrefixesFound(boolean b){
        printPrefixesFound = b;
    }
    private class TrieNode
    {
        TrieNode[] children = new TrieNode[ALPHABET_SIZE];
        boolean isEndOfWord;
        TrieNode(){
            isEndOfWord = false;
            for (int i = 0; i < ALPHABET_SIZE; i++)
                children[i] = null;
        }
    }
    private TrieNode root = new TrieNode();

    //return true if string doesn't have prefix present
    public boolean insert(String key)
    {
        int level;
        int length = key.length();
        int index;

        TrieNode pCrawl = root;

        for (level = 0; level < length; level++)
        {
            // if prefix encountered : no addition necessary
            if(pCrawl.isEndOfWord){
                //debugging purposes
                if(printPrefixesFound)System.out.println("String : "+key+"\nPrefix : "+key.substring(0,level));

                return false;
            }
            index = key.charAt(level) - 'a'+97;
            if (pCrawl.children[index] == null)
                pCrawl.children[index] = new TrieNode();

            pCrawl = pCrawl.children[index];
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
}