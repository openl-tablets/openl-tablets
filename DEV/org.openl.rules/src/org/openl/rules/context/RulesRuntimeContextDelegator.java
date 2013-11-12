package org.openl.rules.context;


/**
 * Runtime context delegator.
 * 
 * @author PUdalau
 */
public class RulesRuntimeContextDelegator extends DefaultRulesRuntimeContext {

    private IRulesRuntimeContext delegate;

    public RulesRuntimeContextDelegator(IRulesRuntimeContext delegate) {
        this.delegate = delegate;
    }

    public Object getValue(String name) {
        Object value = super.getValue(name);
        if(value == null){
            value = delegate.getValue(name) ;
        }
        return value;
    }

    @Override
    public synchronized String toString() {
        return super.toString()+ "Delegated context:" + delegate.toString();
    }

}
