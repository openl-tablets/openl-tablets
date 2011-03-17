/**
 * 
 */
package com.exigen.le;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.ss.formula.IExternalWorkbookResolver;
import org.apache.poi.ss.usermodel.Workbook;

import com.exigen.le.datalogger.DataLogger;
import com.exigen.le.evaluator.LiveExcelEvaluator;
import com.exigen.le.evaluator.ThreadEvaluationContext;
import com.exigen.le.evaluator.function.UDFRegister;
import com.exigen.le.evaluator.selector.DummyFunctionSelector;
import com.exigen.le.evaluator.selector.FunctionSelector;
import com.exigen.le.project.ExternalBranchedWorkbookResolver;
import com.exigen.le.project.ProjectLoader;
import com.exigen.le.servicedescr.evaluator.BeanWrapper;
import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.SMHelper;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.accessor.ValueHolder;
import com.exigen.le.smodel.provider.ServiceModelProvider;

/**
 * LEEvaluator entry point
 * 
 * @author vabramovs
 * 
 */
public class LiveExcel {

    private static final Log LOG = LogFactory.getLog(LiveExcel.class);

    private ServiceModelProvider serviceModelProvider;
    private FunctionSelector functionSelector;

    private String logFile;
    private DataLogger logger;
    private boolean doLog;
    private ServiceModel serviceModel;

    public LiveExcel(ServiceModelProvider serviceModelProvider) {
        this(new DummyFunctionSelector(), serviceModelProvider);
    }

    public LiveExcel(FunctionSelector functionSelector, ServiceModelProvider serviceModelProvider) {
        this.serviceModelProvider = serviceModelProvider;
        this.functionSelector = functionSelector;
        serviceModel = serviceModelProvider.create();
    }

    private DataLogger getLogger() {
        if (logger == null) {

            String logFilePath = null;
            if (logFile == null) {
                logFilePath = serviceModelProvider.getProjectLocation().getAbsolutePath() + "/log/LE.log";
            } else {
                logFilePath = serviceModelProvider.getProjectLocation().getAbsolutePath() + "/" + logFile;
            }
            logger = DataLogger.createInstance(logFilePath);
        }
        return logger;
    }

