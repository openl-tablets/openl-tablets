/**
 * 
 */
package org.openl.rules.ruleservice.loader;

import java.io.File;

/**
 * @author Sergey Zyrianov
 *
 */
public interface LoadingListener {

    public void beforeLoading(DeploymentInfo di);

    public void afterLoading(DeploymentInfo di, File deploymentLocalFolder);

}
