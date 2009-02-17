package org.openl.rules.cmatch.algorithm;

import java.util.HashMap;
import java.util.Map;


public class MatchAlgorithmFactory {
    private static final Map<String, IMatchAlgorithmCompilerBuilder> builders = new HashMap<String, IMatchAlgorithmCompilerBuilder>();
    private static IMatchAlgorithmCompilerBuilder defaultBuilder = null;

    static {
        // add well-known algorithms
        registerBuilder("SIMPLE", new MatchAlgorithmCompilerBuilder());
        registerBuilder("WEIGHT", new WeightAlgorithmCompilerBuilder());
    }

    public static void registerBuilder(String nameOfAlgorithm, IMatchAlgorithmCompilerBuilder builder) {
        builders.put(nameOfAlgorithm, builder);
    }

    public static IMatchAlgorithmCompiler getAlgorithm(String nameOfAlgorithm) {
        IMatchAlgorithmCompilerBuilder builder = null;
        if (nameOfAlgorithm == null) {
            if (defaultBuilder == null) {
                throw new IllegalArgumentException("Default algorithm builder was not defined!");
            }

            builder = defaultBuilder;
        } else {
            builder = builders.get(nameOfAlgorithm);
        }

        if (builder == null) {
            throw new IllegalArgumentException("Cannot find algorithm for name '" + nameOfAlgorithm + "'!");
        }

        return builder.build();
    }
}
