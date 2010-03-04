package org.openl.rules.types.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.runtime.IRuntimeContext;
import org.openl.types.IOpenMethod;

public class MatchingOpenMethodDispatcher extends OpenMethodDispatcher {

    private IPropertiesContextMatcher matcher = new DefaultPropertiesContextMatcher();

    // list of properties that have non-null values in candidates space
    // could be used to optimize performance, if null - all properties from the
    // group
    // Business Dimension will have to apply
    private Set<String> propertiesSet;

    public MatchingOpenMethodDispatcher(IOpenMethod method) {
        super();
        decorate(method);
    }

    @Override
    protected IOpenMethod findMatchingMethod(List<IOpenMethod> candidates, IRuntimeContext context) {

        Set<IOpenMethod> selected = new HashSet<IOpenMethod>(candidates);

        selectCandidates(selected, (IRulesRuntimeContext) context);

        // Temporal implementation of the active/inactive method feature for
        // overloaded methods only.
        // 
        // Use case: if method has the active table property with 'false' value
        // it will be ignored by method dispatcher.
        //
        removeInactiveMethods(selected);
        
        switch (selected.size()) {
            case 0:
                // TODO add more detailed information about error, consider
                // context values printout, may be log of constraints that
                // removed candidates
                throw new RuntimeException(String.format(
                        "No matching methods for the context. Details: \n%1$s\nContext: %2$s", toString(candidates),
                        context.toString()));

            case 1:
                return selected.iterator().next();

            default:
                // TODO add more detailed information about error, consider
                // context values printout, may be log of constraints,
                // list of remaining methods with properties
                throw new RuntimeException(String.format("Ambigous method dispatch. Details: \n%1$s\nContext: %2$s",
                        toString(candidates), context.toString()));
        }

    }

    private void selectCandidates(Set<IOpenMethod> selected, IRulesRuntimeContext context) {
        // <<< INSERT MatchingProperties >>>
		selectCandidatesByProperty("effectiveDate", selected, context);
		selectCandidatesByProperty("expirationDate", selected, context);
		selectCandidatesByProperty("lob", selected, context);
		selectCandidatesByProperty("usregion", selected, context);
		selectCandidatesByProperty("country", selected, context);
		selectCandidatesByProperty("state", selected, context);
        // <<< END INSERT MatchingProperties >>>
    }

    private void selectCandidatesByProperty(String propName, Set<IOpenMethod> selected, IRulesRuntimeContext context) {

        if (propertiesSet != null && !propertiesSet.contains(propName)) {
            return;
        }

        List<IOpenMethod> nomatched = new ArrayList<IOpenMethod>();
        List<IOpenMethod> matchedByDefault = new ArrayList<IOpenMethod>();

        boolean matchExists = false;

        for (IOpenMethod method : selected) {

            ITableProperties props = getTableProperties(method);
            MatchingResult res = matcher.match(propName, props, context);

            switch (res) {
                case NO_MATCH:
                    nomatched.add(method);
                    break;
                case MATCH_BY_DEFAULT:
                    matchedByDefault.add(method);
                    break;
                case MATCH:
                    matchExists = true;
            }
        }

        selected.removeAll(nomatched);

        if (matchExists) {
            selected.removeAll(matchedByDefault);
        }
    }

    private ITableProperties getTableProperties(IOpenMethod method) {
        return ((TableSyntaxNode) method.getInfo().getSyntaxNode()).getTableProperties();
    }

    private void removeInactiveMethods(Set<IOpenMethod> candidates) {

        List<IOpenMethod> inactiveCandidates = new ArrayList<IOpenMethod>();

        for (IOpenMethod candidate : candidates) {
            if (!isActive(candidate)) {
                inactiveCandidates.add(candidate);
            }
        }

        candidates.removeAll(inactiveCandidates);
    }

    private boolean isActive(IOpenMethod method) {

        ITableProperties tableProperties = ((TableSyntaxNode) method.getInfo().getSyntaxNode()).getTableProperties();

        return tableProperties.getActive() == null || tableProperties.getActive().booleanValue();
    }

    private String toString(List<IOpenMethod> methods) {

        StringBuilder builder = new StringBuilder();
        builder.append("Candidates: {\n");

        for (IOpenMethod method : methods) {
            ITableProperties tableProperties = getTableProperties(method);
            builder.append(tableProperties.toString());
            builder.append("\n");
        }

        builder.append("}\n");

        return builder.toString();
    }
}
