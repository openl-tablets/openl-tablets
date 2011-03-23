package org.openl.rules.project.resolving;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.openl.main.OpenLProjectPropertiesLoader;
import org.openl.rules.lang.xls.main.IRulesLaunchConstants;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ModuleType;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.FileTool;
import org.openl.util.StringTool;
import org.openl.util.tree.FileTreeIterator;
import org.openl.util.tree.TreeIterator;
import org.openl.util.tree.FileTreeIterator.FileTreeAdaptor;

/**
 * Resolves projects that match default OpenL Eclipse-based convention: 1.
 * "openlbuilder" specified in ".project" file 2.
 * "openl.project.classpath.properties" file 2. Existing wrapper
 * 
 * @author PUdalau
 */
public class EclipseBasedResolvingStrategy implements ResolvingStrategy {

    /**
     * {@link FileTreeAdaptor} that have to be used for file search inside the
     * project to determine which files/folders have to be used in search.
     */
    private FileTreeAdaptor treeAdaptor;

    public FileTreeAdaptor getTreeAdaptor() {
        if (treeAdaptor == null) {
            treeAdaptor = new FileTreeAdaptor();
        }
        return treeAdaptor;
    }

    public void setTreeAdaptor(FileTreeAdaptor treeAdaptor) {
        this.treeAdaptor = treeAdaptor;
    }

    public boolean isRulesProject(File folder) {
        try {
            if (!folder.exists() || !folder.isDirectory()) {
                return false;
            }
            if (!FileTool.containsFile(folder, ".project", false)) {
                return false;
            }
            if (!FileTool.containsFileText(folder, ".project", "openlbuilder")) {
                return false;
            }
            if(listPotentialOpenLWrappersClassNames(folder).length == 0){
                return false; //no modules.
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public String[] listPotentialOpenLWrappersClassNames(File project) throws IOException {

        List<String> list = new ArrayList<String>();

        String startDirs = System.getProperty(IRulesLaunchConstants.WRAPPER_SEARCH_START_DIR_PROPERTY,
                IRulesLaunchConstants.WRAPPER_SEARCH_START_DIR_DEFAULT);
        String wrapperSuffixes = System.getProperty(IRulesLaunchConstants.WRAPPER_SOURCE_SUFFIX_PROPERTY,
                IRulesLaunchConstants.WRAPPER_SOURCE_SUFFIX_DEFAULT);

        String[] srcRoots = StringTool.tokenize(startDirs, ", ");
        String[] suffixes = StringTool.tokenize(wrapperSuffixes, ", ");

        for (String srcRoot : srcRoots)
            listPotentialOpenLWrappersClassNames(project, srcRoot, suffixes, list);

        return list.toArray(new String[list.size()]);
    }

    private String javaClassName(File f, String srcroot) {

        String path = f.getPath();
        int inc = 1;
        if (srcroot.endsWith(File.separator)) {
            inc = 0;
        }

        String jpath = path.substring(srcroot.length() + inc, path.length() - 5);
        return jpath.replace(File.separatorChar, '.');

    }

    public void listPotentialOpenLWrappersClassNames(File project, String srcRoot, String[] suffixes, List<String> list)
            throws IOException {

        File searchDir = new File(project.getCanonicalPath(), srcRoot);
        TreeIterator<File> fti = new FileTreeIterator(searchDir, getTreeAdaptor(), 0);
        for (; fti.hasNext();) {
            File f = fti.next();
            for (String suffix : suffixes)
                if (f.getName().endsWith(suffix)) {
                    list.add(javaClassName(f, searchDir.getCanonicalPath()));
                }
        }

    }

    public ProjectDescriptor resolveProject(File folder) {
        ProjectDescriptor descriptor = new ProjectDescriptor();
        descriptor.setId(folder.getName());
        descriptor.setName(folder.getName());
        descriptor.setProjectFolder(folder);
        String[] wrapperClassNames;
        try {
            wrapperClassNames = listPotentialOpenLWrappersClassNames(folder);
        } catch (IOException e) {
            wrapperClassNames = new String[] {};
        }
        List<Module> projectModules = new ArrayList<Module>();
        for (String className : wrapperClassNames) {
            projectModules.add(createModule(descriptor, className));
        }
        descriptor.setModules(projectModules);
        descriptor.setClasspath(getClassPath(folder.getAbsolutePath()));
        return descriptor;
    }

    private Module createModule(ProjectDescriptor project, String className) {
        Module module = new Module();
        module.setProject(project);
        module.setClassname(className);
        module.setName(getModuleName(project.getProjectFolder(), className));
        module.setType(ModuleType.STATIC);
        return module;
    }

    public String getModuleName(File projectFolder, String wrapperClassName) {
        OpenLProjectPropertiesLoader propertiesLoader = new OpenLProjectPropertiesLoader();
        Properties p = propertiesLoader.loadProjectProperties(projectFolder.getAbsolutePath());
        if (p == null || !p.containsKey(wrapperClassName + OpenLProjectPropertiesLoader.DISPLAY_NAME_SUFFIX)) {
            return wrapperClassName;
        }
        return p.getProperty(wrapperClassName + OpenLProjectPropertiesLoader.DISPLAY_NAME_SUFFIX, wrapperClassName);
    }

    private List<PathEntry> getClassPath(String projectFolder) {
        String classPath;

        OpenLProjectPropertiesLoader propertiesLoader = new OpenLProjectPropertiesLoader();
        String usedClassPath = propertiesLoader.loadExistingClasspath(projectFolder);

        String usedClasspathSeparator = propertiesLoader.loadExistingClasspathSeparator(projectFolder);
        if (usedClasspathSeparator != null) {
            classPath = usedClassPath.replace(usedClasspathSeparator, File.pathSeparator);
        } else {
            classPath = usedClassPath;
        }

        String[] files = StringTool.tokenize(classPath, File.pathSeparator);

        List<PathEntry> pathEntries = new ArrayList<PathEntry>(files.length);
        for (int i = 0; i < files.length; i++) {
            pathEntries.add(new PathEntry(files[i]));
        }
        return pathEntries;
    }
}
