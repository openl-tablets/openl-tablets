/**
 * 
 */
package com.exigen.le.evaluator;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;

import com.exigen.le.evaluator.selector.DummyFunctionSelector;
import com.exigen.le.evaluator.selector.FunctionSelector;
import com.exigen.le.project.ProjectLoader;
import com.exigen.le.smodel.ServiceModel;

/**
 * Keep LE Evaluation context for each thread
 * 
 * @author vabramovs
 * 
 * 
 */
public class ThreadEvaluationContext {
    private static ThreadEvaluationContext INSTANCE = new ThreadEvaluationContext();
    private static final Log LOG = LogFactory.getLog(ThreadEvaluationContext.class);

    private static ThreadLocal<Map<String, String>> envProperties = new ThreadLocal<Map<String, String>>() {
        protected synchronized Map<String, String> initialValue() {
            return new HashMap<String, String>();
        }
    };

    private static ThreadLocal<File> project = new ThreadLocal<File>() {
        protected synchronized File initialValue() {
            return new File(".");
        }
    };

    private static ThreadLocal<FunctionSelector> functionSelector = new ThreadLocal<FunctionSelector>() {
        protected synchronized FunctionSelector initialValue() {
            return new DummyFunctionSelector();
        }
    };

    private static ThreadLocal<DataPool> dataPool = new ThreadLocal<DataPool>() {
        protected synchronized DataPool initialValue() {
            return new DataPool();
        }
    };
    private static ThreadLocal<ServiceModel> serviceModel = new ThreadLocal<ServiceModel>() {
        protected synchronized ServiceModel initialValue() {
            return new ServiceModel();
        }
    };

    private static ThreadLocal<Connection> connection = new ThreadLocal<Connection>() {
        protected synchronized Connection initialValue() {
            return null;
        }
    };

    public static ThreadEvaluationContext getInstance() {
        return INSTANCE;
    }

    public static FunctionSelector getFunctionSelector() {
        return functionSelector.get();
    }

    public static void setFunctionSelector(FunctionSelector selector) {
        functionSelector.set(selector);
    }

    /**
     * @return
     */
    public static Map<String, String> getEnvProperties() {
        return envProperties.get();
    }

    /**
     * @param newContext
     */
    public static void setEnvProperties(Map<String, String> newContext) {
        envProperties.set(newContext);
    }

    /**
     * @return the version
     */
    /**
     * @return the Data Pool
     */
    public static DataPool getDataPool() {
        return dataPool.get();
    }

    /**
     * @return the Service Model
     */
    public static ServiceModel getServiceModel() {
        return serviceModel.get();
    }

    /**
     * @param ServiceModel the project to set
     */
    public static void setServiceModel(ServiceModel newServiceModel) {
        serviceModel.set(newServiceModel);
    }

    public static Connection getConnection() {
        return connection.get();
    }

    public static void setConnection(Connection conn) {
        connection.set(conn);
    }

    public static File getProject() {
        return project.get();
    }

    public static void setProject(File prj) {
        if (prj == null || !prj.equals(project.get())) {
            project.set(prj);
            ProjectLoader.reset();
        }
    }

    /**
     * @param newProject
     * @param newVersion
     */
    public static void buildEvalContext() {
        buildEvalContext(null);
    }

    /**
     * @param javaUDF
     */
    public static void buildEvalContext(Map<String, FreeRefFunction> javaUDF) {
        freeEvalContext();
    }

    /**
     * @param javaUDF
     */
    public static void buildEvalContext(Map<String, FreeRefFunction> javaUDF, Map<String, String> envProperties) {
        freeEvalContext();
        setEnvProperties(new HashMap<String, String>(envProperties));
    }

    /**
     * Free context resources
     * 
     */
    public static void freeEvalContext() {
        getEnvProperties().clear();
        getDataPool().removeAll();
        // clean and close connection
        try {
            Connection conn = getConnection();
            if (conn != null) {
                conn.close();
                setConnection(null);
            }
        } catch (SQLException se) {
            LOG.error("failed to close connection on thread", se);
        }
    }
    
    public static void reset(){
        setProject(null);
    }
}
