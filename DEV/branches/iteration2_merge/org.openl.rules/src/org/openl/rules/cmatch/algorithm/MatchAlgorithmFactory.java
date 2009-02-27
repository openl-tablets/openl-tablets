package org.openl.rules.cmatch.algorithm;

import java.util.HashMap;
import java.util.Map;


public class MatchAlgorithmFactory {
    private static final Map<String, IMatchAlgorithmCompilerBuilder> builders = new HashMap<String, IMatchAlgorithmCompilerBuilder>();
    private static IMatchAlgorithmCompilerBuilder defaultBuilder = null;

    static {
        // add well-known algorithms
    	// snshor: slightly renamed the algorithms, left old names for compatibility
        registerBuilder("SIMPLE", new MatchAlgorithmCompilerBuilder(), true);
        registerBuilder("MATCH", new MatchAlgorithmCompilerBuilder(), true);
        registerBuilder("WEIGHT", new WeightAlgorithmCompilerBuilder(), false);
        registerBuilder("WEIGHTED", new WeightAlgorithmCompilerBuilder(), false);
    }

    public static void registerBuilder(String nameOfAlgorithm, IMatchAlgorithmCompilerBuilder builder) {
    	registerBuilder(nameOfAlgorithm, builder, false);
    }
    public static void registerBuilder(String nameOfAlgorithm, IMatchAlgorithmCompilerBuilder builder, boolean isDefault) {
        builders.put(nameOfAlgorithm, builder);
        if (isDefault)
        	defaultBuilder = builder;
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
