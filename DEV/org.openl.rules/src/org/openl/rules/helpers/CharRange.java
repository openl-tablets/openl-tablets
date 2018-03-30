package org.openl.rules.helpers;

import java.util.regex.Pattern;

public class CharRange extends IntRange {

    public CharRange(char min, char max) {
        super(min, max);
    }

    public CharRange(char c) {
        super((int) c);
    }

    static private Pattern p0 = Pattern.compile("\\S");
    static private Pattern p1 = Pattern.compile("\\S\\-\\S");
    static private Pattern p2 = Pattern.compile("\\S\\+");
    static private Pattern p3 = Pattern.compile("<\\S");

    public CharRange(String range) {
        super(0, 0);
        if (range == null)
            throw new NullPointerException("CharRange value can not be null");

        ParseStruct parsed = parseRange(range);
        min = parsed.min;
        max = parsed.max;
    }

    private static ParseStruct parseRange(String range) {
        if (p1.matcher(range).matches()) {
            return new ParseStruct(range.charAt(0), range.charAt(2));
        }

        if (p2.matcher(range).matches()) {
            return new ParseStruct(range.charAt(0), Character.MAX_VALUE);
        }

        if (p3.matcher(range).matches()) {
            return new ParseStruct( Character.MIN_VALUE, (char)(range.charAt(1)-1));
        }
        if (p0.matcher(range).matches()) {
            return new ParseStruct( range.charAt(0), range.charAt(0));
        }
        throw new RuntimeException("Invalid Char Range: " + range);
    }

    private static class ParseStruct {
        char min, max;

        public ParseStruct(char min, char max) {
            super();
            this.min = min;
            this.max = max;
        }
    }

    @Override
    public String toString() {
        
        return printChar(min) + "-" + printChar(max);
    }

    private String printChar(int ch) {
        return isPrintable(ch) ? String.valueOf((char)ch) : "'u" +Integer.toHexString(ch)+"'" ;
    }

    private boolean isPrintable(int ch) {
        if (Character.isWhitespace(ch) || Character.isISOControl(ch))
            return false;
        
        if (ch < 255)
            return true;
        if (Character.isUnicodeIdentifierPart(ch))
            return true;
        return false;
    }
    
    public static CharRange autocast(char x, CharRange y) {
        return new CharRange(x);
    }
    
}
