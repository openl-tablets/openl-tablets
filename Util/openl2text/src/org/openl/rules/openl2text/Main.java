package org.openl.rules.openl2text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.util.FileUtils;

import me.tongfei.progressbar.ProgressBar;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "open2text", description = "CLI tool that converts OpenL Tablets rules to text format.")
public class Main implements Runnable {

    private static final Log log = LogFactory.getLog(Main.class);

    @Parameters(index = "0..*", description = "Input files or folders")
    private List<File> inputFiles;

    @CommandLine.Option(names = { "-o", "--output-dir" }, description = "Output directory path")
    private File outputDir;

    @CommandLine.Option(names = {
            "--omit-types" }, description = "Specifies whether to exclude details of table arguments in the output")
    private boolean omitTypes;

    @CommandLine.Option(names = {
            "--types-deep" }, description = "Specifies how many levels of types to include in the output")
    private int maxTypesDeep = 1;

    @CommandLine.Option(names = {
            "--aliases-as-base" }, description = "Specifies whether to replace Alias types with base type in the output")
    private boolean replaceAliasesWithBaseTypes;

    @CommandLine.Option(names = {
            "--include-dimensional-properties" }, description = "Specifies whether to include dimensional properties in the output")
    private boolean includeDimensionalProperties;

    @CommandLine.Option(names = {
            "--omit-method-refs" }, description = "Specifies whether to exclude details of referenced methods in the output")
    private boolean omitMethodRefs;

    @CommandLine.Option(names = {
            "--include-all-rules-methods" }, description = "Specifies whether to include all rules methods details in the output")
    private boolean includeAllRulesMethods;

    @CommandLine.Option(names = { "--table-as-code" }, description = "Present tables as pseudo-code")
    private boolean tableAsCode;

    @CommandLine.Option(names = {
            "--omit-dispatching-methods" }, description = "If specified only one method from dispatching methods will be included in the output")
    private boolean omitDispatchingMethods;

    @CommandLine.Option(names = {
            "--only-method-cells" }, description = "If specified only method cells will be included in the output. This option is ignored if --table-as-code is not specified.")
    private boolean onlyMethodCells;

    @CommandLine.Option(names = {
            "--max-rows" }, description = "Specifies maximum number of rows to include in the output. If not specified all rows will be included.")
    private int maxRows;

    @CommandLine.Option(names = {
            "--parsing-mode" }, description = "If specified the tables will not be compiled. This option can be used if Excel file contains non OpenL Tablets rules")
    private boolean parsingMode;

    private Path tempDirectory;

    @Override
    public void run() {
        if (outputDir == null) {
            outputDir = new File("");
        }
        if (outputDir.exists() && !outputDir.isDirectory()) {
            log.error("The specified output directory is not a valid directory: " + outputDir.getAbsolutePath());
            return;
        }
        try {
            Files.createDirectories(outputDir.toPath());
        } catch (IOException e) {
            log.error("Cannot create output directory", e);
            return;
        }
        try {
            tempDirectory = createTempDirectory();
            if (tempDirectory == null) {
                return;
            }
            if (inputFiles == null || inputFiles.isEmpty()) {
                log.error("No input files or folders provided.");
                return;
            }
            log.info("Processing input files: " + inputFiles.size() + " files found.");
            for (File file : inputFiles) {
                if (isValidFile(file)) {
                    process(file);
                } else {
                    log.warn(
                        String.format("Skipping '%s' as it is not a recognized file type (Excel, zip, or directory).",
                            file.getName()));
                }
            }

            convertToText();

        } finally {
            if (tempDirectory != null) {
                try {
                    FileUtils.delete(tempDirectory.toFile());
                } catch (IOException e) {
                    log.error("Failed to delete temporary directory: " + tempDirectory, e);
                }
            }
        }
    }

