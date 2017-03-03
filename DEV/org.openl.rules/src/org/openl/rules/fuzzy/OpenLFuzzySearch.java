package org.openl.rules.fuzzy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

public final class OpenLFuzzySearch {

    private static final double ACCEPTABLE_SIMILARITY_VALUE = 0.85;
    private static final int DEEP_LEVEL = 5;

    public static ThreadLocal<Map<IOpenClass, Map<Token, IOpenMethod[][]>>> openlClassRecursivelyCacheForSetterMethods = new ThreadLocal<Map<IOpenClass, Map<Token, IOpenMethod[][]>>>() {
        @Override
        protected Map<IOpenClass, Map<Token, IOpenMethod[][]>> initialValue() {
            return new HashMap<IOpenClass, Map<Token, IOpenMethod[][]>>();
        }
    };

    public static ThreadLocal<Map<IOpenClass, Map<Token, IOpenMethod[]>>> openlClassCacheForSetterMethods = new ThreadLocal<Map<IOpenClass, Map<Token, IOpenMethod[]>>>() {
        @Override
        protected Map<IOpenClass, Map<Token, IOpenMethod[]>> initialValue() {
            return new HashMap<IOpenClass, Map<Token, IOpenMethod[]>>();
        }
    };

    public static void clearCaches() {
        openlClassCacheForSetterMethods.remove();
        openlClassRecursivelyCacheForSetterMethods.remove();
    }

