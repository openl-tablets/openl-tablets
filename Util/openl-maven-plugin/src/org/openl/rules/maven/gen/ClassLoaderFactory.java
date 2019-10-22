package org.openl.rules.maven.gen;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.openl.util.StringUtils;

@Deprecated
public class ClassLoaderFactory {

    public static ClassLoader createClassLoader(String classpath,
            ClassLoader parent,
            String userHome) throws Exception {

        final List<URL> urls = new ArrayList<>();
        String[] cps = StringUtils.split(classpath, File.pathSeparatorChar);
        // PathMatcher is not thread-safe.
        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**.{zip,jar}");
        for (String cp : cps) {

            if (cp.endsWith("*")) {
                String substring = cp.substring(0, cp.length() - 1);
                Files.walkFileTree(makeFile(userHome, substring).toPath(), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (matcher.matches(file)) {
                            urls.add(file.toUri().toURL());
                        }
                        return super.visitFile(file, attrs);
                    }
                });
            } else {

                File f = makeFile(userHome, cp);

                if (!f.exists()) {
                    throw new IOException(String.format("File %s does not exist", f.getPath()));
                }

                urls.add(f.toURI().toURL());
            }

        }

        URL[] uurl = urls.toArray(new URL[0]);
        return new URLClassLoader(uurl, parent);
    }

    private static File makeFile(String root, String name) throws Exception {
        File f = new File(name);

        if (f.isAbsolute() || name.startsWith("/")) {
            return f.getCanonicalFile();
        }

        return new File(root, name).getCanonicalFile();
    }
}
