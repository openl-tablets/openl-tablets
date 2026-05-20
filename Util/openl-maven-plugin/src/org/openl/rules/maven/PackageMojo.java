package org.openl.rules.maven;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.DirectoryScanner;
import org.jspecify.annotations.Nullable;

import org.openl.info.OpenLVersion;
import org.openl.rules.dataformat.yaml.YamlMapperFactory;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.util.CollectionUtils;
import org.openl.util.ProjectPackager;
import org.openl.util.StringUtils;
import org.openl.util.ZipArchiver;

/**
 * Packages an OpenL Tablets project in a ZIP archive.
 *
 * @author Yury Molchan
 * @since 5.19.1
 */
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.RUNTIME,
        requiresDependencyCollection = ResolutionScope.RUNTIME)
public final class PackageMojo extends BaseOpenLMojo {

    private static final String DEPLOYMENT_YAML = "deployment.yaml";
    static final String DEPLOYMENT_CLASSIFIER = "deployment";
    private static final String TESTS_CLASSIFIER = "tests";

    private static final byte[] EMPTY_PUBLISHERS_RULES_DEPLOY = """
            <rules-deploy>
                <publishers/>
            </rules-deploy>
            """.getBytes(StandardCharsets.UTF_8);

    @Parameter(defaultValue = "${project.packaging}", readonly = true)
    private String packaging;

    @Component
    private MavenProjectHelper projectHelper;

