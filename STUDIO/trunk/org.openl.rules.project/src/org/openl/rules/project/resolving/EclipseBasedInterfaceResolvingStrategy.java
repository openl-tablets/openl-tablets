package org.openl.rules.project.resolving;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.lang.xls.main.IRulesLaunchConstants;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ModuleType;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.FileTool;
import org.openl.util.StringTool;

/**
 * Resolves the OpenL project based on next criteria:
 * 1) see the 1st criteria in {@link EclipseBasedResolvingStrategy}
 * 2) Java rules interface should exist. See {@link IRulesLaunchConstants.INTERFACE_SOURCE_SUFFIX_DEFAULT}
 *  
 * @author DLiauchuk
 *
 */
public class EclipseBasedInterfaceResolvingStrategy extends EclipseBasedResolvingStrategy {
    
    private static final String SRC_FILE = "srcFile";
    private static final String GENERATE_JAVA_INTERFACE_BUILD_XML = "build/GenerateJavaInterface.build.xml";
    // Path to upper level rules file
    //
//    private String rulesPath = null;
    
    private final Log log = LogFactory.getLog(EclipseBasedInterfaceResolvingStrategy.class);
    
    // Overriden to add the possibility to search for interface class in the project.
    //
    @Override
    public String[] listPotentialOpenLWrappersClassNames(File project) throws IOException {
        List<String> list = new ArrayList<String>();

        String startDirs = System.getProperty(IRulesLaunchConstants.WRAPPER_SEARCH_START_DIR_PROPERTY,
                IRulesLaunchConstants.WRAPPER_SEARCH_START_DIR_DEFAULT);
        String wrapperSuffixes = System.getProperty(IRulesLaunchConstants.WRAPPER_SOURCE_SUFFIX_PROPERTY,
                IRulesLaunchConstants.INTERFACE_SOURCE_SUFFIX_DEFAULT);

        String[] srcRoots = StringTool.tokenize(startDirs, ", ");
        String[] suffixes = StringTool.tokenize(wrapperSuffixes, ", ");

        for (String srcRoot : srcRoots) {
            listPotentialOpenLWrappersClassNames(project, srcRoot, suffixes, list);
        }
        
        // Initialize the path to the rules source
        //
        boolean rulesPathInitialized = StringUtils.isNotBlank(getRulesPath(project));
        
        if (rulesPathInitialized) {
            return list.toArray(new String[list.size()]);
        } else {
            // Returning empty array, means that the project is not resolved as OpenL project by current strategy
            return new String[0];
        }
    }
    
    // Overriden to create DYNAMIC module based on interface class
    //
    @Override
    protected Module createModule(ProjectDescriptor project, String className) {
        Module module = new Module();
        module.setProject(project);
        module.setClassname(className);
        module.setName(getModuleName(project.getProjectFolder(), className));
        
        String rulesPath = getRulesPath(project.getProjectFolder());
        if (StringUtils.isNotBlank(rulesPath)) {
            module.setRulesRootPath(new PathEntry(rulesPath));
        }
        module.setType(ModuleType.DYNAMIC);
        return module;
    }
    
    private String getRulesPath(File project) {
//        boolean result = true;
        String rulesPath = null;
        if (StringUtils.isBlank(rulesPath)) {
            String relativePath = getSourceEntry(project);
            if (StringUtils.isNotBlank(relativePath)) {
                rulesPath = String.format("%s/%s", project.getAbsolutePath(), relativePath);
            }            
        }
        return rulesPath;
    }
    
    /**
     * Try to get the relative path to the rules source file from the 
     * {@link EclipseBasedInterfaceResolvingStrategy#GENERATE_JAVA_INTERFACE_BUILD_XML} file
     * 
     * @param project main folder of the project
     * @return relative path to the rules source file
     */
    private String getSourceEntry(File project) {
        String result = null;        
        try {            
            String line = FileTool.readLineWithText(project, GENERATE_JAVA_INTERFACE_BUILD_XML, SRC_FILE);            
            if (StringUtils.isNotBlank(line)) {
                result = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""));            
            }
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot find the path to the rules source.", e);
            }            
        }
        return result;
    }
}
