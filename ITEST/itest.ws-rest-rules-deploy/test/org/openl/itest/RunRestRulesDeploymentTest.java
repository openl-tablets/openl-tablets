package org.openl.itest;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runners.model.MultipleFailureException;
import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.worker.AsyncExecutor;
import org.openl.itest.core.worker.TaskScheduler;

public class RunRestRulesDeploymentTest {

    private static JettyServer server;
    private static HttpClient client;

    @BeforeClass
    public static void setUp() throws Exception {
        server = JettyServer.start();
        client = server.client();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void testDeployRules() {
        client.send("ui-info.get");
        client.send("admin_services_no_services.json.get");
        client.send("deployed-rules_hello_not_found.post");

        client.post("/admin/deploy", "/rules-to-deploy.zip", 201);
        client.send("deployed-rules_services.get");
        client.send("deployed-rules_methods.get");
        client.get("/admin/deploy/deploy/rules-to-deploy/rules-to-deploy", "/rules-to-deploy.zip");
        client.send("deployed-rules_hello.post");

        // should be always redeployed
        client.post("/admin/deploy", "/rules-to-deploy_v2.zip", 201);
        client.send("deployed-rules_services.get");
        client.send("deployed-rules_methods.get");
        client.send("deployed-rules_ui-info.get");
        client.send("deployed-rules_hello_2.post");

        client.post("/admin/deploy", "/rules-to-deploy-failed.zip", 201);
        client.send("deployed-rules_services_failed.get");
        client.send("deployed-rules_errors.get");
        client.send("deployed-rules_manifest.get");

        client.send("deployed-rules.delete");
        client.send("admin_services_no_services.json.get");

        client.post("/admin/deploy", "/empty_project.zip", 400);
        client.send("admin_services_no_services.json.get");
    }

    @Test
    public void testDeployRules_multipleDeployment() {
        client.send("admin_services_no_services.json.get");

        client.post("/admin/deploy", "/multiple-deployment_v1.zip", 201);
        client.send("yaml_project_services.get");
        client.send("project1_methods.get");
        client.send("yaml_project_project2_methods.get");
        client.send("project1_sayHello.post");

        // should be updated
        client.post("/admin/deploy", "/multiple-deployment_v2.zip", 201);
        client.send("project1_sayHello_2.post");

        client.send("project1.delete");
        client.send("yaml_project_project2.delete");
        client.send("admin_services_no_services.json.get");
    }

    @Test
    public void testMissingServiceMethods() {
        client.send("missing-service_methods.get");
        client.send("missing-service.delete");
        client.send("missing-service.get");
        client.send("missing-service_errors.get");
        client.send("missing-service_manifest.get");
    }

    @Test
    public void test_EPBDS_8758_multithread() throws Exception {
        client.send("admin_services_no_services.json.get");

        client.post("/admin/deploy", "/EPBDS-8758/EPBDS-8758-v1.zip", 201);
        client.send("EPBDS-8758/doSomething_v1.get");

        AsyncExecutor executor = new AsyncExecutor(AsyncExecutor.MAX_THREADS,
            () -> client.send("EPBDS-8758/doSomething.get"));
        TaskScheduler taskScheduler = new TaskScheduler();

        executor.start();

        taskScheduler.schedule(() -> {
            client.post("/admin/deploy", "/EPBDS-8758/EPBDS-8758-v2.zip", 201);
            client.send("EPBDS-8758/doSomething_v2.get");
        }, 1, TimeUnit.SECONDS);

        taskScheduler.schedule(() -> {
            client.post("/admin/deploy", "/EPBDS-8758/EPBDS-8758-v3.zip", 201);
            client.send("EPBDS-8758/doSomething_v3.get");
        }, 2, TimeUnit.SECONDS);

        List<Throwable> deployErrors = taskScheduler.await();
        List<Throwable> invocationErrors = executor.stop();

        client.send("EPBDS-8758/EPBDS-8758.delete");

        MultipleFailureException
            .assertEmpty(Stream.concat(deployErrors.stream(), invocationErrors.stream()).collect(Collectors.toList()));
        client.send("admin_services_no_services.json.get");
    }

    @Test
    public void test_EPBDS_8758_multithread2() throws Exception {
        client.send("admin_services_no_services.json.get");

        client.post("/admin/deploy", "/EPBDS-8758/EPBDS-8758-v1.zip", 201);
        client.send("EPBDS-8758/doSomething_v1.get");

        AsyncExecutor executor = new AsyncExecutor(AsyncExecutor.MAX_THREADS,
            () -> client.send("EPBDS-8758/doSomething.get"));
        executor.start();

        AsyncExecutor deployers = new AsyncExecutor(
            () -> client.post("/admin/deploy", "/EPBDS-8758/EPBDS-8758-v2.zip", 201),
            () -> client.post("/admin/deploy", "/EPBDS-8758/EPBDS-8758-v3.zip", 201));

        deployers.start();
        TimeUnit.SECONDS.sleep(1);
        List<Throwable> deployErrors = deployers.stop();
        List<Throwable> invocationErrors = executor.stop();

        client.send("EPBDS-8758/EPBDS-8758.delete");

        MultipleFailureException
            .assertEmpty(Stream.concat(deployErrors.stream(), invocationErrors.stream()).collect(Collectors.toList()));
        client.send("admin_services_no_services.json.get");
    }

    @Test
    public void test_EPBDS_10068_MANIFEST() {
        client.post("/admin/deploy", "/EPBDS-10068/EPBDS-10068.zip", 201);
        client.send("EPBDS-10068/MANIFEST.MF.get");
        client.send("EPBDS-10068/EPBDS-10068.delete");
    }

    @Test
    public void test_EPBDS_10157() {
        client.send("admin_services_no_services.json.get");

        client.post("/admin/deploy/EPBDS-10157", "/EPBDS-10157/EPBDS-10157.zip", 201);
        client.send("EPBDS-10157/EPBDS-10157-doSomething.get");

        client.post("/admin/deploy/EPBDS-10157_2", "/EPBDS-10157/EPBDS-10157_2.zip", 201);
        client.send("EPBDS-10157/EPBDS-10157_whitespace-doSomething.get");

        client.post("/admin/deploy/EPBDS-9902", "/EPBDS-10157/EPBDS-9902.zip", 201);
        client.send("EPBDS-10157/EPBDS-9902-doSomething.get");

        client.send("EPBDS-10157/EPBDS-10157.delete");
        client.send("EPBDS-10157/EPBDS-10157_whitespace.delete");
        client.send("EPBDS-10157/EPBDS-9902.delete");
        client.send("admin_services_no_services.json.get");
    }

    @Test
    public void EPBDS_10891() {
        client.send("admin_services_no_services.json.get");

        client.post("/admin/deploy", "/EPBDS-10891/EPBDS-10891.zip", 201);
        client.send("EPBDS-10891/services.get");
        client.send("EPBDS-10891/yaml_project_Project1.delete");
        client.send("EPBDS-10891/yaml_project_Project2.delete");
        client.send("admin_services_no_services.json.get");

        client.post("/admin/deploy", "/EPBDS-10891/EPBDS-10891.zip", 201);
        client.send("EPBDS-10891/services.get");
        client.send("EPBDS-10891/yaml_project_Project1.delete");
        client.send("EPBDS-10891/yaml_project_Project2.delete");
        client.send("admin_services_no_services.json.get");
    }
}
