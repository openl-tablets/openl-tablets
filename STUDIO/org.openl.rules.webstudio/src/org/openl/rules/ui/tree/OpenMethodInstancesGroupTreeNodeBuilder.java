package org.openl.rules.ui.tree;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openl.rules.lang.xls.OverloadedMethodsDictionary;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeKey;
import org.openl.rules.ui.IProjectTypes;
import org.openl.rules.webstudio.WebStudioFormats;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.MethodKey;
import org.openl.util.StringUtils;

/**
 * Builds tree node for group of methods.
 *
 */
public class OpenMethodInstancesGroupTreeNodeBuilder extends OpenMethodsGroupTreeNodeBuilder {

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getDisplayValue(Object nodeObject, int i) {

        TableSyntaxNode tableSyntaxNode = (TableSyntaxNode) nodeObject;

        return TableSyntaxNodeUtils.getTableDisplayValue(tableSyntaxNode, i, WebStudioFormats.getInstance());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType(Object sorterObject) {
        return IProjectTypes.PT_TABLE_GROUP;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUrl(Object sorterObject) {
        return null;
    }

    @Override
    public boolean isBuilderApplicableForObject(TableSyntaxNode tableSyntaxNode) {
        if (tableSyntaxNode.getMember() instanceof IOpenMethod) {
            IOpenMethod method = (IOpenMethod) tableSyntaxNode.getMember();
            OverloadedMethodsDictionary openMethodGroupsDictionary = getOpenMethodGroupsDictionary();
            if (openMethodGroupsDictionary.contains(method)) {
                Set<TableSyntaxNodeKey> methodOverloads = openMethodGroupsDictionary.getAllMethodOverloads(method);
                // If group of methods size is over then 1 create the tree
                // element (folder); otherwise - method is unique and additional
                // element will not be created.
                // author: Alexey Gamanovich
                return methodOverloads != null && methodOverloads.size() > 1;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProjectTreeNode makeNode(TableSyntaxNode tableSyntaxNode, int i) {
        IOpenMethod method = (IOpenMethod) tableSyntaxNode.getMember();
        String folderName = getFolderName(method);
        return new ProjectTreeNode(new String[] {folderName, folderName, folderName}, IProjectTypes.PT_FOLDER, null);
    }

    private String getFolderName(IOpenMethod method) {
        OverloadedMethodsDictionary openMethodGroupsDictionary = getOpenMethodGroupsDictionary();
        Set<TableSyntaxNodeKey> methodOverloads = openMethodGroupsDictionary.getAllMethodOverloads(method);
        return getMajorityName(methodOverloads);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Comparable<?> makeKey(TableSyntaxNode tableSyntaxNode, int i) {

        if (tableSyntaxNode.getMember() instanceof IOpenMethod) {

            IOpenMethod method = (IOpenMethod) tableSyntaxNode.getMember();
            MethodKey methodKey = new MethodKey(method);

            String keyString = methodKey.toString();

            String folderName = getFolderName(method);

            Object nodeObject = makeObject(tableSyntaxNode);

            String[] displayNames = new String[3];
            Arrays.fill(displayNames, folderName + keyString);
            return new NodeKey(getWeight(nodeObject), displayNames);
        }

        return null;
    }

    /**
     * Gets the majority name of methods group.
     *
     * @param overloads collection of TableSyntaxNodeKeys what belong to group.
     * @return majority name
     */
    private String getMajorityName(Set<TableSyntaxNodeKey> overloads) {

        Map<String, Integer> map = new HashMap<>();

        for (TableSyntaxNodeKey oneOverloadVariant : overloads) {
            String[] names = getDisplayValue(oneOverloadVariant.getTableSyntaxNode(), 0);
            String name = names[0];

            Integer value = map.get(name);

            if (value == null) {
                value = 0;
            }

            value += 1;
            map.put(name, value);
        }

        Integer maxNameWeight = 0;
        String majorName = StringUtils.EMPTY;

        Set<Map.Entry<String, Integer>> entries = map.entrySet();

        for (Map.Entry<String, Integer> entry : entries) {

            if (maxNameWeight.compareTo(entry.getValue()) < 0) {
                maxNameWeight = entry.getValue();
                majorName = entry.getKey();
            }
        }

        return majorName;
    }
}