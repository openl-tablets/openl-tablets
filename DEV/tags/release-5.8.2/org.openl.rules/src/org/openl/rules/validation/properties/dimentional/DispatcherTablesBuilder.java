package org.openl.rules.validation.properties.dimentional;

import java.util.ArrayList;
import java.util.List;

import org.openl.OpenL;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.rules.source.impl.VirtualSourceCodeModule;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.rules.types.impl.OverloadedMethodsDispatcherTable;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;

/**
 * Builds dispatcher decision table for each
 * {@link MatchingOpenMethodDispatcher} in ModuleOpenClass.
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
        for (MatchingOpenMethodDispatcher dispatcher : getAllMethodDispatchers()) {
            build(dispatcher);
        }
    }

    private void build(MatchingOpenMethodDispatcher dispatcher) {
            TableSyntaxNodeDispatcherBuilder dispBuilder = new TableSyntaxNodeDispatcherBuilder(openl,
                moduleContext,
                moduleOpenClass,
                dispatcher);
            TableSyntaxNode tsn = dispBuilder.build();
            if (!isVirtualWorkbook()) {
                addNewTsnToTopNode(tsn);
            }
            if (dispatcher instanceof OverloadedMethodsDispatcherTable) {
                ((OverloadedMethodsDispatcherTable) dispatcher).setDispatchingOpenMethod((IOpenMethod) tsn.getMember());
            }
    }
    
    private boolean isVirtualWorkbook(){
        return moduleOpenClass.getXlsMetaInfo().getXlsModuleNode().getModule() instanceof VirtualSourceCodeModule;
    }
    
    private void addNewTsnToTopNode(TableSyntaxNode tsn) {        
        XlsMetaInfo xlsMetaInfo = moduleOpenClass.getXlsMetaInfo();
        ((WorkbookSyntaxNode)xlsMetaInfo.getXlsModuleNode().getWorkbookSyntaxNodes()[0])
            .getWorksheetSyntaxNodes()[0].addNode(tsn);     
    }
    
    private List<MatchingOpenMethodDispatcher> getAllMethodDispatchers(){
        List<MatchingOpenMethodDispatcher> dispatchers = new ArrayList<MatchingOpenMethodDispatcher>();
        for (IOpenMethod method : moduleOpenClass.getMethods()) {
            if (method instanceof MatchingOpenMethodDispatcher) {
                dispatchers.add((MatchingOpenMethodDispatcher) method);
            }
        }
        return dispatchers;
    }
}
