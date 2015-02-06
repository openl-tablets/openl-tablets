package org.openl.rules.types.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.DefaultInvokerWithTrace;
import org.openl.rules.method.TracedObjectFactory;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.runtime.IRuntimeContext;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

/**
 * Represents group of methods(rules) overloaded by dimension properties.
 * 
 * TODO: refactor invoke functionality. Use {@link DefaultInvokerWithTrace}.
 */
public class MatchingOpenMethodDispatcher extends OpenMethodDispatcher {
    // The fields below hold only algorithms and they don't change during application lifetime. There is no need
    // to hold a new instance of that objects for every of thousands of MatchingOpenMethodDispatchers. That's why
    // they were made static.
    private static final IPropertiesContextMatcher matcher = new DefaultPropertiesContextMatcher();
    private static final DefaultTablePropertiesSorter prioritySorter = new DefaultTablePropertiesSorter();
    private static final DefaultPropertiesIntersectionFinder intersectionMatcher = new DefaultPropertiesIntersectionFinder();

    private XlsModuleOpenClass moduleOpenClass;

    private List<IOpenMethod> candidatesSorted;

    private ATableTracerNode traceObject;

    private IOpenMethod dispatchingOpenMethod;

    public IOpenMethod getDispatchingOpenMethod() {
        return dispatchingOpenMethod;
    }

    public void setDispatchingOpenMethod(IOpenMethod dispatchingOpenMethod) {
        this.dispatchingOpenMethod = dispatchingOpenMethod;
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

    public Object invokeTraced(Object target, Object[] params, IRuntimeEnv env) {
        Object returnResult = null;
        traceObject = null;
        Tracer tracer = Tracer.getTracer();

        /**
         * this block is for overloaded by active property tables without any
         * dimension property. All not active tables should be ignored.
         */
        List<IOpenMethod> methods = getCandidates();
        Set<IOpenMethod> selected = new HashSet<IOpenMethod>(methods);
        if (selected.size() > 1) {
            traceObject = getTracedObject(selected, params);
            tracer.push(traceObject);
        }
        try {
            returnResult = super.invoke(target, params, env);
        } catch (RuntimeException e) {
            if (traceObject == null) {
                traceObject = getTracedObject(selected, params);
                tracer.push(traceObject);
            }
            traceObject.setError(e);
            throw e;
        } finally {
            if (traceObject != null) {
                tracer.pop();
            }
        }
        return returnResult;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        if (Tracer.isTracerOn()) {
            return invokeTraced(target, params, env);
        } else {
            return super.invoke(target, params, env);
        }
    }

    private ATableTracerNode getTracedObject(Set<IOpenMethod> selected, Object[] params) {
        if (selected.size() == 1) {
            /**
             * if only one table left, we need traced object for this type of
             * table.
             */
            return TracedObjectFactory.getTracedObject((IOpenMethod) selected.toArray()[0], params);
        } else {
            /**
             * in other case trace object for overloaded methods.
             */
            try {
                DecisionTable dispatcherTable = (DecisionTable) getDispatcherTable().getMember();
                return new OverloadedMethodChoiceTraceObject(dispatcherTable, params, getCandidates());
            } catch (OpenLRuntimeException e) {
                ATableTracerNode traceObject = TracedObjectFactory.getTracedObject((IOpenMethod) selected.toArray()[0],
                    params);
                traceObject.setError(e);
                return traceObject;
            }

        }
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
                throw new OpenLRuntimeException(String.format("No matching methods for the context. Details: \n%1$s\nContext: %2$s",
                    toString(candidates),
                    context.toString()));

            case 1:

                IOpenMethod matchingMethod = selected.iterator().next();
                // TODO : refactor
                // traceObject shouldn`t be the field of the class.
                // trace information should be set only into trace method.
                //
                if (Tracer.isTracerOn() && traceObject != null) {
                    traceObject.setResult(matchingMethod);
                }

                return matchingMethod;

            default:
                // TODO add more detailed information about error, consider
                // context values printout, may be log of constraints,
                // list of remaining methods with properties
                throw new OpenLRuntimeException(String.format("Ambiguous method dispatch. Details: \n%1$s\nContext: %2$s",
                    toString(selected),
                    context.toString()));
        }

    }

    private TableSyntaxNode[] getTableSyntaxNodes() {
        XlsMetaInfo xlsMetaInfo = moduleOpenClass.getXlsMetaInfo();
        return xlsMetaInfo.getXlsModuleNode().getXlsTableSyntaxNodes();
    }

    public TableSyntaxNode getDispatcherTable() {
        if (dispatchingOpenMethod != null) {
            return (TableSyntaxNode) dispatchingOpenMethod.getInfo().getSyntaxNode();
        }

        TableSyntaxNode[] tables = getTableSyntaxNodes();
        for (TableSyntaxNode tsn : tables) {
            if (DispatcherTablesBuilder.isDispatcherTable(tsn) && tsn.getMember().getName().endsWith(getName())) {
                return tsn;
            }
        }

        throw new OpenLRuntimeException(String.format("There is no dispatcher table for [%s] method.", getName()));
    }

    @Override
    public IMemberMetaInfo getInfo() {
        if (getCandidates().size() == 1){
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
        if (context.getLob() != null) {
            propNames.add("lob");
        }
        if (context.getUsRegion() != null) {
            propNames.add("usregion");
        }
        if (context.getCountry() != null) {
            propNames.add("country");
        }
        if (context.getCurrency() != null) {
            propNames.add("currency");
        }
        if (context.getLang() != null) {
            propNames.add("lang");
        }
        if (context.getUsState() != null) {
            propNames.add("state");
        }
        if (context.getRegion() != null) {
            propNames.add("region");
        }

        return propNames;
    }

    // <<< END INSERT MatchingProperties >>>
}
