package org.openl.rules.runtime;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;

import org.openl.conf.IUserContext;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.runtime.ASourceCodeEngineFactory;
import org.openl.runtime.IRuntimeEnvBuilder;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IOpenMember;
import org.openl.vm.IRuntimeEnv;

public abstract class ASourceCodeRulesEngineFactory extends ASourceCodeEngineFactory {

    public ASourceCodeRulesEngineFactory(String openlName, File file) {
        super(openlName, file);
    }

    public ASourceCodeRulesEngineFactory(String openlName, IOpenSourceCodeModule sourceCode, IUserContext userContext) {
        super(openlName, sourceCode, userContext);
    }

    public ASourceCodeRulesEngineFactory(String openlName, IOpenSourceCodeModule sourceCode, String userHome) {
        super(openlName, sourceCode, userHome);
    }

    public ASourceCodeRulesEngineFactory(String openlName, IOpenSourceCodeModule sourceCode) {
        super(openlName, sourceCode);
    }

    public ASourceCodeRulesEngineFactory(String openlName, String sourceFile, String userHome) {
        super(openlName, sourceFile, userHome);
    }

    public ASourceCodeRulesEngineFactory(String openlName, String sourceFile) {
        super(openlName, sourceFile);
    }

    public ASourceCodeRulesEngineFactory(String openlName, URL source) {
        super(openlName, source);
    }
    
    private IRuntimeEnvBuilder runtimeEnvBuilder = null;
    
    @Override
    protected IRuntimeEnvBuilder getRuntimeEnvBuilder() {
        if (runtimeEnvBuilder == null){
            runtimeEnvBuilder = new IRuntimeEnvBuilder() {
                @Override
                public IRuntimeEnv buildRuntimeEnv() {
                    return new SimpleRulesVM().getRuntimeEnv();
                }
            };
        }
        return runtimeEnvBuilder;
    }

    @Override
    protected InvocationHandler prepareInvocationHandler(Object openClassInstance, Map<Method, IOpenMember> methodMap,
            IRuntimeEnv runtimeEnv) {
        return new OpenLRulesInvocationHandler(openClassInstance, runtimeEnv, methodMap);
    }
}
