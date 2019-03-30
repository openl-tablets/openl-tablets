package org.openl.binding;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.ExecutableMethod;

/**
 * @author snshor
 *
 */

public class BindingDependencies {

    /**
     * All methods.
     */
    private HashMap<IOpenMethod, IBoundNode> methods = new HashMap<>();

    /**
     * Dependencies to executable Openl rules.
     */
    private HashMap<ExecutableMethod, IBoundNode> rulesMethods = new HashMap<>();

    private HashMap<IBoundNode, IOpenField> fields = new HashMap<>();

    public void addAssign(IBoundNode target, IBoundNode node) {
        target.updateAssignFieldDependency(this);
    }

    public void addFieldDependency(IOpenField field, IBoundNode node) {
        fields.put(node, field);
    }

    public void addMethodDependency(IOpenMethod method, IBoundNode node) {
        methods.put(method, node);
        // check if method is instance of Openl executable rules method.
        if (method instanceof ExecutableMethod) {
            rulesMethods.put((ExecutableMethod) method, node);
        }
    }

    public Map<IBoundNode, IOpenField> getFieldsMap() {
        return fields;
    }

    /**
     * Gets dependencies to executable Openl rules.
     * 
     * @return dependencies to executable Openl rules.
     */
    public Set<ExecutableMethod> getRulesMethods() {
        return rulesMethods.keySet();
    }

    protected Map<ExecutableMethod, IBoundNode> getRulesMethodsMap() {
        return rulesMethods;
    }

    protected Map<IOpenMethod, IBoundNode> getMethodsMap() {
        return methods;
    }

    String setToString(Set<?> set) {
        return set.toString();
    }

    @Override
    public String toString() {
        return "Fields:\n" + setToString(fields.keySet()) + "\nMethods:\n" + setToString(methods.keySet());
    }

    public void visit(IBoundNode node) {
        if (node == null) {
            return;
        }
        node.updateDependency(this);
        visit(node.getTargetNode());
        IBoundNode[] ch = node.getChildren();
        if (ch != null) {
            for (IBoundNode child : ch) {
                visit(child);
            }
        }
    }

}
