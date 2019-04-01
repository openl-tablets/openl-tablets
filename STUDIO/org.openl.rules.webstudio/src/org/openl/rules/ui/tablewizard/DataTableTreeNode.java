package org.openl.rules.ui.tablewizard;

import java.util.Iterator;
import java.util.LinkedHashMap;

import org.openl.rules.table.xls.builder.DataTableField;
import org.richfaces.model.TreeNode;

/**
 * A tree node, containing description of Data Table's field columns
 * 
 * @author NSamatov
 * 
 */
public class DataTableTreeNode implements TreeNode {
    private static final String SIMPLE = "simple";
    private static final String COMPLEX = "complex";

    private final DataTableField value;
    private final LinkedHashMap<Object, DataTableTreeNode> children = new LinkedHashMap<>();
    private final boolean root;
    private boolean editForeignKey;

    /**
     * Create instance of a node
     * 
     * @param value description of a data table field
     * @param root if true, then this node is root
     */
    public DataTableTreeNode(DataTableField value, boolean root) {
        this.value = value;
        this.root = root;

        editForeignKey = value.isComplex(); // set default value
    }

    @Override
    public TreeNode getChild(Object key) {
        return getChildren().get(key);
    }

    @Override
    public int indexOf(Object key) {
        int i = 0;

        for (Object k : getChildren().keySet()) {
            if (k.equals(key))
                return i;
            i++;
        }

        return -1;
    }

    @Override
    public Iterator<Object> getChildrenKeysIterator() {
        return getChildren().keySet().iterator();
    }

    @Override
    public boolean isLeaf() {
        return getChildren().isEmpty();
    }

    @Override
    public void addChild(Object key, TreeNode child) {
        if (child == null) {
            removeChild(key);
            return;
        }

        if (!(child instanceof DataTableTreeNode)) {
            String message = String.format("Unsupported node type %s", child.getClass().getName());
            throw new IllegalArgumentException(message);
        }

        getChildren().put(key, (DataTableTreeNode) child);
    }

    @Override
    public void insertChild(int idx, Object key, TreeNode child) {
        throw new UnsupportedOperationException("insertChild() method is not supported. Use addChild() instead.");
    }

    @Override
    public void removeChild(Object key) {
        getChildren().remove(key);
    }

    /**
     * Get description of a data table field
     * 
     * @return description of a data table field
     */
    public DataTableField getValue() {
        return value;
    }

    public boolean isRoot() {
        return root;
    }

    /**
     * Determine if we should show to a user a foreign key input text field
     * 
     * @return if true, then show foreign key input text field else hide it
     */
    public boolean isEditForeignKey() {
        return editForeignKey;
    }

    /**
     * Set if we should show to a user a foreign key input text field
     * 
     * @param editForeignKey if true, then show foreign key input text field else hide it
     */
    public void setEditForeignKey(boolean editForeignKey) {
        this.editForeignKey = editForeignKey;
    }

    /**
     * Get node type
     * 
     * @return node type
     */
    public String getNodeType() {
        return value.isComplex() ? COMPLEX : SIMPLE;
    }

    // delegates

    public String getName() {
        return value.getName();
    }

    public String getBusinessName() {
        return value.getBusinessName();
    }

    public void setBusinessName(String businessName) {
        value.setBusinessName(businessName);
    }

    public void setForeignKeyTable(String table) {
        value.setForeignKeyTable(table);
    }

    public String getForeignKeyTable() {
        return value.getForeignKeyTable();
    }

    public void setForeignKeyColumn(String column) {
        value.setForeignKeyColumn(column);
    }

    public String getForeignKeyColumn() {
        return value.getForeignKeyColumn();
    }

    public String getTypeName() {
        return value.getTypeName();
    }

    public boolean isComplex() {
        return value.isComplex();
    }

    public void useAggregatedFields() {
        value.useAggregatedFields();

        for (DataTableField field : value.getAggregatedFields()) {
            children.put(field.getName(), new DataTableTreeNode(field, false));
        }
    }

    public void useForeignKey() {
        value.useForeignKey();
        children.clear();
    }

    protected LinkedHashMap<Object, DataTableTreeNode> getChildren() {
        return children;
    }
}
