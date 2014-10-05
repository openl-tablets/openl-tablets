package org.openl.conf.ant;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.Project;
import org.openl.rules.project.ProjectDescriptorManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ModuleType;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;

public class JavaInterfaceAntTask extends JavaAntTask {
    
    private static final String DEFAULT_CLASSPATH = "./bin";

    private boolean ignoreTestMethods = true;
    private String defaultProjectName;
    private String defaultClasspaths[] = {DEFAULT_CLASSPATH};
    private boolean createProjectDescriptor = true;

    protected ProjectDescriptor createNewProject() {
        ProjectDescriptor project = new ProjectDescriptor();      
        project.setName(defaultProjectName != null ? defaultProjectName : getDisplayName());
        
        List<PathEntry> classpath = new ArrayList<PathEntry>();
        for (String path : defaultClasspaths) {
            classpath.add(new PathEntry(path));
        }
        project.setClasspath(classpath);
        
        return project;
    }

    protected Module createNewModule() {
        Module module = new Module();
        
        module.setName(getDisplayName());
        module.setType(ModuleType.API);
        module.setClassname(getTargetClass());
        module.setRulesRootPath(new PathEntry(getSrcFile()));
        return module;
    }

    @Override
    protected OpenLToJavaGenerator getJavaGenerator() {
        return new JavaInterfaceGenerator.Builder(getOpenClass(), getTargetClass())
        .methodsToGenerate(getMethods()).fieldsToGenerate(getFields()).ignoreNonJavaTypes(isIgnoreNonJavaTypes())
        .ignoreTestMethods(isIgnoreTestMethods()).build();
    }

    @Override
    protected void writeSpecific() {
        if (createProjectDescriptor) {
            writeRulesXML();
        }
    }

    // TODO extract the code that writes rules.xml, to another class
    protected void writeRulesXML() {
        File rulesDescriptor = new File(getResourcesPath() + ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
        ProjectDescriptorManager manager = new ProjectDescriptorManager();

        ProjectDescriptor projectToWrite;
        List<Module> modulesToWrite = new ArrayList<Module>();
        long timeSinceModification = System.currentTimeMillis() - rulesDescriptor.lastModified();

        // FIXME: This is tricky to rely on the time since modification.
        // Consider that if the time since last modification is small enough it
        // will be the modification
        // made for previously created module by this ant task and we need to add one more module to the project
        // @author DLiauchuk
        if (rulesDescriptor.exists() && timeSinceModification < 2000) {
            // There is a previously created project descriptor, with modules in it.
            // The time was small enough to consider that it was modified/created by the generator.
            // Add current module to existed project.
            ProjectDescriptor existedDescriptor;
            try {
                existedDescriptor = manager.readOriginalDescriptor(rulesDescriptor);
                Module newModule = createNewModule();
                boolean exist = false;
                for (Module existedModule : existedDescriptor.getModules()) {                    
                    if (existedModule.getClassname().equals(newModule.getClassname())) {
                        modulesToWrite.add(newModule);
                        exist = true;
                    } else {
                        modulesToWrite.add(copyOf(existedModule));
                    }
                }
                if (!exist) {
                    modulesToWrite.add(newModule);
                }
                projectToWrite = existedDescriptor;
            } catch (Exception e) {
                log("Error while reading previously created project descriptor file " + ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME, e, Project.MSG_ERR);
                throw new IllegalStateException(e);
            }
        } else {
            // Create new project and add new module
            projectToWrite = createNewProject();
            modulesToWrite.add(createNewModule());
        }        
        projectToWrite.setModules(modulesToWrite);

        try {
            FileOutputStream fous = new FileOutputStream(rulesDescriptor);
            manager.writeDescriptor(projectToWrite, fous);
        } catch (Exception e) {
            log("Error while writing project descriptor file " + ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME, e, Project.MSG_ERR);
        }
    }
    
    /**
     * Copy the module without {@link Module#getProject()}, as it prevents
     * to Circular dependency.
     * @param module income module
     * @return copy of income module without project field
     */
    private Module copyOf(Module module) {
        Module copy = new Module();
        copy.setClassname(module.getClassname());
        copy.setName(module.getName());
        copy.setProperties(module.getProperties());
        copy.setRulesRootPath(module.getRulesRootPath());
        copy.setType(module.getType());
        return copy;
    }

    public boolean isIgnoreTestMethods() {
        return ignoreTestMethods;
    }

    public void setIgnoreTestMethods(boolean ignoreTestMethods) {
        this.ignoreTestMethods = ignoreTestMethods;
    }

    public String getDefaultProjectName() {
        return defaultProjectName;
    }

    public void setDefaultProjectName(String defaultProjectName) {
        this.defaultProjectName = defaultProjectName;
    }

    public String[] getDefaultClasspaths() {
        return defaultClasspaths;
    }

    public void setDefaultClasspaths(String[] defaultClasspaths) {
        this.defaultClasspaths = defaultClasspaths;
    }

    public boolean isCreateProjectDescriptor() {
        return createProjectDescriptor;
    }

    public void setCreateProjectDescriptor(boolean createProjectDescriptor) {
        this.createProjectDescriptor = createProjectDescriptor;
    }

}
