package org.openl.rules.indexer;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the input string for tokens. Consider space (' ') is a separator for words.
 * Cuts off the double brackets from expressions. If finds a
 * double bracket in the middle of the word, consider it as a separator for tokens. 
 */
public class IndexQueryParser {

    private List<String[]> included = new ArrayList<String[]>();
    
    /*
     * Excluded are expressions of several words, divided by dash
     */
    private List<String[]> excluded = new ArrayList<String[]>();

    private String input;
    
    // tokens in the input string 
    private String[] tokens = new String[100];
    private int nTokens = 0;    
    private int startPos = 0;
    private boolean wordStarted = false;
    private boolean openBracket = false;    
    private boolean letter = false;
    private boolean space = false;
    private boolean bracket = false;    

    private int pos = 0;

    private IndexQueryParser(String input) {
        this.input = input;
    }
    
    /**
     * Mark that the token was found. Add it to collection.
     */
    private void closeToken() {
        if (wordStarted) {
            tokens[nTokens++] = input.substring(startPos, pos);
            wordStarted = false;
        }
    }

    private void flushTokens() {
        if (nTokens != 0) {
            String[] newtokens = new String[nTokens];
            for (int i = 0; i < nTokens; i++) {
                newtokens[i] = tokens[i];
            }
            
            included.add(newtokens);
            openBracket = wordStarted = false;
            nTokens = 0;
        }        
    }
    
    /**
     * Mark the start position of a new word in the input string.
     */
    private void newToken() {
        startPos = pos;
        wordStarted = true;
    }
    
    /**
     * Parses the input string for tokens. Consider space (' ') is a separator for words.
     * Cuts off the double brackets from expressions. If finds a 
     * double bracket in the middle of the word, consider it as a separator for tokens.
     * If input string is bordered with curly brackets, process it as a query.   
     * 
     * @return {@link IndexQuery}
     */
    public static IndexQuery parse(String strInput) {
        IndexQueryParser iqp = new IndexQueryParser(strInput);
        IndexQuery iq = iqp.parse();
        return iq;        
    }
    
    private IndexQuery parse() {
        for (; pos < input.length(); pos++) {
            letter = space = bracket = false;
            char charAtPos = input.charAt(pos);

            if (charAtPos == '"') {
                bracket = true;
            } else if (charAtPos == ' ') {
                space = true;
            } else {
                letter = true;
            }

            if (!wordStarted && letter) {
                newToken();
            } else if (!openBracket && wordStarted && space) { //when next to last letter
                closeToken();
                flushTokens();
            } else if (openBracket && wordStarted && space) {
                closeToken();
            } else if (!openBracket && bracket) {
                closeToken();
                flushTokens();
                openBracket = true;
            } else if (openBracket && bracket) {
                closeToken();
                flushTokens();
                openBracket = false;
            } 
        }

        closeToken();
        flushTokens();

        IndexQuery iq = new IndexQuery(included.toArray(new String[0][]),  excluded
                .toArray(new String[0][]), null);

        return iq;
    }
    
}
