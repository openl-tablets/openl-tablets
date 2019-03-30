package org.openl.rules.tbasic.compile;

import org.openl.base.INamedThing;
import org.openl.meta.StringValue;
import org.openl.rules.tbasic.AlgorithmFunction;
import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.rules.tbasic.TBasicSpecificationKey;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;

import java.util.List;

/**
 * The <code>AlgorithmFunctionCompiler</code> class describes some function and serves for compiling and checking it.
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
    public AlgorithmFunctionCompiler(List<AlgorithmTreeNode> functionBody,
            CompileContext compileContext,
            AlgorithmFunction method,
            AlgorithmCompiler compiler) {
        this.functionBody = functionBody;
        this.compileContext = compileContext;
        this.method = method;
        this.compiler = compiler;
    }

    private void analyzeReturnCorrectness() throws SyntaxNodeException {
        if (!functionBody.isEmpty()) {
            int i = 0;
            for (AlgorithmTreeNode algorithmTreeNode : functionBody) {
                StringValue operation = algorithmTreeNode.getAlgorithmRow().getOperation();
                if (operation != null && TBasicSpecificationKey.RETURN.toString().equals(operation.toString())) {
                    SuitablityAsReturn status = new ReturnAnalyzer(getReturnType(), compiler)
                        .analyze(functionBody.get(i).getChildren());
                    if (status == SuitablityAsReturn.NONE) {
                        IOpenSourceCodeModule errorSource = functionBody.get(i)
                            .getAlgorithmRow()
                            .getOperation()
                            .asSourceCodeModule();
                        throw SyntaxNodeExceptionUtils
                            .createError(
                                "The method must return value of type '" + getReturnType()
                                    .getDisplayName(INamedThing.REGULAR) + "'",
                                errorSource);
                    }
                }
                i++;
            }
        }
    }

    /**
     * Compile body of function.
     *
     * @throws Exception If code of function has errors.
     */
    public void compile() throws Exception {
        compileContext.addOperations(
            new AlgoritmNodesCompiler(getReturnType(), compileContext, compiler).compileNodes(functionBody));
        analyzeReturnCorrectness();
    }

    public IOpenClass getReturnType() {
        return method.getHeader().getType();
    }

    /**
     * Finalize compilation of function.
     *
     */
    public void postprocess() {
        method.setAlgorithmSteps(compileContext.getOperations());
        method.setLabels(compileContext.getLocalLabelsRegister());
    }
}
