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
public class SimpleRulesProjectResolver implements RulesProjectResolver {
    private final Log LOG = LogFactory.getLog(SimpleRulesProjectResolver.class);
    
//    private File xlsFile;
//    private Map<String, WSEntryPoint> openlWrappers;
//    
    // TODO: rewrite resolving: there are logical issues
    /* (non-Javadoc)
     * @see org.openl.rules.ruleservice.resolver.RulesProjectResolver#resolve(org.openl.rules.ruleservice.loader.DeploymentInfo, java.io.File)
     */
    public List<RulesProjectInfo> resolve(File workspaceFolder) {
        List<RulesProjectInfo> rulesProjects = new ArrayList<RulesProjectInfo>();

        for (File projectFolder : workspaceFolder.listFiles()) {
            if (projectFolder.isDirectory()) {
                RulesProjectInfo rulesProject = resolveRulesProject(projectFolder);
                if (rulesProject != null) {
                    rulesProjects.add(rulesProject);
                }
            }
        }

        return rulesProjects;
    }
    
    public RulesProjectInfo resolveRulesProject(File rulesProjectFolder) {
        if (!rulesProjectFolder.isDirectory()){
            throw new IllegalArgumentException("Argument \"rulesProjectFolder\" must be folder.");
        }
        
        List<RulesModuleInfo> rulesModules = new ArrayList<RulesModuleInfo>();
        File binFolder = new File(rulesProjectFolder, "bin");
        
        try {
            //FIXME: this resolution logic has no sense: there can be several xls files, folders can differ from predefined
            Map<String, WSEntryPoint> openlWrappers = findAllWrappersInProject(rulesProjectFolder);
            File xlsFile = findXlsFileInProject(rulesProjectFolder);
            for (Map.Entry<String, WSEntryPoint> wsCandidate : openlWrappers.entrySet()) {
                addWrapperIfValid(rulesModules, wsCandidate, rulesProjectFolder, binFolder, xlsFile);
            }
            
            if (rulesModules.size() > 0) {
                return new RulesProjectInfo(rulesProjectFolder, binFolder, rulesModules);
            } else {
                return null;
            }
        } catch (Exception e) {
            LOG.error("Failed to resolve rules projects for " + rulesProjectFolder.getName(), e);
        }
        return null;
    }

    public static Map<String, WSEntryPoint> findAllWrappersInProject(File projectFolder) {
        final File projectGenFolder = new File(projectFolder, "gen");
        
        OpenLWrapperRecognizer wrappersRecognizer = new OpenLWrapperRecognizer(projectGenFolder);
        FileSystemWalker.walk(projectGenFolder, wrappersRecognizer);
        return wrappersRecognizer.getWrappers();
    }

    public static File findXlsFileInProject(File projectFolder) {
        final File rulesFolder = new File(projectFolder, "rules");
        
        XlsFileRecognizer projectXls = new XlsFileRecognizer();
        FileSystemWalker.walk(rulesFolder, projectXls);
        return projectXls.getFile();
    }

    private void addWrapperIfValid(List<RulesModuleInfo> serviceClasses, Map.Entry<String, WSEntryPoint> wsCandidate,
            File rulesProjectFolder, File binFolder, File xlsFile) {

        WSEntryPoint wsEntryPoint = wsCandidate.getValue();
        String dif = wsEntryPoint.getFullFilename();
        File file = new File(binFolder, FileSystemWalker.changeExtension(dif, "class"));

        if (file.exists()) {
            String className = FileSystemWalker.removeExtension(dif).replaceAll("[/\\\\]", ".");
            RulesServiceType serviceType = wsEntryPoint.isInterface() ? RulesServiceType.DYNAMIC_WRAPPER : RulesServiceType.STATIC_WRAPPER;
            String serviceName = wsCandidate.getKey();
            RulesModuleInfo serviceInfo = new RulesModuleInfo(rulesProjectFolder, xlsFile, className, serviceName, serviceType);
            
            serviceClasses.add(serviceInfo);
        }
    }

}
