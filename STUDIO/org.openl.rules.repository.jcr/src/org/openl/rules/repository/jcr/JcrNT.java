package org.openl.rules.repository.jcr;

/**
 * Node related constants.
 *
 * @author Aleh Bykhavets
 *
 */
// TODO extract properties
public interface JcrNT {
    // Node Types
    String NT_COMMON_ENTITY = "openl:entity";
    String NT_REPOSITORY = "openl:repository";
    @Deprecated
    String NT_PROJECT = "openl:project";
    String NT_APROJECT = "openl:aproject";
    @Deprecated
    String NT_PROD_PROJECT = "openl:dproject";
    @Deprecated
    String NT_FILES = "openl:files";
    @Deprecated
    String NT_PROD_FILES = "openl:dfiles";
    String NT_FOLDER = "openl:folder";
    @Deprecated
    String NT_PROD_FOLDER = "openl:dfolder";
    String NT_FILE = "openl:file";
    @Deprecated
    String NT_PROD_FILE = "openl:dfile";
    @Deprecated
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
