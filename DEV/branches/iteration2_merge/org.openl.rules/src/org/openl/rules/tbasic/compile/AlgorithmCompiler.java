package org.openl.rules.tbasic.compile;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.IOpenSourceCodeModule;
import org.openl.OpenL;
import org.openl.OpenlTool;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.meta.StringValue;
import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.rules.tbasic.TableParserManager;
import org.openl.rules.tbasic.runtime.OperationConstructorInfo;
import org.openl.rules.tbasic.runtime.RuntimeOperation;
import org.openl.syntax.impl.StringSourceCodeModule;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.DelegatedDynamicObject;

public class AlgorithmCompiler {
    /********************************
     *  Initial data
     *******************************/
    private OpenL openl;
    private IOpenMethodHeader header;
    private List<AlgorithmTreeNode> parsedNodes;
    
    /********************************
     * Intermediate values
     *******************************/
    private ConversionRuleBean[] conversionRules;
    
    /*********************************
     * Compiler output
     ********************************/
    private List<RuntimeOperation> operations;
    private IOpenClass thisTarget;
    
    public AlgorithmCompiler(OpenL openl, IOpenMethodHeader header, List<AlgorithmTreeNode> parsedNodes){
        this (openl, header, parsedNodes, true);
    }
    
    public AlgorithmCompiler(OpenL openl, IOpenMethodHeader header, List<AlgorithmTreeNode> parsedNodes, boolean compileImmediately){
        this.openl = openl;
        this.header = header;
        this.parsedNodes = parsedNodes;
        if (compileImmediately) {
            compile();
        }
    }
    
    public void compile(){
        operations = new ArrayList<RuntimeOperation>();
        thisTarget = new ModuleOpenClass(null, generateOpenClassName(), openl); 
        
        conversionRules = TableParserManager.instance().getConversionRules();
        
        preProcess();
        process();
        
    }

    private void process() {
        for (int i = 0; i < parsedNodes.size(); i++){
            // get nodes to generate code from         
            
            AlgorithmTreeNode parsedNode = parsedNodes.get(i);
            ArrayList operationsToGroupWithCurrent;// = Collection.; new ArrayList<AlgorithmTreeNode>(new String[] {});
            int shiftToNextToGroupOperation = 1;
            for (;shiftToNextToGroupOperation < parsedNodes.size() - i; shiftToNextToGroupOperation++){
                AlgorithmTreeNode groupCandidateNode = parsedNodes.get(i + shiftToNextToGroupOperation);
//                if (!operationsToGroupWithCurrent.contains(groupCandidateNode.getSpecification().getKeyword())){
//                    break;
//                }
            }
            
            List<AlgorithmTreeNode> nodesToCompile = null;
            
            compileLinkedNodes(nodesToCompile);
      }
    }

    private void compileLinkedNodes(List<AlgorithmTreeNode> nodesToCompile) {
        List<RuntimeOperation> emitedOperations = new ArrayList<RuntimeOperation>();
        
        ConversionRuleBean conversionRule = getConvertionRule(nodesToCompile);

        for (int i = 0; i < conversionRule.getOperationType().length; i++){
            String operationType = conversionRule.getOperationType()[i];
            String operationParam1 = conversionRule.getOperationParam1()[i];
            String operationParam2 = conversionRule.getOperationParam2()[i];
            String label = conversionRule.getLabel()[i];
            
            RuntimeOperation emmitedOperation = createOperation(nodesToCompile, operationType, operationParam1, operationParam2);
            emitedOperations.add(emmitedOperation);
        }
        //invoke;
        
        operations.addAll(emitedOperations);
    }

    private RuntimeOperation createOperation(List<AlgorithmTreeNode> nodesToCompile, String operationType, String operationParam1, String operationParam2) {
        try {
            Class clazz = Class.forName("org.openl.rules.tbasic.runtime." + operationType + "Operation");
            Constructor constructor = clazz.getConstructors()[0];
            
            Object[] params = new Object[constructor.getParameterTypes().length];
            
            if (constructor.getParameterTypes().length > 0){
                params[0] = convertParam(nodesToCompile, constructor.getParameterTypes()[0], operationParam1);
            }
            
            if (constructor.getParameterTypes().length > 1){
                params[1] = convertParam(nodesToCompile, constructor.getParameterTypes()[1], operationParam2);
            }
            
            return (RuntimeOperation)constructor.newInstance(params);
            
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    private Object convertParam(List<AlgorithmTreeNode> nodesToCompile, Class clazz, String operationParam) {
        if (clazz.equals(String.class)){
            // FIXME
            return operationParam;
        } else if (clazz.equals(boolean.class)) {
            // FIXME
            return Boolean.parseBoolean(operationParam);
        } else if (clazz.equals(IMethodCaller.class)){
            
            return OpenlTool.makeMethod(createSourceCode(operationParam), openl, createMethodHeader(), createBindingContext());
        } else {
            // FIXME
            throw new RuntimeException("Unknown type");
        }
    }

    private IOpenSourceCodeModule createSourceCode(String operationParam) {
        StringValue openLCodeValue = getOpenLCode(operationParam);
        
        return openLCodeValue.asSourceCodeModule();
    }

    private StringValue getOpenLCode(String operationParam) {
        // TODO Auto-generated method stub
        return null;
    }

    private IBindingContext createBindingContext() {
        // TODO Auto-generated method stub
        return null;
    }

    private IOpenMethodHeader createMethodHeader() {
        // TODO Auto-generated method stub
        return null;
    }

    private ConversionRuleBean getConvertionRule(List<AlgorithmTreeNode> nodesToCompile) {
        // TODO Auto-generated method stub
        return null;
    }

    private String generateOpenClassName() {
        return header.getName();
    }

    /**
     * 
     */
    private void preProcess() {
//        for (int i = 0; i < parsedNodes.size(); i++){
//            AlgorithmTreeNode parsedNode = parsedNodes.get(i);
//            ArrayList operationsToGroupWithCurrent;// = Collection.; new ArrayList<AlgorithmTreeNode>(new String[] {});
//            int shiftToNextToGroupOperation = 1;
//            for (;shiftToNextToGroupOperation < parsedNodes.size() - i; shiftToNextToGroupOperation++){
//                AlgorithmTreeNode groupCandidateNode = parsedNodes.get(i + shiftToNextToGroupOperation);
//                if (!operationsToGroupWithCurrent.contains(groupCandidateNode.getSpecification().getKeyword())){
//                    break;
//                }
//            }
//        }
    }
}
