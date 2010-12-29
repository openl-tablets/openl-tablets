package org.openl.rules.types.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.TableProperties;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.runtime.IRuntimeContext;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

public class MatchingOpenMethodDispatcher extends OpenMethodDispatcher {

    private IPropertiesContextMatcher matcher = new DefaultPropertiesContextMatcher();

    // list of properties that have non-null values in candidates space
    // could be used to optimize performance, if null - all properties from the
    // group
    // Business Dimension will have to apply
    private Set<String> propertiesSet;

    private XlsModuleOpenClass moduleOpenClass;
    private OverloadedMethodChoiceTraceObject traceObject;

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
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        if (Tracer.isTracerOn()) {
            return invokeTraced(target, params, env);
        } else {
            return super.invoke(target, params, env);
        }
    }

    public Object invokeTraced(Object target, Object[] params, IRuntimeEnv env) {
        Tracer tracer = Tracer.getTracer();

        try {
            traceObject = new OverloadedMethodChoiceTraceObject((DecisionTable)getDispatcherTable().getMember(), params, getCandidates());
            tracer.push(traceObject);
            return super.invoke(target, params, env);
        }finally{
            tracer.pop();
        }
    }

    @Override
    protected IOpenMethod findMatchingMethod(List<IOpenMethod> candidates, IRuntimeContext context) {

        List<IOpenMethod> methods = extractCandidates(candidates);
        Set<IOpenMethod> selected = new HashSet<IOpenMethod>(methods);

        selectCandidates(selected, (IRulesRuntimeContext) context);

        // Temporal implementation of the active/inactive method feature for
        // overloaded methods only.
        // 
        // Use case: if method has the active table property with 'false' value
        // it will be ignored by method dispatcher.
        //
        removeInactiveMethods(selected);
        if (Tracer.isTracerOn()) {
            traceObject.setResult(selected);
        }
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
                throw new OpenLRuntimeException(String.format("Ambiguous method dispatch. Details: \n%1$s\nContext: %2$s",
                        toString(candidates), context.toString()));
        }

    }
    
    private TableSyntaxNode[] getTableSyntaxNodes() {
        XlsMetaInfo xlsMetaInfo = moduleOpenClass.getXlsMetaInfo();
        return xlsMetaInfo.getXlsModuleNode().getXlsTableSyntaxNodes();
    }

    private TableSyntaxNode getDispatcherTable() {
        TableSyntaxNode[] tables = getTableSyntaxNodes();
        for (TableSyntaxNode tsn : tables) {
            if (DispatcherTablesBuilder.isDispatcherTable(tsn) && tsn.getMember().getName().endsWith(getName())) {
                return tsn;
            }
        }
        throw new OpenLRuntimeException(String.format("There is no dispatcher table for [%s] method.", getName()));
    }

    private void selectCandidates(Set<IOpenMethod> selected, IRulesRuntimeContext context) {
        // <<< INSERT MatchingProperties >>>
		selectCandidatesByProperty("effectiveDate", selected, context);
		selectCandidatesByProperty("expirationDate", selected, context);
		selectCandidatesByProperty("lob", selected, context);
		selectCandidatesByProperty("usregion", selected, context);
		selectCandidatesByProperty("country", selected, context);
		selectCandidatesByProperty("state", selected, context);
		selectCandidatesByProperty("region", selected, context);
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
        //FIXME
        TableProperties properties = new TableProperties();
        if(method.getInfo().getProperties() != null){
            for (Entry<String, Object> property : method.getInfo().getProperties().entrySet()) {
                properties.setFieldValue(property.getKey(), property.getValue());
            }
        }
        return properties;
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

        ITableProperties tableProperties = getTableProperties(method);

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
    
    private List<IOpenMethod> extractCandidates(List<IOpenMethod> methods) {

        List<IOpenMethod> result = new ArrayList<IOpenMethod>();
        
        for (IOpenMethod method : methods) {
            if (method instanceof OpenMethodDispatcher) {
                OpenMethodDispatcher dispatcher = (OpenMethodDispatcher) method;
                List<IOpenMethod> candidates = extractCandidates(dispatcher.getCandidates());
                
                for (IOpenMethod candidate : candidates) {
//                    if (!(candidate instanceof OpenMethodDispatcher)) {
                        result.add(candidate);
//                    }
                }
            } else {
                result.add(method);
            }
        }
        
        return result;
    }
    
}
