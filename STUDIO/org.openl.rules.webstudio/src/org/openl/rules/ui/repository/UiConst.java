package org.openl.rules.ui.repository;

/**
 * UI / RichFaces related constants
 *
 * @author Aleh Bykhavets
 */
public class UiConst {
    // <rich:treeNode type="..."
    public static final String TYPE_FILE = "file";
    public static final String TYPE_FOLDER = "folder";
    public static final String TYPE_PROJECT = "project";
    public static final String TYPE_REPOSITORY = "repository";

    // <rich:treeNode icon="..." iconLeaf="..."
    public static final String ICON_FILE = "/images/jcr/tree_document.gif";
    public static final String ICON_FOLDER = "/images/jcr/tree_folder.gif";
    public static final String ICON_PROJECT = "/images/jcr/tree_server.gif";
    public static final String ICON_REPOSITORY = "/images/jcr/tree_storage.gif";

    //
    public static final String ICON_FOLDER_MOD = "/images/jcr/tree_folder_major.gif";

    //
    public static final String ICON_PROJECT_CHECKED_OUT = "/images/jcr/tree-prj-checkedout.png";
    public static final String ICON_PROJECT_CLOSED = "/images/jcr/tree-prj-closed.png";
    public static final String ICON_PROJECT_CLOSED_LOCKED = "/images/jcr/tree-prj-closed-locked.png";
    public static final String ICON_PROJECT_DELETED = "/images/jcr/tree-prj-deleted.png";
    public static final String ICON_PROJECT_LOCAL = "/images/jcr/tree-prj-local.png";
    public static final String ICON_PROJECT_OPENED = "/images/jcr/tree-prj-opened.png";
    public static final String ICON_PROJECT_OPENED_LOCKED = "/images/jcr/tree-prj-opened-locked.png";

    //
    public static final String OUTCOME_SUCCESS = "success";
    public static final String OUTCOME_FAILURE = "failure";
}
