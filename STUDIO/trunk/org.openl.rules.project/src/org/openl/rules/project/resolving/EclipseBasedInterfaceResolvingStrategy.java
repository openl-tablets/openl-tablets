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
import org.openl.rules.project.resolving.utils.RuleFinderInXML;
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

    private static final String GENERATE_JAVA_INTERFACE_BUILD_XML = "build/GenerateJavaInterface.build.xml";

    private final Log log = LogFactory.getLog(EclipseBasedInterfaceResolvingStrategy.class);

    // Overriden to add the possibility to search for interface class in the project.
    //
    @Override
    public String[] listPotentialOpenLWrappersClassNames(File project) throws IOException {
        List<String> list = new ArrayList<String>();

        String startDirs = System.getProperty(IRulesLaunchConstants.WRAPPER_SEARCH_START_DIR_PROPERTY,
                IRulesLaunchConstants.WRAPPER_SEARCH_START_DIR_DEFAULT);
        String interfaceSuffixes = System.getProperty(IRulesLaunchConstants.WRAPPER_SOURCE_SUFFIX_PROPERTY,
                IRulesLaunchConstants.INTERFACE_SOURCE_SUFFIX_DEFAULT);

        String[] srcRoots = StringTool.tokenize(startDirs, ", ");
        String[] suffixes = StringTool.tokenize(interfaceSuffixes, ", ");

        for (String srcRoot : srcRoots) {
            listPotentialOpenLWrappersClassNames(project, srcRoot, suffixes, list);
        }

        boolean anyRuleSource = false;
        if (!list.isEmpty()) {
            anyRuleSource = hasAnyRuleForInterface(project, list);
        }

        if (anyRuleSource) {
            return list.toArray(new String[list.size()]);
        } else {
            // Returning empty array, means that the project is not resolved as OpenL project by current strategy.
            // Potential Interface wrappers exists, but it is not possible to get paths to the rule sources
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
        
        String rulesPath = getRulePath(project.getProjectFolder(), className);
        if (StringUtils.isNotBlank(rulesPath)) {
            module.setRulesRootPath(new PathEntry(rulesPath));
        }
        module.setType(ModuleType.DYNAMIC);
        return module;
    }
    
    private boolean hasAnyRuleForInterface(File project, List<String> interfaces) {
        File destinationOfSeacrh = null;
        try {
            destinationOfSeacrh = new File(project.getCanonicalPath(), GENERATE_JAVA_INTERFACE_BUILD_XML);
            RuleFinderInXML finder = getFinder(destinationOfSeacrh);
            for (String interfaceName : interfaces) {
                if (StringUtils.isNotBlank(finder.getRulePath(interfaceName))) {
                    return true;
                }
            }
        } catch (IOException e) {
            // Ignore exception
        }
        return false;
    }

    private RuleFinderInXML getFinder(File destinationOfSeacrh) {
        return new RuleFinderInXML(destinationOfSeacrh);
    }

    private String getRulePath(File projectFolder, String interfaceName) {
        String relativePath = null;
        File destinationOfSeacrh = null;
        
        String projectPath = projectFolder.getAbsolutePath();
        destinationOfSeacrh = new File(projectPath, GENERATE_JAVA_INTERFACE_BUILD_XML);
        RuleFinderInXML finder = getFinder(destinationOfSeacrh);
        relativePath = finder.getRulePath(interfaceName);
        
        String rulesPath = null;
        if (StringUtils.isNotBlank(relativePath)) {
            rulesPath = String.format("%s/%s", projectPath, relativePath);
        }
        return rulesPath;
    }
}
