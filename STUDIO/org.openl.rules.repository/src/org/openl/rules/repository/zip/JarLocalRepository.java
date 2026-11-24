package org.openl.rules.repository.zip;

/**
 * Read only repository of the OpenL projects and deployments packed into the archives of the classpath, which reads
 * an archive without unzipping it to a temporary folder.
 *
 * <p>The projects are found when the repository is created. Which archives are read, and how a nested one is read in
 * place, is told by {@link ClasspathArchives}.
 *
 * <p>NOTE: This repository type doesn't support write actions!
 *
 * @author Vladyslav Pikus
 */
public class JarLocalRepository extends AbstractArchiveRepository {

    /**
     * @throws IllegalStateException when the classpath cannot be read, or two archives have the same base name
     */
    JarLocalRepository() {
        super(new ClasspathArchives().find());
    }
}
