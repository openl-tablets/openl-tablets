package org.openl.rules.webstudio.web.repository;

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
    public static final String TYPE_DEPLOYMENT_PROJECT = "deployment";
    public static final String TYPE_REPOSITORY = "repository";
    public static final String TYPE_DEPLOYMENT_REPOSITORY = "drepository";

    // <rich:treeNode icon="..." iconLeaf="..."
    public static final String ICON_FILE = "/images/repository/file.gif";
    public static final String ICON_FOLDER = "/images/repository/folder.gif";
    public static final String ICON_REPOSITORY = "/images/repository/storage.gif";

    //
    public static final String ICON_PROJECT_CHECKED_OUT = "/images/repository/checkedout.gif";
    public static final String ICON_PROJECT_CLOSED = "/images/repository/closed.gif";
    public static final String ICON_PROJECT_CLOSED_LOCKED = "/images/repository/closed-locked.gif";
    public static final String ICON_PROJECT_DELETED = "/images/repository/deleted.gif";
    public static final String ICON_PROJECT_LOCAL = "/images/repository/local.gif";
    public static final String ICON_PROJECT_OPENED = "/images/repository/opened.gif";
    public static final String ICON_PROJECT_OPENED_LOCKED = "/images/repository/opened-locked.gif";

    //
    public static final String OUTCOME_SUCCESS = "success";
    public static final String OUTCOME_FAILURE = "failure";
}
