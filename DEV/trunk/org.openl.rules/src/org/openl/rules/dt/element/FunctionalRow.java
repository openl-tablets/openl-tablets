/*
 * Created on Sep 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.dt.element;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.engine.OpenLManager;
import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.dt.IDecisionTableConstants;
import org.openl.rules.table.ALogicalTable;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.Log;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 * 
 */
public abstract class FunctionalRow implements IDecisionRow {

    private static final String NO_PARAM = "___NO_PARAM___";

    private String name;
    private int row;

    private CompositeMethod method;

    private IParameterDeclaration[] params;
    private Object[][] paramValues;
    private Boolean hasNoParams = null;

    private ILogicalTable decisionTable;
    private ILogicalTable paramsTable;
    private ILogicalTable codeTable;
    private ILogicalTable presentationTable;

    private int noParamsIndex = 0;

    public FunctionalRow(String name, int row, ILogicalTable decisionTable) {

        this.name = name;
        this.row = row;
        this.decisionTable = decisionTable;

        this.paramsTable = decisionTable.getLogicalRegion(IDecisionTableConstants.PARAM_COLUMN, row, 1, 1);
        this.codeTable = decisionTable.getLogicalRegion(IDecisionTableConstants.CODE_COLUMN, row, 1, 1);
        this.presentationTable = decisionTable.getLogicalRegion(IDecisionTableConstants.PRESENTATION_COLUMN, row, 1, 1);
    }

    public String getName() {
        return name;
    }

    public IOpenMethod getMethod() {
        return method;
    }

    public IParameterDeclaration[] getParams() {
        return params;
    }

    public Object[][] getParamValues() {
        return paramValues;
    }

    public DecisionTableParameterInfo getParameterInfo(int i) {
        return new DecisionTableParameterInfo(i, this);
    }

    public IOpenSourceCodeModule getSourceCodeModule() {

        if (method != null) {
            return method.getMethodBodyBoundNode().getSyntaxNode().getModule();
        }

        return null;
    }

    public int numberOfParams() {
        return params.length;
    }
    
    public ILogicalTable getDecisionTable() {
        return decisionTable;
    }

    public String[] getParamPresentation() {

        int length = paramsTable.getLogicalHeight();

        String[] result = new String[length];
        int fromHeight = 0;

        for (int i = 0; i < result.length; i++) {

            int gridHeight = paramsTable.getLogicalRow(i).getGridTable().getGridHeight();

            IGridTable singleParamGridTable = (IGridTable) presentationTable.getGridTable().rows(fromHeight,
                fromHeight + gridHeight - 1);
            result[i] = singleParamGridTable.getCell(0, 0).getStringValue();

            fromHeight += gridHeight;
        }

        return result;
    }

    public void prepare(IOpenClass methodType,
            IMethodSignature signature,
            OpenL openl,
            ModuleOpenClass module,
            IBindingContextDelegator bindingContextDelegator,
            RuleRow ruleRow) throws Exception {

        method = generateMethod(signature, openl, bindingContextDelegator, module, methodType);

        OpenlToolAdaptor openlAdaptor = new OpenlToolAdaptor(openl, bindingContextDelegator);

        IOpenMethodHeader header = new OpenMethodHeader(name, null, signature, module);
        openlAdaptor.setHeader(header);

        paramValues = prepareParamValues(openlAdaptor, ruleRow);
    }

    private IParameterDeclaration[] getParams(IBindingContext bindingContext) throws Exception {

        if (params == null) {

            Set<String> paramNames = new HashSet<String>();
            int length = paramsTable.getLogicalHeight();

            params = new IParameterDeclaration[length];

            for (int i = 0; i < length; i++) {

                ILogicalTable paramTable = paramsTable.getLogicalRow(i);
                IOpenSourceCodeModule source = new GridCellSourceCodeModule(paramTable.getGridTable());
                
                IdentifierNode[] nodes = Tokenizer.tokenize(source, " \n\r");
                
                if (nodes.length == 0) {
                    // no parameters
                    params[i] = makeNoParamParameterDeclaration();
                    continue;
                }

                String errMsg;

                if (nodes.length != 2) {
                    errMsg = "Parameter Cell format: <type> <name>";
                    throw SyntaxNodeExceptionUtils.createError(errMsg, null, null, source);
                }

                String typeCode = nodes[0].getIdentifier();
                IOpenClass type = RuleRowHelper.getType(typeCode, bindingContext);
                
                if (type == null) {
                    throw SyntaxNodeExceptionUtils.createError("Type not found: " + typeCode, nodes[0]);
                }

                String name = nodes[1].getIdentifier();
                
                if (paramNames.contains(name)) {
                    throw SyntaxNodeExceptionUtils.createError("Duplicated parameter name: " + name, nodes[1]);
                }

                paramNames.add(name);

                params[i] = new ParameterDeclaration(type, name);
            }
        }

        return params;
    }

