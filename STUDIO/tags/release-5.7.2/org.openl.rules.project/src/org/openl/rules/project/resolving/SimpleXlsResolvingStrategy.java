package org.openl.rules.project.resolving;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ModuleType;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Resolver for simple OpenL project with only xls file.
 * 
 * ProjectDescriptor will be created with modules for each xls.
 * 
 * @author PUdalau
 */
public class SimpleXlsResolvingStrategy implements ResolvingStrategy {

    public boolean isRulesProject(File folder) {
        if (!folder.isDirectory()) {
            return false;
        }
        for (File f : folder.listFiles()) {
            if (f.getName().endsWith(".xls") || f.getName().endsWith(".xlsx")) {
                return true;
            }
        }
        return false;
    }

    public ProjectDescriptor resolveProject(File folder) {
        ProjectDescriptor project = createDescriptor(folder);
        List<Module> modules = new ArrayList<Module>();
        for (File f : folder.listFiles()) {
            if (f.getName().endsWith(".xls") || f.getName().endsWith(".xlsx")) {
                modules.add(createModule(project, f));
            }
        }
        project.setModules(modules);
        return project;
    }

    private Module createModule(ProjectDescriptor project, File xlsFile) {
        Module module = new Module();
        module.setProject(project);
        module.setRulesRootPath(new PathEntry(xlsFile.getAbsolutePath()));
        module.setType(ModuleType.API);
        //FIXME: classname just for webstudio
        module.setClassname(FilenameUtils.removeExtension(xlsFile.getName()));
        module.setName(FilenameUtils.removeExtension(xlsFile.getName()));
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
