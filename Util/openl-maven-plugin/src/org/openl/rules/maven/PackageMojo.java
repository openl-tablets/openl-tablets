package org.openl.rules.maven;

import static org.codehaus.plexus.archiver.util.DefaultFileSet.fileSet;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;

import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;

/**
 * Package an OpenL project in ZIP archive.
 *
 * @author Yury Molchan
 */
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public final class PackageMojo extends BaseOpenLMojo {

    private static final String RULES_XML = "rules.xml";
    private static final String RULES_DEPLOY_XML = "rules-deploy.xml";

    @Component
    ArchiverManager archiverManager;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

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
     * Classifier to add to the artifact generated. If given, the artifact will
     * be attached as a supplemental artifact. If not given this will create the
     * main artifact which is the default behavior. If you try to do that a
     * second time without using a classifier the build will fail.
     */
    @Parameter
    private String classifier;

    @Override
    void execute(String sourcePath) throws Exception {

        File existingArtifact = project.getArtifact().getFile();
        String[] types = StringUtils.split(format, ',');
        if (CollectionUtils.isNotEmpty(types)) {
            for (String type : types) {
                execute(sourcePath, type, existingArtifact);
            }
        }
    }

    private void execute(String sourcePath, String type, File mainArtifact) throws NoSuchArchiverException,
                                                                            IOException {
        File outputFile = getOutputFile(outputDirectory, finalName, classifier, type);
        Archiver arch = archiverManager.getArchiver(type);
        arch.setIncludeEmptyDirs(false);
        addFile(arch, sourcePath, RULES_XML);
        addFile(arch, sourcePath, RULES_DEPLOY_XML);
        arch.addFileSet(fileSet(new File(sourcePath)).includeEmptyDirs(false)
            .exclude(new String[] { RULES_XML, RULES_DEPLOY_XML }));

        Set<Artifact> artifacts = project.getArtifacts();

        HashSet<String> skipped = new HashSet<String>();

        for (Artifact artifact : artifacts) {
            collectToSkip(skipped, artifact);
        }
        for (Artifact artifact : artifacts) {
            if (skipped.contains(ArtifactUtils.versionlessKey(artifact))) {
                debug("SKIP: ", artifact);
            } else {
                debug("ADD : ", artifact);
                File file = artifact.getFile();
                arch.addFile(file, classpathFolder + file.getName());
            }
        }
        if (outputFile.equals(mainArtifact)) {
            outputFile = getOutputFile(outputDirectory, finalName, "override", type);
        }
        if (mainArtifact != null && mainArtifact.isFile()) {
            arch.addFile(mainArtifact, classpathFolder + mainArtifact.getName());
        }

        arch.setDestFile(outputFile);
        arch.createArchive();
        if ("openl".equals(packaging)) {
            project.getArtifact().setFile(outputFile);
        } else if (StringUtils.isBlank(classifier) && type.equals(packaging)) {
            if (mainArtifact != null) {
                warn("Replacing pre-existing project main-artifact file: ", mainArtifact);
                warn("with OpenL file: " + outputFile);
            }
            project.getArtifact().setFile(outputFile);
        } else {
            projectHelper.attachArtifact(project, type, classifier, outputFile);
        }
    }

    private void collectToSkip(HashSet<String> skipped, Artifact artifact) {
        boolean skip = false;
        String scope = artifact.getScope();
        if (Artifact.SCOPE_PROVIDED.equals(scope) || Artifact.SCOPE_RUNTIME.equals(scope)) {
            skipped.add(ArtifactUtils.versionlessKey(artifact));
            skip = true;
        }
        List<String> trail = artifact.getDependencyTrail();
        for (String tr : trail) {
            if (tr.startsWith("org.openl.rules:") || tr.startsWith("org.openl:") || tr.startsWith("org.slf4j:")) {
                skip = true;
            }
            if (skip) {
                String key = tr.substring(0, tr.indexOf(':', tr.indexOf(':') + 1));
                skipped.add(key);
            }
        }
    }

    private void addFile(Archiver arch, String folder, String fileName) {
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
