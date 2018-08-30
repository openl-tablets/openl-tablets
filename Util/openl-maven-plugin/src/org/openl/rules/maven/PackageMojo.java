package org.openl.rules.maven;

import static org.codehaus.plexus.archiver.util.DefaultFileSet.fileSet;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;

/**
 * Package an OpenL project in ZIP archive.
 *
 * @author Yury Molchan
 * @since 5.19.1
 */
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public final class PackageMojo extends BaseOpenLMojo {

    private static final String RULES_XML = "rules.xml";
    private static final String RULES_DEPLOY_XML = "rules-deploy.xml";

    @Component
    ArchiverManager archiverManager;

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
     * Comma separated list of packaging formats.
     */
    @Parameter(defaultValue = "zip")
    private String format;

    /**
     * A folder to store dependencies inside the OpenL project.
     */
    @Parameter(defaultValue = "lib/")
    private String classpathFolder;

    /**
     * Classifier to add to the artifact generated. If given, the artifact will be
     * attached as a supplemental artifact. If not given this will create the main
     * artifact which is the default behavior. If you try to do that a second time
     * without using a classifier the build will fail.
     */
    @Parameter
    private String classifier;

    /**
     * An allowed quantity of dependencies which can be included into the ZIP
     * archive. Usually OpenL rules require a few dependencies like: domain models
     * (Java beans) or some utils (e.g. JSON parsing). So the quantity of required
     * dependencies does not exceed 3 usually. In case incorrect declaring of
     * transitive dependencies, the size of the ZIP package increases dramatically.
     * This parameter allows to prevent such situation by failing packaging.
     */
    @Parameter(defaultValue = "3", required = true)
    private int dependenciesThreshold;

    /**
     * Artifact group names to exclude from zip. Use this parameter only if that dependency is needed for build and can't be excluded from pom.
     *
     * @since 5.21.6
     */
    @Parameter
    private List<String> excludeGroupIds;

    /**
     * Artifact names to exclude from zip. Use this parameter only if that dependency is needed for build and can't be excluded from pom.
     *
     * @since 5.21.6
     */
    @Parameter
    private List<String> excludeArtifactIds;

    /**
     * Artifact scopes to exclude from zip. Use this parameter only if that dependency is needed for build and can't be excluded from pom.
     *
     * @since 5.21.6
     */
    @Parameter
    private List<String> excludeScopes;

    @Override
    void execute(String sourcePath, boolean hasDependencies) throws Exception {

        File openLSourceDir = new File(sourcePath);
        if (CollectionUtils.isEmpty(openLSourceDir.list())) {
            info("No OpenL sources have been found at '", sourcePath, "' path");
            info("Skipping packaging of the empty OpenL project.");
            return;
        }
        String[] types = StringUtils.split(format, ',');
        if (CollectionUtils.isEmpty(types)) {
            throw new MojoFailureException("No formats have been defined in the plugin configuration.");
        }
        File dependencyLib = project.getArtifact().getFile();

        boolean mainArtifactExists = dependencyLib != null && dependencyLib.isFile();
        if (mainArtifactExists && StringUtils.isBlank(classifier) && Arrays.asList(types).contains(packaging)) {
            error("The main artifact have been attached already.");
            error("You have to use classifier to attach supplemental artifacts to the project instead of replacing them.");
            throw new MojoFailureException("It is not possible to replace the main artifact.");
        }
        Set<Artifact> dependencies = getDependencies();
        int dependensiesSize = dependencies.size();
        if (dependensiesSize > dependenciesThreshold) {
            error("The quantity of dependencies (",
                dependensiesSize,
                ") exceedes the defined threshold in 'dependenciesThreshold=",
                dependenciesThreshold,
                "' parameter.");
            for (Artifact artifact : dependencies) {
                error("    : ", artifact);
            }
            throw new MojoFailureException("The quantity of dependencies exceedes the limit");
        }
        if (!mainArtifactExists && CollectionUtils.isNotEmpty(classesDirectory.list())) {
            // create a jar file with compiled Java sources for OpenL rules
            dependencyLib = File.createTempFile(finalName, "-lib.jar", outputDirectory);
            Archiver jarArch = new JarArchiver();
            jarArch.setIncludeEmptyDirs(false);
            jarArch.setDestFile(dependencyLib);
            jarArch.addFileSet(fileSet(classesDirectory).includeEmptyDirs(false));
            jarArch.createArchive();
        }

        for (String type : types) {
            File outputFile = getOutputFile(outputDirectory, finalName, classifier, type);
            Archiver arch = archiverManager.getArchiver(type);
            arch.setIncludeEmptyDirs(false);
            addFile(arch, openLSourceDir, RULES_XML);
            addFile(arch, openLSourceDir, RULES_DEPLOY_XML);
            arch.addFileSet(fileSet(openLSourceDir).includeEmptyDirs(false).exclude(new String[] { RULES_XML, RULES_DEPLOY_XML }));

            if (dependencyLib != null && dependencyLib.isFile()) {
                arch.addFile(dependencyLib, classpathFolder + finalName + ".jar");
            }
            for (Artifact artifact : dependencies) {
                File file = artifact.getFile();
                arch.addFile(file, classpathFolder + file.getName());
            }

            arch.setDestFile(outputFile);
            arch.createArchive();
            if (mainArtifactExists || StringUtils.isNotBlank(classifier)) {
                info("Attaching the supplemental artifact '", outputFile, ",");
                projectHelper.attachArtifact(project, type, classifier, outputFile);
            } else {
                info("Registering the main artifact '", outputFile, ",");
                mainArtifactExists = true;
                project.getArtifact().setFile(outputFile);
            }
        }
    }

    private Set<Artifact> getDependencies() {
        Set<Artifact> artifacts = project.getArtifacts();

        HashSet<String> skipped = new HashSet<>();
        Set<Artifact> dependencies = new HashSet<>();

        for (Artifact artifact : artifacts) {
            collectToSkip(skipped, artifact);
        }
        for (Artifact artifact : artifacts) {
            if (skipped.contains(ArtifactUtils.versionlessKey(artifact))) {
                debug("SKIP: ", artifact);
            } else {
                debug("ADD : ", artifact);
                dependencies.add(artifact);
            }
        }
        return dependencies;
    }

    private void collectToSkip(HashSet<String> skipped, Artifact artifact) {
        boolean skip = false;
        String scope = artifact.getScope();
        // There is no need to add to OpenL project other zipped projects or provided dependencies.
        if (Artifact.SCOPE_PROVIDED.equals(scope) || OPENL_ARTIFACT_TYPE.equals(artifact.getType())) {
            skipped.add(ArtifactUtils.versionlessKey(artifact));
            return;
        }
        List<String> trail = artifact.getDependencyTrail();
        for (String tr : trail) {
            if (!skip) {
                String[] parts = tr.split(":");
                String groupName = parts[0];
                String artifactName = parts[1];

                if ("org.openl.rules".equals(groupName) || "org.openl".equals(groupName) || "org.slf4j".equals(groupName)) {
                    skip = true;
                }
                if (excludeGroupIds != null && excludeGroupIds.contains(groupName)) {
                    skip = true;
                }
                if (excludeArtifactIds != null && excludeArtifactIds.contains(artifactName)) {
                    skip = true;
                }
                if (excludeScopes != null) {
                    for (Artifact dependentArtifact : project.getArtifacts()) {
                        if (dependentArtifact.getId().equals(tr)) {
                            // Found artifact. Check its scope ignoring the case.
                            for (String excludeScope : excludeScopes) {
                                if (dependentArtifact.getScope().equalsIgnoreCase(excludeScope)) {
                                    skip = true;
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
            }

            if (skip) {
                String key = tr.substring(0, tr.indexOf(':', tr.indexOf(':') + 1));
                skipped.add(key);
            }
        }
    }

    private void addFile(Archiver arch, File folder, String fileName) {
        File file = new File(folder, fileName);
        if (file.isFile()) {
            arch.addFile(file, fileName);
        }
    }

    @Override
    String getHeader() {
        return "OPENL PACKAGING";
    }

    /**
     * Returns the Jar file to generate, based on an optional classifier.
     *
     * @param basedir the output directory
     * @param resultFinalName the name of the ear file
     * @param classifier an optional classifier
     * @return the file to generate
     */
    private File getOutputFile(File basedir, String resultFinalName, String classifier, String format) {
        if (basedir == null) {
            throw new IllegalArgumentException("basedir is not allowed to be null");
        }
        if (resultFinalName == null) {
            throw new IllegalArgumentException("finalName is not allowed to be null");
        }

        StringBuilder fileName = new StringBuilder(resultFinalName);

        if (StringUtils.isNotBlank(classifier)) {
            fileName.append('-').append(classifier);
        }

        fileName.append('.').append(format);

        return new File(basedir, fileName.toString());
    }
}
