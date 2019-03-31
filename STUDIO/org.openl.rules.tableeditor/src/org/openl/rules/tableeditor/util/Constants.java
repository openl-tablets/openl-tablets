package org.openl.rules.tableeditor.util;

public final class Constants {
    public static final String MODE_VIEW = "view";
    public static final String MODE_EDIT = "edit";

    public static final String ATTRIBUTE_TABLE = "table";
    public static final String ATTRIBUTE_VIEW = "view";
    public static final String ATTRIBUTE_FILTERS = "filters";
    public static final String ATTRIBUTE_MODE = "mode";
    public static final String ATTRIBUTE_EDITABLE = "editable";
    public static final String ATTRIBUTE_SHOW_FORMULAS = "showFormulas";
    public static final String ATTRIBUTE_COLLAPSE_PROPS = "collapseProps";
    public static final String ATTRIBUTE_BEFORE_EDIT_ACTION = "beforeEditAction";
    public static final String ATTRIBUTE_BEFORE_SAVE_ACTION = "beforeSaveAction";
    public static final String ATTRIBUTE_AFTER_SAVE_ACTION = "afterSaveAction";
    public static final String ATTRIBUTE_ON_BEFORE_EDIT = "onBeforeEdit";
    public static final String ATTRIBUTE_ON_BEFORE_SAVE = "onBeforeSave";
    public static final String ATTRIBUTE_ON_AFTER_SAVE = "onAfterSave";
    public static final String ATTRIBUTE_ON_ERROR = "onError";
    public static final String ATTRIBUTE_ON_REQUEST_START = "onRequestStart";
    public static final String ATTRIBUTE_ON_REQUEST_END = "onRequestEnd";
    public static final String ATTRIBUTE_EXCLUDE_SCRIPTS = "excludeScripts";
    public static final String ATTRIBUTE_ROW_INDEX = "rowIndex";
    public static final String ATTRIBUTE_LINK_BUILDER = "linkBuilder";

    public static final String REQUEST_PARAM_EDITOR_ID = "editorId";
    public static final String REQUEST_PARAM_MODE = "mode";
    public static final String REQUEST_PARAM_CELL = "cell";
    public static final String REQUEST_PARAM_ERROR_CELL = "errorCell";
    public static final String REQUEST_PARAM_URI = "uri";
    public static final String REQUEST_PARAM_VALUE = "value";
    public static final String REQUEST_PARAM_EDITOR = "editor";
    // public static final String REQUEST_PARAM_MOVE = "move";
    public static final String REQUEST_PARAM_ALIGN = "align";
    public static final String REQUEST_PARAM_COLOR = "color";
    public static final String REQUEST_PARAM_INDENT = "indent";
    public static final String REQUEST_PARAM_FONT_BOLD = "bold";
    public static final String REQUEST_PARAM_FONT_ITALIC = "italic";
    public static final String REQUEST_PARAM_FONT_UNDERLINE = "underline";

    public static final String REQUEST_PARAM_ROW = "row";
    public static final String REQUEST_PARAM_COL = "col";
    public static final String REQUEST_PARAM_PROP_NAME = "propName";
    public static final String REQUEST_PARAM_PROP_VALUE = "propValue";

    public static final String TABLE_VIEWER_TYPE = "org.openl.rules.tableeditor.TableViewer";
    public static final String TABLE_EDITOR_TYPE = "org.openl.rules.tableeditor.TableEditor";

    public static final String TABLE_EDITOR_PATTERN = "/tableEditor/";

    public static final String TABLE_EDITOR_CONTROLLER_NAME = "_tableEditorController";

    public static final String TABLE_EDITOR_MODEL_NAME = "_tableEditorModel";

    public static final String TABLE_EDITOR_RESOURCES = "_tableEditorResources";

    public static final String TABLE_EDITOR_PREFIX = "_te";
    public static final String TABLE_EDITOR_WRAPPER_PREFIX = "_editorWrapper";
    public static final String ID_POSTFIX_TABLE = "_table";
    public static final String ID_POSTFIX_MENU = "_menu";
    public static final String ID_POSTFIX_CELL = "_c-";

    /**
     * Indicates cells that can show some meta info. Can contain links. Used in TableEditor.js and TableViewer class
     */
    public static final String TABLE_EDITOR_META_INFO_CLASS = "te-meta-info";

    public static final String THIRD_PARTY_LIBS_PROTOTYPE = "prototype";

    private Constants() {
    }
}
