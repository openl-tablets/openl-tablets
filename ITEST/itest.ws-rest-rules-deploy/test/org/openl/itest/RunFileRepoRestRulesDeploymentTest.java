package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.openl.itest.core.HttpClient;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.worker.AsyncExecutor;

public class RunFileRepoRestRulesDeploymentTest {

    @AutoClose
    private static final HttpClient client = JettyServer.get().withProfile("file").start();


    @Test
    public void testDeployRules() {
        client.test("test-resources/deploy-single");
    }

    @Test
    public void testDeployVersion() {
        client.test("test-resources/deploy-version");
    }

    @Test
    public void testDeployRules_multipleDeployment() {
        client.test("test-resources/deploy-multi");
    }

    @Test
    public void testMissingServiceMethods() {
        client.test("test-resources/missing-service");
    }

    @Test
    public void test_EPBDS_8758_multithread() throws InterruptedException {
        client.send("admin_services_no_services.json.get");

        client.send("EPBDS-8758/doSomething_v1.deploy.post");
        client.send("EPBDS-8758/doSomething_v1.get");

        AsyncExecutor executor = new AsyncExecutor(() -> client.send("EPBDS-8758/doSomething.get"));
        executor.start();

        TimeUnit.SECONDS.sleep(1);
        client.send("EPBDS-8758/doSomething_v2.deploy.post");
        client.send("EPBDS-8758/doSomething_v2.get");
        client.send("EPBDS-8758/doSomething_v3.deploy.post");
        client.send("EPBDS-8758/doSomething_v3.get");
        TimeUnit.SECONDS.sleep(1);

        boolean invocationErrors = executor.stop();

        client.send("EPBDS-8758/EPBDS-8758.delete");

        assertFalse(invocationErrors);
        client.send("admin_services_no_services.json.get");
    }

    @Test
    @Disabled("Check EPBDS-10940 issue")
    public void test_EPBDS_8758_multithread2() throws Exception {
        client.send("admin_services_no_services.json.get");

        client.send("EPBDS-8758/doSomething_v1.deploy.post");
        client.send("EPBDS-8758/doSomething_v1.get");

        AsyncExecutor executor = new AsyncExecutor(() -> client.send("EPBDS-8758/doSomething.get"));
        executor.start();

        AsyncExecutor deployers = AsyncExecutor.start(() -> client.send("EPBDS-8758/doSomething_v2.deploy.post"),
                () -> client.send("EPBDS-8758/doSomething_v3.deploy.post"));

        TimeUnit.SECONDS.sleep(1);
        boolean deployErrors = deployers.stop();
        boolean invocationErrors = executor.stop();

        client.send("EPBDS-8758/EPBDS-8758.delete");

        assertFalse(deployErrors);
        assertFalse(invocationErrors);
        client.send("admin_services_no_services.json.get");
    }

    @Test
    public void test_EPBDS_10068_MANIFEST() {
        client.send("admin_services_no_services.json.get");
        client.send("EPBDS-10068/EPBDS-10068.deploy.post");
        client.send("EPBDS-10068/MANIFEST.MF.get");
        client.send("EPBDS-10068/EPBDS-10068.delete");
        client.send("admin_services_no_services.json.get");
    }

    @Test
    public void test_EPBDS_10157() {
        client.send("admin_services_no_services.json.get");

        client.send("EPBDS-10157/EPBDS-10157.deploy.post");
        client.send("EPBDS-10157/EPBDS-10157-doSomething.get");

        client.send("EPBDS-10157/EPBDS-10157_2.deploy.post");
        client.send("EPBDS-10157/EPBDS-10157_whitespace-doSomething.get");

        client.send("EPBDS-10157/EPBDS-9902.deploy.post");
        client.send("EPBDS-10157/EPBDS-9902-doSomething.get");

        client.send("EPBDS-10157/EPBDS-10157.delete");
        client.send("EPBDS-10157/EPBDS-10157_whitespace.delete");
        client.send("EPBDS-10157/EPBDS-9902.delete");
        client.send("admin_services_no_services.json.get");
    }

    @Test
    public void EPBDS_10891() {
        client.send("admin_services_no_services.json.get");

        client.send("EPBDS-10891/EPBDS-10891.deploy.post");
        client.send("EPBDS-10891/services.get");
        client.send("EPBDS-10891/yaml_project_all.delete");
        client.send("admin_services_no_services.json.get");

        client.send("EPBDS-10891/EPBDS-10891.deploy.post");
        client.send("EPBDS-10891/services.get");
        client.send("EPBDS-10891/yaml_project_all.delete");
        client.send("admin_services_no_services.json.get");
    }

    @Test
    public void EPBDS_9876() {
        client.send("admin_services_no_services.json.get");
        client.send("EPBDS-9876/deploy_name1_name1.deploy.post");
        client.send("EPBDS-9876/deploy_samename_name1.deploy.post");
        client.send("EPBDS-9876/deploy_url1_url1.deploy.post");
        client.send("EPBDS-9876/deploy_url2_url2.deploy.post");
        client.send("EPBDS-9876/deployed-rules_services.get");
        client.send("EPBDS-9876/deployed-rules.delete");
        client.send("EPBDS-9876/deployed-rules.delete2");
        client.send("EPBDS-9876/deployed-rules.delete3");
        client.send("EPBDS-9876/deployed-rules.delete4");
        client.send("admin_services_no_services.json.get");
    }

    @Disabled
    @Test
    public void EPBDS_11144() {
        client.send("admin_services_no_services.json.get");
        client.send("EPBDS-11144/deploy.post");
        client.send("EPBDS-11144/deployed-rules_services.get");
        client.send("EPBDS-11144/download.get");
        client.send("EPBDS-11144/delete.delete");
        client.send("admin_services_no_services.json.get");
    }

    @Test
    public void EPBDS_8987() {
        client.send("admin_services_no_services.json.get");
        client.send("EPBDS-8987/someDeployment.deploy.post");
        client.send("EPBDS-8987/deployed-rules_services_1.get");
        client.send("EPBDS-8987/multiple-deployment_v1.deploy.post");
        client.send("EPBDS-8987/deployed-rules_services_2.1.get");
        client.send("EPBDS-8987/delete_all_1.delete");
        client.send("admin_services_no_services.json.get");
        client.send("EPBDS-8987/deployment1.deploy.post");
        client.send("EPBDS-8987/deployed-rules_services_3.get");
        client.send("EPBDS-8987/deployment2.deploy.post");
        client.send("EPBDS-8987/deployed-rules_services_4.1.get");
        client.send("EPBDS-8987/delete_all_2.delete");
        client.send("admin_services_no_services.json.get");
        client.send("EPBDS-8987/deployment1.deploy.post");
        client.send("EPBDS-8987/deployed-rules_services_3.get");
        client.send("EPBDS-8987/delete_all_2.delete");
        client.send("admin_services_no_services.json.get");
    }

    @Test
    public void EPBDS_11177() {
        client.send("admin_services_no_services.json.get");
        client.send("EPBDS-11177/project1.deploy.post");
        client.send("EPBDS-11177/project2.deploy.post");
        client.send("EPBDS-11177/project2.deploy.post");
        client.send("EPBDS-11177/projects.get");
        client.send("EPBDS-11177/project1.delete");
        client.send("EPBDS-11177/project2.delete");
        client.send("admin_services_no_services.json.get");
    }
}
