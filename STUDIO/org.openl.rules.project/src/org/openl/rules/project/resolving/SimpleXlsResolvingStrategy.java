package org.openl.rules.project.resolving;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * Resolver for simple OpenL project with only xls file.
 * <p/>
 * ProjectDescriptor will be created with modules for each xls.
 *
 * @author PUdalau
 */
public class SimpleXlsResolvingStrategy implements ResolvingStrategy {

    private final Logger log = LoggerFactory.getLogger(SimpleXlsResolvingStrategy.class);

    public boolean isRulesProject(File folder) {
        if (!folder.isDirectory()) {
            return false;
        }
        for (File f : folder.listFiles()) {
            if (!f.isHidden() && FileTypeHelper.isExcelFile(f.getName())) {
                log.debug("Project in {} folder was resolved as simple xls project", folder.getPath());
                return true;
            }
        }
        log.debug("Simple xls strategy failed to resolve project folder: there is no excel files in given folder {}",
            folder.getPath());
        return false;
    }

    public ProjectDescriptor resolveProject(File folder) throws ProjectResolvingException {
        Map<String, Module> modules = new TreeMap<String, Module>();
        try {
            ProjectDescriptor project = createDescriptor(folder);
            for (File f : folder.listFiles()) {
                if (!f.isHidden() && f.isFile() && FileTypeHelper.isExcelFile(f.getName())) {

                    String name = FileUtils.removeExtension(f.getName());
                    if (!modules.containsKey(name)) {
                        PathEntry rootPath = new PathEntry(f.getCanonicalFile().getAbsolutePath());
                        Module module = createModule(project, rootPath, name);
                        modules.put(name, module);
                    } else {
                        if (log.isErrorEnabled()){
                            log.error("A module with this name already exists: {}", name);
                        }
                    }
                }
            }
            project.setModules(new ArrayList<Module>(modules.values()));
            return project;
        } catch (IOException e) {
            throw new ProjectResolvingException(e);
        }
    }

    private Module createModule(ProjectDescriptor project, PathEntry rootPath, String name) {
        Module module = new Module();
        module.setProject(project);
        module.setRulesRootPath(rootPath);
        module.setName(name);
        return module;
    }

    private ProjectDescriptor createDescriptor(File folder) throws IOException{
        ProjectDescriptor project = new ProjectDescriptor();
        project.setProjectFolder(folder.getCanonicalFile());
        project.setName(folder.getName());
        return project;
    }
}
