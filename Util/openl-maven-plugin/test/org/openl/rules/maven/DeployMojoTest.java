package org.openl.rules.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers how {@link DeployMojo} chooses the archive to deploy: the deployment archive wins over the project's own
 * archive, and a single archive named after the build, the tests aside, is deployed otherwise.
 *
 * @author Yury Molchan
 */
class DeployMojoTest {

    private static final String FINAL_NAME = "demo-1.0";

    @TempDir
    Path target;

    @Test
    void deploysTheDeploymentArchiveNextToTheProjectArchive() throws IOException {
        touch(FINAL_NAME + ".zip");
        var deployment = touch(FINAL_NAME + "-deployment.zip");

        assertEquals(deployment.toFile(), DeployMojo.findZipFile(target.toFile(), FINAL_NAME));
    }

    @Test
    void deploysTheProjectArchiveNextToTheTestsArchive() throws IOException {
        var archive = touch(FINAL_NAME + ".zip");
        touch(FINAL_NAME + "-tests.zip");

        assertEquals(archive.toFile(), DeployMojo.findZipFile(target.toFile(), FINAL_NAME));
    }

    @Test
    void deploysTheOnlyArchiveNamedAfterTheBuild() throws IOException {
        var archive = touch(FINAL_NAME + "-rules.zip");
        touch("other-1.0.zip");
        touch(FINAL_NAME + ".jar");

        assertEquals(archive.toFile(), DeployMojo.findZipFile(target.toFile(), FINAL_NAME));
    }

    @Test
    void refusesNoArchiveOrSeveralOnes() throws IOException {
        var folder = target.toFile();
        assertThrows(IllegalStateException.class, () -> DeployMojo.findZipFile(folder, FINAL_NAME));

        touch(FINAL_NAME + ".zip");
        touch(FINAL_NAME + "-rules.zip");
        assertThrows(IllegalStateException.class, () -> DeployMojo.findZipFile(folder, FINAL_NAME));
    }

    private Path touch(String name) throws IOException {
        return Files.writeString(target.resolve(name), "stub");
    }
}
