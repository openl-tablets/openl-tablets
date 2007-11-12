package org.openl.rules.repository.jcr;

/**
 * Node related constants.
 * 
 * @author Aleh Bykhavets
 *
 */
public interface JcrNT {
	// Node Types
	public static final String NT_PROJECT = "openl:project";
	public static final String NT_FILES = "openl:files";
	public static final String NT_FOLDER = "openl:folder";
	public static final String NT_FILE = "openl:file";
	public static final String NT_RESOURCE = "nt:resource";

	public static final String MIX_VERSIONABLE = "mix:versionable";

	// Global Properties to track security issues
	public static final String PROP_MODIFIED_TIME = "modifiedTime";
	public static final String PROP_MODIFIED_BY = "modifiedBy";
	
        // Common Rules properties
        public static final String PROP_EFFECTIVE_DATE = "effectiveDate";
        public static final String PROP_EXPIRATION_DATE = "expirationDate";
        public static final String PROP_LINE_OF_BUSINESS = "LOB";
        
	// Properties for File->Resource
	public static final String PROP_RES_CONTENT = "jcr:content";
	public static final String PROP_RES_MIMETYPE = "jcr:mimeType";
	public static final String PROP_RES_ENCODING = "jcr:encoding";
	public static final String PROP_RES_DATA = "jcr:data";
	public static final String PROP_RES_LASTMODIFIED = "jcr:lastModified";

	// Properties for Project
	public static final String PROP_PRJ_NAME = "name";
	public static final String PROP_PRJ_DESCR = "description";
	public static final String PROP_PRJ_MARKED_4_DELETION = "marked4deletion";

        public static final String PROP_VERSION = "rVersion";
        public static final String PROP_REVISION = "rRevision";
}
