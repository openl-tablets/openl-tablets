package org.openl.ruleservice.loader;

import java.util.List;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.model.Module;
import org.openl.ruleservice.ServiceDescription;

/**
 * Wrapper on data source that gives access to data source and resolves the
 * OpenL projects/modules inside the projects. Contains own storage for all
 * projects that is used in services.
 * 
 * @author MKamalov
 * 
 */
public interface IRulesLoader {
    /**
     * Gets data source
     * 
     * @return data source
     */
    IDataSource getDataSource();

    /**
     * 
     * @return list of deployments
     */
    List<Deployment> getDeployments();

    /**
     * Gets deployment
     * @param deploymentName
     * @param deploymentVersion
     * @return
     */
    Deployment getDeployment(String deploymentName, CommonVersion deploymentVersion);

    /**
     * 
     * @param deploymentName
     * @param deploymentVersion
     * @param projectName
     * @return
     */
    List<Module> resolveModulesForProject(String deploymentName, CommonVersion deploymentVersion, String projectName);

    /**
     * 
     * @param serviceDescription
     * @return
     */
    List<Module> getModulesForService(ServiceDescription serviceDescription);
}
