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
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

public final class OpenLFuzzyUtils {

    private static final double ACCEPTABLE_SIMILARITY_VALUE = 0.85d;
    private static final int DEEP_LEVEL = 5;

    private static final ThreadLocal<Map<IOpenClass, Map<String, Map<Token, IOpenMethod[][]>>>> openlClassRecursivelyCacheForSetterMethods = ThreadLocal
        .withInitial(HashMap::new);

    private static final ThreadLocal<Map<IOpenClass, Map<String, Map<Token, IOpenMethod[][]>>>> openlClassRecursivelyCacheForGetterMethods = ThreadLocal
        .withInitial(HashMap::new);

    private static final ThreadLocal<Map<IOpenClass, Map<Token, IOpenMethod[]>>> openlClassCacheForSetterMethods = ThreadLocal
        .withInitial(HashMap::new);

    private OpenLFuzzyUtils() {
    }

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
        return tokensMapToOpenClassSetterMethodsRecursively(openClass, null, 0);
    }

    public static Map<Token, IOpenMethod[][]> tokensMapToOpenClassSetterMethodsRecursively(IOpenClass openClass,
            String tokenPrefix,
            int startLevel) {
        return tokensMapToOpenClassMethodsRecursively(openClass, tokenPrefix, startLevel, true);
    }

    public static Map<Token, IOpenMethod[][]> tokensMapToOpenClassGetterMethodsRecursively(IOpenClass openClass) {
        return tokensMapToOpenClassGetterMethodsRecursively(openClass, null, 0);
    }

    public static Map<Token, IOpenMethod[][]> tokensMapToOpenClassGetterMethodsRecursively(IOpenClass openClass,
            String tokenPrefix,
            int startLevel) {
        return tokensMapToOpenClassMethodsRecursively(openClass, tokenPrefix, startLevel, false);
    }

    @SuppressWarnings("unchecked")
    private static Map<Token, IOpenMethod[][]> tokensMapToOpenClassMethodsRecursively(IOpenClass openClass,
            String tokenPrefix,
            int startLevel,
            boolean setterMethods) {
        Map<IOpenClass, Map<String, Map<Token, IOpenMethod[][]>>> cache;
        if (setterMethods) {
            cache = openlClassRecursivelyCacheForSetterMethods.get();
        } else {
            cache = openlClassRecursivelyCacheForGetterMethods.get();
        }
        Map<String, Map<Token, IOpenMethod[][]>> cache1 = cache.computeIfAbsent(openClass, e -> new HashMap<>());
        final String tokenizedPrefix = toTokenString(tokenPrefix);
        Map<Token, IOpenMethod[][]> ret = cache1.get(tokenizedPrefix);
        if (ret == null) {
            Map<Token, LinkedList<LinkedList<IOpenMethod>>> map;
            if (StringUtils.isBlank(tokenPrefix)) {
                map = buildTokensMapToOpenClassMethodsRecursively(openClass, startLevel, setterMethods);
            } else {
                map = buildTokensMapToOpenClassMethodsRecursively(openClass, startLevel, setterMethods);
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
            cache1.put(tokenizedPrefix, Collections.unmodifiableMap(ret));
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
            .startsWith("set") && JavaOpenClass.isVoid(method.getType());
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

                    IOpenClass type;
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
        String[] tokens = source.split("(?<=.)(?=\\p{Lu}|\\d|\\s|[_]|\\.|,|;)");

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

    // Matching (graph theory) in bigraph
    // Ford–Fulkerson algorithm
    public static List<Pair<Integer, Integer>> findMaximumMatching(List<Pair<Integer, Integer>> edges) {
        int n1 = 0;
        int n2 = 0;
        // Find vertex numbers
        for (Pair<Integer, Integer> e : edges) {
            if (e.getLeft() > n1) {
                n1 = e.getLeft();
            }
            if (e.getRight() > n2) {
                n2 = e.getRight();
            }
        }
        n1++;
        n2++;
        int n = n1 + n2 + 2;
        int s = n1 + n2;
        int t = n1 + n2 + 1;
        int[][] edgesMatrix = new int[n][n];
        // Build graph
        for (int i = 0; i < n1; i++) {
            edgesMatrix[s][i] = 1;
        }
        for (int i = n1; i < n1 + n2; i++) {
            edgesMatrix[i][t] = 1;
        }
        for (Pair<Integer, Integer> e : edges) {
            edgesMatrix[e.getLeft()][n1 + e.getRight()] = 1;
        }
        while (true) {
            int[] m = new int[n];
            Arrays.fill(m, -1);
            m[s] = 0;
            boolean[] f = new boolean[n];
            Arrays.fill(f, true);
            int[] d = new int[n];
            Arrays.fill(d, Integer.MAX_VALUE);
            d[s] = 0;
            // Deijstra to find a path
            for (int i = 0; i < n; i++) {
                int k = -1;
                int min = Integer.MAX_VALUE;
                for (int j = 0; j < n; j++) {
                    if (f[j] && d[j] < min) {
                        min = d[j];
                        k = j;
                    }
                }
                if (k < 0) {
                    break;
                }
                f[k] = false;
                for (int j = 0; j < n; j++) {
                    if (edgesMatrix[k][j] > 0 && (d[k] != Integer.MAX_VALUE && d[k] + edgesMatrix[k][j] < d[j])) {
                        d[j] = d[k] + edgesMatrix[k][j];
                        m[j] = k;
                    }
                }
            }
            if (d[t] == Integer.MAX_VALUE || d[t] == 0) {
                break;
            }
            int j = t;
            while (j != s) {
                edgesMatrix[m[j]][j] = edgesMatrix[m[j]][j] - 1;
                edgesMatrix[j][m[j]] = edgesMatrix[j][m[j]] + 1;
                j = m[j];
            }
        }
        List<Pair<Integer, Integer>> ret = new ArrayList<>();
        for (int i = 0; i < n1; i++) {
            for (int j = n1; j < n1 + n2; j++) {
                if (edgesMatrix[j][i] > 0) {
                    ret.add(Pair.of(i, j - n1));
                }
            }
        }
        return ret;
    }

    public static List<FuzzyResult> openlFuzzyExtract(String source, Token[] tokens, boolean ignoreDistances) {
        source = toTokenString(source);

        String[] sourceTokens = source.split(" ");

        String[][] tokensList = new String[tokens.length][];
        for (int i = 0; i < tokens.length; i++) {
            tokensList[i] = tokens[i].getValue().split(" ");
        }

        BuildBySimilarity buildBySimilarity1 = new BuildBySimilarity(1.0d, sourceTokens, tokensList).invoke();
        BuildBySimilarity buildBySimilarity = new BuildBySimilarity(ACCEPTABLE_SIMILARITY_VALUE,
            sourceTokens,
            tokensList).invoke();
        int maxMatchedTokens = buildBySimilarity.getMaxMatchedTokens();
        if (buildBySimilarity1.getMaxMatchedTokens() == buildBySimilarity.getMaxMatchedTokens()) {
            buildBySimilarity = buildBySimilarity1;
        } else {
            double a = ACCEPTABLE_SIMILARITY_VALUE;
            double b = 1.0d;
            while (b - a > 1e-4) {
                double p = (a + b) / 2;
                BuildBySimilarity pSimilarity = new BuildBySimilarity(p, sourceTokens, tokensList).invoke();
                if (pSimilarity.maxMatchedTokens == maxMatchedTokens) {
                    a = p;
                    buildBySimilarity = pSimilarity;
                } else {
                    b = p;
                }
            }
        }

        List<Pair<String, String>> similarity = buildBySimilarity.getSimilarity();
        int[] f = buildBySimilarity.getF();

        if (maxMatchedTokens == 0) {
            return Collections.emptyList();
        }

        int missedTokensMin = Integer.MAX_VALUE;
        int minDistance = Integer.MAX_VALUE;
        for (int i = 0; i < tokensList.length; i++) {
            if (f[i] == maxMatchedTokens) {
                if (missedTokensMin > tokensList[i].length - f[i]) {
                    missedTokensMin = tokensList[i].length - f[i];
                }
                if (minDistance > tokens[i].getDistance()) {
                    minDistance = tokens[i].getDistance();
                }
            }
        }

        List<Token> ret = new ArrayList<>();
        int best = 0;
        int bestL = Integer.MAX_VALUE;
        for (int i = 0; i < tokensList.length; i++) {
            if (f[i] == maxMatchedTokens && tokensList[i].length - f[i] == missedTokensMin && (ignoreDistances || tokens[i]
                .getDistance() == minDistance)) {
                Pair<String, String> pair = similarity.get(i);
                if (!ignoreDistances) {
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
                } else {
                    ret.add(tokens[i]);
                }
            }
        }
        int missedTokensMin1 = missedTokensMin;
        double acceptableSimilarity = buildBySimilarity.getAcceptableSimilarity();
        return ret.stream()
            .map(e -> new FuzzyResult(e, maxMatchedTokens, missedTokensMin1, acceptableSimilarity))
            .collect(Collectors.toList());
    }

    public static final class FuzzyResult implements Comparable<FuzzyResult> {
        Token token;
        int foundTokensCount;
        int missedTokensCount;
        double acceptableSimilarity;

        public FuzzyResult(Token token, int foundTokensCount, int missedTokensCount, double acceptableSimilarity) {
            this.token = token;
            this.foundTokensCount = foundTokensCount;
            this.missedTokensCount = missedTokensCount;
            this.acceptableSimilarity = acceptableSimilarity;
        }

        @Override
        public int compareTo(FuzzyResult o) {
            if (this.foundTokensCount > o.foundTokensCount) {
                return -1;
            }
            if (this.foundTokensCount < o.foundTokensCount) {
                return 1;
            }
            if (this.missedTokensCount > o.missedTokensCount) {
                return 1;
            }
            if (this.missedTokensCount < o.missedTokensCount) {
                return -1;
            }
            if (this.token.getDistance() < o.token.getDistance()) {
                return -1;
            }
            if (this.token.getDistance() > o.token.getDistance()) {
                return 1;
            }
            return Double.compare(o.acceptableSimilarity, this.acceptableSimilarity);
        }

        public Token getToken() {
            return token;
        }

        public int getFoundTokensCount() {
            return foundTokensCount;
        }

        public int getMissedTokensCount() {
            return missedTokensCount;
        }

        public double getAcceptableSimilarity() {
            return acceptableSimilarity;
        }
    }

    private static class BuildBySimilarity {
        private String[] sourceTokens;
        private String[][] tokensList;
        private List<Pair<String, String>> similarity;
        private int maxMatchedTokens;
        private int[] f;
        private double acceptableSimilarity;

        public BuildBySimilarity(double acceptableSimilarity, String[] sourceTokens, String[]... tokensList) {
            this.sourceTokens = sourceTokens;
            this.tokensList = tokensList;
            this.acceptableSimilarity = acceptableSimilarity;
        }

        public List<Pair<String, String>> getSimilarity() {
            return similarity;
        }

        public int getMaxMatchedTokens() {
            return maxMatchedTokens;
        }

        public int[] getF() {
            return f;
        }

        public double getAcceptableSimilarity() {
            return acceptableSimilarity;
        }

        public BuildBySimilarity invoke() {
            similarity = new ArrayList<>();
            maxMatchedTokens = 0;
            f = new int[tokensList.length];
            for (int i = 0; i < tokensList.length; i++) {
                int c = 0;
                List<String> source1 = new ArrayList<>();
                List<String> target1 = new ArrayList<>();
                List<Pair<Integer, Integer>> edges = new ArrayList<>();
                for (int k = 0; k < sourceTokens.length; k++) {
                    for (int q = 0; q < tokensList[i].length; q++) {
                        double d = StringUtils.getJaroWinklerDistance(sourceTokens[k], tokensList[i][q]);
                        if (d >= acceptableSimilarity) {
                            edges.add(Pair.of(k, q));
                        }
                    }
                }
                List<Pair<Integer, Integer>> maximumMatching = findMaximumMatching(edges);
                for (Pair<Integer, Integer> pair : maximumMatching) {
                    source1.add(sourceTokens[pair.getLeft()]);
                    target1.add(tokensList[i][pair.getRight()]);
                    c++;
                }

                if (maxMatchedTokens < c) {
                    maxMatchedTokens = c;
                }
                f[i] = c;
                source1.sort(String::compareTo);
                target1.sort(String::compareTo);
                similarity
                    .add(Pair.of(String.join(StringUtils.SPACE, source1), String.join(StringUtils.SPACE, target1)));
            }
            return this;
        }
    }
}
