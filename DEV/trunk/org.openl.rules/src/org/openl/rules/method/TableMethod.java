package org.openl.rules.method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundMethodNode;
import org.openl.rules.annotations.Executable;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.CompositeMethod;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

/**
 * {@link IOpenMethod} implementation for table method component.
 */
@Executable
public class TableMethod extends CompositeMethod implements IMemberMetaInfo {

    private final Log LOG = LogFactory.getLog(TableMethod.class);

    /**
     * Table syntax node that defines method table.
     */
    private MethodTableBoundNode methodTableBoundNode;

    /**
     * Constructs new instance of class.
     * 
     * @param header method header
     * @param methodBodyBoundNode method body bound node - code block that will
     *            be invoked by OpenL engine at runtime
     * @param methodTableBoundNode table bound node (table itself)
     */
    public TableMethod(IOpenMethodHeader header,
            IBoundMethodNode methodBodyBoundNode,
            MethodTableBoundNode methodTableBoundNode) {

        super(header, methodBodyBoundNode);

        this.methodTableBoundNode = methodTableBoundNode;
    }
    
    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        if (Tracer.isTracerOn()) {
            return invokeTraced(target, params, env);
        }
        return super.invoke(target, params, env);
    }

    public Object invokeTraced(Object target, Object[] params, IRuntimeEnv env) {
        Tracer tracer = Tracer.getTracer();

        MethodTableTraceObject traceObject = new MethodTableTraceObject(this, params);
        tracer.push(traceObject);

        Object result = null; 

        try {
            result = super.invoke(target, params, env);
            traceObject.setResult(result);
            return result;

        } catch (RuntimeException e) {
            traceObject.setError(e);
            LOG.error("Error when tracing Method table", e);
            throw e;
        } finally {
            tracer.pop();
        }        
    }

    public BindingDependencies getDependencies() {

        BindingDependencies bindingDependencies = new BindingDependencies();
        updateDependency(bindingDependencies);

        return bindingDependencies;
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return this;
    }

    public String getSourceUrl() {
        return methodTableBoundNode.getTableSyntaxNode().getUri();
    }

    public TableSyntaxNode getSyntaxNode() {
        return methodTableBoundNode.getTableSyntaxNode();
    }

}
