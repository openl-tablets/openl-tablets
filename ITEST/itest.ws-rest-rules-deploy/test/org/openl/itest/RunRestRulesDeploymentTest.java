package org.openl.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import org.openl.itest.response.ServiceInfoResponse;
import org.openl.itest.response.UiInfoResponse;

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
        client.get("/admin/services", "/no_services.resp.txt");
        client.post("/REST/deployed-rules/hello", "/deployed-rules_hello.req.json", 404, "/404.html");
        long createServiceTime = System.currentTimeMillis();
        client.post("/admin/deploy", "/rules-to-deploy.zip", 201);

        ServiceInfoResponse[] servicesInfo2 = client.get("/admin/services", ServiceInfoResponse[].class);
        assertEquals(1, servicesInfo2.length);
        checkServiceInfo(servicesInfo2[0], "deployed-rules", "deployed-rules", "REST/deployed-rules");
        long service2Time = servicesInfo2[0].getStartedTime().getTime();
        checkServiceTime(service2Time, createServiceTime);

        client.get("/admin/read/deployed-rules", "/rules-to-deploy.zip");
        client.get("/admin/services/deployed-rules/methods", "/deployed-rules_methods.resp.txt");

        client.post("/REST/deployed-rules/hello", "/deployed-rules_hello.req.json", "/deployed-rules_hello.resp.txt");
        // should not be updated
        client.post("/admin/deploy", "/rules-to-deploy_v2.zip", 201);

        ServiceInfoResponse[] servicesInfo3 = client.get("/admin/services", ServiceInfoResponse[].class);
        assertEquals(1, servicesInfo3.length);
        checkServiceInfo(servicesInfo3[0], "deployed-rules", "deployed-rules", "REST/deployed-rules");
        long service3Time = servicesInfo3[0].getStartedTime().getTime();
        assertEquals(service2Time, service3Time);

        client.post("/REST/deployed-rules/hello", "/deployed-rules_hello.req.json", "/deployed-rules_hello.resp.txt");

        // should be updated
        createServiceTime = System.currentTimeMillis();
        client.put("/admin/deploy", "/rules-to-deploy_v2.zip", 201);

        ServiceInfoResponse[] servicesInfo4 = client.get("/admin/services", ServiceInfoResponse[].class);
        assertEquals(1, servicesInfo4.length);
        checkServiceInfo(servicesInfo4[0], "deployed-rules", "deployed-rules", "REST/deployed-rules");
        checkServiceTime(servicesInfo4[0].getStartedTime().getTime(), createServiceTime);

        UiInfoResponse uiInfoResponseResponseEntity = client.get("/admin/ui/info", UiInfoResponse.class);
        ServiceInfoResponse[] serviceInfo = uiInfoResponseResponseEntity.getServices();
        assertTrue(uiInfoResponseResponseEntity.getDeployerEnabled());
        assertEquals(1, serviceInfo.length);
        assertEquals("deployed-rules", serviceInfo[0].getName());
        assertEquals(ServiceInfoResponse.ServiceStatus.DEPLOYED, serviceInfo[0].getStatus());

        client.post("/REST/deployed-rules/hello", "/deployed-rules_hello.req.json", "/deployed-rules_hello_2.resp.txt");

        client.put("/admin/deploy", "/rules-to-deploy-failed.zip", 201);
        ServiceInfoResponse[] servicesInfo5 = client.get("/admin/services", ServiceInfoResponse[].class);
        assertEquals(1, servicesInfo5.length);
        assertEquals(ServiceInfoResponse.ServiceStatus.FAILED, servicesInfo5[0].getStatus());
        client.get("/admin/services/deployed-rules/errors", "/deployed-rules_errors.resp.json");

        client.delete("/admin/delete/deployed-rules");
        UiInfoResponse uiInfoResponseResponseEntity2 = client.get("/admin/ui/info", UiInfoResponse.class);
        assertEquals(0, uiInfoResponseResponseEntity2.getServices().length);
        assertTrue(uiInfoResponseResponseEntity.getDeployerEnabled());

        client.post("/admin/deploy", "/empty_project.zip", 400);
    }

    @Test
    public void testDeployRules_multipleDeployment() {
        client.get("/admin/services", "/no_services.resp.txt");
        client.post("/REST/project1/sayHello", "/project1_sayHello.req.txt", 404, "/404.html");

        long createServiceTime = System.currentTimeMillis();
        client.post("/admin/deploy", "/multiple-deployment_v1.zip", 201);

        ServiceInfoResponse[] servicesInfo = client.get("/admin/services", ServiceInfoResponse[].class);
        assertEquals(2, servicesInfo.length);
        checkServiceInfo(servicesInfo[0], "project1", "project1", "REST/project1");
        checkServiceTime(servicesInfo[0].getStartedTime().getTime(), createServiceTime);
        checkServiceInfo(servicesInfo[1],
            "yaml_project_project2",
            "yaml_project/project2",
            "REST/yaml_project/project2");
        checkServiceTime(servicesInfo[1].getStartedTime().getTime(), createServiceTime);

        client.get("/admin/services/project1/methods", "/project1_methods.resp.txt");
        client.get("/admin/services/yaml_project_project2/methods", "/yaml_project_project2_methods.resp.txt");

        client.post("/REST/project1/sayHello", "/project1_sayHello.req.txt", "/project1_sayHello.resp.txt");

        // should not be updated
        client.post("/admin/deploy", "/multiple-deployment_v2.zip", 201);
        client.post("/REST/project1/sayHello", "/project1_sayHello.req.txt", "/project1_sayHello.resp.txt");

        // should be updated
        client.put("/admin/deploy", "/multiple-deployment_v2.zip", 201);
        client.post("/REST/project1/sayHello", "/project1_sayHello.req.txt", "/project1_sayHello_2.resp.txt");
    }

    @Test
    public void testMissingServiceMethods() {
        client.get("/admin/services/missing-name/methods", 404);
    }

    @Test
    public void test_EPBDS_8758_multithread() throws Exception {
        client.post("/admin/deploy", "/EPBDS-8758/EPBDS-8758-v1.zip", 201);
        client.get("/REST/EPBDS-8758/doSomething", "/EPBDS-8758/doSomething_v1.resp.txt");
        AsyncExecutor executor = new AsyncExecutor(AsyncExecutor.MAX_THREADS, () -> client.get("/REST/EPBDS-8758/doSomething", String.class));
        TaskScheduler taskScheduler = new TaskScheduler();

        executor.start();

        taskScheduler.schedule(() -> {
            client.put("/admin/deploy", "/EPBDS-8758/EPBDS-8758-v2.zip", 201);
            client.get("/REST/EPBDS-8758/doSomething", "/EPBDS-8758/doSomething_v2.resp.txt");
        }, 1, TimeUnit.SECONDS);
        taskScheduler.schedule(() -> {
            client.put("/admin/deploy", "/EPBDS-8758/EPBDS-8758-v3.zip", 201);
            client.get("/REST/EPBDS-8758/doSomething", "/EPBDS-8758/doSomething_v3.resp.txt");
        }, 2, TimeUnit.SECONDS);

        List<Throwable> deployErrors = taskScheduler.await();
        List<Throwable> invocationErrors = executor.stop();

        client.delete("/admin/delete/EPBDS-8758");

        MultipleFailureException.assertEmpty(Stream.concat(deployErrors.stream(), invocationErrors.stream())
                .collect(Collectors.toList()));
    }

    private void checkServiceInfo(ServiceInfoResponse service,
            String expectedName,
            String expectedSoap,
            String expectedRest) {
        assertEquals(expectedName, service.getName());
        assertEquals(expectedSoap, service.getUrls().get("WEBSERVICE"));
        assertEquals(expectedRest, service.getUrls().get("RESTFUL"));
    }

    private void checkServiceTime(long timeToCheckMs, long createServiceTime) {
        assertTrue((timeToCheckMs >= createServiceTime && timeToCheckMs <= System.currentTimeMillis()));
    }

}
