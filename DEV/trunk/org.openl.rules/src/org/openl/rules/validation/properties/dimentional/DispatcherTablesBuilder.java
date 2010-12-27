package org.openl.rules.validation.properties.dimentional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openl.OpenL;
import org.openl.rules.ExecutableRulesMethod;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.rules.table.properties.DimensionPropertiesMethodKey;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.MethodKey;

/**
 * Builder for Dispatcher Decision tables for every group of overloaded by dimension properties methods.
 * 
 * @author DLiauchuk
 *
 */
public class DispatcherTablesBuilder {
    
    public static final String DEFAULT_DISPATCHER_TABLE_NAME = "validateGapOverlap";
    
    /**
     * Checks whether the specified TableSyntaxNode is auto generated gap/overlap table or not.  
     * @param tsn TableSyntaxNode to check.
     * @return <code>true</code> if table is dispatcher table.
     */
    public static boolean isDispatcherTable(TableSyntaxNode tsn) {
        IOpenMember member = tsn.getMember();
        if (member instanceof IOpenMethod) {
            return member.getName().startsWith(DEFAULT_DISPATCHER_TABLE_NAME);
        }
        return false;
    }
    
    private OpenL openl;    
    private XlsModuleOpenClass moduleOpenClass;
    private RulesModuleBindingContext moduleContext;
    
    public DispatcherTablesBuilder(OpenL openl, XlsModuleOpenClass moduleOpenClass, 
            RulesModuleBindingContext moduleContext) {
        this.openl = openl;
        this.moduleContext = moduleContext;
        this.moduleOpenClass = moduleOpenClass;        
    }
    
    /**
     * Builds dispatcher tables for every group of overloaded methods.
     * As a result new {@link TableSyntaxNode} objects appears in module.
     */
    public void build() {        
//        Map<MethodKey, List<TableSyntaxNode>> groupedTables = groupExecutableTables();
        Map<MethodKey, List<ExecutableRulesMethod>> groupedMethods = groupExecutableMethods();
        for (List<ExecutableRulesMethod> methodsGroup : groupedMethods.values()) {
            build(methodsGroup);
        }
    }

    private void build(List<ExecutableRulesMethod> methodsGroup) {
        List<ExecutableRulesMethod> overloadedMethodsGroup = excludeOveloadedByVersion(methodsGroup);
        if (overloadedMethodsGroup.size() > 1) {
            TableSyntaxNodeDispatcherBuilder dispBuilder = 
                new TableSyntaxNodeDispatcherBuilder(openl, moduleContext, moduleOpenClass, overloadedMethodsGroup);
            TableSyntaxNode tsn = dispBuilder.build();
            addNewTsnToTopNode(tsn);
        }
    }
    
    private void addNewTsnToTopNode(TableSyntaxNode tsn) {        
        XlsMetaInfo xlsMetaInfo = moduleOpenClass.getXlsMetaInfo();
        ((WorkbookSyntaxNode)xlsMetaInfo.getXlsModuleNode().getWorkbookSyntaxNodes()[0])
            .getWorksheetSyntaxNodes()[0].addNode(tsn);     
    }
    
    private List<ExecutableRulesMethod> excludeOveloadedByVersion(List<ExecutableRulesMethod> methodsGroup) {
        Set<DimensionPropertiesMethodKey> differentTables = new HashSet<DimensionPropertiesMethodKey>();
        List<ExecutableRulesMethod> result = new ArrayList<ExecutableRulesMethod>();        
        for (ExecutableRulesMethod method : methodsGroup) {
            DimensionPropertiesMethodKey key = new DimensionPropertiesMethodKey(method);
            if (!differentTables.contains(key)) {                
                differentTables.add(key);
                result.add(method);
            }
        }
        return result;
    }    
    
    private Map<MethodKey, List<ExecutableRulesMethod>> groupExecutableMethods() {  
        Map<MethodKey, List<ExecutableRulesMethod>> groupedMethods = new HashMap<MethodKey, List<ExecutableRulesMethod>>();
        for (IOpenMethod method : moduleOpenClass.getMethods()) {
            if (method instanceof OpenMethodDispatcher) {
                unwrapDispatcherTable(groupedMethods, (OpenMethodDispatcher)method);
            }
            
            if (method instanceof ExecutableRulesMethod) {                
                addMethod(groupedMethods, (ExecutableRulesMethod)method);
            }
        }
        return groupedMethods;
    }

    private void unwrapDispatcherTable(Map<MethodKey, List<ExecutableRulesMethod>> groupedMethods,
            OpenMethodDispatcher method) {
        for (IOpenMethod dispMethod : method.getCandidates()) {
            addMethod(groupedMethods, dispMethod);
        }
    }

    private void addMethod(Map<MethodKey, List<ExecutableRulesMethod>> groupedMethods, IOpenMethod method) {
        MethodKey key = new MethodKey(method);
        if (!groupedMethods.containsKey(key)) {
            groupedMethods.put(key, new ArrayList<ExecutableRulesMethod>());
        }
        groupedMethods.get(key).add((ExecutableRulesMethod)method);
    }
    
}
