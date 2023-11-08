package org.openl.rules.maven;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.stream.Collectors;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.ClassMatcher;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;

public class AppServer {

    /**
     * Checks that the OpenL application can be run successfully.
     * It is called via reflection in the isolated classloader.
     *
     * @param pathDeployment - location of the zipped OpenL projects
     * @param jars - OpenL classpath
     * @param workDir - folder location for temporary or working files
     * @throws Exception if any errors
     */
    public static void check(String pathDeployment, Collection<File> jars, String workDir) throws Exception {
        var libs = jars.stream().map(Resource::newResource).collect(Collectors.toList());
        var webAppContext = new WebAppContext();
        webAppContext.setResourceBase(""); // No resources
        webAppContext.addSystemClassMatcher(new ClassMatcher("org.slf4j.")); // For logging via Maven SLF4J
        webAppContext.addSystemClassMatcher(new ClassMatcher("-javax.activation."));
        webAppContext.setExtraClasspath(libs); // WebAppClassLoader

        webAppContext.setInitParameter("openl.config.location", ""); // to be not affected by external configurations
        webAppContext.setInitParameter("user.home", workDir); // to be not affected by external configurations
        webAppContext.setInitParameter("production-repository.factory", "repo-zip");
        webAppContext.setInitParameter("production-repository.archives", pathDeployment);

        webAppContext.setAttribute(MetaInfConfiguration.WEBINF_JAR_PATTERN, ".*ruleservice.ws[^/]*\\.jar$"); // For scanning annotations of the RuleService WS

        var server = new Server(0); // Random port
        server.setHandler(webAppContext);

        var backupProperties = System.getProperties();
        try {
            System.setProperty("groovy.use.classvalue", "false"); // Prevent memory leak via JDK ClassValue. See GROOVY-7591
            server.start();

            int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
            var client = HttpClient.newBuilder()
                    .executor(Runnable::run) // To prevent memory leak via the default thread pool
                    .version(java.net.http.HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(60)) // wait a minute, it is usual enough a second
                    .build();
            var uri = new URL("http", "localhost", port, "/admin/healthcheck/readiness").toURI();
            var req = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(60)) // wait a minute, it is usual enough a second
                    .GET()
                    .build();
            var resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200 || !resp.body().equals("READY")) {
                throw new IOException("Server startup failure: " + resp);
            }
        } finally {
            server.stop();
            server.destroy();
            System.setProperties(backupProperties);
        }
    }
}
