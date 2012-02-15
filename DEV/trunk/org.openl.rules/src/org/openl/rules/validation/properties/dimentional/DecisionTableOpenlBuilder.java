package org.openl.rules.validation.properties.dimentional;

import java.util.LinkedHashMap;
import java.util.Map;

import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.DecisionTableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.OpenMethodHeader;

/**
 * Builder for executable Decision table. 
 * 
 * @author DLiauchuk
 *
 */
public class DecisionTableOpenlBuilder implements Builder<DecisionTable>{
        
    private String tableName;
    private IOpenClass returnType;
    private Map<String, IOpenClass> incomeParams;
    private TableSyntaxNode tsn;
    private ModuleOpenClass moduleOpenClass;
    
    public DecisionTableOpenlBuilder(String tableName, IOpenClass returnType, Map<String, IOpenClass> incomeParams) {
        this.tableName = tableName;
        this.returnType = returnType;        
        //LinkedHashMap to save the sequence of params
        this.incomeParams = new LinkedHashMap<String, IOpenClass>(incomeParams);
    }
    
    public DecisionTable build() {
        IMethodSignature signature = new MethodSignature(incomeParams.values().toArray(
                new IOpenClass[incomeParams.size()]), incomeParams.keySet().toArray(new String[incomeParams.size()]));
        IOpenClass declaringClass = moduleOpenClass;

        OpenMethodHeader header = new OpenMethodHeader(tableName, returnType, signature, declaringClass);
        DecisionTableBoundNode boundNode = new DecisionTableBoundNode(tsn, moduleOpenClass.getOpenl(), header, moduleOpenClass);
        return new DecisionTable(header, boundNode);
    }

	public void setModuleOpenClass(XlsModuleOpenClass moduleOpenClass) {
		this.moduleOpenClass = moduleOpenClass;
	}

	public void setTableSyntaxNode(TableSyntaxNode tsn) {
		this.tsn = tsn;
	}
}