    public static void createZipFromFolder(Path folderPath, Path zipFilePath) throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(zipFilePath.toFile());
                ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
                Stream<Path> pathStream = Files.walk(folderPath)) {
            pathStream.forEach(file -> {
                try {
                    String entryName = folderPath.relativize(file).toString().replace('\\', '/');
                    if (entryName.isEmpty()) {
                        return;
                    }
                    ZipEntry zipEntry = new ZipEntry(entryName);
                    zipOutputStream.putNextEntry(zipEntry);
                    if (file.toFile().isFile()) {
                        FileInputStream fileInputStream = new FileInputStream(file.toFile());
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                            zipOutputStream.write(buffer, 0, bytesRead);
                        }
                        fileInputStream.close();
                    }
                    zipOutputStream.closeEntry();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void convertToText() {
        try (Stream<Path> entries = Files.list(tempDirectory)) {
            Path[] paths = entries.filter(Files::isDirectory).toArray(Path[]::new);
            try (ProgressBar pb = new ProgressBar("", paths.length)) {
                for (Path entry : paths) {
                    Path tempOutDirectory = createTempDirectory();
                    try {
                        OpenL2TextCommand extractor = new OpenL2TextCommand(entry,
                            tempDirectory,
                            tempOutDirectory,
                            omitTypes,
                            maxTypesDeep,
                            replaceAliasesWithBaseTypes,
                            includeDimensionalProperties,
                            omitMethodRefs,
                            includeAllRulesMethods,
                            omitDispatchingMethods,
                            tableAsCode,
                            onlyMethodCells,
                            maxRows,
                            parsingMode);
                        extractor.run();
                        // Create a zip file from tempOutDirectory in the output directory, tar has a limitation of 100
                        // chars for file names
                        createZipFromFolder(tempOutDirectory, outputDir.toPath().resolve(entry.getFileName() + ".zip"));
                    } catch (Exception | LinkageError e) {
                        log.error(String.format("An error occurred while extracting '%s'", entry), e);
                    } finally {
                        pb.step();
                        // Delete tempOutDirectory
                        if (tempOutDirectory != null) {
                            try {
                                FileUtils.delete(tempOutDirectory.toFile());
                            } catch (IOException e) {
                                log.error("Failed to delete temporary directory: " + tempOutDirectory, e);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("An error occurred while accessing files", e);
        }
    }

    private boolean isValidFile(File file) {
        return file.isDirectory() || isZipFile(file) || isExcelFile(file);
    }

    private boolean isZipFile(File file) {
        return file.getName().endsWith(".zip");
    }

    private boolean isExcelFile(File file) {
        return file.getName().endsWith(".xls") || file.getName().endsWith(".xlsx") || file.getName()
            .endsWith(".xlsm") || file.getName().endsWith(".xlsb");
    }

    // Don't use FilesUtils from commons-io because it doesn't work on Windows with space in the end of the path
    private static void copyDirectory(Path sourceDir, Path targetDir) throws IOException {
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }
        try (Stream<Path> stream = Files.walk(sourceDir)) {
            stream.forEach(source -> {
                try {
                    Path target = targetDir.resolve(sourceDir.relativize(source));
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    log.error(String.format("An error occurred while copying the directory '%s' to the directory '%s'.",
                        source.getFileName(),
                        targetDir.getFileName()), e);
                }
            });
        }
    }

    private void processDirectory(File directory) {
        ProjectResolver projectResolver = new ProjectResolver();
        if (projectResolver.isRulesProject(directory.toPath()) != null) {
            try {
                copyDirectory(directory.toPath(), tempDirectory.resolve(directory.getName()));
            } catch (IOException e) {
                log.error(
                    String.format("An error occurred while copying the directory '%s' to the temporary directory.",
                        directory.getName()),
                    e);
            }
        } else {
            log.warn(String.format("Skipping '%s' as it is not a rules project.", directory.getName()));
        }
    }

    private void process(File file) {
        if (isZipFile(file)) {
            processZipFile(file);
        } else {
            if (file.isDirectory()) {
                processDirectory(file);
            } else {
                processExcelFile(file);
            }
        }
    }

    private void processExcelFile(File file) {
        try {
            // Create a subfolder in the temporary directory
            Path projectFolder = tempDirectory.resolve(file.getName() + "-" + UUID.randomUUID());
            Files.createDirectories(projectFolder); // Create parent directories if necessary
            // Copy Excel file to the temporary directory
            Path filePath = projectFolder.resolve(file.getName());
            Files.copy(file.toPath(), filePath);
        } catch (IOException e) {
            log.error(String.format("An error occurred while processing the Excel file '%s'", e));
        }
    }

    private static Path createTempDirectory() {
        try {
            String tempDirName = "picocli-temp-" + UUID.randomUUID();
            return Files.createTempDirectory(tempDirName);
        } catch (IOException e) {
            log.error("An error occurred while creating temporary directory: " + e.getMessage());
            return null;
        }
    }

    private void processZipFile(File zipFile) {
        Path folderToExtract = null;
        try {
            // Create a subfolder in the temporary directory

            // Get the name of the zip file without the extension
            String folderName = zipFile.getName() + "-" + UUID.randomUUID();
            folderToExtract = tempDirectory.resolve(folderName);
            Files.createDirectory(folderToExtract);
            // Extract zip file to the temporary directory
            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        Path entryPath = folderToExtract.resolve(entry.getName());
                        Files.createDirectories(entryPath.getParent()); // Create parent directories if necessary
                        Files.copy(zis, entryPath);
                    }
                }
            }
        } catch (IOException e) {
            log.error(String.format("An error occurred while processing zip file '%s'", zipFile.getName()), e);
        }
        ProjectResolver projectResolver = new ProjectResolver();
        if (projectResolver.isRulesProject(folderToExtract.toFile()) == null) {
            // Delete the folder as it is not a rules project
            log.warn(String.format("Skipping '%s' as it is not a rules project.", zipFile.getName()));
            FileUtils.deleteQuietly(folderToExtract.toFile());
        } else {
            // Check if the project descriptor file exists no need to rename the folder because project name is used
            // from the project descriptor file
            if (!Files.exists(
                folderToExtract.resolve(ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME))) {
                // Rename the folder
                String newFolderName = zipFile.getName();
                // Remove the extension if any
                int index = newFolderName.lastIndexOf('.');
                if (index != -1) {
                    newFolderName = newFolderName.substring(0, index);
                }
                if (Files.exists(tempDirectory.resolve(newFolderName))) {
                    log.warn(String.format("Cannot rename the folder '%s' to '%s' as the folder already exists.",
                        folderToExtract.getFileName(),
                        newFolderName));
                } else {
                    // Rename the folder to the name of the zip file without the extension (if any)
                    try {
                        Files.move(folderToExtract,
                            tempDirectory.resolve(newFolderName),
                            StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        log.error(String.format("An error occurred while renaming the folder '%s' to '%s'",
                            folderToExtract.getFileName(),
                            newFolderName), e);
                    }
                }
            }
        }

    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
