package org.openl.rules.validation.properties.dimentional;

import java.util.HashMap;
import java.util.Map;

import org.openl.OpenL;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.DecisionTableBoundNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.OpenMethodHeader;

public class DecisionTableOpenlBuilder {
        
    private String tableName;
    private IOpenClass returnType;
    private Map<String, IOpenClass> incomeParams;
    
    
    public DecisionTableOpenlBuilder(String tableName, IOpenClass returnType, Map<String, IOpenClass> incomeParams) {
        this.tableName = tableName;
        this.returnType = returnType;        
        this.incomeParams = new HashMap<String, IOpenClass>(incomeParams);
    }
    
    public DecisionTable build(TableSyntaxNode tsn, OpenL openl, ModuleOpenClass moduleOpenClass) {
        IMethodSignature signature = new MethodSignature(incomeParams.values().toArray(
                new IOpenClass[incomeParams.size()]), incomeParams.keySet().toArray(new String[incomeParams.size()]));
        IOpenClass declaringClass = null; // can be null.

        OpenMethodHeader header = new OpenMethodHeader(tableName, returnType, signature, declaringClass);
        DecisionTableBoundNode boundNode = new DecisionTableBoundNode(tsn, openl, header, moduleOpenClass);
        return new DecisionTable(header, boundNode);
    }
}
