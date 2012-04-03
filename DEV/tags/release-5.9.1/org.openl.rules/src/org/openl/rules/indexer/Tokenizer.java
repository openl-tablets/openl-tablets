/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.indexer;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds all words and numbers in the string and returns array of them (in the
 * sequence they were found). <br />
 * 
 * Word starts from letter and can contain letters, digits and "_", "#", "&"
 * symbols.<br />
 * Number starts from digit and can contain digits, ".", "%" symbols.<br />
 * 
 * All other symbols are ignored and are considered delimiters between words and
 * numbers.<br />
 * 
 * @author snshor
 * 
 * <br />
 * 
 */
public class Tokenizer {

    /**
     * Default constructor
     */
    private Tokenizer() {
    }

    private static String getAlphanumericToken(String textToTokenize, int startPos) {        
        int pos = startPos + 1;

        for (; pos < textToTokenize.length(); pos++) {
            char c = textToTokenize.charAt(pos);

            if (!(Character.isLetterOrDigit(c) || c == '_' || c == '#' || c == '&')) {
                break;
            }
        }
        return textToTokenize.substring(startPos, pos);
    }

    private static String getNumberToken(String textToTokenize, int startPos) {
        int pos = startPos + 1;

        for (; pos < textToTokenize.length(); pos++) {
            char c = textToTokenize.charAt(pos);

            if (!(Character.isDigit(c) || c == '.' || c == '%')) {
                break;
            }
        }

        return textToTokenize.substring(startPos, pos);
    }

    /**
     * Finds all words and numbers in the string and returns array of them (in
     * the sequence they were found). All other symbols are ignored and are
     * considered delimiters between words and numbers.<br />
     * 
     * Word starts from letter and can contain letters, digits and "_", "#", "&"
     * symbols.<br />
     * Number starts from digit and can contain digits, ".", "%" symbols.<br />
     * 
     * @return Sequence of tokens in the text
     * 
     */
    public static String[] parse(String textToTokenize) {
        List<String> tokens = new ArrayList<String>();
        
        tokens.add(textToTokenize);
        
        if(textIsComposite(textToTokenize)){
            tokens.add(textToTokenize);
        }
        for (int pos = 0; pos < textToTokenize.length(); pos++) {
            char currentSymbol = textToTokenize.charAt(pos);
            String currentToken = null;

            if (Character.isLetter(currentSymbol)) {                
                currentToken = getAlphanumericToken(textToTokenize, pos);
            } else if (Character.isDigit(currentSymbol)) {
                currentToken = getNumberToken(textToTokenize, pos);
            }

            if (currentToken != null) {
                tokens.add(currentToken);
                pos += currentToken.length();
            }
        }

        return (String[]) tokens.toArray(new String[tokens.size()]);
    }
    
    /**
     * Check if the given text is a composite word. It means contains of several words, 
     * separated by dash symbol ('-'). 
     * @param textToTokenize 
     * @return
     */
    private static boolean textIsComposite(String textToTokenize) {
        boolean result = false;
        for(int pos = 0; pos < textToTokenize.length(); pos++) {
            int indexOfDash = textToTokenize.indexOf('-', pos);
            if(indexOfDash > 0) {
                char charBeforeDash = textToTokenize.charAt(indexOfDash-1);
                if(indexOfDash+1 < textToTokenize.length()) {
                    char charAfterDash = textToTokenize.charAt(indexOfDash+1);
                    if(Character.isLetter(charBeforeDash) && Character.isLetter(charAfterDash)) {
                        result = true;
                        pos = indexOfDash + 1;
                    }
                }
            }               
        }
        return result;         
    }

}
