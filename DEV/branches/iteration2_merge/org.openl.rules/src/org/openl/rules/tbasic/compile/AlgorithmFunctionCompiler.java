package org.openl.rules.tbasic.compile;

import java.util.List;

import org.openl.IOpenSourceCodeModule;
import org.openl.base.INamedThing;
import org.openl.binding.impl.BoundError;
import org.openl.rules.tbasic.AlgorithmFunction;
import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.types.IOpenClass;

/**
 * The <code>AlgorithmFunctionCompiler</code> class describes some function
 * and serves for compiling and checking it.
 * 
 */
public class AlgorithmFunctionCompiler {

    private List<AlgorithmTreeNode> functionBody;
    private CompileContext compileContext;
    private AlgorithmCompiler compiler;
    private AlgorithmFunction method;

    /**
     * Create an instance of <code>AlgorithmFunctionCompiler</code>.
     * 
     * @param functionBody Code of function.
     * @param compileContext Context of function.
     * @param method Description of function.
     * @param compiler Main algorithm compiler.
     */
    public AlgorithmFunctionCompiler(List<AlgorithmTreeNode> functionBody, CompileContext compileContext,
            AlgorithmFunction method, AlgorithmCompiler compiler) {
        this.functionBody = functionBody;
        this.compileContext = compileContext;
        this.method = method;
        this.compiler = compiler;
    }

    public IOpenClass getReturnType() {
        return method.getHeader().getType();
    }

    /**
     * Compile body of function.
     * 
     * @throws Exception If code of function has errors.
     */
    public void compile() throws Exception {
        compileContext.getOperations().addAll(
                new AlgoritmNodesCompiler(compiler.getLabelManager(), compileContext, compiler)
                        .compileNodes(functionBody));
        analyzeReturnCorrectness();
    }

    private void analyzeReturnCorrectness() throws BoundError {
        if (functionBody.size() > 0) {
            SuitablityAsReturn status = new ReturnAnalyzer(getReturnType(), compiler).analyze(functionBody.get(0)
                    .getChildren());
            if (status == SuitablityAsReturn.NONE) {
                IOpenSourceCodeModule errorSource = functionBody.get(functionBody.size() - 1).getAlgorithmRow()
                        .getOperation().asSourceCodeModule();
                throw new BoundError("The method must return value of type '"
                        + getReturnType().getDisplayName(INamedThing.REGULAR) + "'", errorSource);
            }
        }
    }
    
    /**
     * Finalize compilation of function.
     * 
     */
    public void postprocess(){
        method.setAlgorithmSteps(compileContext.getOperations());
        method.setLabels(compileContext.getLocalLabelsRegister());
    }
}