    private Object loadParam(ILogicalTable dataTable,
            IOpenClass paramType,
            String paramName,
            String ruleName,
            OpenlToolAdaptor openlAdaptor) throws SyntaxNodeException {

        boolean indexed = paramType.getAggregateInfo().isAggregate(paramType);

        if (!indexed) {
            return RuleRowHelper.loadSingleParam(paramType, paramName, ruleName, dataTable, openlAdaptor);
        }

        IOpenClass indexedParamType = paramType.getAggregateInfo().getComponentType(paramType);
        dataTable = ALogicalTable.make1ColumnTable(dataTable);

        int height = RuleRowHelper.calculateHeight(dataTable);

        if (height == 0) {
            return null;
        }

        if (height == 1) {
            // attempt to load as a single paramType(will work in case of
            // expressions)
            try {
                return RuleRowHelper.loadSingleParam(paramType, paramName, ruleName, dataTable, openlAdaptor);
            } catch (Exception e) {

                Log.debug(e);
                // do nothing, assume the type was wrong or this was not an
                // expression
                // let the regular flow of events take it's course
            }
        }

        CompositeMethod[] methods = null;
        Object ary = indexedParamType.getAggregateInfo().makeIndexedAggregate(indexedParamType, new int[] { height });

        for (int i = 0; i < height; i++) {
            
            ILogicalTable cell = dataTable.getLogicalRow(i);
            Object x = RuleRowHelper.loadSingleParam(indexedParamType, paramName, ruleName, cell, openlAdaptor);
            
            if (x instanceof CompositeMethod) {
                if (methods == null) {
                    methods = new CompositeMethod[height];
                }
                methods[i] = (CompositeMethod) x;
            } else {
                Array.set(ary, i, x);
            }
        }

        return methods == null ? ary : new ArrayHolder(ary, methods);
    }

    private Object[][] prepareParamValues(OpenlToolAdaptor ota, RuleRow ruleRow) throws Exception {
        
        int len = nValues();
        Object[][] values = new Object[len][];

        IParameterDeclaration[] paramDecl = getParams(ota.getBindingContext());

        ArrayList<SyntaxNodeException> errors = new ArrayList<SyntaxNodeException>();

        for (int col = 0; col < len; col++) {
            ILogicalTable valueCell = getValueCell(col);
            IGridTable paramGridColumn = valueCell.getGridTable();

            Object[] valueAry = new Object[paramDecl.length];

            int fromHeight = 0;
            boolean notEmpty = false;
            String ruleName = ruleRow == null ? "R" + (col + 1) : ruleRow.getRuleName(col);

            for (int j = 0; j < paramDecl.length; j++) {
                if (paramDecl[j] == null) {
                    continue;
                }

                int gridHeight = paramsTable.getLogicalRow(j).getGridTable().getGridHeight();
                IGridTable singleParamGridTable = (IGridTable) paramGridColumn.rows(fromHeight,
                    fromHeight + gridHeight - 1);

                Object v = null;
                try {
                    v = loadParam(LogicalTable.logicalTable(singleParamGridTable),
                        paramDecl[j].getType(),
                        paramDecl[j].getName(),
                        ruleName,
                        ota);
                } catch (SyntaxNodeException error) {
                    errors.add(error);
                }

                if (v != null) {
                    notEmpty = true;
                }
                valueAry[j] = v;

                fromHeight += gridHeight;
            }

            if (notEmpty) {
                values[col] = valueAry;
            }
        }

        if (errors.size() > 0) {
            throw new CompositeSyntaxNodeException("Error:", errors.toArray(new SyntaxNodeException[0]));
        }

        return values;
    }

    protected boolean hasNoParams() {

        if (hasNoParams == null) {
            hasNoParams = params[0].getName().startsWith(NO_PARAM);
        }

        return hasNoParams.booleanValue();
    }

    protected Object[] mergeParams(Object target, Object[] dtParams, IRuntimeEnv env, Object[] params) {

        Object[] newParams = new Object[dtParams.length + params.length];

        System.arraycopy(dtParams, 0, newParams, 0, dtParams.length);
        RuleRowHelper.loadParams(newParams, dtParams.length, params, target, dtParams, env);

        return newParams;
    }

    private ILogicalTable getValueCell(int column) {
        return decisionTable.getLogicalRegion(column + IDecisionTableConstants.DATA_COLUMN, row, 1, 1);
    }

    private int nValues() {
        return decisionTable.getLogicalWidth() - IDecisionTableConstants.DATA_COLUMN;
    }

    private IParameterDeclaration makeNoParamParameterDeclaration() {
        return new ParameterDeclaration(JavaOpenClass.STRING, NO_PARAM + noParamsIndex++);
    }

    private CompositeMethod generateMethod(IMethodSignature signature,
            OpenL openl,
            IBindingContextDelegator bindingContextDelegator,
            IOpenClass declaringClass,
            IOpenClass methodType) throws Exception {

        IOpenSourceCodeModule source = new GridCellSourceCodeModule(codeTable.getGridTable());

        IParameterDeclaration[] methodParams = getParams(bindingContextDelegator);
        IMethodSignature newSignature = hasNoParams() ? signature : ((MethodSignature) signature).merge(methodParams);
        OpenMethodHeader methodHeader = new OpenMethodHeader(null, methodType, newSignature, declaringClass);

        return OpenLManager.makeMethod(openl, source, methodHeader, bindingContextDelegator);
    }

}
