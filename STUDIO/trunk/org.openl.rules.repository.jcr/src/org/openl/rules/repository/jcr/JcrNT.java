package org.openl.rules.repository.jcr;

/**
 * Node related constants.
 *
 * @author Aleh Bykhavets
 *
 */
public interface JcrNT {

    // Node Types
    String NT_PROJECT = "openl:project";
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

    String NT_DEPLOYMENT_PROJECT = "openl:deploymentProject";
    String NT_PROJECT_DESCRIPTOR = "openl:projectDescriptor";

    String NT_LOCK = "openl:lock";

    String MIX_VERSIONABLE = "mix:versionable";

    String NT_FROZEN_NODE = "nt:frozenNode";
    String FROZEN_NODE = "jcr:frozenNode";

    // Global Properties to track security issues
    // take time from version
    // String PROP_MODIFIED_TIME = "modifiedTime";
    String PROP_MODIFIED_BY = "modifiedBy";
    String PROP_LOCKED_AT = "lockedAt";
    String PROP_LOCKED_BY = "lockedBy";

    // Common Rules properties
    String PROP_EFFECTIVE_DATE = "effectiveDate";
    String PROP_EXPIRATION_DATE = "expirationDate";
    String PROP_LINE_OF_BUSINESS = "LOB";

    // Node properties
    int PROPS_COUNT = 15;
    String PROP_ATTRIBUTE = "attribute";

    // Properties for File->Resource
    String PROP_RES_CONTENT = "jcr:content";
    String PROP_RES_MIMETYPE = "jcr:mimeType";
    String PROP_RES_ENCODING = "jcr:encoding";
    String PROP_RES_DATA = "jcr:data";
    String PROP_RES_LASTMODIFIED = "jcr:lastModified";

    // Properties for Project
    String PROP_PRJ_DESCR = "description";
    String PROP_PRJ_MARKED_4_DELETION = "marked4deletion";

    String PROP_VERSION = "rVersion";
    String PROP_REVISION = "rRevision";

    // Deployment Descriptor
    String PROP_PROJECT_NAME = "projectName";

}
