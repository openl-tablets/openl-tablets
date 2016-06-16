package org.openl.rules.types.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.runtime.IRuntimeContext;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

/**
 * Represents group of methods(rules) overloaded by dimension properties.
 * 
 * TODO: refactor invoke functionality. Use
 * {@link org.openl.rules.method.RulesMethodInvoker}.
 */
public class MatchingOpenMethodDispatcher extends OpenMethodDispatcher {
    // The fields below hold only algorithms and they don't change during
    // application lifetime. There is no need
    // to hold a new instance of that objects for every of thousands of
    // MatchingOpenMethodDispatchers. That's why
    // they were made static.
    private static final IPropertiesContextMatcher matcher = new DefaultPropertiesContextMatcher();
    private static final DefaultTablePropertiesSorter prioritySorter = new DefaultTablePropertiesSorter();
    private static final DefaultPropertiesIntersectionFinder intersectionMatcher = new DefaultPropertiesIntersectionFinder();

    private XlsModuleOpenClass moduleOpenClass;

    private List<IOpenMethod> candidatesSorted;

    private IOpenMethod decisionTableOpenMethod;

    public IOpenMethod getDecisionTableOpenMethod() {
        return decisionTableOpenMethod;
    }

    public void setDecisionTableOpenMethod(IOpenMethod decisionTableOpenMethod) {
        this.decisionTableOpenMethod = decisionTableOpenMethod;
    }

    public MatchingOpenMethodDispatcher() { // For CGLIB proxing
    }

    public MatchingOpenMethodDispatcher(IOpenMethod method, XlsModuleOpenClass moduleOpenClass) {
        super();
        decorate(method);
        this.moduleOpenClass = moduleOpenClass;
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return moduleOpenClass;
    }

    @Override
    public void addMethod(IOpenMethod candidate) {
        super.addMethod(candidate);
        candidatesSorted = null;
    }

    @Override
    protected IOpenMethod findMatchingMethod(List<IOpenMethod> candidates, IRuntimeContext context) {

        Set<IOpenMethod> selected = new HashSet<IOpenMethod>(candidates);

        selectCandidates(selected, (IRulesRuntimeContext) context);
        maxMinSelectCandidates(selected, (IRulesRuntimeContext) context);

        switch (selected.size()) {
            case 0:
                // TODO add more detailed information about error, consider
                // context values printout, may be log of constraints that
                // removed candidates
                throw new OpenLRuntimeException(
                    String.format("No matching methods for the context. Details: \n%1$s\nContext: %2$s",
                        toString(candidates),
                        context.toString()));

            case 1:
                IOpenMethod matchingMethod = selected.iterator().next();
                return matchingMethod;
            default:
                // TODO add more detailed information about error, consider
                // context values printout, may be log of constraints,
                // list of remaining methods with properties
                throw new OpenLRuntimeException(
                    String.format("Ambiguous method dispatch. Details: \n%1$s\nContext: %2$s",
                        toString(selected),
                        context.toString()));
        }

    }

    public TableSyntaxNode getDispatcherTable() {
        if (decisionTableOpenMethod == null) {
            DispatcherTablesBuilder dispTableBuilder = new DispatcherTablesBuilder(moduleOpenClass,
                moduleOpenClass.getRulesModuleBindingContext());
            dispTableBuilder.build(this);
        }
        if (decisionTableOpenMethod != null) {
            return (TableSyntaxNode) decisionTableOpenMethod.getInfo().getSyntaxNode();
        }
        throw new IllegalStateException(String.format("There is no dispatcher table for [%s] method.", getName()));
    }

    @Override
    public IMemberMetaInfo getInfo() {
        if (getCandidates().size() == 1) {
            return getCandidates().get(0).getInfo();
        }
        return getDispatcherTable().getMember().getInfo();
    }