    /**
     * Directory containing the generated artifact.
     */
    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${project.build.finalName}", readonly = true)
    private String finalName;

    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = true)
    private File classesDirectory;

    /**
     * Comma separated list of packaging formats. Supported values: zip, jar.
     */
    @Parameter(defaultValue = "zip")
    private String format;

    /**
     * Folder to store dependencies inside the OpenL Tablets project.
     */
    @Parameter(defaultValue = "lib/")
    private String classpathFolder;

    /**
     * Classifier that identifies the generated artifact as a supplemental one. By default, if a classifier is not
     * provided, the system creates the artifact as the main one. Maven does not support using multiple main artifacts.
     * Upon the second attempt to create the main artifact without using a classifier, the build fails.
     */
    @Parameter
    private String classifier;

    /**
     * Allowed quantity of dependencies which can be included into the ZIP archive. Usually OpenL Tablets rules require
     * a few dependencies, such as domain models, that is, Java beans, or some utils, for example, JSON parsing.
     * Typically, the quantity of required dependencies does not exceed 3. If transitive dependencies are declared
     * incorrectly, the size of the ZIP package increases dramatically. This parameter allows preventing such situation
     * by failing packaging.
     */
    @Parameter(defaultValue = "3", required = true)
    private int dependenciesThreshold;

    /**
     * @deprecated The parameter has no effect and will be removed in a future release. Each dependent OpenL project's
     * {@code rules-deploy.xml} is always replaced with a stub that declares empty {@code <publishers/>}, suppressing
     * publication of the dependency — only the main project is published as a service.
     */
    @Deprecated(forRemoval = true)
    @Parameter
    private Boolean deploymentPackage;

    /**
     * Deployment archive name.
     */
    @Parameter(defaultValue = "${project.build.finalName}")
    private String deploymentName;

    /**
     * Parameter that adds default manifest entries into MANIFEST.MF file.
     *
     * @since 5.23.4
     */
    @Parameter(defaultValue = "true")
    private boolean addDefaultManifest;

    /**
     * Set of key/values to be included to MANIFEST.MF. This parameter overrides default values added by
     * {@linkplain #addDefaultManifest} parameter.
     *
     * @since 5.23.4
     */
    @Parameter
    private Map<String, String> manifestEntries;

    @Parameter(defaultValue = "${user.name}", readonly = true, required = true)
    private String userName;

    /**
     * Sets the list of include patterns to use. All '/' and '\' characters are replaced by
     * <code>File.separatorChar</code>, so the separator used need not match <code>File.separatorChar</code>.
     * <p/>
     * When a pattern ends with a '/' or '\', "**" is appended.
     * <p>
     * If it is not defined, then all files will be included.
     *
     * @since 5.23.6
     */
    @Parameter
    private String[] includes;

    /**
     * Sets the list of exclude patterns to use. All '/' and '\' characters are replaced by
     * <code>File.separatorChar</code>, so the separator used need not match <code>File.separatorChar</code>.
     * <p/>
     * When a pattern ends with a '/' or '\', "**" is appended.
     * <p>
     * If it is not defined, then no files will be excluded.
     * <p>
     * Note: 'pom.xml' file and 'target' directory are excluded always independently on this parameter.
     *
     * @since 5.23.6
     */
    @Parameter
    private final String[] excludes = StringUtils.EMPTY_STRING_ARRAY;

    @Parameter(defaultValue = "${basedir}", readonly = true, required = true)
    private String projectBaseDir;

    @Override
    void execute(String sourcePath, boolean hasDependencies) throws Exception {

        File openLSourceDir = new File(sourcePath);
        if (CollectionUtils.isEmpty(openLSourceDir.list())) {
            info("No OpenL sources have been found at '", sourcePath, "' path");
            info("Skipping packaging of the empty OpenL project.");
            return;
        }
        String[] types = getFormats();
        if (CollectionUtils.isEmpty(types)) {
            throw new MojoFailureException("No formats have been defined in the plugin configuration.");
        }
        File dependencyLib = project.getArtifact().getFile();

        boolean mainArtifactExists = dependencyLib != null && dependencyLib.isFile();
        if (mainArtifactExists && StringUtils.isBlank(classifier) && Arrays.asList(types).contains(packaging)) {
            error("The main artifact have been attached already.");
            error(
                    "You have to use classifier to attach supplemental artifacts " +
                            "to the project instead of replacing them."
            );
            throw new MojoFailureException("It is not possible to replace the main artifact.");
        }
        Set<Artifact> dependencies = getDependencies();
        int dependenciesSize = dependencies.size();
        if (dependenciesSize > dependenciesThreshold) {
            error("The quantity of dependencies (",
                    dependenciesSize,
                    ") exceeds the defined threshold in 'dependenciesThreshold=",
                    dependenciesThreshold,
                    "' parameter.");
            for (Artifact artifact : dependencies) {
                error("    : ", artifact);
            }
            throw new MojoFailureException("The quantity of dependencies exceeds the limit");
        }
        final boolean openLJarPackaging = packaging.equals("openl-jar");
        if (!mainArtifactExists && CollectionUtils.isNotEmpty(classesDirectory.list()) && !openLJarPackaging) {
            // create a jar file with compiled Java sources for OpenL rules
            dependencyLib = File.createTempFile(finalName, "-lib.jar", outputDirectory);

            JarArchiver.archive(classesDirectory, dependencyLib);
        }

        final var includedFiles = scanFiles(openLSourceDir, includes, getExcludes());

        for (String type : types) {
            File outputFile = getOutputFile(outputDirectory, finalName, classifier, type);

            final boolean itselfLink = outputFile.equals(dependencyLib);

            try (ZipArchiver arch = new ZipArchiver(outputFile.toPath())) {
                writeManifest(arch);

                if (openLJarPackaging && CollectionUtils.isNotEmpty(classesDirectory.list())) {
                    ProjectPackager.addOpenLProject(classesDirectory, arch);
                }

                ProjectPackager.addOpenLProject(openLSourceDir, includedFiles, arch);

                if (dependencyLib != null && dependencyLib.isFile() && !itselfLink) {
                    arch.addFile(dependencyLib, classpathFolder + finalName + ".jar");
                }
                for (Artifact artifact : dependencies) {
                    File file = artifact.getFile();
                    arch.addFile(file, classpathFolder + file.getName());
                }
            }

            if (mainArtifactExists || StringUtils.isNotBlank(classifier)) {
                info("Attaching the supplemental artifact '", outputFile, ",");
                projectHelper.attachArtifact(project, type, classifier, outputFile);
            } else {
                info("Registering the main artifact '", outputFile, ",");
                mainArtifactExists = true;
                project.getArtifact().setFile(outputFile);
            }
        }

        buildTestsArtifact(openLSourceDir, types);

        if (deploymentPackage != null) {
            warn("Parameter 'deploymentPackage' is deprecated and has no effect. " +
                    "Dependent OpenL projects always receive a stub 'rules-deploy.xml' with empty publishers " +
                    "to suppress their publication.");
        }

        Set<Artifact> openLDependencies = getDependentOpenLProjects();
        if ((openLJarPackaging || "openl".equals(packaging)) && !openLDependencies.isEmpty()) {
            if (hasEmptyPublishers(openLSourceDir)) {
                info("Project's '", RulesDeploy.FILE_NAME,
                        "' declares empty <publishers/>; skipping the deployment artifact.");
            } else {
                atachDeploymentArtifact(openLDependencies);
            }
        }
    }

    private void atachDeploymentArtifact(Set<Artifact> openLDependencies) throws IOException {
        final String artifactType = getFormats()[0];
        File outputFile = getOutputFile(outputDirectory,
                deploymentName,
                DEPLOYMENT_CLASSIFIER,
                artifactType);

        var deploymentYamlBytes = YamlMapperFactory.getYamlMapper()
                .writeValueAsBytes(Map.of("name", deploymentName));

        try (ZipArchiver arch = new ZipArchiver(outputFile.toPath())) {
            // deployment.yaml must be the first entry in the archive
            arch.addFile(deploymentYamlBytes, DEPLOYMENT_YAML);
            var mainArtifact = project.getArtifact();
            arch.addZipEntries(mainArtifact.getFile(), mainArtifact.getArtifactId());

            for (Artifact openLArtifact : openLDependencies) {
                if (!isRuntimeScope(openLArtifact.getScope())) {
                    continue;
                }
                debug("ADD : ", openLArtifact);
                var depArtifactId = openLArtifact.getArtifactId();
                arch.addZipEntries(openLArtifact.getFile(),
                        depArtifactId,
                        name -> !RulesDeploy.FILE_NAME.equals(name));
                arch.addFile(EMPTY_PUBLISHERS_RULES_DEPLOY, depArtifactId + "/" + RulesDeploy.FILE_NAME);
            }
        }

        info("Attaching the deployment artifact '", outputFile, ",");
        projectHelper.attachArtifact(project, artifactType, DEPLOYMENT_CLASSIFIER, outputFile);
    }

    private String[] getFormats() {
        return switch (packaging) {
            case "openl" -> new String[]{"zip"};
            case "openl-jar" -> new String[]{"jar"};
            default -> StringUtils.split(format, ',');
        };
    }

    private Set<Artifact> getDependencies() {
        return getFilteredDependencies(BaseOpenLMojo::isRuntimeScope);
    }

    @Override
    String getHeader() {
        return "OPENL PACKAGING";
    }

    /**
     * Returns the Jar file to generate, based on an optional classifier.
     *
     * @param basedir         the output directory
     * @param resultFinalName the name of the ear file
     * @param classifier      an optional classifier
     * @return the file to generate
     */
    private File getOutputFile(File basedir, String resultFinalName, String classifier, String format) {
        Objects.requireNonNull(basedir, "basedir is not allowed to be null.");
        Objects.requireNonNull(resultFinalName, "finalName is not allowed to be null.");

        StringBuilder fileName = new StringBuilder(resultFinalName);

        if (StringUtils.isNotBlank(classifier)) {
            fileName.append('-').append(classifier);
        }

        fileName.append('.').append(format);

        return new File(basedir, fileName.toString());
    }

    private Manifest createManifest() {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (addDefaultManifest) {
            // initialize with default values
            attributes.putValue("Build-Date", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            attributes.putValue("Built-By", userName);
            attributes.put(Attributes.Name.IMPLEMENTATION_TITLE,
                    "%s:%s".formatted(project.getGroupId(), project.getArtifactId()));
            attributes.put(Attributes.Name.IMPLEMENTATION_VERSION, project.getVersion());
            if (project.getOrganization() != null) {
                attributes.put(Attributes.Name.IMPLEMENTATION_VENDOR, project.getOrganization().getName());
            }
            attributes.putValue("Created-By", "OpenL Maven Plugin v" + OpenLVersion.getVersion());
        }

        if (manifestEntries != null) {
            for (Map.Entry<String, String> entry : manifestEntries.entrySet()) {
                String key = entry.getKey();
                // if value is empty, create an entry with empty string to prevent nulls in file
                String value = StringUtils.trimToEmpty(entry.getValue());
                attributes.putValue(key, value);
            }
        }
        return manifest;
    }

    private String[] getExcludes() {
        var strings = new ArrayList<String>(excludes.length + 4);
        Collections.addAll(strings, excludes);

        final var targetDir = Path.of(projectBaseDir).relativize(outputDirectory.toPath()) + "/**";
        strings.add(targetDir);
        strings.add("pom.xml");
        strings.add("tests/**");
        return strings.toArray(StringUtils.EMPTY_STRING_ARRAY);
    }

    private static boolean hasEmptyPublishers(File openLSourceDir) {
        var rulesDeploy = RulesDeploy.read(openLSourceDir.toPath());
        return rulesDeploy != null
                && rulesDeploy.getPublishers() != null
                && rulesDeploy.getPublishers().length == 0;
    }

    private static String[] scanFiles(File basedir, @Nullable String[] includes, @Nullable String[] excludes) {
        var scanner = new DirectoryScanner();
        scanner.setBasedir(basedir);
        scanner.setIncludes(includes);
        scanner.setExcludes(excludes);
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    private void writeManifest(ZipArchiver arch) throws IOException {
        if (addDefaultManifest || manifestEntries != null) {
            var manifest = createManifest();
            var baos = new ByteArrayOutputStream();
            manifest.write(baos);
            arch.addFile(baos.toByteArray(), JarFile.MANIFEST_NAME);
        }
    }

    private void buildTestsArtifact(File openLSourceDir, String[] types) throws Exception {
        var testsFiles = scanFiles(openLSourceDir, new String[]{"tests/**"}, null);
        if (testsFiles.length == 0) {
            debug("No files found under 'tests/' folder, skipping tests artifact.");
            return;
        }

        var basePath = openLSourceDir.toPath();
        for (String type : types) {
            var outputFile = getOutputFile(outputDirectory, finalName, TESTS_CLASSIFIER, type);
            try (ZipArchiver arch = new ZipArchiver(outputFile.toPath())) {
                writeManifest(arch);
                for (String file : testsFiles) {
                    arch.addFile(basePath.resolve(file), file);
                }
            }
            info("Attaching the tests artifact '", outputFile, ",");
            projectHelper.attachArtifact(project, type, TESTS_CLASSIFIER, outputFile);
        }
    }

}
