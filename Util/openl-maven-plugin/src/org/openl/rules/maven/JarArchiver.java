package org.openl.rules.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import org.openl.info.OpenLVersion;
import org.openl.util.IOUtils;

/**
 * Internal. Pack all files in a source directory to a jar file.
 *
 * @author Yury Molchan
 */
final class JarArchiver extends SimpleFileVisitor<Path> {
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final Pattern BACK_SLASH = Pattern.compile("\\\\");

    private final Path dir;
    private final JarOutputStream zos;
    private final byte[] buffer = new byte[BUFFER_SIZE];

    private JarArchiver(Path dir, JarOutputStream zos) {
        this.dir = dir;
        this.zos = zos;
    }

    /**
     * Pack a file or all files in a source directory to a zipped output stream. This method does not close the output
     * stream.
     *
     * @param sourceDir a not empty directory for compressing.
     * @param file a jar file.
     * @throws IOException
     */
    static void archive(File sourceDir, File file) throws IOException {
        Path path = sourceDir.toPath();
        Manifest man = new Manifest();
        man.getMainAttributes().putValue("Manifest-Version", "1.0");
        man.getMainAttributes().putValue("Created-By", "OpenL Maven Plugin v" + OpenLVersion.getVersion());
        file.createNewFile();
        try (JarOutputStream zos = new JarOutputStream(new FileOutputStream(file), man)) {
            Files.walkFileTree(path, new JarArchiver(path, zos));
        }
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        // A directory was defined for compression, so make inner files relative to a base directory
        String relativePath = dir.relativize(file).toString();
        String zipPath = BACK_SLASH.matcher(relativePath).replaceAll("/");

        try (InputStream fis = Files.newInputStream(file)) {
            JarEntry entry = new JarEntry(zipPath);
            zos.putNextEntry(entry);
            IOUtils.copy(fis, zos, buffer);
        }
        return FileVisitResult.CONTINUE;
    }
}
