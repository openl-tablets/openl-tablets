package org.openl.rules.cmatch;

import java.util.HashMap;
import java.util.Map;

public class ColumnMatchAlgorithmFactory {
    private static final Map<String, ColumnMatchAlgorithmBuilder> builders = new HashMap<String, ColumnMatchAlgorithmBuilder>();
    private static ColumnMatchAlgorithmBuilder defaultBuilder = null;

    public static void registerBuilder(String nameOfAlgorithm, ColumnMatchAlgorithmBuilder builder) {
        builders.put(nameOfAlgorithm, builder);
    }

    public static ColumnMatchAlgorithm getAlgorithm(String nameOfAlgorithm) {
        ColumnMatchAlgorithmBuilder builder = null;
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

    // public ColumnMatchAlgorithmBuilder getDefaultBuilder() {
    // return defaultBuilder;
    // }
    //
    // public void setDefaultBuilder(ColumnMatchAlgorithmBuilder builder) {
    // defaultBuilder = builder;
    // }
}
