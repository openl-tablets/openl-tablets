package org.openl.rules.method;

import java.util.Map;

import org.openl.binding.impl.cast.CastFactory;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IModuleInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.Invokable;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.impl.ExecutableMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public abstract class ExecutableRulesMethod extends ExecutableMethod implements ITablePropertiesMethod, IModuleInfo {

    private ITableProperties properties;
    // FIXME: it should be AMethodBasedNode but currently it will be
    // ATableBoundNode due to TestSuiteMethod instance of
    // ExecutableRulesMethod(but test table is firstly data table)
    private ATableBoundNode boundNode;
    private boolean hasAliasTypeParams;
    private IOpenCast[] aliasDatatypeCasts;

    private String moduleName;
    private String singleExpression;
    private boolean singleExpressionRead;

    @Override
    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public ExecutableRulesMethod(IOpenMethodHeader header, ATableBoundNode boundNode) {
        super(header);
        this.boundNode = boundNode;
        hasAliasTypeParams = false;
        if (header != null) {
            var i = 0;
            var castFactory = new CastFactory();
            for (IOpenClass param : header.getSignature().getParameterTypes()) {
                if (param instanceof DomainOpenClass) {
                    hasAliasTypeParams = true;
                    if (aliasDatatypeCasts == null) {
                        aliasDatatypeCasts = new IOpenCast[header.getSignature().getNumberOfParameters()];
                    }
                    if (param.getInstanceClass() != null) {
                        aliasDatatypeCasts[i] = castFactory
                                .getCast(JavaOpenClass.getOpenClass(param.getInstanceClass()), param);
                    }
                }
                i++;
            }
        }
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return env.getTracer().invoke(invoke2, target, params, env, this);
    }

    private final Invokable invoke2 = (Invokable) this::invoke2;

    private Object invoke2(Object target, Object[] params, IRuntimeEnv env) {
        if (hasAliasTypeParams) {
            for (var i = 0; i < getSignature().getNumberOfParameters(); i++) {
                if (aliasDatatypeCasts[i] != null) {
                    aliasDatatypeCasts[i].convert(params[i]); // Validate alias
                    // datatypes
                }
            }
        }
        return innerInvoke(target, params, env);
    }

    protected abstract Object innerInvoke(Object target, Object[] params, IRuntimeEnv env);

    public void setBoundNode(ATableBoundNode node) {
        this.boundNode = node;
    }

    /**
     * Returns the expression the table is written of, or {@code null} when it is written of anything else.
     *
     * <p>A table that carries dimension properties is written of no expression of its own: which of its versions
     * answers a call is decided at run time.
     *
     * <p>The text is read once and kept, so that it can be asked for after the table itself is let go.
     */
    public String getSingleExpression() {
        if (!singleExpressionRead && getBoundNode() != null) {
            singleExpression = TablePropertyDefinitionUtils.isDimensionalPropertyPresented(this) ? null
                    : readSingleExpression();
            singleExpressionRead = true;
        }
        return singleExpression;
    }

    /**
     * Reads the expression the table is written of. A table of several lines is written of no single expression.
     */
    protected String readSingleExpression() {
        return null;
    }

    /**
     * Returns the expression the cell is written of, without the word that opens it.
     *
     * <p>Returns {@code null} when the cell holds anything else than a single expression.
     */
    protected static String readExpressionText(ILogicalTable cell, String opening) {
        var code = new GridCellSourceCodeModule(cell.getSource(), null).getCode().strip();
        if (!code.startsWith(opening) || code.length() > opening.length() && Character
                .isJavaIdentifierPart(code.charAt(opening.length()))) {
            return null;
        }
        code = code.substring(opening.length()).strip();
        if (code.endsWith(";")) {
            code = code.substring(0, code.length() - 1).strip();
        }
        // what is left has to be one expression, so a second statement is not a single expression
        return code.isEmpty() || code.indexOf(';') >= 0 ? null : code;
    }

    public void clearForExecutionMode() {
        // the text is read while the table is still there
        getSingleExpression();
        setBoundNode(null);
        var methodProperties = getMethodProperties();
        if (methodProperties != null) {
            methodProperties.setModulePropertiesTableSyntaxNode(null);
            methodProperties.setCategoryPropertiesTableSyntaxNode(null);
            methodProperties.setPropertiesSection(null);
        }
    }

    public ATableBoundNode getBoundNode() {
        return boundNode;
    }

    @Override
    public Map<String, Object> getProperties() {
        if (getMethodProperties() != null) {
            return getMethodProperties().getAllProperties();
        }
        return null;

    }

    @Override
    public ITableProperties getMethodProperties() {
        return properties;
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return this;
    }

    protected void initProperties(ITableProperties tableProperties) {
        this.properties = tableProperties;
    }

    /**
     * Overridden to get access to {@link TableSyntaxNode} from current implementation.
     */
    @Override
    public TableSyntaxNode getSyntaxNode() {
        if (boundNode != null) {
            return boundNode.getTableSyntaxNode();
        }

        return null;
    }

    public boolean isAlias() {
        return false;
    }
}
