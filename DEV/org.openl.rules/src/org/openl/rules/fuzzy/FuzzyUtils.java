package org.openl.rules.fuzzy;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

public final class FuzzyUtils {

    private static final double TOKEN_ORDER_PENALTY = 0.80;
    private static final double TOKEN_MATCH_PERSENTAGE = 0.85;

    public static double getFuzzyDistance(String target, String source) {
        if (target.equalsIgnoreCase(source)) {
            return Double.MAX_VALUE;
        }

        String[] tokens1 = source.split("(?<=.)(?=\\p{Lu}|\\d|\\s)");
        String[] tokens2 = target.split("(?<=.)(?=\\p{Lu}|\\d|\\s)");

        double[] distance = new double[tokens1.length];
        int[] position = new int[tokens1.length];
        boolean[] f = new boolean[tokens2.length];
        Arrays.fill(f, true);
        Arrays.fill(position, -1);
        for (int i = 0; i < tokens1.length; i++) {
            double best = 0;
            int bestj = -1;
            for (int j = 0; j < tokens2.length; j++) {
                if (f[j]) {
                    double current = StringUtils.getJaroWinklerDistance(tokens1[i].trim().toLowerCase(),
                        tokens2[j].trim().toLowerCase());
                    if (best < current) {
                        best = current;
                        bestj = j;
                    }
                }
            }
            if (bestj >= 0) {
                f[bestj] = false;
                distance[i] = best;
                position[i] = bestj;
            }
        }

        double result = 0;
        int j = -1;
        boolean[] g = new boolean[tokens1.length];
        Arrays.fill(g, true);

        // Add all matched tokens in source order
        for (int i = 0; i < tokens1.length; i++) {
            if (distance[i] > TOKEN_MATCH_PERSENTAGE && position[i] != -1 && position[i] > j && g[i]) {
                result = result + distance[i];
                j = position[i];
                g[i] = false;
            }
        }

        // Add other matched tokens with penalty
        for (int i = 0; i < tokens1.length; i++) {
            if (distance[i] > TOKEN_MATCH_PERSENTAGE && g[i]) {
                result = result + distance[i] * TOKEN_ORDER_PENALTY;
            }
        }

        return result;
    }

}
