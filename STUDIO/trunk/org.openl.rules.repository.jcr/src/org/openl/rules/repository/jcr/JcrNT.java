package org.openl.rules.repository.jcr;

/**
 * Node related constants.
 *
 * @author Aleh Bykhavets
 *
 */
//TODO extract properties
public interface JcrNT {
    // Node Types
    @Deprecated
    String NT_PROJECT = "openl:project";
    String NT_APROJECT = "openl:aproject";
    String NT_PROD_PROJECT = "openl:dproject";
    String NT_FILES = "openl:files";
    String NT_PROD_FILES = "openl:dfiles";
    String NT_FOLDER = "openl:folder";
    String NT_PROD_FOLDER = "openl:dfolder";
    String NT_FILE = "openl:file";
    String NT_PROD_FILE = "openl:dfile";
    String NT_DEPLOYMENT = "openl:deployment";
    String NT_RESOURCE = "nt:resource";

    String NT_DEPENDENCIES = "openl:dependencies";
    String NT_DEPENDENCY = "openl:dependency";

    @Deprecated
    String NT_DEPLOYMENT_PROJECT = "openl:deploymentProject";
    String NT_PROJECT_DESCRIPTOR = "openl:projectDescriptor";

    String NT_LOCK = "openl:lock";

    String MIX_VERSIONABLE = "mix:versionable";
    String MIX_LOCABLE = "mix:lockable";

    String NT_FROZEN_NODE = "nt:frozenNode";
    String FROZEN_NODE = "jcr:frozenNode";

}
