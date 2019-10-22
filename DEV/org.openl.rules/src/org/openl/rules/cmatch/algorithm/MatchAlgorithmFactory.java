package org.openl.rules.cmatch.algorithm;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class MatchAlgorithmFactory {

    private MatchAlgorithmFactory() {
    }

    private static final Map<String, IMatchAlgorithmCompilerBuilder> builders = new LinkedHashMap<>();
    private static IMatchAlgorithmCompilerBuilder defaultBuilder = null;

    static {
        // add well-known algorithms
        IMatchAlgorithmCompilerBuilder matchBuilder = new MatchAlgorithmCompilerBuilder();
        registerBuilder("MATCH", matchBuilder);
        registerBuilder("WEIGHTED", new WeightAlgorithmCompilerBuilder());
        registerBuilder("SCORE", new ScoreAlgorithmCompilerBuilder());

        setDefaultBuilder(matchBuilder);
    }

    /**
     * Get compiler for algorithm by its name.
     * <p>
     * You can pass {@literal null} as {@literal nameOfAlgorithm} to get compiler for default algorithm. But default
     * algorithm may be undefined.
     *
     * @param nameOfAlgorithm name of algorithm
     * @return compiler for algorithm
     * @throws IllegalArgumentException if no algorithm is registered for that name
     */
    public static IMatchAlgorithmCompiler getAlgorithm(String nameOfAlgorithm) {
        IMatchAlgorithmCompilerBuilder builder = null;
        if (nameOfAlgorithm == null) {
            if (defaultBuilder == null) {
                throw new IllegalArgumentException("Default algorithm builder is not defined.");
            }

            builder = defaultBuilder;
        } else {
            builder = builders.get(nameOfAlgorithm);
        }

        if (builder == null) {
            throw new IllegalArgumentException(String.format("Cannot find algorithm for name '%s'.", nameOfAlgorithm));
        }

        return builder.build();
    }

    /**
     * List all valid algorithm names.
     *
     * @return collection of names
     */
    public static Collection<String> getAlgorithmNames() {
        return Collections.unmodifiableSet(builders.keySet());
    }

    public static IMatchAlgorithmCompilerBuilder getDefaultBuilder() {
        return defaultBuilder;
    }

    /**
     * Register algorithm compiler builder for specified algorithm name.
     * <p>
     * Note that if builder for such name was registered already it will be overwritten.
     *
     * @param nameOfAlgorithm algorithm name
     * @param builder compiler builder for algorithm
     */
    public static void registerBuilder(String nameOfAlgorithm, IMatchAlgorithmCompilerBuilder builder) {
        builders.put(nameOfAlgorithm, builder);
    }

    public static void setDefaultBuilder(IMatchAlgorithmCompilerBuilder builder) {
        defaultBuilder = builder;
    }
}
