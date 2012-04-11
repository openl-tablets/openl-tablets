package org.openl.rules.types.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.rules.context.IRulesContext;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.runtime.IContext;
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
    protected IOpenMethod findMatchingMethod(List<IOpenMethod> candidates, IContext context) {

        Set<IOpenMethod> selected = new HashSet<IOpenMethod>(candidates);

        selectCandidates(selected, (IRulesContext) context);

        switch (selected.size()) {
            case 0:
                // TODO add more detailed information about error, consider
                // context values printout, may be log of constraints that
                // removed candidates
                throw new RuntimeException("No matching methods for the context");

            case 1:
                return selected.iterator().next();

            default:
                // TODO add more detailed information about error, consider
                // context values printout, may be log of constraints,
                // list of remaining methods with properties
                throw new RuntimeException("Ambigous method dispatch");
        }

    }

    private void selectCandidates(Set<IOpenMethod> selected, IRulesContext context) {
        // <<< INSERT MatchingProperties >>>
		selectCandidatesByProperty("effectiveDate", selected, context);
		selectCandidatesByProperty("expirationDate", selected, context);
		selectCandidatesByProperty("lob", selected, context);
		selectCandidatesByProperty("usregion", selected, context);
		selectCandidatesByProperty("country", selected, context);
		selectCandidatesByProperty("state", selected, context);
        // <<< END INSERT MatchingProperties >>>
    }

    private void selectCandidatesByProperty(String propName, Set<IOpenMethod> selected, IRulesContext context) {

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
        return ((TableSyntaxNode) method.getInfo().getSyntaxNode()).getTableProperties2();
    }
}