    public static Map<Token, IOpenMethod[]> tokensMapToOpenClassSetterMethods(IOpenClass openClass) {
        Map<IOpenClass, Map<Token, IOpenMethod[]>> cache = openlClassCacheForSetterMethods.get();
        Map<Token, IOpenMethod[]> ret = cache.get(openClass);
        if (ret == null) {
            ret = new HashMap<Token, IOpenMethod[]>();
            Map<Token, LinkedList<IOpenMethod>> map = new HashMap<Token, LinkedList<IOpenMethod>>();
            if (!openClass.isSimple()) {
                for (IOpenMethod method : openClass.getMethods()) {
                    if (!method.isStatic() && method.getSignature().getNumberOfParameters() == 1 && method.getName()
                        .startsWith("set")) {
                        String t = OpenLFuzzySearch.toTokenString(method.getName().substring(3));
                        LinkedList<IOpenMethod> m = map.get(t);
                        if (m == null) {
                            m = new LinkedList<IOpenMethod>();
                            m.add(method);
                            map.put(new Token(t, 0), m);
                        } else {
                            m.add(method);
                        }
                    }
                }
            }
            for (Entry<Token, LinkedList<IOpenMethod>> entry : map.entrySet()) {
                ret.put(entry.getKey(), entry.getValue().toArray(new IOpenMethod[] {}));
            }
            cache.put(openClass, ret);
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public static Map<Token, IOpenMethod[][]> tokensMapToOpenClassSetterMethodsRecursively(IOpenClass openClass) {
        Map<IOpenClass, Map<Token, IOpenMethod[][]>> cache = openlClassRecursivelyCacheForSetterMethods.get();
        Map<Token, IOpenMethod[][]> ret = cache.get(openClass);
        if (ret == null) {
            Map<String, Integer> distanceMap = new HashMap<String, Integer>(); // For
                                                                               // optimization
            Map<Token, LinkedList<LinkedList<IOpenMethod>>> map = buildTokensMapToOpenClassSetterMethodsRecursively(
                openClass, distanceMap, 0);

            Map<Token, LinkedList<IOpenMethod>[]> tmp = new HashMap<Token, LinkedList<IOpenMethod>[]>();
            for (Entry<Token, LinkedList<LinkedList<IOpenMethod>>> entry : map.entrySet()) {
                tmp.put(entry.getKey(), entry.getValue().toArray(new LinkedList[] {}));
            }

            ret = new HashMap<Token, IOpenMethod[][]>();
            for (Entry<Token, LinkedList<IOpenMethod>[]> entry : tmp.entrySet()) {
                IOpenMethod[][] m = new IOpenMethod[entry.getValue().length][];
                int i = 0;
                for (LinkedList<IOpenMethod> x : entry.getValue()) {
                    m[i] = x.toArray(new IOpenMethod[] {});
                    i++;
                }
                ret.put(entry.getKey(), m);
            }
            cache.put(openClass, ret);
        }
        return ret;
    }

    private static boolean isSetterMethod(IOpenMethod method) {
        return !method.isStatic() && method.getSignature().getNumberOfParameters() == 1 && method.getName()
            .startsWith("set");
    }

    private static Map<Token, LinkedList<LinkedList<IOpenMethod>>> buildTokensMapToOpenClassSetterMethodsRecursively(
            IOpenClass openClass, Map<String, Integer> distanceMap, int deepLevel) {
        if (deepLevel >= DEEP_LEVEL) {
            return Collections.emptyMap();
        }
        Map<Token, LinkedList<LinkedList<IOpenMethod>>> ret = new HashMap<Token, LinkedList<LinkedList<IOpenMethod>>>();
        if (!openClass.isSimple()) {
            for (IOpenMethod method : openClass.getMethods()) {
                if (isSetterMethod(method)) {
                    String t = OpenLFuzzySearch.toTokenString(method.getName().substring(3));
                    LinkedList<IOpenMethod> methods = new LinkedList<IOpenMethod>();
                    methods.add(method);
                    LinkedList<LinkedList<IOpenMethod>> x = ret.get(t);
                    if (x == null) {
                        x = new LinkedList<LinkedList<IOpenMethod>>();
                        ret.put(new Token(t, deepLevel), x);
                        x.add(methods);
                        distanceMap.put(t, deepLevel);
                    } else {
                        Integer d = distanceMap.get(t);
                        if (d == null || d == deepLevel) {
                            x.add(methods);
                        } else {
                            if (d < deepLevel) {
                                x.clear();
                                x.add(methods);
                                distanceMap.put(t, deepLevel);
                            }
                        }
                    }

                    if (!method.getSignature().getParameterType(0).isSimple() && !method.getSignature()
                        .getParameterType(0)
                        .isArray()) {
                        Map<Token, LinkedList<LinkedList<IOpenMethod>>> map = buildTokensMapToOpenClassSetterMethodsRecursively(
                            method.getSignature().getParameterType(0), distanceMap, deepLevel + 1);
                        for (Entry<Token, LinkedList<LinkedList<IOpenMethod>>> entry : map.entrySet()) {
                            String k = t + " " + entry.getKey().getValue();
                            LinkedList<LinkedList<IOpenMethod>> x1 = ret.get(k);
                            for (LinkedList<IOpenMethod> y : entry.getValue()) {
                                y.addFirst(method);
                                if (x1 == null) {
                                    x1 = new LinkedList<LinkedList<IOpenMethod>>();
                                    x1.add(y);
                                    ret.put(new Token(k, entry.getKey().getDistance() + 1), x1);
                                    distanceMap.put(k, entry.getKey().getDistance() + 1);
                                } else {
                                    Integer d = distanceMap.get(k);
                                    if (d == null || d == entry.getKey().getDistance() + 1) {
                                        x1.add(y);
                                    } else {
                                        if (d < entry.getKey().getDistance() + 1) {
                                            x1.clear();
                                            x1.add(y);
                                            distanceMap.put(k, entry.getKey().getDistance() + 1);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return ret;
    }
    
    private static String[] concatTokens(String[] tokens, String pattern) {
        List<String> t = new ArrayList<String>();
        StringBuilder sbBuilder = new StringBuilder();
        boolean g = false;
        for (String s : tokens){
            if (s.length() == 1 && s.matches(pattern)){
                g = true;
                sbBuilder.append(s);
            } else {
                if (g){
                    t.add(sbBuilder.toString());
                    g = false;
                    sbBuilder = new StringBuilder();
                }
                t.add(s);
            }
        }
        if (g){
            t.add(sbBuilder.toString());
        }
        return t.toArray(new String[]{});
    }

    private static String[] cleanUpTokens(String[] tokens) {
        List<String> t = new ArrayList<String>();
        for (String token : tokens) {
            String s = token.trim().toLowerCase();
            if (s.isEmpty()) {
                continue;
            }
            if (!s.matches("[\\p{IsAlphabetic}\\d]*")) {
                continue;
            }
            t.add(token);
        }
        return t.toArray(new String[] {});
    }

    public static String toTokenString(String source) {
        if (source == null){
            return StringUtils.EMPTY;
        }
        String[] tokens = source.split("(?<=.)(?=\\p{Lu}|\\d|\\s|[_])");
        
        tokens = concatTokens(tokens, "\\p{Lu}+");
        tokens = concatTokens(tokens, "\\d+");
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
    
    public static Token[] openlFuzzyExtract(String source, Token[] tokens) {
        String[] sourceTokens = source.split(" ");

        String[][] tokensList = new String[tokens.length][];
        for (int i = 0; i < tokens.length; i++) {
            tokensList[i] = tokens[i].getValue().split(" ");
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
                double d = StringUtils.getJaroWinklerDistance(sortedSourceTokens[l], sortedTokens[r]);
                if (d > ACCEPTABLE_SIMILARITY_VALUE) {
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

        if (max == 0) {
            return new Token[]{};
        }

        int min = Integer.MAX_VALUE;
        int minDistance = Integer.MAX_VALUE;
        for (int i = 0; i < tokensList.length; i++) {
            if (f[i] == max) {
                if (min > tokensList[i].length - f[i]) {
                    min = tokensList[i].length - f[i];
                }
                if (minDistance > tokens[i].getDistance()) {
                    minDistance = tokens[i].getDistance();
                }
            }
        }

        int count = 0;
        for (int i = 0; i < tokensList.length; i++) {
            if (f[i] == max && tokensList[i].length - f[i] == min && tokens[i].getDistance() == minDistance) {
                count++;
            }
        }
        if (count == 0) {
            return new Token[] {};
        }
        if (count == 1) {
            for (int i = 0; i < tokensList.length; i++) {
                if (f[i] == max && tokensList[i].length - f[i] == min && tokens[i].getDistance() == minDistance) {
                    return new Token[] { tokens[i] };
                }
            }
        } else {
            List<Token> ret = new ArrayList<Token>();
            int best = 0;
            for (int i = 0; i < tokensList.length; i++) {
                if (f[i] == max && tokensList[i].length - f[i] == min) {
                    int d = StringUtils.getFuzzyDistance(tokens[i].getValue(), source, Locale.ENGLISH);
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
            return ret.toArray(new Token[] {});
        }
        return new Token[] {};
    }

}
