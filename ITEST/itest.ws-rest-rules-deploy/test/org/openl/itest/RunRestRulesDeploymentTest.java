package org.openl.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.openl.itest.core.RestClientFactory.assertText;
import static org.openl.itest.core.RestClientFactory.file;
import static org.openl.itest.core.RestClientFactory.json;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.RestClientFactory;
import org.openl.itest.response.ServiceInfoResponse;
import org.openl.itest.response.UiInfoResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class RunRestRulesDeploymentTest {

    private static final String SINGLE_DEPLOYMENT_ENDPOINT = "/REST/deployed-rules/hello";
    private static final String MULTIPLE_DEPLOYMENT_ENDPOINT = "/REST/project1/sayHello";

    private static JettyServer server;
    private static String baseURI;

    private RestTemplate rest;

    @BeforeClass
    public static void setUp() throws Exception {
        server = new JettyServer();
        baseURI = server.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }

    @Before
    public void before() {
        rest = new RestClientFactory(baseURI).create();
    }

    @Test
    public void testDeployRules() throws IOException {
        assertEquals(HttpStatus.NOT_FOUND,
            rest.postForEntity(SINGLE_DEPLOYMENT_ENDPOINT, json("{`name`: `Vlad`}"), String.class).getStatusCode());

        ServiceInfoResponse[] servicesInfo = rest.getForObject("/admin/services", ServiceInfoResponse[].class);
        assertEquals(0, servicesInfo.length);

        long createServiceTime = System.currentTimeMillis();
        doDeploy("/rules-to-deploy.zip");

        ClassPathResource classPathResource = new ClassPathResource("/rules-to-deploy.zip");

        File file = classPathResource.getFile();
        byte[] fileContent = Files.readAllBytes(file.toPath());

        ServiceInfoResponse[] servicesInfo2 = rest.getForObject("/admin/services", ServiceInfoResponse[].class);
        byte[] fileContent2 = rest.getForObject("/admin/read/" + servicesInfo2[0].getName(), byte[].class);
        assertTrue(Arrays.equals(fileContent, fileContent2));

        assertEquals(1, servicesInfo2.length);
        checkServiceInfo(servicesInfo2[0], "deployed-rules", "deployed-rules", "REST/deployed-rules");
        long service2Time = servicesInfo2[0].getStartedTime().getTime();
        checkServiceTime(service2Time, createServiceTime);

        String body = rest.postForObject(SINGLE_DEPLOYMENT_ENDPOINT, json("{`name`: `Vlad`}"), String.class);
        assertEquals("Hello, Vlad", body);

        // should not be updated
        doDeploy("/rules-to-deploy_v2.zip");

        ServiceInfoResponse[] servicesInfo3 = rest.getForObject("/admin/services", ServiceInfoResponse[].class);
        assertEquals(1, servicesInfo3.length);
        checkServiceInfo(servicesInfo3[0], "deployed-rules", "deployed-rules", "REST/deployed-rules");
        long service3Time = servicesInfo3[0].getStartedTime().getTime();
        assertEquals(service2Time, service3Time);

        body = rest.postForObject(SINGLE_DEPLOYMENT_ENDPOINT, json("{`name`: `Vlad`}"), String.class);
        assertEquals("Hello, Vlad", body);

        // should be updated
        createServiceTime = System.currentTimeMillis();
        doRedeploy("/rules-to-deploy_v2.zip");

        ServiceInfoResponse[] servicesInfo4 = rest.getForObject("/admin/services", ServiceInfoResponse[].class);
        assertEquals(1, servicesInfo4.length);
        checkServiceInfo(servicesInfo4[0], "deployed-rules", "deployed-rules", "REST/deployed-rules");
        checkServiceTime(servicesInfo4[0].getStartedTime().getTime(), createServiceTime);

        UiInfoResponse uiInfoResponseResponseEntity = rest.getForObject("/admin/ui/info", UiInfoResponse.class);
        ServiceInfoResponse[] serviceInfo = uiInfoResponseResponseEntity.getServices();
        assertTrue(uiInfoResponseResponseEntity.getDeployerEnabled());
        assertEquals(1, serviceInfo.length);
        assertEquals("deployed-rules", serviceInfo[0].getName());

        body = rest.postForObject(SINGLE_DEPLOYMENT_ENDPOINT, json("{`name`: `Vlad`}"), String.class);
        assertEquals("Hello, Mr. Vlad", body);
        HttpEntity<?> entity = new HttpEntity<>(new HttpHeaders());
        rest.delete("/admin/delete/deployed-rules");
        UiInfoResponse uiInfoResponseResponseEntity2 = rest.getForObject("/admin/ui/info", UiInfoResponse.class);
        assertEquals(0, uiInfoResponseResponseEntity2.getServices().length);
        assertTrue(uiInfoResponseResponseEntity.getDeployerEnabled());

    }

    @Test
    public void testDeployRules_multipleDeployment() {

        assertEquals(HttpStatus.NOT_FOUND,
            rest.postForEntity(MULTIPLE_DEPLOYMENT_ENDPOINT, json("{`name`: `Vlad`}"), String.class).getStatusCode());

        long createServiceTime = System.currentTimeMillis();
        doDeploy("/multiple-deployment_v1.zip");

        ServiceInfoResponse[] servicesInfo = rest.getForObject("/admin/services", ServiceInfoResponse[].class);
        assertEquals(2, servicesInfo.length);
        checkServiceInfo(servicesInfo[0], "project1", "project1", "REST/project1");
        checkServiceTime(servicesInfo[0].getStartedTime().getTime(), createServiceTime);
        checkServiceInfo(servicesInfo[1],
            "yaml_project_project2",
            "yaml_project/project2",
            "REST/yaml_project/project2");
        checkServiceTime(servicesInfo[1].getStartedTime().getTime(), createServiceTime);

        String serviceMethods2 = rest.getForObject("/admin/services" + "/project1" + "/methods", String.class);
        assertText(
            "[{`name`:`getProject2Version`,`paramTypes`:[],`returnType`:`String`},{`name`:`sayHello`,`paramTypes`:[`String`],`returnType`:`String`}]",
            serviceMethods2);
        String serviceMethods3 = rest.getForObject("/admin/services" + "/yaml_project_project2" + "/methods",
            String.class);
        assertText("[{`name`:`getProject2Version`,`paramTypes`:[`IRulesRuntimeContext`],`returnType`:`String`}]",
            serviceMethods3);

        String body = rest.postForObject(MULTIPLE_DEPLOYMENT_ENDPOINT, json("{`name`: `Vlad`}"), String.class);
        // TODO: looks like we have a bug. because response looks wrong. It should return "Hello, Vlad! v1"
        assertText("Hello, {`name`: `Vlad`}! v1", body);

        // should not be updated
        doDeploy("/multiple-deployment_v2.zip");

        body = rest.postForObject(MULTIPLE_DEPLOYMENT_ENDPOINT, json("{`name`: `Vlad`}"), String.class);
        assertText("Hello, {`name`: `Vlad`}! v1", body);

        // should not be updated
        doRedeploy("/multiple-deployment_v2.zip");

        body = rest.postForObject(MULTIPLE_DEPLOYMENT_ENDPOINT, json("{`name`: `Vlad`}"), String.class);
        assertText("Hello, Mr. {`name`: `Vlad`}! v2", body);
    }

    @Test
    public void testMissingServiceMethods() {
        ResponseEntity<String> response = rest.getForEntity("/admin/services/missing-name/methods", String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    private void checkServiceInfo(ServiceInfoResponse service,
            String expectedName,
            String expectedSoap,
            String expectedRest) {
        assertEquals(expectedName, service.getName());
        assertEquals(expectedSoap, service.getUrls().get("SOAP"));
        assertEquals(expectedRest, service.getUrls().get("REST"));
    }

    private void checkServiceTime(long timeToCheckMs, long createServiceTime) {
        assertTrue((timeToCheckMs >= createServiceTime && timeToCheckMs <= System.currentTimeMillis()));
    }

    private void doDeploy(String rules) {
        ResponseEntity<Void> response = rest.postForEntity("/admin/deploy", file(rules), Void.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    private void doRedeploy(String rules) {
        ResponseEntity<Void> response = rest.exchange("/admin/deploy", HttpMethod.PUT, file(rules), Void.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }
}
