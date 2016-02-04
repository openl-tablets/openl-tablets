package org.openl.binding.impl;

import java.lang.reflect.Array;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

/**
 * Bound node for constructions like 'Name(drivers)', that will return an array of names for each driver.
 * 
 * @author DLiauchuk
 *
 */
public class MultiCallFieldAccessMethodBoundNode extends ATargetBoundNode {

    private IOpenField singleField;
    
    private IOpenClass returnType;
    
    public MultiCallFieldAccessMethodBoundNode(ISyntaxNode syntaxNode, IBoundNode containerField, IOpenField boundField) {
        super(syntaxNode, new IBoundNode[0], containerField);
        this.singleField = boundField;
    }    
    
    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
        Object target = getTargetNode() == null ? env.getThis() : getTargetNode().evaluate(env);
                        
        int paramsLenght = Array.getLength(target);        
        
        // create an array of results        
        Object results = Array.newInstance(singleField.getType().getInstanceClass(), paramsLenght);
        
        // populate the results array by invoking method for single parameter
        for (int i = 0; i < paramsLenght; i++) {
            Array.set(results , i, singleField.get(Array.get(target, i), env));            
        }
        return results;        
    }

    public IOpenClass getType() {
        if (returnType == null) {
            returnType = singleField.getType().getAggregateInfo().getIndexedAggregateType(singleField.getType(), 1);
        }
       
        return returnType;
    }
    
    @Override
    public void updateDependency(BindingDependencies dependencies) {
        dependencies.addFieldDependency(singleField, this);
    }

    public IOpenField getBoundField() {
        return singleField;
    }
}
