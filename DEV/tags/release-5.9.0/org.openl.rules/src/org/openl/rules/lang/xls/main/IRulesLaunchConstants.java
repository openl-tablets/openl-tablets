package org.openl.rules.lang.xls.main;

public interface IRulesLaunchConstants {

    String START_PROJECT_PROPERTY_NAME = "org.openl.rules.start.project";
    String LOCAL_WORKSPACE_PROPERTY_NAME = "workspace.local.home";
    String WEBSTUDIO_BROWSER_URL = "http://localhost:8080/webstudio/";
    
    String WRAPPER_SEARCH_START_DIR_DEFAULT = "gen";
    //comma-separated list of search directories
    String WRAPPER_SEARCH_START_DIR_PROPERTY = "org.openl.rules.wrapper.dir";
    

    String WRAPPER_SOURCE_SUFFIX_DEFAULT = "Wrapper.java";
    //comma-separated list of suffixes
    String WRAPPER_SOURCE_SUFFIX_PROPERTY = "org.openl.rules.wrapper.suffixes";
    
    
}
