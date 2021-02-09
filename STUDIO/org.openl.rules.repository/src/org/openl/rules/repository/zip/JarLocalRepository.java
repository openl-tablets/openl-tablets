package org.openl.rules.repository.zip;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.openl.util.FileUtils;
import org.openl.util.ZipUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

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

    private static final String PROJECT_DESCRIPTOR_FILE = "rules.xml";
    private static final String DEPLOYMENT_DESCRIPTOR_XML_FILE = "deployment.xml";
    private static final String DEPLOYMENT_DESCRIPTOR_YAML_FILE = "deployment.yaml";

    private final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    public void initialize() {
        final Map<String, Path> localStorage = new HashMap<>();
        final Consumer<Resource> collector = res -> {
            try {
                final URI uri = res.getURI();
                final String name;
                final Path path;
                if (uri.getScheme().startsWith("vfs")) {
                    // JBoss VFS Support
                    String urlString = res.getURL().toString();
                    int extPos = urlString.lastIndexOf(".jar");
                    if (extPos < 0) {
                        extPos = urlString.lastIndexOf(".zip");
                    }
                    urlString = urlString.substring(0, extPos + 4);
                    VfsFile vfsFile = new VfsURLConnection(new URL(urlString).openConnection()).getContent();
                    path = vfsFile.getFile().toPath().getParent().resolve(vfsFile.getName());
                    name = FileUtils.getBaseName(vfsFile.getName());
                } else {
                    path = ZipUtils.toPath(uri);
                    name = FileUtils.getBaseName(path.getFileName().toString());
                }
                if (localStorage.containsKey(name)) {
                    throw new IllegalStateException(String.format("The resource with name '%s' already exits.", name));
                }
                localStorage.put(name, path);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to initialize a repository.", e);
            }
        };

        try {
            getResources(PROJECT_DESCRIPTOR_FILE).forEach(collector);
            getResources(DEPLOYMENT_DESCRIPTOR_XML_FILE).forEach(collector);
            getResources(DEPLOYMENT_DESCRIPTOR_YAML_FILE).forEach(collector);
            Stream<Resource> archives;
            try {
                archives = Stream.of(resourceResolver.getResources("/openl/*.zip"));
            } catch (FileNotFoundException ignored) {
                archives = Stream.empty();// OK
            }
            archives.forEach(collector);
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

    private Stream<Resource> getResources(String fileName) throws IOException {
        String locationPattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX  + fileName;
        return Stream.of(resourceResolver.getResources(locationPattern));
    }

}