    private LE_Value evaluate(String functionName, List<Object> args) {
        LE_Value answer;
        ServiceModel sm = ThreadEvaluationContext.getServiceModel();

        Function func = functionSelector
                .selectFunction(functionName, sm.getFunctions(), ThreadEvaluationContext.getInstance());

        Workbook workbook = ProjectLoader.getWorkbook(serviceModelProvider.getProjectLocation(), func.getExcel());

        // Build args array and wrap arguments to BeanWrapper if they are
        // basic, has complex type and do not support ValueHolder
        Object[] argsArray = new Object[args.size()];
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i) instanceof ValueHolder || i >= func.getArguments().size()
                    || func.getArguments().get(i).getType() == null
                    || func.getArguments().get(i).getType().isComplex() == false) {
                argsArray[i] = args.get(i);
            } else {
                argsArray[i] = new BeanWrapper(args.get(i), func.getArguments().get(i).getType());
            }
        }
        if (isDoLog()) {
            getLogger().write(argsArray);
        }

        IExternalWorkbookResolver resolver = new ExternalBranchedWorkbookResolver();
        LiveExcelEvaluator evaluator = new LiveExcelEvaluator(workbook, resolver);
        ValueEval value = evaluator.evaluateServiceModelUDF(functionName, argsArray);
        answer = LE_Value.fromValueEval(value);
        return answer;
    }

    private void logAfterEvaluation(String functionName, LE_Value answer) {
        LOG.debug("Stop calculate function '" + functionName + "'.");
        if (LOG.isTraceEnabled()) {
            String result = "Result of function " + functionName + ":";
            result = result + "\n" + SMHelper.valueToString(answer);
            LOG.trace(result);
        }
    }

    private void prepareEvaluationContext(Map<String, String> envProps) {
        ThreadEvaluationContext.reset();
        ThreadEvaluationContext.setProject(serviceModelProvider.getProjectLocation());
        ThreadEvaluationContext.setServiceModel(serviceModel);
        ThreadEvaluationContext.setFunctionSelector(this.functionSelector);
        if (envProps == null) {
            ThreadEvaluationContext.buildEvalContext(UDFRegister.getInstance().getJavaUDF());
        } else {
            ThreadEvaluationContext.buildEvalContext(UDFRegister.getInstance().getJavaUDF(), envProps);
        }
    }

    private void prepareEvaluationContext() {
        prepareEvaluationContext(null);
    }

    private void logBeforeEvaluation(String functionName, List<Object> args) {
        LOG.debug("Start calculate function '" + functionName + "'");
        if (LOG.isTraceEnabled()) {
            String arguments = "Context of function " + functionName + ":";
            for (Object arg : args) {
                arguments = arguments + "\n" + SMHelper.valueToString(arg);
            }
            LOG.trace(arguments);
        }
    }

    /**
     * Calculate LiveExcel "service" function
     * 
     * @param functionName
     * @param args
     * @return
     */
    public LE_Value calculate(String functionName, List<Object> args) {
        LE_Value answer;
        logBeforeEvaluation(functionName, args);

        prepareEvaluationContext();

        try {

            answer = evaluate(functionName, args);

        } catch (Throwable e) {
            LOG.error("Error during function '" + functionName + "' calculation.", e);
            e.printStackTrace();
            throw new RuntimeException("Error during function '" + functionName + "' calculation", e);

        } finally {
            ThreadEvaluationContext.freeEvalContext();
        }

        logAfterEvaluation(functionName, answer);
        return answer;

    }

    /**
     * Calculate LiveExcel "service" function with context
     * 
     * @param functionName
     * @param args
     * @param envProperties
     * @return
     */
    public LE_Value calculate(String functionName, List<Object> args, Map<String, String> envProperties) {
        LE_Value answer;
        logBeforeEvaluation(functionName, args);

        prepareEvaluationContext(envProperties);
        try {

            answer = evaluate(functionName, args);

        } catch (Throwable e) {
            LOG.error("Error during function '" + functionName + "' calculation.", e);
            e.printStackTrace();
            throw new RuntimeException("Error during function '" + functionName + "' calculation", e);

        } finally {
            ThreadEvaluationContext.freeEvalContext();
        }

        logAfterEvaluation(functionName, answer);
        return answer;

    }
    /**
     * Get List of service function
     * 
     * @param projectName
     * @param versionDesc
     * @return
     */
    public List<Function> getServiceFunctions(Map<String, String> envProp) {
        prepareEvaluationContext(envProp);
        ServiceModel sm = ThreadEvaluationContext.getServiceModel();
        return sm.getServiceFunctions();
    }

    /**
     * Get root of service model
     * 
     * @param projectName
     * @param version - version to became default
     * @return
     */
    public ServiceModel getServiceModel() {
        prepareEvaluationContext();
        return ThreadEvaluationContext.getServiceModel();

    }

    /**
     * Print out service model
     * 
     * @param out
     * @param projectName
     * @param versionDesc
     */
    public void printoutServiceModel(PrintStream out) {
        ServiceModel sm = ThreadEvaluationContext.getServiceModel();
        out.println("Types" + ":");
        SMHelper.printoutStructure(out, "", sm.getTypes());
        SMHelper.printoutFunctions(out, "", sm.getFunctions());
        SMHelper.printoutTables(out, "", sm.getTables());

    }

    /**
     * Register new java Class to calculate UDF New registered UDF will effect
     * to new lifted workbooks, not to cached
     * 
     * @param functionName
     * @param executor
     */
    public static void registerJavaFunction(String functionName, LE_Function executor) {
        UDFRegister.getInstance().registerJavaFunction(functionName, executor);
    }

    /**
     * Register new java Class to calculate UDF New registered UDF will effect
     * to new lifted workbooks, not to cached
     * 
     * @param functionName
     * @param executor
     */
    public static void registerJavaUDF(String functionName, FreeRefFunction executor) {
        UDFRegister.getInstance().registerJavaUDF(functionName, executor);
    }

    /**
     * @return the UDFRegister.getInstance().getJavaUDF()
     */
    public static Map<String, FreeRefFunction> getJavaUDF() {
        return UDFRegister.getInstance().getJavaUDF();
    }

    public String getLogFile() {
        return logFile;
    }

    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }

    public boolean isDoLog() {
        return doLog;
    }

    public void setDoLog(boolean doLog) {
        this.doLog = doLog;
    }
}
