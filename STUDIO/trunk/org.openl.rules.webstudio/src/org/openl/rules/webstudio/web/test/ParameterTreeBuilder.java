package org.openl.rules.webstudio.web.test;

import java.util.Date;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.base.INamedThing;
import org.openl.rules.calc.result.SpreadsheetResultHelper;
import org.openl.rules.testmethod.ParameterWithValueDeclaration;
import org.openl.rules.webstudio.web.trace.TracerObjectDecorator;
import org.openl.types.IOpenClass;
import org.openl.types.java.OpenClassHelper;
import org.openl.vm.SimpleVM;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

/**
 * @author DLiauchuk
 */
@ManagedBean
@RequestScoped
public class ParameterTreeBuilder {

    public static ParameterDeclarationTreeNode createNode(IOpenClass fieldType,Object value,
            String fieldName, ParameterDeclarationTreeNode parent) {
        if (OpenClassHelper.isCollection(fieldType)) {
            return new CollectionParameterTreeNode(fieldName, value, fieldType, parent);
        } else if (!fieldType.isSimple()) {
            return createComplexBeanNode(fieldType, value, fieldName, parent);
        } else {
            return createSimpleNode(fieldType, value, fieldName, parent);
        }
    }

    public static ParameterDeclarationTreeNode createComplexBeanNode(IOpenClass fieldType,
            Object value,
            String fieldName,
            ParameterDeclarationTreeNode parent) {
        if (canConstruct(fieldType)) {
            return new ComplexParameterTreeNode(fieldName, value, fieldType, parent);
        } else {
            UnmodifiableParameterTreeNode node = new UnmodifiableParameterTreeNode(fieldName, value, fieldType, parent);
            node.setWarnMessage(String.format("Can not construct bean of type '%s'. Make sure that it has public constructor without parameters.",
                fieldType.getDisplayName(INamedThing.SHORT)));
            return node;
        }
    }

    /**
     * TODO: Refactor. Not a good way to check if it is possible to instantiate
     * @return
     */
    public static boolean canConstruct(IOpenClass type) {
        boolean result = true;
        try {
            type.newInstance(new SimpleVM().getRuntimeEnv());
        } catch (Exception ex) {
            result = false;
        }
        return result;
    }
    public static ParameterDeclarationTreeNode createSimpleNode(IOpenClass fieldType,
            Object value,
            String fieldName,
            ParameterDeclarationTreeNode parent) {
        if (parent == null || parent.getType().isArray() || parent.getType().getField(fieldName).isWritable()) {
            return new SimpleParameterTreeNode(fieldName, value, fieldType, parent);
        } else {
            UnmodifiableParameterTreeNode node = new UnmodifiableParameterTreeNode(fieldName, value, fieldType, parent);
            node.setWarnMessage(String.format("Field '%s' is not writable.", fieldName));
            return node;
        }
    }

    public TreeNode getRoot(Object objParam) {
        ParameterWithValueDeclaration param = (ParameterWithValueDeclaration) objParam;
        TreeNodeImpl root = new TreeNodeImpl();

        ParameterDeclarationTreeNode treeNode = null;
        if (param != null) {
            treeNode = createNode(param.getType(), param.getValue(), null, null);
            root.addChild(param.getName(), treeNode);
        }
        return root;
    }

    public boolean isDateParameter(Object value) {
        return value instanceof Date;
    }
    
    public boolean isSpreadsheetResult(Object value) {
    	if (value != null) {
    		return SpreadsheetResultHelper.isSpreadsheetResult(value.getClass());
    	} 
    	return false;
    }
    
    public String formattedResult(Object value) {
    	return TracerObjectDecorator.format(value);
    }

}
