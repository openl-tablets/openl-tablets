/*
 * Created on May 24, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.rules.helpers;

/**
 * @author snshor
 */

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Util {
    public static final String DEFAULT_DOUBLE_FORMAT = "#,##0.00";
    
    public static boolean contains(Object[] ary, Object obj) {
        if (obj == null || ary == null) {
            return false;
        }        
        return Arrays.asList(ary).contains(obj);
    }
    
    public static boolean contains(int[] array, int elem) {
        if (array == null) {             
            return false;
        } 
                
        for (int arrayElememt : array) {
            if (arrayElememt == elem) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean contains(long[] array, long elem) {
        if (array == null) {             
            return false;
        } 
                
        for (long arrayElememt : array) {
            if (arrayElememt == elem) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean contains(byte[] array, byte elem) {
        if (array == null) {             
            return false;
        } 
                
        for (byte arrayElememt : array) {
            if (arrayElememt == elem) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean contains(short[] array, short elem) {
        if (array == null) {             
            return false;
        } 
                
        for (short arrayElememt : array) {
            if (arrayElememt == elem) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean contains(char[] array, char elem) {
        if (array == null) {             
            return false;
        } 
                
        for (char arrayElememt : array) {
            if (arrayElememt == elem) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean contains(float[] array, float elem) {
        if (array == null) {             
            return false;
        } 
                
        for (float arrayElememt : array) {
            if (arrayElememt == elem) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean contains(double[] array, double elem) {
        if (array == null) {             
            return false;
        } 
                
        for (double arrayElememt : array) {
            if (arrayElememt == elem) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean contains(boolean[] array, boolean elem) {
        if (array == null) {             
            return false;
        } 
                
        for (boolean arrayElememt : array) {
            if (arrayElememt == elem) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean contains(Object[] ary1, Object[] ary2) {
        if (ary1 == null || ary2 == null) {
            return false;
        }        
        return Arrays.asList(ary1).containsAll(Arrays.asList(ary2));
    }
    
    public static boolean contains(int[] ary1, int[] ary2) {
        if (ary1 == null || ary2 == null) {
            return false;
        }
        
        for (int arrayElement : ary2) {
            if(!contains(ary1, arrayElement)) {
                return false;
            }
        }        
        return true;   
    }
    
    public static boolean contains(byte[] ary1, byte[] ary2) {
        if (ary1 == null || ary2 == null) {
            return false;
        }
        
        for (byte arrayElement : ary2) {
            if(!contains(ary1, arrayElement)) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean contains(short[] ary1, short[] ary2) {
        if (ary1 == null || ary2 == null) {
            return false;
        }
        
        for (short arrayElement : ary2) {
            if(!contains(ary1, arrayElement)) {
                return false;
            }
        }        
        return true;
    }
    
    public static boolean contains(long[] ary1, long[] ary2) {
        if (ary1 == null || ary2 == null) {
            return false;
        }
        
        for (long arrayElement : ary2) {
            if(!contains(ary1, arrayElement)) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean contains(char[] ary1, char[] ary2) {
        if (ary1 == null || ary2 == null) {
            return false;
        }
        
        for (char arrayElement : ary2) {
            if(!contains(ary1, arrayElement)) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean contains(float[] ary1, float[] ary2) {
        if (ary1 == null || ary2 == null) {
            return false;
        }
        
        for (float arrayElement : ary2) {
            if(!contains(ary1, arrayElement)) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean contains(String[] ary1, String[] ary2) {
        if (ary1 == null || ary2 == null) {
            return false;
        }
        
        for (String arrayElement : ary2) {
            if(!contains(ary1, arrayElement)) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean contains(double[] ary1, double[] ary2) {
        if (ary1 == null || ary2 == null) {
            return false;
        }
        
        for (double arrayElement : ary2) {
            if(!contains(ary1, arrayElement)) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean contains(boolean[] ary1, boolean[] ary2) {
        if (ary1 == null || ary2 == null) {
            return false;
        }
        for (boolean arrayElement : ary2) {
            if(!contains(ary1, arrayElement)) {
                return false;
            }
        }
        return true;
    }

    public static void error(String msg) {
        throw new RuntimeException(msg);
    }

    public static void error(Throwable t) throws Throwable {
        throw t;
    }

    public static String format(Date date) {
        return format(date, null);
    }

    public static String format(Date date, String format) {
        DateFormat df = format == null ? DateFormat.getDateInstance(DateFormat.SHORT) : new SimpleDateFormat(format);
        return df.format(date);
    }

    public static String format(double d) {
        return format(d, DEFAULT_DOUBLE_FORMAT);
    }

    public static String format(double d, String fmt) {
        DecimalFormat df = new DecimalFormat(fmt);
        return df.format(d);
    }

    public static String[] intersection(String[] ary1, String[] ary2) {
        List<String> v = new ArrayList<String>();
        for (int j = 0; j < ary2.length; ++j) {
            if (contains(ary1, ary2[j])) {
                v.add(ary2[j]);
            }
        }
        return  v.toArray(new String[v.size()]);
    }

    public static void out(String output) {
        System.out.println(output);
    }

    public static double parseFormattedDouble(String s) throws ParseException {
        return parseFormattedDouble(s, DEFAULT_DOUBLE_FORMAT);
    }

    public static double parseFormattedDouble(String s, String fmt) throws ParseException {
        DecimalFormat df = new DecimalFormat(fmt);
        return df.parse(s).doubleValue();
    }

    /*
     *
     * public static boolean eval(String code) { OpenL language =
     * OpenL.getInstance("org.openl.j");//? Object result = language.
     * evaluate(new StringSourceCodeModule(code, null)); return result; ??? }
     */
}
