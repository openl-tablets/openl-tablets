package org.openl.rules.tableeditor.model.ui.util;

/**
 * Finds the given array of strings in the text and highlight it with <b> tags 
 * for further displaying on UI. Finds detached words and parts of the words.
 */
public class StringHighlighter {
    
    private final String BOLD_OPEN = "<b>";
    private final String BOLD_CLOSE = "</b>";

    // tokens to be highlighted in the text
    private String[] tokensToHighlight;

    //last found token as it is in text
    private String currentToken;

    //text for highlighting
    private String text;

    //index of last found word
    private int lastSelected = -1;

    public StringHighlighter(String[] tokens, String src) {           
        this.tokensToHighlight = tokens;
        this.text = src;
    }

    private int findTokenFromPos(int startPos) {
        int textLength = text.length();
        lastSelected = -1;

        for (int i = tokensToHighlight.length - 1; i >= 0; i--) {                
            int idx = findToken(tokensToHighlight[i].toLowerCase(), startPos);
            if (idx >=0 && idx < textLength) {
                lastSelected = i;
                textLength = idx;                    
            }
        }

        if (lastSelected < 0) {
            return -1;
        }
        currentToken = text.substring(textLength, textLength + tokensToHighlight[lastSelected].length()); 
        return textLength;
    }

    /**
     * 
     * Searches the start index of a token in the text from the given position.
     * If no such token exists in the text, then -1 is returned.
     * @param token Token to be found.
     * @param startPos Position in the text to start looking for a token.
     * @return Index in the text where token starts.
     */
    private int findToken(String token, int startPos) {
        String text = this.text;
        int tokenIndex = 0;
        tokenIndex = text.toLowerCase().indexOf(token, startPos);                
        return tokenIndex;            
    }

    /**
     * Highlight strings in the text. Find  
     * detached words and parts of words.
     * @return Highlighted strings in text. For further use on UI.
     */
    public String highlightStringsInText() {
        StringBuffer buf = new StringBuffer();
        int startPos = 0;
        for (;;) {
            int nextStart = findTokenFromPos(startPos);
            if (nextStart < 0) {
                buf.append(text.substring(startPos));
                return buf.toString();
            }
            buf.append(text.substring(startPos, nextStart));
            buf.append(BOLD_OPEN);
            buf.append(currentToken);
            buf.append(BOLD_CLOSE);
            startPos = nextStart + tokensToHighlight[lastSelected].length();
        }
    }
}