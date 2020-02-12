package org.openl.rules.webstudio.web.repository;

/**
 * UI / RichFaces related constants
 *
 * @author Aleh Bykhavets
 */
public final class UiConst {
    // <rich:treeNode type="..."
    public static final String TYPE_FILE = "file";
    public static final String TYPE_FOLDER = "folder";
    public static final String TYPE_PROJECT = "project";
    public static final String TYPE_DEPLOYMENT_PROJECT = "deployment";
    public static final String TYPE_REPOSITORY = "repository";
    public static final String TYPE_DEPLOYMENT_REPOSITORY = "drepository";

    public static final String TYPE_PRODUCTION_FILE = "prodfile";
    public static final String TYPE_PRODUCTION_FOLDER = "prodfolder";
    public static final String TYPE_PRODUCTION_PROJECT = "prodproject";
    public static final String TYPE_PRODUCTION_REPOSITORY = "prodrepository";
    public static final String TYPE_PRODUCTION_DEPLOYMENT_PROJECT = "proddeployment";

    // <rich:treeNode icon="..." iconLeaf="..."
    public static final String ICON_FILE = "/webresource/images/repository/file.gif";
    public static final String ICON_FOLDER = "/webresource/images/repository/folder.gif";
    public static final String ICON_REPOSITORY = "/webresource/images/repository/storage.gif";

    public static final String ICON_PROJECT_OPENED_FOR_EDITING = "/webresource/images/repository/checkedout.gif";
    public static final String ICON_PROJECT_CLOSED = "/webresource/images/repository/closed.gif";
    public static final String ICON_PROJECT_CLOSED_LOCKED = "/webresource/images/repository/closed-locked.gif";
    public static final String ICON_PROJECT_DELETED = "/webresource/images/repository/deleted.gif";
    public static final String ICON_PROJECT_LOCAL = "/webresource/images/repository/local.gif";
    public static final String ICON_PROJECT_OPENED = "/webresource/images/repository/opened.gif";
    public static final String ICON_PROJECT_OPENED_LOCKED = "/webresource/images/repository/opened-locked.gif";

    // <rich:treeNode icon="..." iconLeaf="..."
    public static final String ICON_DIFF_ADDED = "/images/diff/fileadd.gif";
    public static final String ICON_DIFF_REMOVED = "/images/diff/filedelete.gif";
    public static final String ICON_DIFF_EQUALS = "/images/diff/filemodified.gif";
    public static final String ICON_DIFF_DIFFERS = "/images/diff/leaf.gif";

    //
    public static final String OUTCOME_SUCCESS = "success";
    public static final String OUTCOME_FAILURE = "failure";

    // default.css
    public static final String STYLE_ERROR = "error";
    public static final String STYLE_WARNING = "warning";

    private UiConst() {
    }
}
