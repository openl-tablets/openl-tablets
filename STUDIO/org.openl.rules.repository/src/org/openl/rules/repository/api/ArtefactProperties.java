package org.openl.rules.repository.api;

public interface ArtefactProperties {
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

    String PROP_VERSION = "rVersion";
    String PROP_REVISION = "rRevision";

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
    String PROP_PRJ_MARKED_4_DELETION = "marked4deletion";

    // Deployment Descriptor
    String DESCRIPTORS_FILE = "openl_repository_descriptors.xml";
    String VERSION_COMMENT = "versionComment";
}
