package org.openl.rules.helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CharRange extends IntRange {

    public CharRange(char min, char max) {
        super(min, max);
    }

    public CharRange(char c) {
        super((int) c);
    }

    //ADD support for all formats
    static private Pattern p0 = Pattern.compile("\\s*(\\S)\\s*");
    static private Pattern p1 = Pattern.compile("\\s*(\\S)\\s*(?:\\-|\\.\\.)\\s*(\\S)\\s*");
    
    static private Pattern p2 = Pattern.compile("\\s*(\\S)\\s*\\+\\s*"); 
    
    static private Pattern p3 = Pattern.compile("\\s*(<|>|>=|<=)\\s*(\\S)\\s*");
    
    static private Pattern p4 = Pattern.compile("\\s*\\[\\s*(\\S)\\s*(?:\\;|\\.\\.)\\s*(\\S)\\s*\\]\\s*");

    public CharRange(String range) {
        super(0, 0);
        if (range == null)
            throw new NullPointerException("CharRange value can not be null");

        ParseStruct parsed = parseRange(range);
        min = parsed.min;
        max = parsed.max;
    }

    public static ParseStruct parseRange(String range) {
        Matcher m1 = p1.matcher(range);
        if (m1.matches()) {
            String s1 = m1.group(1);
            String s2 = m1.group(2);
            return new ParseStruct(s1.charAt(0), s2.charAt(0));
        }

        Matcher m4 = p4.matcher(range);
        if (m4.matches()) {
            String s1 = m4.group(1);
            String s2 = m4.group(2);
            return new ParseStruct(s1.charAt(0), s2.charAt(0));
        }

        Matcher m2 = p2.matcher(range);
        if (m2.matches()) {
            String s = m2.group(1);
            return new ParseStruct(s.charAt(0), Character.MAX_VALUE);
        }

        Matcher m3 = p3.matcher(range);
        if (m3.matches()) {
            String q = m3.group(1);
            String s = m3.group(2);
            if (q.length() == 1 && q.charAt(0) == '<') {
                return new ParseStruct(Character.MIN_VALUE, (char) (s.charAt(0) - 1));
            }
            if (q.length() > 1 && q.charAt(0) == '<') {
                return new ParseStruct(Character.MIN_VALUE, (char) (s.charAt(0)));
            }
            if (q.length() == 1 && q.charAt(0) == '>') {
                return new ParseStruct((char) (s.charAt(0) + 1), Character.MAX_VALUE);
            }
            if (q.length() > 1 && q.charAt(0) == '>') {
                return new ParseStruct((char) (s.charAt(0)), Character.MAX_VALUE);
            }
        }
        
        Matcher m0 = p0.matcher(range);
        if (m0.matches()) {
            String s = m0.group(1);
            return new ParseStruct(s.charAt(0), s.charAt(0));
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
