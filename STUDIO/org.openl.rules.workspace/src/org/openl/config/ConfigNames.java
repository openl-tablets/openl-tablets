package org.openl.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class contains default prefixes for deploy and design repositories configuration
 *
 */
public class ConfigNames {
    public static final String DEPLOY_CONFIG = "deploy-config";
    public static final String DESIGN_CONFIG = "design";

    public static final Set<String> DEFAULT_CONFIGS = Collections
        .unmodifiableSet(new HashSet<>(Arrays.asList(DEPLOY_CONFIG, DESIGN_CONFIG)));
}
