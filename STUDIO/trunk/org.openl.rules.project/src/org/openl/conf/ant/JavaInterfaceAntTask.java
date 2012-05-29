package org.openl.conf.ant;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.openl.rules.project.ProjectDescriptorManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ModuleType;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;

public class JavaInterfaceAntTask extends JavaAntTask {

    private static final String RULES_XML = "rules.xml";

    protected ProjectDescriptor getProjectDescriptor() {
        ProjectDescriptor project = new ProjectDescriptor();      
        project.setId(getDisplayName());
        project.setName(getDisplayName());        
        Module module = new Module();
        
        module.setName(getDisplayName());
        module.setType(ModuleType.DYNAMIC);
        module.setClassname(getTargetClass());
        module.setRulesRootPath(new PathEntry(getSrcFile()));
        
        List<Module> modules = new ArrayList<Module>();
        modules.add(module);
        
        project.setModules(modules);
        return project;
    }

    @Override
    protected OpenLToJavaGenerator getJavaGenerator() {
        return new JavaInterfaceGenerator.Builder(getOpenClass(), getTargetClass())
        .methodsToGenerate(getMethods()).fieldsToGenerate(getFields()).ignoreNonJavaTypes(isIgnoreNonJavaTypes())
        .srcFile(getSrcFile()).deplSrcFile(getDeplSrcFile()).build();
    }

    @Override
    protected void writeSpecific() {
        writeRulesXML();
    }

    protected void writeRulesXML() {
        ProjectDescriptor project = getProjectDescriptor();
        
        ProjectDescriptorManager manager = new ProjectDescriptorManager();
        try {
            FileOutputStream fous = new FileOutputStream(new File(RULES_XML));
            
            manager.writeDescriptor(project, fous);
        } catch (Exception e) {
            // Ignore exception
        }
    }
}
