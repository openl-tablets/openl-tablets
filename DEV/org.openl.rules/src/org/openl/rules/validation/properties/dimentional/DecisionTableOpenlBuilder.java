package org.openl.rules.validation.properties.dimentional;

import java.util.Map;

import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.DecisionTableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.impl.ParameterDeclaration;

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
    
    public DecisionTableOpenlBuilder(String tableName, IOpenClass returnType, Map<String, IOpenClass> incomeParams) {
        this.tableName = tableName;
        this.returnType = returnType;
        IParameterDeclaration[] params = new IParameterDeclaration[incomeParams.size()];
        int i = 0;
        for (Map.Entry<String, IOpenClass> field : incomeParams.entrySet()) {
            params[i] = new ParameterDeclaration(field.getValue(), field.getKey());
            i++;
        }
        this.signature = new MethodSignature(params);
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
