package org.openl.rules.types.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.binding.TableVersionComparator;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.DefaultInvokerWithTrace;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.method.TracedObjectFactory;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.properties.DimensionPropertiesMethodKey;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.TableProperties;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.runtime.IRuntimeContext;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.MethodDelegator;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

/**
 * Represents group of methods(rules) overloaded by dimension properties.
 * 
 * TODO: refactor to use {@link ExecutableRulesMethod} in this class instead of
 * common {@link IOpenMethod}
 * 
 * TODO: refactor invoke functionality. Use {@link DefaultInvokerWithTrace}.
 */
public class MatchingOpenMethodDispatcher extends OpenMethodDispatcher {
    public static final String DISPATCHING_MODE_PROPERTY = "dispatching.mode";
    public static final String DISPATCHING_MODE_JAVA = "java";

    private IPropertiesContextMatcher matcher = new DefaultPropertiesContextMatcher();
    private ITablePropertiesSorter prioritySorter = new DefaultTablePropertiesSorter();

    // list of properties that have non-null values in candidates space
    // could be used to optimize performance, if null - all properties from the
    // group
    // Business Dimension will have to apply
    private Set<String> propertiesSet;

    private XlsModuleOpenClass moduleOpenClass;

    private ATableTracerNode traceObject;

    private List<IOpenMethod> candidatesSorted;

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
        int pos = searchForTheSameTable(candidate);
        if (pos == -1) {
            // add new candidate
            super.addMethod(candidate);
            candidatesSorted = null;
        } else {
            // replace by newer or active
            if (new TableVersionComparator().compare((ExecutableRulesMethod) getCandidates().get(pos),
                (ExecutableRulesMethod) candidate) > 0) {
                getCandidates().set(pos, candidate);
            }
        }

    }

    /**
     * For different versions of the some table we should use in dispatching
     * only the newest or active table.
     */
    private int searchForTheSameTable(IOpenMethod method) {
        if (method instanceof ExecutableRulesMethod) {
            DimensionPropertiesMethodKey methodKey = new DimensionPropertiesMethodKey((ExecutableRulesMethod) method);
            for (int i = 0; i < getCandidates().size(); i++) {
                IOpenMethod candidate = getCandidates().get(i);
                if (candidate instanceof ExecutableRulesMethod && methodKey.equals(new DimensionPropertiesMethodKey((ExecutableRulesMethod) candidate))) {
                    return i;
                }
            }
        }
        return -1;
    }

    public Object invokeTraced(Object target, Object[] params, IRuntimeEnv env) {
        traceObject = null;
        Tracer tracer = Tracer.getTracer();

        /**
         * this block is for overloaded by active property tables without any
         * dimension property. all not active tables should be ignored.
         */
        List<IOpenMethod> methods = getCandidates();
        Set<IOpenMethod> selected = new HashSet<IOpenMethod>(methods);

        traceObject = getTracedObject(selected, params);
        tracer.push(traceObject);
        try {
            return super.invoke(target, params, env);
        } catch (Exception e) {
            traceObject.setError(e);
            return null;
        } finally {
            tracer.pop();
        }

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

        if (Tracer.isTracerOn()) {
            traceObject.setResult(selected);
        }

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
                return selected.iterator().next();

            default:
                // TODO add more detailed information about error, consider
                // context values printout, may be log of constraints,
                // list of remaining methods with properties
                throw new OpenLRuntimeException(String.format("Ambiguous method dispatch. Details: \n%1$s\nContext: %2$s",
                    toString(candidates),
                    context.toString()));
        }

    }

    private TableSyntaxNode[] getTableSyntaxNodes() {
        XlsMetaInfo xlsMetaInfo = moduleOpenClass.getXlsMetaInfo();
        return xlsMetaInfo.getXlsModuleNode().getXlsTableSyntaxNodes();
    }

    public TableSyntaxNode getDispatcherTable() {
        TableSyntaxNode[] tables = getTableSyntaxNodes();
        for (TableSyntaxNode tsn : tables) {
            if (DispatcherTablesBuilder.isDispatcherTable(tsn) && tsn.getMember().getName().endsWith(getName())) {
                return tsn;
            }
        }
        throw new OpenLRuntimeException(String.format("There is no dispatcher table for [%s] method.", getName()));
    }

    private void maxMinSelectCandidates(Set<IOpenMethod> selected, IRulesRuntimeContext context) {
        List<IOpenMethod> sorted = prioritySorter.sort(selected);
        // FIXME temporary solution sort all candidates and remove all that has
        // priority different from most prior table.
        for (IOpenMethod candidate : sorted) {
            if (prioritySorter.getMethodsComparator().compare(candidate, sorted.get(0)) != 0) {
                selected.remove(candidate);
            }
        }
    }

    // <<< INSERT MatchingProperties >>>
    private void selectCandidates(Set<IOpenMethod> selected, IRulesRuntimeContext context) {
        selectCandidatesByProperty("effectiveDate", selected, context);
        selectCandidatesByProperty("expirationDate", selected, context);
        selectCandidatesByProperty("startRequestDate", selected, context);
        selectCandidatesByProperty("endRequestDate", selected, context);
        selectCandidatesByProperty("lob", selected, context);
        selectCandidatesByProperty("usregion", selected, context);
        selectCandidatesByProperty("country", selected, context);
        selectCandidatesByProperty("currency", selected, context);
        selectCandidatesByProperty("lang", selected, context);
        selectCandidatesByProperty("state", selected, context);
        selectCandidatesByProperty("region", selected, context);
    }

    // <<< END INSERT MatchingProperties >>>

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

    public static ITableProperties getTableProperties(IOpenMethod method) {
        // FIXME
        TableProperties properties = new TableProperties();
        if (method.getInfo().getProperties() != null) {
            for (Entry<String, Object> property : method.getInfo().getProperties().entrySet()) {
                properties.setFieldValue(property.getKey(), property.getValue());
            }
        }
        return properties;
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

    @Override
    public List<IOpenMethod> getCandidates() {
        if (candidatesSorted == null) {
            candidatesSorted = prioritySorter.sort(super.getCandidates());
        }
        return candidatesSorted;
    }
}
