package org.openl.extension.xmlrules;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openl.exception.OpenLRuntimeException;
import org.openl.extension.xmlrules.model.ExtensionModule;
import org.openl.extension.xmlrules.model.lazy.LazyWorkbook;
import org.openl.extension.xmlrules.project.XmlRulesModule;
import org.openl.extension.xmlrules.project.XmlRulesModuleSourceCodeModule;
import org.openl.rules.extension.instantiation.IExtensionDescriptor;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.source.IOpenSourceCodeModule;

public class ExtensionDescriptor implements IExtensionDescriptor {
    @Override
    public String getOpenLName() {
        return getClass().getPackage().getName();
    }

    @Override
    public List<Module> getInternalModules(Module module) {
        List<Module> modules = new ArrayList<Module>();
        try {
            File projectFolder = module.getProject().getProjectFolder();
            String uri = new File(projectFolder, module.getRulesRootPath().getPath()).getCanonicalFile()
                    .toURI()
                    .toURL()
                    .toExternalForm();
            ExtensionModule extensionModule = new ZipFileXmlDeserializer(uri).deserialize();
            for (LazyWorkbook workbook : extensionModule.getWorkbooks()) {
                PathEntry rulesRootPath = new PathEntry(new File(projectFolder,
                        module.getRulesRootPath().getPath()).getPath());

                String xlsFileName = workbook.getXlsFileName();
                if (xlsFileName == null || xlsFileName.isEmpty()) {
                    throw new IllegalArgumentException("Empty xls-file");
                }
                String internalModuleName = xlsFileName.substring(xlsFileName.lastIndexOf('/') + 1,
                        xlsFileName.lastIndexOf('.'));

                XmlRulesModule m = new XmlRulesModule();
                m.setProject(module.getProject());
                m.setRulesRootPath(rulesRootPath);
                m.setName(module.getName() + "." + internalModuleName);
                m.setType(module.getType());
                m.setMethodFilter(module.getMethodFilter());
                m.setExtension(module.getExtension());
                m.setInternalModulePath(xlsFileName);
                modules.add(m);
            }
        } catch (IOException e) {
            throw new OpenLRuntimeException(e);
        }
        return modules;
    }

    @Override
    public IOpenSourceCodeModule getSourceCode(Module module) {
        XmlRulesModule m = ((XmlRulesModule) module);
        File sourceFile = new File(m.getRulesRootPath().getPath());
        return new XmlRulesModuleSourceCodeModule(sourceFile, m);
    }
}
