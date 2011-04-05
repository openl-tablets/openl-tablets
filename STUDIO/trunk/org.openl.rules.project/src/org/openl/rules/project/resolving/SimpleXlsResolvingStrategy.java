package org.openl.rules.project.resolving;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ModuleType;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.FileTypeHelper;

/**
 * Resolver for simple OpenL project with only xls file.
 * 
 * ProjectDescriptor will be created with modules for each xls.
 * 
 * @author PUdalau
 */
public class SimpleXlsResolvingStrategy implements ResolvingStrategy {

    private static final Log LOG = LogFactory.getLog(SimpleXlsResolvingStrategy.class);

    public boolean isRulesProject(File folder) {
        if (!folder.isDirectory()) {
            return false;
        }
        for (File f : folder.listFiles()) {
            if (!f.isHidden()
                    && FileTypeHelper.isExcelFile(f.getName())) {
                return true;
            }
        }
        return false;
    }

    public ProjectDescriptor resolveProject(File folder) {
        ProjectDescriptor project = createDescriptor(folder);
        Map<String, Module> modules = new TreeMap<String, Module>();
        for (File f : folder.listFiles()) {
            if (!f.isHidden()
                    && FileTypeHelper.isExcelFile(f.getName())) {
                
                String name = FilenameUtils.removeExtension(f.getName());
                if (!modules.containsKey(name)) {
                    PathEntry rootPath = new PathEntry(f.getAbsolutePath());
                    Module module = createModule(project, rootPath, name);
                    modules.put(name, module);
                } else {
                    LOG.error("A module with this name already exists: " + name);
                }
            }
        }
        project.setModules(
                new ArrayList<Module>(modules.values()));
        return project;
    }

    private Module createModule(ProjectDescriptor project, PathEntry rootPath, String name) {
        Module module = new Module();
        module.setProject(project);
        module.setRulesRootPath(rootPath);
        module.setType(ModuleType.API);
        //FIXME: classname just for webstudio
        module.setClassname(name);
        module.setName(name);
        return module;
    }

    private ProjectDescriptor createDescriptor(File folder) {
        ProjectDescriptor project = new ProjectDescriptor();
        project.setProjectFolder(folder);
        project.setName(folder.getName());
        project.setId(folder.getName());
        return project;
    }
}
