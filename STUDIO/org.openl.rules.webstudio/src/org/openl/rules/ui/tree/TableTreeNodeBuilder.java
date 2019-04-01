package org.openl.rules.ui.tree;

import java.util.HashMap;
import java.util.Map;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.ui.IProjectTypes;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.impl.InternalDatatypeClass;

/**
 * Builds tree node using table type.
 *
 */
public class TableTreeNodeBuilder extends BaseTableTreeNodeBuilder {

    private static final String OTHER_NODE_KEY = "Other";
    private static final String TABLE_TYPE_NAME = "Table Type";
    private static final String ALIAS_SUFFIX = ".alias";

    /**
     * Internal map that represent dictionary of available table types.
     */
    private Map<String, NodeKey> nodeKeysMap;

    /**
     * Default constructor.
     */
    public TableTreeNodeBuilder() {
        init();
    }

    /**
     * Initialize instance of class.
     */
    private void init() {

        nodeKeysMap = new HashMap<>();

        nodeKeysMap.put(XlsNodeTypes.XLS_DT.toString(),
            new NodeKey(0, new String[] { "Decision", "Decision Tables", "" }));
        nodeKeysMap.put(XlsNodeTypes.XLS_SPREADSHEET.toString(),
            new NodeKey(1, new String[] { "Spreadsheet", "Spreadsheet Tables", "" }));
        nodeKeysMap.put(XlsNodeTypes.XLS_TBASIC.toString(),
            new NodeKey(2, new String[] { "TBasic", "Structured Algorithm Tables", "" }));
        nodeKeysMap.put(XlsNodeTypes.XLS_COLUMN_MATCH.toString(),
            new NodeKey(3, new String[] { "Column Match", "Column Match Tables", "" }));
        nodeKeysMap.put(XlsNodeTypes.XLS_DATA.toString(), new NodeKey(4, new String[] { "Data", "Data Tables", "" }));
        nodeKeysMap.put(XlsNodeTypes.XLS_TEST_METHOD.toString(),
            new NodeKey(5, new String[] { "Test", "Tables with data for method unit tests", "" }));
        nodeKeysMap.put(XlsNodeTypes.XLS_RUN_METHOD.toString(),
            new NodeKey(5, new String[] { "Run", "Tables with run data for methods", "" }));
        nodeKeysMap.put(XlsNodeTypes.XLS_DATATYPE.toString(),
            new NodeKey(6, new String[] { "Datatype", "OpenL Datatypes", "" }));
        nodeKeysMap.put(XlsNodeTypes.XLS_DATATYPE.toString() + ALIAS_SUFFIX,
            new NodeKey(7, new String[] { "Vocabulary", "OpenL Vocabularies", "" }));
        nodeKeysMap.put(XlsNodeTypes.XLS_METHOD.toString(),
            new NodeKey(8, new String[] { "Method", "OpenL Methods", "" }));
        nodeKeysMap.put(XlsNodeTypes.XLS_CONSTANTS.toString(),
            new NodeKey(9, new String[] { "Constants", "Constants Tables", "" }));
        nodeKeysMap.put(XlsNodeTypes.XLS_CONDITIONS.toString(),
            new NodeKey(10, new String[] { "Conditions", "Conditions Tables", "" }));
        nodeKeysMap.put(XlsNodeTypes.XLS_ACTIONS.toString(),
            new NodeKey(11, new String[] { "Actions", "Actions Tables", "" }));
        nodeKeysMap.put(XlsNodeTypes.XLS_RETURNS.toString(),
            new NodeKey(12, new String[] { "Returns", "Returns Tables", "" }));
        nodeKeysMap.put(XlsNodeTypes.XLS_ENVIRONMENT.toString(),
            new NodeKey(13,
                new String[] { "Configuration", "Environment table, used to configure OpenL project", "" }));

        nodeKeysMap.put(OTHER_NODE_KEY,
            new NodeKey(14, new String[] { "Other", "The Tables that do not belong to any known OpenL type", "" }));
        nodeKeysMap.put(XlsNodeTypes.XLS_PROPERTIES.toString(),
            new NodeKey(15, new String[] { "Properties", "Properties Tables", "" }));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getDisplayValue(Object nodeObject, int i) {

        NodeKey nodeKey = getNodeKey(nodeObject);

        return nodeKey.getValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return TABLE_TYPE_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType(Object nodeObject) {
        /*
         * TableSyntaxNode tableSyntaxNode = (TableSyntaxNode) nodeObject; return IProjectTypes.PT_FOLDER + "." +
         * tableSyntaxNode.getType();
         */
        return IProjectTypes.PT_FOLDER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUrl(Object nodeObject) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWeight(Object nodeObject) {

        NodeKey nodeKey = getNodeKey(nodeObject);

        return nodeKey.getWeight();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object makeObject(TableSyntaxNode tableSyntaxNode) {

        return tableSyntaxNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getProblems(Object nodeObject) {
        TableSyntaxNode tsn = (TableSyntaxNode) nodeObject;
        return tsn.getErrors() != null ? tsn.getErrors() : tsn.getValidationResult();
    }

    /**
     * Gets node key for node object.
     *
     * @param nodeObject node object
     * @return node key
     */
    private NodeKey getNodeKey(Object nodeObject) {

        TableSyntaxNode tsn = (TableSyntaxNode) nodeObject;
        String type = tsn.getType();

        // Separate alias datatypes from ordinary datatypes.
        if (tsn.getMember() instanceof InternalDatatypeClass && tsn.getMember().getType() instanceof DomainOpenClass) {
            type += ALIAS_SUFFIX;
        }

        NodeKey nodeKey = nodeKeysMap.get(type);

        if (nodeKey == null) {
            nodeKey = nodeKeysMap.get(OTHER_NODE_KEY);
        }

        return nodeKey;
    }
}
