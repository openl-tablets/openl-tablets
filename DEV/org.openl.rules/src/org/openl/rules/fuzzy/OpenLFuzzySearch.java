package org.openl.rules.fuzzy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

public final class OpenLFuzzySearch {

    private static final int ACCEPTABLE_LEVENSTAIN_DISTANCE = 90;

    private static int calculateLevensteinDistance(String s1, String s2) {
        int distance = StringUtils.getLevenshteinDistance(s1, s2);
        double ratio = ((double) distance) / (Math.max(s1.length(), s2.length()));
        return 100 - new Double(ratio * 100).intValue();
    }
    
    public static Map<String, IOpenMethod> tokensMapToOpenClassSetterMethods(IOpenClass openClass) {
        Map<String, IOpenMethod> ret = new HashMap<String, IOpenMethod>();
        for (IOpenMethod method : openClass.getMethods()) {
            if (!method.isStatic() && method.getSignature().getNumberOfParameters() == 1 && method.getName()
                .startsWith("set")) {
                ret.put(OpenLFuzzySearch.toTokensString(method.getName().substring(3)), method);
            }
        }
        return ret;
    }

    private static String[] cleanUpTokens(String[] tokens) {
        List<String> t = new ArrayList<String>();
        for (String token : tokens) {
            String s = token.trim().toLowerCase();
            if (s.isEmpty()) {
                continue;
            }
            if (!s.matches("[a-z0-9_]*")) {
                continue;
            }
            t.add(token);
        }
        return t.toArray(new String[] {});
    }

    public static String toTokensString(String source) {
        String[] tokens = source.split("(?<=.)(?=\\p{Lu}|\\d|\\s|[_])");
        tokens = cleanUpTokens(tokens);
        StringBuilder sb = new StringBuilder();
        boolean f = false;
        for (String s : tokens) {
            if (!f) {
                f = true;
            } else {
                sb.append(" ");
            }
            sb.append(s.trim().toLowerCase());
        }
        return sb.toString();
    }

    public static String[] openlFuzzyExtract(String source, String[] tokens) {
        String[] sourceTokens = source.split(" ");

        String[][] tokensList = new String[tokens.length][];
        for (int i = 0; i < tokens.length; i++) {
            tokensList[i] = tokens[i].split(" ");
        }

        String[] sortedSourceTokens = new String[sourceTokens.length];
        System.arraycopy(sourceTokens, 0, sortedSourceTokens, 0, sourceTokens.length);
        Arrays.sort(sortedSourceTokens);
        int f[] = new int[tokensList.length];
        int max = 0;
        for (int i = 0; i < tokensList.length; i++) {
            String[] sortedTokens = new String[tokensList[i].length];
            System.arraycopy(tokensList[i], 0, sortedTokens, 0, tokensList[i].length);
            Arrays.sort(sortedTokens);
            int l = 0;
            int r = 0;
            int c = 0;
            while (l < sortedSourceTokens.length && r < sortedTokens.length) {
                int d = calculateLevensteinDistance(sortedSourceTokens[l], sortedTokens[r]);
                if (d > ACCEPTABLE_LEVENSTAIN_DISTANCE) {
                    l++;
                    r++;
                    c++;
                } else {
                    if (sortedSourceTokens[l].compareTo(sortedTokens[r]) < 0) {
                        l++;
                    } else {
                        r++;
                    }
                }
            }
            if (max < c) {
                max = c;
            }
            f[i] = c;
        }
        
        if (max == 0){
            return new String[]{};
        }
        
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < tokensList.length; i++) {
            if (f[i] == max) {
                if (min > tokensList[i].length - f[i]) {
                    min = tokensList[i].length - f[i];
                }
            }
        }

        int count = 0;
        for (int i = 0; i < tokensList.length; i++) {
            if (f[i] == max && tokensList[i].length - f[i] == min) {
                count++;
            }
        }
        if (count == 0) {
            return new String[] {};
        }
        if (count == 1) {
            for (int i = 0; i < tokensList.length; i++) {
                if (f[i] == max && tokensList[i].length - f[i] == min) {
                    return new String[] { tokens[i] };
                }
            }
        } else {
            List<String> ret = new ArrayList<String>();
            int best = 0;
            for (int i = 0; i < tokensList.length; i++) {
                if (f[i] == max && tokensList[i].length - f[i] == min) {
                    int d = StringUtils.getFuzzyDistance(tokens[i], source, Locale.ENGLISH);
                    if (d > best) {
                        best = d;
                        ret.clear();
                        ret.add(tokens[i]);
                    } else {
                        if (d == best) {
                            ret.add(tokens[i]);
                        }
                    }
                }
            }
            return ret.toArray(new String[] {});
        }
        return new String[] {};
    }

}
