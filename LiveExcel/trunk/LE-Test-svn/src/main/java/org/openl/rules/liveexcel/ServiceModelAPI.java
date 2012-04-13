package org.openl.rules.liveexcel;

import java.util.Set;

import com.exigen.le.calc.PropertyEvaluator;

/**
 * API to interact with service model.
 * 
 * @author PUdalau
 */
public class ServiceModelAPI {

    private String projectName;

    /**
     * Creates ServiceModelAPI associated with some project.
     * 
     * @param projectName Name of project.
     */
    public ServiceModelAPI(String projectName) {
        this.projectName = projectName;
    }

    /**
     * @return Set of all UDF names defined by service model.
     */
    public Set<String> getAllServiceModelUDFs() {
        return PropertyEvaluator.getAllPropertyNames(projectName);
    }

    /**
     * @param name Name of domain type.
     * @return Class of domain type.
     */
    public Class<?> getServiceModelObjectDomainType(String name) {
        return PropertyEvaluator.getPropertyType(projectName, name);
    }

    /**
     * @param name Name of property to get.
     * @param object Object that contains specified property.
     * @return Value of property of object if its exists.
     */
    public Object getValue(String name, Object object) {
        return PropertyEvaluator.getValue(projectName, object, name);
    }
    
    /**
     * @return Set of all root instance names.
     */
    public Set<String> getRootNames() {
        return PropertyEvaluator.getRootNames(projectName);
    }
    
    /**
     * @param rootName name of root Service Model object
     * @return type of root Service Model object
     */
    public Class<?> getRootType(String rootName) {
        return PropertyEvaluator.getRootType(projectName, rootName);
    }

}
