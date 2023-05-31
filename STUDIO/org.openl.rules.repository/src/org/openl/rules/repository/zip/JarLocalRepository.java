package org.openl.rules.repository.zip;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import org.openl.util.ClassUtils;
import org.openl.util.FileUtils;

/**
 * Read only implementation of Jar Repository to support deploying of jars from classpath as it is without
 * unzipping to temporary directories.</br>
 *
 * <p>
 * NOTE: This repository type doesn't support write actions!
 * </p>
 *
 * @author Vladyslav Pikus
 */
public class JarLocalRepository extends AbstractArchiveRepository {

    private final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    public void initialize() {
        final Map<String, Path> localStorage = new HashMap<>();
        final Consumer<String> collector = urlString -> {
            try {
                final String name;
                final Path path;
                if (urlString.startsWith("vfs")) {
                    // JBoss VFS Support
                    int extPos = urlString.lastIndexOf(".jar");
                    if (extPos < 0) {
                        extPos = urlString.lastIndexOf(".zip");
                    }
                    urlString = urlString.substring(0, extPos + 4);
                    VfsFile vfsFile = new VfsURLConnection(new URL(urlString).openConnection()).getContent();
                    path = vfsFile.getFile().toPath().getParent().resolve(vfsFile.getName());
                    name = FileUtils.getBaseName(vfsFile.getName());
                } else {
                    URL a = new URL(urlString);
                    URI b = a.toURI();
                    FileSystem c = FileSystems.newFileSystem(b, Collections.emptyMap());
                    Path d = c.getPath("/");
                    String e = FileUtils.getName(urlString);
                    path = d;
                    name = FileUtils.getBaseName(e);
                }
                if (localStorage.containsKey(name)) {
                    throw new IllegalStateException(String.format("The resource with name '%s' already exits.", name));
                }
                localStorage.put(name, path);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to initialize a repository.", e);
            }
        };

        try {
            getResources("rules.xml", collector);
            getResources("deployment.xml", collector);
            getResources("deployment.yaml", collector);
            URL resource = ClassUtils.getCurrentClassLoader(getClass()).getResource("/openl/");
            try {
                var archives = resourceResolver.getResources("/openl/*.zip");
                for (Resource res : archives) {
                    collector.accept(res.getURL().toExternalForm());
                }
            } catch (FileNotFoundException ignore) {
                // Nothing to add
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize a repository.", e);
        }

        Path root = findCommonParentPath(localStorage.values());
        if (root == null) {
            root = Paths.get(System.getProperty("java.io.tmpdir")); // just a stab to prevent NPE
        }

        setStorage(localStorage);
        setRoot(root);
    }

    private void getResources(String fileName, Consumer<String> collector) throws IOException {
        var urls = ClassUtils.getCurrentClassLoader(getClass()).getResources(fileName);
        while (urls.hasMoreElements()) {
            var url = urls.nextElement().toExternalForm();
            collector.accept(url.substring(0, url.length() - fileName.length() - 2));
        }
    }

}
