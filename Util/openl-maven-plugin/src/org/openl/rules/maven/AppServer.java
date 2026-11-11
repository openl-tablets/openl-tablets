package org.openl.rules.maven;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.Collection;

import org.eclipse.jetty.ee10.webapp.MetaInfConfiguration;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ClassMatcher;
import org.eclipse.jetty.util.resource.ResourceFactory;

public class AppServer {

    private AppServer() {
        // Utility class
    }

    /**
     * Checks that the OpenL application can be run successfully.
     * It is called via reflection in the isolated classloader.
     *
     * @param pathDeployment - location of the zipped OpenL projects
     * @param jars           - OpenL classpath
     * @param workDir        - folder location for temporary or working files
     * @throws Exception if any errors
     */
    public static void check(String pathDeployment, Collection<File> jars, String workDir) throws Exception {
        var webAppContext = new WebAppContext();
        var resourceFactory = ResourceFactory.of(webAppContext);
        var libs = jars.stream().map(File::toURI).map(resourceFactory::newResource).toList();
        var warFolder = Files.createTempDirectory("openl-maven-plugin", ownerOnly());
        webAppContext.setWar(warFolder.toString()); // No resources
        webAppContext.addProtectedClassMatcher(new ClassMatcher("org.slf4j.")); // For logging via Maven SLF4J
        webAppContext.addProtectedClassMatcher(new ClassMatcher("-jakarta.activation."));
        webAppContext.setExtraClasspath(libs); // WebAppClassLoader

        webAppContext.setInitParameter("openl.config.location", ""); // to be not affected by external configurations
        webAppContext.setInitParameter("user.home", workDir); // to be not affected by external configurations
        webAppContext.setInitParameter("production-repository.factory", "repo-zip");
        webAppContext.setInitParameter("production-repository.archives", pathDeployment);

        webAppContext.setAttribute(MetaInfConfiguration.WEBINF_JAR_PATTERN, ".*ruleservice.ws[^/]*\\.jar$"); // For scanning annotations of the RuleService WS

        var server = new Server(0); // Random port
        server.setHandler(webAppContext);

        // Prevent memory leak via JDK ClassValue. See GROOVY-7591.
        // GroovyClassValueLeakTest guards the property, which Groovy 5 dropped and Groovy 6 brought back.
        // Groovy reads the property once, when the application loads it, so the property is never restored:
        // removing it would break another module, which starts its server at the same time in a parallel build.
        System.setProperty("groovy.use.classvalue", "false");
        try {
            server.start();

            var port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
            var uri = new URI("http://localhost:" + port + "/admin/healthcheck/readiness");
            checkReadiness(uri, Duration.ofSeconds(60)); // wait a minute, it is usual enough a second
        } finally {
            server.stop();
            server.destroy();
            Files.deleteIfExists(warFolder);
        }
    }

    /**
     * Asks the application whether it is ready to serve requests.
     *
     * <p>The request is sent and awaited in the calling thread. An answer that does not come in time fails
     * the check, even when the application has lost the threads that serve its requests, as it happens after
     * an {@link OutOfMemoryError}.
     *
     * @param uri     - address of the readiness check of the application
     * @param timeout - how long to wait for the connection, and then for the answer
     * @throws IOException if the application does not answer in time or is not ready
     */
    static void checkReadiness(URI uri, Duration timeout) throws IOException {
        var connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setConnectTimeout((int) timeout.toMillis());
        connection.setReadTimeout((int) timeout.toMillis());
        connection.setRequestProperty("Connection", "close"); // No pooled connection outlives the check
        try {
            var status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("Server startup failure: HTTP status " + status);
            }
            try (var body = connection.getInputStream()) {
                var answer = new String(body.readAllBytes(), StandardCharsets.UTF_8);
                if (!answer.equals("READY")) {
                    throw new IOException("Server startup failure: " + answer);
                }
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Lets only the owner enter a directory where the file system tracks POSIX permissions.
     * The isolated classloader running this class has no OpenL utilities, hence the local copy.
     */
    private static FileAttribute<?>[] ownerOnly() {
        if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            return new FileAttribute<?>[0];
        }
        return new FileAttribute<?>[]{PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"))};
    }
}
