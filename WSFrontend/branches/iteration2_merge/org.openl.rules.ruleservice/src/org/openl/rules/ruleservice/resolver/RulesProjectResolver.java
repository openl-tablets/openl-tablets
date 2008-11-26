/**
 * 
 */
package org.openl.rules.ruleservice.resolver;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.ruleservice.loader.DeploymentInfo;

/**
 *
 */
public class RulesProjectResolver {
    private final Log log = LogFactory.getLog(getClass());

    // TODO: rewrite resolving: there are logical issues
    public synchronized List<RuleServiceInfo> resolve(DeploymentInfo di, File deploymentLocalFolder) {
        
        List<RuleServiceInfo> serviceClasses = new ArrayList<RuleServiceInfo>();

        try {
            Map<String, WSEntryPoint> openlWrappers = new HashMap<String, WSEntryPoint>();
            XlsFileRecognizer projectXls;

            for (File projectFolder : deploymentLocalFolder.listFiles()) {
                if (projectFolder.isDirectory()) {
                    final File projectGenFolder = new File(projectFolder, "gen");
                    final File binFolder = new File(projectFolder, "bin");
                    projectXls = new XlsFileRecognizer();

                    openlWrappers.clear();
                    FileSystemWalker.walk(projectGenFolder, new OpenLWrapperRecognizer(projectGenFolder, openlWrappers));
                    FileSystemWalker.walk(new File(projectFolder, "rules"), projectXls);

                    for (Map.Entry<String, WSEntryPoint> wsCandidate : openlWrappers.entrySet()) {
                        WSEntryPoint wsEntryPoint = wsCandidate.getValue();
                        String dif = wsEntryPoint.getFullFilename();

                        if (!new File(binFolder, FileSystemWalker.changeExtension(dif, "class")).exists()) continue;

                        String className = FileSystemWalker.removeExtension(dif).replaceAll("[/\\\\]", ".");
                        serviceClasses.add(new RuleServiceInfo(projectFolder, binFolder, projectXls.getFile(),
                                className, wsCandidate.getKey(), wsEntryPoint.isInterface()));
                    }

                }
            }
        } catch (Exception e) {
            log.error("failed to deploy project " + di.getDeployID(), e);
        }
        
        return serviceClasses;
    }


}




