package org.openl.rules.fuzzy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

public final class OpenLFuzzyUtils {

    private static final List<String> TOKENS_STRONG_MATCH = Arrays
            .asList("at", "on", "for", "to", "with", "of", "on", "by", "from");

    private static final double ACCEPTABLE_SIMILARITY_VALUE = 0.86d;
    private static final int DEEP_LEVEL = 5;

    private static final ThreadLocal<Map<IOpenClass, Map<String, Map<Token, IOpenField[][]>>>> openClassRecursivelyCacheForWritableFields = ThreadLocal
            .withInitial(HashMap::new);

    private static final ThreadLocal<Map<IOpenClass, Map<String, Map<Token, IOpenField[][]>>>> openClassRecursivelyCacheForReadableFields = ThreadLocal
            .withInitial(HashMap::new);

    private static final ThreadLocal<Map<IOpenClass, Map<Token, IOpenField[]>>> openClassCacheForWritableFields = ThreadLocal
            .withInitial(HashMap::new);

    private OpenLFuzzyUtils() {
    }

    public static void clearCaches() {
        openClassCacheForWritableFields.remove();
        openClassRecursivelyCacheForReadableFields.remove();
        openClassRecursivelyCacheForWritableFields.remove();
    }

    public static Map<Token, IOpenField[][]> tokensMapToOpenClassWritableFieldsRecursively(IOpenClass openClass,
                                                                                           String tokenPrefix,
                                                                                           int startLevel) {
        return tokensMapToOpenClassFieldsRecursively(openClass, tokenPrefix, startLevel, true);
    }

    public static Map<Token, IOpenField[][]> tokensMapToOpenClassReadableFieldsRecursively(IOpenClass openClass,
                                                                                           String tokenPrefix,
                                                                                           int startLevel) {
        return tokensMapToOpenClassFieldsRecursively(openClass, tokenPrefix, startLevel, false);
    }

    @SuppressWarnings("unchecked")
    private static Map<Token, IOpenField[][]> tokensMapToOpenClassFieldsRecursively(IOpenClass openClass,
                                                                                    String tokenPrefix,
                                                                                    int startLevel,
                                                                                    boolean writable) {
        Map<IOpenClass, Map<String, Map<Token, IOpenField[][]>>> cache;
        if (writable) {
            cache = openClassRecursivelyCacheForWritableFields.get();
        } else {
            cache = openClassRecursivelyCacheForReadableFields.get();
        }
        var cache1 = cache.computeIfAbsent(openClass, e -> new HashMap<>());
        final String tokenizedPrefix = toTokenString(tokenPrefix);
        Map<Token, IOpenField[][]> ret = cache1.get(tokenizedPrefix);
        if (ret == null) {
            Map<Token, LinkedList<LinkedList<IOpenField>>> map;
            if (StringUtils.isBlank(tokenPrefix)) {
                map = buildTokensMapToOpenClassFieldsRecursively(openClass, startLevel, writable);
            } else {
                map = buildTokensMapToOpenClassFieldsRecursively(openClass, startLevel, writable);
                var updatedMap = new HashMap<Token, LinkedList<LinkedList<IOpenField>>>(map);
                for (Entry<Token, LinkedList<LinkedList<IOpenField>>> entry : map.entrySet()) {
                    var updatedToken = new Token(toTokenString(tokenizedPrefix + " " + entry.getKey().getValue()),
                            entry.getKey().getDistance());
                    updatedMap.put(updatedToken, entry.getValue());
                }
                map = updatedMap;
            }

            var tmp = new HashMap<Token, LinkedList<IOpenField>[]>();
            for (Entry<Token, LinkedList<LinkedList<IOpenField>>> entry : map.entrySet()) {
                tmp.put(entry.getKey(), entry.getValue().toArray(new LinkedList[]{}));
            }

            ret = new HashMap<>();
            for (Entry<Token, LinkedList<IOpenField>[]> entry : tmp.entrySet()) {
                IOpenField[][] m = new IOpenField[entry.getValue().length][];
                var i = 0;
                for (LinkedList<IOpenField> x : entry.getValue()) {
                    m[i] = x.toArray(new IOpenField[]{});
                    i++;
                }
                ret.put(entry.getKey(), m);
            }
            cache1.put(tokenizedPrefix, Collections.unmodifiableMap(ret));
        }
        return ret;
    }

