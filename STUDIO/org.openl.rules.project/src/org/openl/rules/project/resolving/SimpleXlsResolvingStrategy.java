package org.openl.rules.project.resolving;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;
import java.util.TreeMap;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.FileTypeHelper;
import org.openl.util.FileUtils;
import org.openl.util.ZipUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolver for simple OpenL project with only xls file.
 * <p/>
 * ProjectDescriptor will be created with modules for each xls.
 *
 * @author PUdalau
 */
public class SimpleXlsResolvingStrategy implements ResolvingStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleXlsResolvingStrategy.class);

    @Override
    public boolean isRulesProject(Path folder) {
        if (!Files.isDirectory(folder)) {
            return false;
        }
        try {
            final boolean isExcelFile = Files.walk(folder, 1).anyMatch(this::isExcelFile);
            if (isExcelFile) {
                LOG.debug("Project in {} folder has been resolved as simple xls project.", folder);
            } else {
                LOG.debug(
                        "Simple xls strategy has failed to resolve project folder: there is no excel files in the folder '{}'.",
                        folder);
            }
            return isExcelFile;
        } catch (IOException e) {
            LOG.debug(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public ProjectDescriptor resolveProject(Path folder) throws ProjectResolvingException {
        Map<String, Module> modules = new TreeMap<>();
        try {
            ProjectDescriptor project = createDescriptor(folder);
            Files.walkFileTree(folder, EnumSet.noneOf(FileVisitOption.class), 1, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path f, BasicFileAttributes attrs) throws IOException {
                    final String fileName = f.getFileName().toString();
                    if (!Files.isHidden(f) && attrs.isRegularFile() && FileTypeHelper.isExcelFile(fileName)) {
                        String name = FileUtils.removeExtension(fileName);
                        if (!modules.containsKey(name)) {
                            final String relativePath = project.getProjectFolder()
                                    .relativize(f.toRealPath().toAbsolutePath())
                                    .toString();
                            PathEntry rootPath = new PathEntry(relativePath);
                            Module module = createModule(project, rootPath, name);
                            modules.put(name, module);
                        } else {
                            LOG.error("A module with the same name already exists: {}", name);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            project.setModules(new ArrayList<>(modules.values()));
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

    private ProjectDescriptor createDescriptor(Path folder) throws IOException {
        ProjectDescriptor project = new ProjectDescriptor();
        project.setProjectFolder(folder.toRealPath());
        Path fileName = folder.getFileName();
        if (folder.getFileName() == null) {
            fileName = ZipUtils.toPath(folder.toUri()).getFileName();
        }
        project.setName(fileName.toString());
        return project;
    }

    private boolean isExcelFile(Path path) {
        try {
            if (!Files.isHidden(path)) {
                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                return attrs.isRegularFile() && FileTypeHelper.isExcelFile(path.getFileName().toString());
            }
        } catch (IOException e) {
            LOG.debug(e.getMessage(), e);
        }
        return false;
    }
}
