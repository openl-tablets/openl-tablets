package org.openl.rules.webstudio.web.repository.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.FileTypeHelper;

/** Provides file locations and a descriptor for the standard project layout. */
public final class DefaultProjectLayout {

    private static final String RULES_FOLDER = "rules/";
    private static final String TESTS_FOLDER = "tests/";

    private DefaultProjectLayout() {
    }

    /**
     * Returns the standard project path for a file. Workbooks outside the standard module folders are placed under
     * {@code rules/}. Other files and workbooks already under {@code rules/} or {@code tests/} keep their paths.
     *
     * <p>The files are expected without the ignored paths, such as the resource forks macOS packs into an archive.
     */
    public static String filePath(String fileName) {
        var normalizedName = fileName.replace('\\', '/');
        if (!isWorkbook(normalizedName) || normalizedName.startsWith(RULES_FOLDER)
                || normalizedName.startsWith(TESTS_FOLDER)) {
            return normalizedName;
        }
        return RULES_FOLDER + normalizedName;
    }

    /**
     * Creates the project descriptor for workbooks stored in the standard layout. Legacy workbook formats are
     * declared explicitly because the default module patterns cover only {@code .xlsx} files.
     */
    public static byte[] rulesXml(Collection<String> fileNames) {
        return descriptor(fileNames).toBytes();
    }

    /** Creates the project descriptor for a named project whose workbooks use the standard layout. */
    public static byte[] rulesXml(Collection<String> fileNames, String projectName) {
        var descriptor = descriptor(fileNames);
        descriptor.setName(projectName);
        return descriptor.toBytes();
    }

    private static ProjectDescriptor descriptor(Collection<String> fileNames) {
        var legacyModules = fileNames.stream()
                .filter(DefaultProjectLayout::isWorkbook)
                .filter(fileName -> !fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx"))
                .map(DefaultProjectLayout::module)
                .toList();
        var descriptor = new ProjectDescriptor();
        if (!legacyModules.isEmpty()) {
            var modules = new ArrayList<>(ProjectDescriptor.defaultModules());
            modules.addAll(legacyModules);
            descriptor.setModules(modules);
        }
        return descriptor;
    }

    private static boolean isWorkbook(String fileName) {
        var normalizedName = fileName.replace('\\', '/');
        return FileTypeHelper.isExcelFile(normalizedName.substring(normalizedName.lastIndexOf('/') + 1));
    }

    private static Module module(String fileName) {
        var module = new Module();
        module.setRulesRootPath(fileName);
        return module;
    }
}
