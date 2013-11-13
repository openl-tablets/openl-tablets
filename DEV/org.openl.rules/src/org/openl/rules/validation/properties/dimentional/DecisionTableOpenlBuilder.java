package org.openl.rules.validation.properties.dimentional;

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
    private IMethodSignature signature;
    private TableSyntaxNode tsn;
    private ModuleOpenClass moduleOpenClass;
    
    public DecisionTableOpenlBuilder(String tableName, IOpenClass returnType, IMethodSignature signature) {
    	this.tableName = tableName;
    	this.returnType = returnType;
    	this.signature = signature;
    }
    
    public DecisionTableOpenlBuilder(String tableName, IOpenClass returnType, Map<String, IOpenClass> incomeParams) {
    	this(tableName, returnType, new MethodSignature(incomeParams.values().toArray(
                new IOpenClass[incomeParams.size()]), incomeParams.keySet().toArray(new String[incomeParams.size()])));        
    }
    
    public DecisionTable build() {
        OpenMethodHeader header = getMethodHeader();
        
        DecisionTableBoundNode boundNode = null;
        if (moduleOpenClass != null) {
        	 boundNode = new DecisionTableBoundNode(tsn, moduleOpenClass.getOpenl(), header, moduleOpenClass);
        }        
        return new DecisionTable(header, boundNode);
    }		

	public void setModuleOpenClass(XlsModuleOpenClass moduleOpenClass) {
		this.moduleOpenClass = moduleOpenClass;
	}

	public void setTableSyntaxNode(TableSyntaxNode tsn) {
		this.tsn = tsn;
	}
	
	protected OpenMethodHeader getMethodHeader() {
		return new OpenMethodHeader(tableName, returnType, signature, moduleOpenClass);		
	}
}
