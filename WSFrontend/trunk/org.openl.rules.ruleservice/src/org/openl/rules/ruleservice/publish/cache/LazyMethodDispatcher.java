package org.openl.rules.ruleservice.publish.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.runtime.IRuntimeContext;
import org.openl.types.IOpenMethod;

/**
 * Lazy method dispatcher uses prebinded previously methods(with signature and
 * properties) to select correct method and have association map between the
 * prebinded methods and lazy methods.
 * 
 * @author PUdalau
 */
public class LazyMethodDispatcher extends MatchingOpenMethodDispatcher {
    private Map<IOpenMethod, LazyMethod> methodMap;

    public LazyMethodDispatcher(IOpenMethod method, XlsModuleOpenClass moduleOpenClass) {
        super(method, moduleOpenClass);
        methodMap = new HashMap<IOpenMethod, LazyMethod>();
    }

    public void addMethod(IOpenMethod preBoundMethod, LazyMethod lazyMethod) {
        super.addMethod(preBoundMethod);
        methodMap.put(preBoundMethod, lazyMethod);
    }

    @Override
    protected IOpenMethod findMatchingMethod(List<IOpenMethod> candidates, IRuntimeContext context) {
        IOpenMethod preBoundMethod = super.findMatchingMethod(candidates, context);
        return methodMap.get(preBoundMethod);
    }

    @Override
    protected void decorate(IOpenMethod delegate) {
        super.decorate(delegate);
        getCandidates().clear();
    }

}
