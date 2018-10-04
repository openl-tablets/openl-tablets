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

    public static ThreadLocal<Map<IOpenClass, Map<Token, IOpenMethod[][]>>> openlClassRecursivelyCacheForGetterMethods = new ThreadLocal<Map<IOpenClass, Map<Token, IOpenMethod[][]>>>() {
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
        openlClassRecursivelyCacheForGetterMethods.remove();
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
                        
                        LinkedList<IOpenMethod> m = null;
                        for (Entry<Token, LinkedList<IOpenMethod>> entry : map.entrySet()) {
                            Token token = entry.getKey();
                            if (token.getValue().equals(t)) {
                                m = entry.getValue();
                                break;
                            }
                        }
                        
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

    public static Map<Token, IOpenMethod[][]> tokensMapToOpenClassSetterMethodsRecursively(IOpenClass openClass) {
        return tokensMapToOpenClassSetterMethodsRecursively(openClass, null);
    }

    public static Map<Token, IOpenMethod[][]> tokensMapToOpenClassSetterMethodsRecursively(IOpenClass openClass,
            String tokenPrefix) {
        return tokensMapToOpenClassMethodsRecursively(openClass, tokenPrefix, true);
    }

    public static Map<Token, IOpenMethod[][]> tokensMapToOpenClassGetterMethodsRecursively(IOpenClass openClass) {
        return tokensMapToOpenClassGetterMethodsRecursively(openClass, null);
    }

    public static Map<Token, IOpenMethod[][]> tokensMapToOpenClassGetterMethodsRecursively(IOpenClass openClass,
            String tokenPrefix) {
        return tokensMapToOpenClassMethodsRecursively(openClass, tokenPrefix, false);
    }
    
    @SuppressWarnings("unchecked")
    private static Map<Token, IOpenMethod[][]> tokensMapToOpenClassMethodsRecursively(IOpenClass openClass,
            String tokenPrefix, boolean setterMethods) {
        Map<IOpenClass, Map<Token, IOpenMethod[][]>> cache = null;
        if (setterMethods) {
            cache = openlClassRecursivelyCacheForSetterMethods.get();
        } else {
            cache = openlClassRecursivelyCacheForGetterMethods.get();
        }
        Map<Token, IOpenMethod[][]> ret = cache.get(openClass);
        if (ret == null) {
            Map<String, Integer> distanceMap = new HashMap<String, Integer>(); // For
                                                                               // optimization
            Map<Token, LinkedList<LinkedList<IOpenMethod>>> map = null;
            if (StringUtils.isBlank(tokenPrefix)) {
                map = buildTokensMapToOpenClassMethodsRecursively(openClass, distanceMap, 0, setterMethods);
            } else {
                map = buildTokensMapToOpenClassMethodsRecursively(openClass, distanceMap, 1, setterMethods);
                Map<Token, LinkedList<LinkedList<IOpenMethod>>> updatedMap = new HashMap<>();
                for (Entry<Token, LinkedList<LinkedList<IOpenMethod>>> entry : map.entrySet()) {
                    Token updatedToken = new Token(toTokenString(tokenPrefix + " " + entry.getKey().getValue()),
                        entry.getKey().getDistance());
                    updatedMap.put(updatedToken, entry.getValue());
                }
                map = updatedMap;
            }

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

    public static boolean isSetterMethod(IOpenMethod method) {
        return !method.isStatic() && method.getSignature().getNumberOfParameters() == 1 && method.getName()
            .startsWith("set");
    }
    
    public static boolean isGetterMethod(IOpenMethod method) {
        return !method.isStatic() && method.getSignature().getNumberOfParameters() == 0 && method.getName()
            .startsWith("get");
    }

    private static Map<Token, LinkedList<LinkedList<IOpenMethod>>> buildTokensMapToOpenClassMethodsRecursively(
            IOpenClass openClass,
            Map<String, Integer> distanceMap,
            int deepLevel,
            boolean setterMethods) {
        if (deepLevel >= DEEP_LEVEL) {
            return Collections.emptyMap();
        }
        Map<Token, LinkedList<LinkedList<IOpenMethod>>> ret = new HashMap<Token, LinkedList<LinkedList<IOpenMethod>>>();
        if (!openClass.isSimple()) {
            for (IOpenMethod method : openClass.getMethods()) {
                boolean g;
                if (setterMethods) {
                    g = isSetterMethod(method);
                } else {
                    g = isGetterMethod(method);
                }
                if (g) {
                    String t = OpenLFuzzySearch.toTokenString(method.getName().substring(3));
                    LinkedList<IOpenMethod> methods = new LinkedList<IOpenMethod>();
                    methods.add(method);
                    LinkedList<LinkedList<IOpenMethod>> x = null;
                    for (Entry<Token, LinkedList<LinkedList<IOpenMethod>>> entry : ret.entrySet()) {
                        Token token = entry.getKey();
                        if (token.getValue().equals(t)) {
                            x = entry.getValue();
                            break;
                        }
                    }
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
                    
                    IOpenClass type = null;
                    if (setterMethods) {
                        type = method.getSignature().getParameterType(0);
                    } else {
                        type = method.getType();
                    }

                    if (!type.isSimple() && !type.isArray()) {
                        Map<Token, LinkedList<LinkedList<IOpenMethod>>> map = buildTokensMapToOpenClassMethodsRecursively(
                            type,
                            distanceMap,
                            deepLevel + 1,
                            setterMethods);
                        for (Entry<Token, LinkedList<LinkedList<IOpenMethod>>> entry : map.entrySet()) {
                            String k = t + " " + entry.getKey().getValue();
                            LinkedList<LinkedList<IOpenMethod>> x1 = null;
                            for (Entry<Token, LinkedList<LinkedList<IOpenMethod>>> entry1 : ret.entrySet()) {
                                Token token = entry1.getKey();
                                if (token.getValue().equals(k)) {
                                    x1 = entry1.getValue();
                                    break;
                                }
                            }
                            
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
        for (String s : tokens) {
            if (s.length() == 1 && s.matches(pattern)) {
                g = true;
                sbBuilder.append(s);
            } else {
                if (g) {
                    t.add(sbBuilder.toString());
                    g = false;
                    sbBuilder = new StringBuilder();
                }
                t.add(s);
            }
        }
        if (g) {
            t.add(sbBuilder.toString());
        }
        return t.toArray(new String[] {});
    }

    private static String[] cleanUpTokens(String[] tokens) {
        List<String> t = new ArrayList<String>();
        for (String token : tokens) {
            String s = token.trim().toLowerCase();
            if (s.isEmpty()) {
                continue;
            }
            boolean f = false;
            for (int i = 0; i < s.length(); i++) {
                if (!Character.isLetterOrDigit(s.charAt(i))) {
                    f = true;
                    break;
                }
            }
            if (f) {
                continue;
            }
            t.add(token);
        }
        return t.toArray(new String[] {});
    }

    public static String toTokenString(String source) {
        if (source == null) {
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
        source = toTokenString(source);
        
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
            return new Token[] {};
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