    public static boolean isEqualsFieldsChains(IOpenField[] fieldsChain1, IOpenField[] fieldsChain2) {
        if (fieldsChain1 == fieldsChain2) {
            return true;
        }
        return Arrays.deepEquals(fieldsChain1, fieldsChain2);
    }

    private static Map<Token, LinkedList<LinkedList<IOpenField>>> buildTokensMapToOpenClassFieldsRecursively(
            IOpenClass openClass,
            int deepLevel,
            boolean writable) {
        if (deepLevel >= DEEP_LEVEL) {
            return Map.of();
        }
        var ret = new HashMap<Token, LinkedList<LinkedList<IOpenField>>>();
        if (!openClass.isSimple()) {
            for (IOpenField field : openClass.getFields()) {
                if (!field.isStatic() && !field.isConst()) {
                    if (writable ? field.isWritable() : field.isReadable()) {
                        var fieldName = field.getName();
                        String t = OpenLFuzzyUtils.toTokenString(phoneticFix(fieldName));
                        var fields = new LinkedList<IOpenField>();
                        fields.add(field);
                        LinkedList<LinkedList<IOpenField>> x = null;
                        for (Entry<Token, LinkedList<LinkedList<IOpenField>>> entry : ret.entrySet()) {
                            var token = entry.getKey();
                            if (token.getValue().equals(t) && entry.getKey().getDistance() == deepLevel) {
                                x = entry.getValue();
                                break;
                            }
                        }
                        if (x == null) {
                            x = new LinkedList<>();
                            x.add(fields);
                            ret.put(new Token(t, deepLevel), x);
                        } else {
                            x.add(fields);
                        }

                        var type = field.getType();
                        if (!type.isSimple() && !type.isArray()) {
                            var map = buildTokensMapToOpenClassFieldsRecursively(
                                    type,
                                    deepLevel + 1,
                                    writable);
                            for (Entry<Token, LinkedList<LinkedList<IOpenField>>> entry : map.entrySet()) {
                                if (!entry.getValue().isEmpty()) {
                                    var k = new Token(t + " " + entry.getKey().getValue(),
                                            entry.getKey().getDistance() + 1);
                                    var v = ret.computeIfAbsent(k,
                                            e -> new LinkedList<>());
                                    for (LinkedList<IOpenField> y : entry.getValue()) {
                                        var y1 = new LinkedList<IOpenField>(y);
                                        y1.addFirst(field);
                                        v.add(y1);
                                    }
                                    v = ret.computeIfAbsent(entry.getKey(), e -> new LinkedList<>());
                                    for (LinkedList<IOpenField> y : entry.getValue()) {
                                        var y1 = new LinkedList<IOpenField>(y);
                                        y1.addFirst(field);
                                        v.add(y1);
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

    public static String phoneticFix(String value) {
        if (value.length() > 1 && Character.isLowerCase(value.charAt(0)) && Character.isUpperCase(value.charAt(1))) {
            value = Character.toUpperCase(value.charAt(0)) + value.substring(1);
        }
        return value;
    }

    private static String[] concatTokens(String[] tokens, String pattern) {
        var t = new ArrayList<String>();
        var sbBuilder = new StringBuilder();
        var g = false;
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
        return t.toArray(new String[]{});
    }

    private static String[] cleanUpTokens(String[] tokens) {
        var t = new ArrayList<String>();
        for (String token : tokens) {
            var s = token.trim().toLowerCase();
            if (s.isEmpty()) {
                continue;
            }
            var sb = new StringBuilder();
            for (var i = 0; i < s.length(); i++) {
                if (Character.isLetterOrDigit(s.charAt(i))) {
                    sb.append(s.charAt(i));
                }
            }
            if (!sb.toString().isEmpty()) {
                t.add(sb.toString());
            }
        }
        return t.toArray(new String[]{});
    }

    public static String toTokenString(String source) {
        if (source == null) {
            return StringUtils.EMPTY;
        }
        var tokens = source.split("(?<=.)(?=\\p{Lu}|\\d|\\s|[_-]|\\.|,|;)");

        tokens = concatTokens(tokens, "\\p{Lu}+");
        tokens = concatTokens(tokens, "\\d+");
        tokens = cleanUpTokens(tokens);

        var sb = new StringBuilder();
        var f = false;
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
        var n1 = 0;
        var n2 = 0;
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
        var n = n1 + n2 + 2;
        var s = n1 + n2;
        var t = n1 + n2 + 1;
        int[][] edgesMatrix = new int[n][n];
        // Build graph
        for (var i = 0; i < n1; i++) {
            edgesMatrix[s][i] = 1;
        }
        for (var i = n1; i < n1 + n2; i++) {
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
            for (var i = 0; i < n; i++) {
                var k = -1;
                var min = Integer.MAX_VALUE;
                for (var j = 0; j < n; j++) {
                    if (f[j] && d[j] < min) {
                        min = d[j];
                        k = j;
                    }
                }
                if (k < 0) {
                    break;
                }
                f[k] = false;
                for (var j = 0; j < n; j++) {
                    if (edgesMatrix[k][j] > 0 && (d[k] != Integer.MAX_VALUE && d[k] + edgesMatrix[k][j] < d[j])) {
                        d[j] = d[k] + edgesMatrix[k][j];
                        m[j] = k;
                    }
                }
            }
            if (d[t] == Integer.MAX_VALUE || d[t] == 0) {
                break;
            }
            var j = t;
            while (j != s) {
                edgesMatrix[m[j]][j] = edgesMatrix[m[j]][j] - 1;
                edgesMatrix[j][m[j]] = edgesMatrix[j][m[j]] + 1;
                j = m[j];
            }
        }
        var ret = new ArrayList<Pair<Integer, Integer>>();
        for (var i = 0; i < n1; i++) {
            for (var j = n1; j < n1 + n2; j++) {
                if (edgesMatrix[j][i] > 0) {
                    ret.add(Pair.of(i, j - n1));
                }
            }
        }
        return ret;
    }

    public static List<FuzzyResult> fuzzyExtract(String source, Token[] tokens, boolean ignoreDistances) {
        source = toTokenString(source);

        var sourceTokens = source.split(" ");

        String[][] tokensList = new String[tokens.length][];
        for (var i = 0; i < tokens.length; i++) {
            tokensList[i] = tokens[i].getValue().split(" ");
        }

        double[][][] distances = new double[tokensList.length][sourceTokens.length][];
        boolean[] sm = new boolean[sourceTokens.length];
        for (var k = 0; k < sourceTokens.length; k++) {
            sm[k] = TOKENS_STRONG_MATCH.contains(sourceTokens[k]);
        }
        for (var i = 0; i < tokensList.length; i++) {
            for (var k = 0; k < sourceTokens.length; k++) {
                double[] w = new double[tokensList[i].length];
                for (var q = 0; q < tokensList[i].length; q++) {
                    if (sm[k] || TOKENS_STRONG_MATCH.contains(tokensList[i][q])) {
                        w[q] = Objects.equals(sourceTokens[k], tokensList[i][q]) ? 1.0d : 0d;
                    } else {
                        w[q] = StringUtils.getJaroWinklerDistance(sourceTokens[k], tokensList[i][q]);
                    }
                }
                distances[i][k] = w;
            }
        }

        var buildBySimilarity1 = new BuildBySimilarity(distances, 1.0d, sourceTokens, tokens, tokensList)
                .invoke();
        var buildBySimilarity = new BuildBySimilarity(distances,
                ACCEPTABLE_SIMILARITY_VALUE,
                sourceTokens,
                tokens,
                tokensList).invoke();
        var maxMatchedTokens = buildBySimilarity.getMaxMatchedTokens();
        if (buildBySimilarity1.getMaxMatchedTokens() == buildBySimilarity.getMaxMatchedTokens()) {
            buildBySimilarity = buildBySimilarity1;
        } else {
            var a = ACCEPTABLE_SIMILARITY_VALUE;
            var b = 1.0d;
            while (b - a > 1e-4) {
                var p = (a + b) / 2;
                var pSimilarity = new BuildBySimilarity(distances, p, sourceTokens, tokens, tokensList)
                        .invoke();
                if (pSimilarity.maxMatchedTokens == maxMatchedTokens) {
                    a = p;
                    buildBySimilarity = pSimilarity;
                } else {
                    b = p;
                }
            }
        }

        List<Pair<String, String>> similarity = buildBySimilarity.getSimilarity();
        var f = buildBySimilarity.getF();

        if (maxMatchedTokens == 0) {
            return List.of();
        }

        var missedTokensMin = Integer.MAX_VALUE;
        var minDistance = Integer.MAX_VALUE;
        for (var i = 0; i < tokensList.length; i++) {
            if (f[i] == maxMatchedTokens) {
                if (missedTokensMin > tokensList[i].length - f[i]) {
                    missedTokensMin = tokensList[i].length - f[i];
                }
                if (minDistance > tokens[i].getDistance()) {
                    minDistance = tokens[i].getDistance();
                }
            }
        }

        var ret = new ArrayList<Token>();
        var best = 0;
        var bestL = Integer.MAX_VALUE;
        for (var i = 0; i < tokensList.length; i++) {
            if (f[i] == maxMatchedTokens && tokensList[i].length - f[i] == missedTokensMin && (ignoreDistances || tokens[i]
                    .getDistance() == minDistance)) {
                var pair = similarity.get(i);
                if (!ignoreDistances) {
                    var d = StringUtils.getFuzzyDistance(pair.getRight(), pair.getLeft(), Locale.ENGLISH);
                    if (d > best) {
                        best = d;
                        bestL = StringUtils.getLevenshteinDistance(pair.getRight(), pair.getLeft());
                        ret.clear();
                        ret.add(tokens[i]);
                    } else {
                        if (d == best) {
                            var l = StringUtils.getLevenshteinDistance(pair.getRight(), pair.getLeft());
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
        var missedTokensMin1 = missedTokensMin;
        var acceptableSimilarity = buildBySimilarity.getAcceptableSimilarity();
        return ret.stream()
                .map(e -> new FuzzyResult(e,
                        maxMatchedTokens,
                        missedTokensMin1,
                        sourceTokens.length - maxMatchedTokens,
                        acceptableSimilarity))
                .collect(Collectors.toList());
    }

    @RequiredArgsConstructor
    public static final class FuzzyResult implements Comparable<FuzzyResult> {
        @Getter
        final Token token;
        @Getter
        final int foundTokensCount;
        @Getter
        final int missedTokensCount;
        @Getter
        final int unmatchedTokensCount;
        @Getter
        final double acceptableSimilarity;

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
            if (this.unmatchedTokensCount > o.unmatchedTokensCount) {
                return 1;
            }
            if (this.unmatchedTokensCount < o.unmatchedTokensCount) {
                return -1;
            }
            return Double.compare(o.acceptableSimilarity, this.acceptableSimilarity);
        }
    }

    private static class BuildBySimilarity {
        private final String[] sourceTokens;
        private final String[][] tokensList;
        @Getter
        private List<Pair<String, String>> similarity;
        @Getter
        private int maxMatchedTokens;
        @Getter
        private int[] f;
        @Getter
        private final double acceptableSimilarity;
        private final Token[] tokens;
        private final double[][][] distances;

        public BuildBySimilarity(double[][][] distances,
                                 double acceptableSimilarity,
                                 String[] sourceTokens,
                                 Token[] tokens,
                                 String[]... tokensList) {
            this.sourceTokens = sourceTokens;
            this.tokens = tokens;
            this.tokensList = tokensList;
            this.acceptableSimilarity = acceptableSimilarity;
            this.distances = distances;
        }

        public BuildBySimilarity invoke() {
            similarity = new ArrayList<>();
            maxMatchedTokens = 0;
            f = new int[tokensList.length];
            for (var i = 0; i < tokensList.length; i++) {
                var c = 0;
                var source1 = new ArrayList<String>();
                var target1 = new ArrayList<String>();
                var edges = new ArrayList<Pair<Integer, Integer>>();
                for (var k = 0; k < sourceTokens.length; k++) {
                    for (var q = 0; q < tokensList[i].length; q++) {
                        var d = distances[i][k][q];
                        if (d >= acceptableSimilarity) {
                            edges.add(Pair.of(k, q));
                        }
                    }
                }
                var maximumMatching = findMaximumMatching(edges);
                for (Pair<Integer, Integer> pair : maximumMatching) {
                    source1.add(sourceTokens[pair.getLeft()]);
                    target1.add(tokensList[i][pair.getRight()]);
                    c++;
                }
                if (c >= tokens[i].getMinMatchedTokens()) {
                    if (maxMatchedTokens < c) {
                        maxMatchedTokens = c;
                    }
                    f[i] = c;

                    source1.sort(Comparator.naturalOrder());
                    target1.sort(Comparator.naturalOrder());
                } else {
                    f[i] = 0;
                    source1.clear();
                    target1.clear();
                }
                similarity
                        .add(Pair.of(String.join(StringUtils.SPACE, source1), String.join(StringUtils.SPACE, target1)));
            }
            return this;
        }
    }
}
