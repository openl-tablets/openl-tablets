package org.openl.rules.lang.xls.main;

public interface IRulesLaunchConstants {

    String START_PROJECT_PROPERTY_NAME = "org.openl.rules.start.project";

    String WRAPPER_SEARCH_START_DIR_DEFAULT = "gen";
    // comma-separated list of search directories
    String WRAPPER_SEARCH_START_DIR_PROPERTY = "org.openl.rules.wrapper.dir";

    @Deprecated
    String WRAPPER_SOURCE_SUFFIX_DEFAULT = "Wrapper.java";
    String INTERFACE_SOURCE_SUFFIX_DEFAULT = "RulesInterface.java";
    // comma-separated list of suffixes
    String WRAPPER_SOURCE_SUFFIX_PROPERTY = "org.openl.rules.wrapper.suffixes";

}