    private void maxMinSelectCandidates(Set<IOpenMethod> selected, IRulesRuntimeContext context) {
        // If more that one method
        if (selected.size() > 1) {
            List<IOpenMethod> notPriorMethods = new ArrayList<IOpenMethod>();

            List<String> notNullPropertyNames = getNotNullPropertyNames(context);
            // Find the most high priority method
            IOpenMethod mostPriority = null;
            ITableProperties mostPriorityProperties = null;

            for (IOpenMethod candidate : selected) {
                if (mostPriority == null) {
                    mostPriority = candidate;
                    mostPriorityProperties = PropertiesHelper.getTableProperties(mostPriority);
                } else {
                    boolean nested = false;
                    boolean contains = false;

                    ITableProperties candidateProperties = PropertiesHelper.getTableProperties(candidate);
                    int cmp = compareMaxMinPriorities(candidateProperties, mostPriorityProperties);
                    if (cmp < 0) {
                        nested = true;
                        contains = false;
                    } else if (cmp > 0) {
                        nested = false;
                        contains = true;
                    }

                    if (!nested && !contains) {
                        propsLoop: for (String propName : notNullPropertyNames) {

                            switch (intersectionMatcher.match(propName, candidateProperties, mostPriorityProperties)) {
                                case NESTED:
                                    nested = true;
                                    break;
                                case CONTAINS:
                                    contains = true;
                                    break;
                                case EQUALS:
                                    // do nothing
                                    break;
                                case NO_INTERSECTION:
                                case PARTLY_INTERSECTS:
                                    nested = false;
                                    contains = false;
                                    break propsLoop;
                            }
                        }
                    }

                    if (nested && !contains) {
                        notPriorMethods.add(mostPriority);
                        mostPriority = candidate;
                        mostPriorityProperties = PropertiesHelper.getTableProperties(mostPriority);
                    } else if (contains && !nested) {
                        notPriorMethods.add(candidate);
                    }
                }
            }
            selected.removeAll(notPriorMethods);
        }
    }

    private int compareMaxMinPriorities(ITableProperties properties1, ITableProperties properties2) {
        for (Comparator<ITableProperties> comparator : prioritySorter.getMaxMinPriorityRules()) {
            int cmp = comparator.compare(properties1, properties2);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    private void selectCandidates(Set<IOpenMethod> selected, IRulesRuntimeContext context) {
        List<IOpenMethod> nomatched = new ArrayList<IOpenMethod>();

        List<String> notNullPropertyNames = getNotNullPropertyNames(context);

        for (IOpenMethod method : selected) {
            ITableProperties props = PropertiesHelper.getTableProperties(method);

            propsLoop: {
                for (String propName : notNullPropertyNames) {
                    MatchingResult res = matcher.match(propName, props, context);

                    switch (res) {
                        case NO_MATCH:
                            nomatched.add(method);
                            break propsLoop;
                    }
                }
            }
        }

        selected.removeAll(nomatched);
    }

    private String toString(Collection<IOpenMethod> methods) {

        StringBuilder builder = new StringBuilder();
        builder.append("Candidates: {\n");

        for (IOpenMethod method : methods) {
            ITableProperties tableProperties = PropertiesHelper.getTableProperties(method);
            builder.append(tableProperties.toString());
            builder.append("\n");
        }

        builder.append("}\n");

        return builder.toString();
    }

    @Override
    public List<IOpenMethod> getCandidates() {
        if (candidatesSorted == null) {
            candidatesSorted = prioritySorter.sort(super.getCandidates());
        }
        return candidatesSorted;
    }

    // <<< INSERT MatchingProperties >>>
    private List<String> getNotNullPropertyNames(IRulesRuntimeContext context) {
        List<String> propNames = new ArrayList<String>();

        if (context.getCurrentDate() != null) {
            propNames.add("effectiveDate");
        }
        if (context.getCurrentDate() != null) {
            propNames.add("expirationDate");
        }
        if (context.getRequestDate() != null) {
            propNames.add("startRequestDate");
        }
        if (context.getRequestDate() != null) {
            propNames.add("endRequestDate");
        }
        if (context.getCaRegion() != null) {
            propNames.add("caRegions");
        }
        if (context.getCaProvince() != null) {
            propNames.add("caProvinces");
        }
        if (context.getCountry() != null) {
            propNames.add("country");
        }
        if (context.getRegion() != null) {
            propNames.add("region");
        }
        if (context.getCurrency() != null) {
            propNames.add("currency");
        }
        if (context.getLang() != null) {
            propNames.add("lang");
        }
        if (context.getLob() != null) {
            propNames.add("lob");
        }
        if (context.getUsRegion() != null) {
            propNames.add("usregion");
        }
        if (context.getUsState() != null) {
            propNames.add("state");
        }

        return propNames;
    }

// <<< END INSERT MatchingProperties >>>
}
