package org.openl.rules.fuzzy;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;

public final class OpenLFuzzyUtils {

    private static final double ACCEPTABLE_SIMILARITY_VALUE = 0.85;
    private static final int DEEP_LEVEL = 5;

    private static final ThreadLocal<Map<IOpenClass, Map<String, Map<Token, IOpenMethod[][]>>>> openlClassRecursivelyCacheForSetterMethods = ThreadLocal
        .withInitial(HashMap::new);

    private static final ThreadLocal<Map<IOpenClass, Map<String, Map<Token, IOpenMethod[][]>>>> openlClassRecursivelyCacheForGetterMethods = ThreadLocal
        .withInitial(HashMap::new);

    private static final ThreadLocal<Map<IOpenClass, Map<Token, IOpenMethod[]>>> openlClassCacheForSetterMethods = ThreadLocal
        .withInitial(HashMap::new);

    public static void clearCaches() {
        openlClassCacheForSetterMethods.remove();
        openlClassRecursivelyCacheForGetterMethods.remove();
        openlClassRecursivelyCacheForSetterMethods.remove();
    }

    public static Map<Token, IOpenMethod[]> tokensMapToOpenClassSetterMethods(IOpenClass openClass) {
        Map<IOpenClass, Map<Token, IOpenMethod[]>> cache = openlClassCacheForSetterMethods.get();
        Map<Token, IOpenMethod[]> ret = cache.get(openClass);
        if (ret == null) {
            ret = new HashMap<>();
            Map<Token, LinkedList<IOpenMethod>> map = new HashMap<>();
            if (!openClass.isSimple()) {
                for (IOpenMethod method : openClass.getMethods()) {
                    if (!method.isStatic() && method.getSignature().getNumberOfParameters() == 1 && method.getName()
                        .startsWith("set")) {
                        String fieldName = method.getName().substring(3);
                        IOpenField openField = openClass.getField(fieldName, false); // Support only Java Beans
                        if (openField == null) {
                            continue;
                        }

                        String t = OpenLFuzzyUtils.toTokenString(fieldName);

                        LinkedList<IOpenMethod> m = null;
                        for (Entry<Token, LinkedList<IOpenMethod>> entry : map.entrySet()) {
                            Token token = entry.getKey();
                            if (token.getValue().equals(t)) {
                                m = entry.getValue();
                                break;
                            }
                        }

                        if (m == null) {
                            m = new LinkedList<>();
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
            String tokenPrefix,
            boolean setterMethods) {
        Map<IOpenClass, Map<String, Map<Token, IOpenMethod[][]>>> cache = null;
        if (setterMethods) {
            cache = openlClassRecursivelyCacheForSetterMethods.get();
        } else {
            cache = openlClassRecursivelyCacheForGetterMethods.get();
        }
        Map<String, Map<Token, IOpenMethod[][]>> cache1 = cache.computeIfAbsent(openClass, e -> new HashMap<>());
        final String tokenizedPrefix = toTokenString(tokenPrefix);
        Map<Token, IOpenMethod[][]> ret = cache1.get(tokenizedPrefix);
        if (ret == null) {
            Map<Token, LinkedList<LinkedList<IOpenMethod>>> map = null;
            if (StringUtils.isBlank(tokenPrefix)) {
                map = buildTokensMapToOpenClassMethodsRecursively(openClass, 0, setterMethods);
            } else {
                map = buildTokensMapToOpenClassMethodsRecursively(openClass, 0, setterMethods);
                Map<Token, LinkedList<LinkedList<IOpenMethod>>> updatedMap = new HashMap<>(map);
                for (Entry<Token, LinkedList<LinkedList<IOpenMethod>>> entry : map.entrySet()) {
                    Token updatedToken = new Token(toTokenString(tokenizedPrefix + " " + entry.getKey().getValue()),
                        entry.getKey().getDistance());
                    updatedMap.put(updatedToken, entry.getValue());
                }
                map = updatedMap;
            }

            Map<Token, LinkedList<IOpenMethod>[]> tmp = new HashMap<>();
            for (Entry<Token, LinkedList<LinkedList<IOpenMethod>>> entry : map.entrySet()) {
                tmp.put(entry.getKey(), entry.getValue().toArray(new LinkedList[] {}));
            }

            ret = new HashMap<>();
            for (Entry<Token, LinkedList<IOpenMethod>[]> entry : tmp.entrySet()) {
                IOpenMethod[][] m = new IOpenMethod[entry.getValue().length][];
                int i = 0;
                for (LinkedList<IOpenMethod> x : entry.getValue()) {
                    m[i] = x.toArray(new IOpenMethod[] {});
                    i++;
                }
                ret.put(entry.getKey(), m);
            }
            cache1.put(tokenizedPrefix, ret);
        }
        return ret;
    }

    public static boolean isEqualsMethodChains(IOpenMethod[] methodChain1, IOpenMethod[] methodChain2) {
        if (methodChain1 == methodChain2) {
            return true;
        }
        return Arrays.deepEquals(methodChain1, methodChain2);
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
            int deepLevel,
            boolean setterMethods) {
        if (deepLevel >= DEEP_LEVEL) {
            return Collections.emptyMap();
        }
        Map<Token, LinkedList<LinkedList<IOpenMethod>>> ret = new HashMap<>();
        if (!openClass.isSimple()) {
            for (IOpenMethod method : openClass.getMethods()) {
                boolean g;
                if (setterMethods) {
                    g = isSetterMethod(method);
                } else {
                    g = isGetterMethod(method);
                }
                if (g) {
                    String fieldName = method.getName().substring(3);
                    IOpenField openField = openClass.getField(fieldName, false); // Support only Java Beans
                    if (openField == null) {
                        continue;
                    }
                    String t = OpenLFuzzyUtils.toTokenString(fieldName);
                    LinkedList<IOpenMethod> methods = new LinkedList<>();
                    methods.add(method);
                    LinkedList<LinkedList<IOpenMethod>> x = null;
                    for (Entry<Token, LinkedList<LinkedList<IOpenMethod>>> entry : ret.entrySet()) {
                        Token token = entry.getKey();
                        if (token.getValue().equals(t) && entry.getKey().getDistance() == deepLevel) {
                            x = entry.getValue();
                            break;
                        }
                    }
                    if (x == null) {
                        x = new LinkedList<>();
                        x.add(methods);
                        ret.put(new Token(t, deepLevel), x);
                    } else {
                        x.add(methods);
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
                            deepLevel + 1,
                            setterMethods);
                        for (Entry<Token, LinkedList<LinkedList<IOpenMethod>>> entry : map.entrySet()) {
                            if (!entry.getValue().isEmpty()) {
                                Token k = new Token(t + " " + entry.getKey().getValue(),
                                    entry.getKey().getDistance() + 1);
                                LinkedList<LinkedList<IOpenMethod>> v = ret.computeIfAbsent(k, e -> new LinkedList<>());
                                for (LinkedList<IOpenMethod> y : entry.getValue()) {
                                    LinkedList<IOpenMethod> y1 = new LinkedList<>(y);
                                    y1.addFirst(method);
                                    v.add(y1);
                                }
                                v = ret.computeIfAbsent(entry.getKey(), e -> new LinkedList<>());
                                for (LinkedList<IOpenMethod> y : entry.getValue()) {
                                    LinkedList<IOpenMethod> y1 = new LinkedList<>(y);
                                    y1.addFirst(method);
                                    v.add(y1);
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
        List<String> t = new ArrayList<>();
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
        List<String> t = new ArrayList<>();
        for (String token : tokens) {
            String s = token.trim().toLowerCase();
            if (s.isEmpty()) {
                continue;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                if (Character.isLetterOrDigit(s.charAt(i))) {
                    sb.append(s.charAt(i));
                }
            }
            if (sb.toString().length() > 0) {
                t.add(sb.toString());
            }
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
            sb.append(s);
        }
        return sb.toString();
    }

    private static final Token[] EMPTY_TOKENS = new Token[] {};

    public static Triple<Token[], Integer, Integer> openlFuzzyExtract(String source, Token[] tokens) {
        source = toTokenString(source);

        String[] sourceTokens = source.split(" ");

        String[][] tokensList = new String[tokens.length][];
        for (int i = 0; i < tokens.length; i++) {
            tokensList[i] = tokens[i].getValue().split(" ");
        }

        List<Pair<String, String>> similarity = new ArrayList<>();

        String[] sortedSourceTokens = new String[sourceTokens.length];
        System.arraycopy(sourceTokens, 0, sortedSourceTokens, 0, sourceTokens.length);
        Arrays.sort(sortedSourceTokens);
        int[] f = new int[tokensList.length];
        int max = 0;
        for (int i = 0; i < tokensList.length; i++) {
            String[] sortedTokens = new String[tokensList[i].length];
            System.arraycopy(tokensList[i], 0, sortedTokens, 0, tokensList[i].length);
            Arrays.sort(sortedTokens);
            int l = 0;
            int r = 0;
            int c = 0;
            List<String> source1 = new ArrayList<>();
            List<String> target1 = new ArrayList<>();
            while (l < sortedSourceTokens.length && r < sortedTokens.length) {
                double d = StringUtils.getJaroWinklerDistance(sortedSourceTokens[l], sortedTokens[r]);
                if (d > ACCEPTABLE_SIMILARITY_VALUE) {
                    source1.add(sortedSourceTokens[l]);
                    target1.add(sortedTokens[r]);
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

            similarity.add(Pair.of(source1.stream().collect(Collectors.joining(StringUtils.SPACE)),
                target1.stream().collect(Collectors.joining(StringUtils.SPACE))));
        }

        if (max == 0) {
            return Triple.of(EMPTY_TOKENS, 0, 0);
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
            return Triple.of(EMPTY_TOKENS, 0, 0);
        }
        if (count == 1) {
            for (int i = 0; i < tokensList.length; i++) {
                if (f[i] == max && tokensList[i].length - f[i] == min && tokens[i].getDistance() == minDistance) {
                    return Triple.of(new Token[] { tokens[i] }, max, min);
                }
            }
        } else {
            List<Token> ret = new ArrayList<>();
            int best = 0;
            int bestL = Integer.MAX_VALUE;
            for (int i = 0; i < tokensList.length; i++) {
                if (f[i] == max && tokensList[i].length - f[i] == min) {
                    Pair<String, String> pair = similarity.get(i);
                    int d = StringUtils.getFuzzyDistance(pair.getRight(), pair.getLeft(), Locale.ENGLISH);
                    if (d > best) {
                        best = d;
                        bestL = StringUtils.getLevenshteinDistance(pair.getRight(), pair.getLeft());
                        ret.clear();
                        ret.add(tokens[i]);
                    } else {
                        if (d == best) {
                            int l = StringUtils.getLevenshteinDistance(pair.getRight(), pair.getLeft());
                            if (l < bestL) {
                                bestL = l;
                                ret.clear();
                                ret.add(tokens[i]);
                            } else {
                                if (l == bestL) {
                                    ret.add(tokens[i]);
                                }
                            }
                        }
                    }
                }
            }

            return Triple.of(ret.toArray(new Token[] {}), max, min);
        }
        return Triple.of(EMPTY_TOKENS, 0, 0);
    }
}
