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
    private File xlsFile;
    private Map<String, WSEntryPoint> openlWrappers;

    public static Map<String, WSEntryPoint> findAllWrappersInProject(File projectFolder) {
        final File projectGenFolder = new File(projectFolder, "gen");
        Map<String, WSEntryPoint> openlWrappers = new HashMap<String, WSEntryPoint>();
        FileSystemWalker.walk(projectGenFolder, new OpenLWrapperRecognizer(projectGenFolder, openlWrappers));
        return openlWrappers;
    }

    public static File findXlsFileInProject(File projectFolder) {
        XlsFileRecognizer projectXls = new XlsFileRecognizer();
        FileSystemWalker.walk(new File(projectFolder, "rules"), projectXls);
        return projectXls.getFile();
    }

    private void addWrapperIfValid(List<RuleServiceInfo> serviceClasses, Map.Entry<String, WSEntryPoint> wsCandidate,
            File projectFolder) {
        final File binFolder = new File(projectFolder, "bin");
        WSEntryPoint wsEntryPoint = wsCandidate.getValue();
        String dif = wsEntryPoint.getFullFilename();
        if (new File(binFolder, FileSystemWalker.changeExtension(dif, "class")).exists()) {
            String className = FileSystemWalker.removeExtension(dif).replaceAll("[/\\\\]", ".");
            serviceClasses.add(new RuleServiceInfo(projectFolder, binFolder, xlsFile, className, wsCandidate.getKey(),
                    wsEntryPoint.isInterface()));
        }
    }

    // TODO: rewrite resolving: there are logical issues
    public synchronized List<RuleServiceInfo> resolve(DeploymentInfo di, File deploymentLocalFolder) {
        List<RuleServiceInfo> serviceClasses = new ArrayList<RuleServiceInfo>();
        try {
            for (File projectFolder : deploymentLocalFolder.listFiles()) {
                if (projectFolder.isDirectory()) {
                    openlWrappers = findAllWrappersInProject(projectFolder);
                    xlsFile = findXlsFileInProject(projectFolder);
                    for (Map.Entry<String, WSEntryPoint> wsCandidate : openlWrappers.entrySet()) {
                        addWrapperIfValid(serviceClasses, wsCandidate, projectFolder);
                    }
                }
            }
        } catch (Exception e) {
            log.error("failed to deploy project " + di.getDeployID(), e);
        }
        return serviceClasses;
    }

}
