package org.openl.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.itest.core.JettyServer;
import org.openl.itest.core.RestClientFactory;
import org.openl.itest.response.ServiceInfoResponse;
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
    private static final String SERVICES_INFO_ENDPOINT = "/admin/services";
    private static final String SERVICES_METHODS_ENDPOINT = "/methods";

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
    public void testDeployRules() {
        assertEquals(HttpStatus.NOT_FOUND, sendHelloRequest(SINGLE_DEPLOYMENT_ENDPOINT).getStatusCode());

        ResponseEntity<ServiceInfoResponse[]> services = fetchServices(SERVICES_INFO_ENDPOINT);
        ServiceInfoResponse[] servicesInfo = services.getBody();
        assertEquals(0, servicesInfo.length);

        long createServiceTime = System.currentTimeMillis();
        ResponseEntity<String> response = doDeploy("/rules-to-deploy.zip", HttpMethod.POST);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        ServiceInfoResponse[] servicesInfo2 = fetchServices(SERVICES_INFO_ENDPOINT).getBody();
        assertEquals(1, servicesInfo2.length);
        checkServiceInfo(servicesInfo2[0], "deployed-rules", "deployed-rules", "REST/deployed-rules");
        long service2Time = servicesInfo2[0].getStartedTime().getTime();
        checkServiceTime(service2Time, createServiceTime);

        String body = pingDeployedService(SINGLE_DEPLOYMENT_ENDPOINT);
        assertEquals("Hello, Vlad", body);

        // should not be updated
        response = doDeploy("/rules-to-deploy_v2.zip", HttpMethod.POST);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        ServiceInfoResponse[] servicesInfo3 = fetchServices(SERVICES_INFO_ENDPOINT).getBody();
        assertEquals(1, servicesInfo3.length);
        checkServiceInfo(servicesInfo3[0], "deployed-rules", "deployed-rules", "REST/deployed-rules");
        long service3Time = servicesInfo3[0].getStartedTime().getTime();
        assertEquals(service2Time, service3Time);

        body = pingDeployedService(SINGLE_DEPLOYMENT_ENDPOINT);
        assertEquals("Hello, Vlad", body);

        // should be updated
        createServiceTime = System.currentTimeMillis();
        response = doDeploy("/rules-to-deploy_v2.zip", HttpMethod.PUT);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        ServiceInfoResponse[] servicesInfo4 = fetchServices(SERVICES_INFO_ENDPOINT).getBody();
        assertEquals(1, servicesInfo4.length);
        checkServiceInfo(servicesInfo4[0], "deployed-rules", "deployed-rules", "REST/deployed-rules");
        checkServiceTime(servicesInfo4[0].getStartedTime().getTime(), createServiceTime);

        body = pingDeployedService(SINGLE_DEPLOYMENT_ENDPOINT);
        assertEquals("Hello, Mr. Vlad", body);
    }

    @Test
    public void testDeployRules_multipleDeployment() {

        assertEquals(HttpStatus.NOT_FOUND, sendHelloRequest(MULTIPLE_DEPLOYMENT_ENDPOINT).getStatusCode());

        long createServiceTime = System.currentTimeMillis();
        ResponseEntity<String> response = doDeploy("/multiple-deployment_v1.zip", HttpMethod.POST);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        ResponseEntity<ServiceInfoResponse[]> services = fetchServices(SERVICES_INFO_ENDPOINT);
        ServiceInfoResponse[] servicesInfo = services.getBody();
        assertEquals(3, servicesInfo.length);
        checkServiceInfo(servicesInfo[0], "deployed-rules", "deployed-rules", "REST/deployed-rules");
        checkServiceInfo(servicesInfo[1], "project1", "project1", "REST/project1");
        checkServiceTime(servicesInfo[1].getStartedTime().getTime(), createServiceTime);
        checkServiceInfo(servicesInfo[2],
            "yaml_project_project2",
            "yaml_project/project2",
            "REST/yaml_project/project2");
        checkServiceTime(servicesInfo[2].getStartedTime().getTime(), createServiceTime);

        ResponseEntity<String> serviceMethods1 = fetchServiceMethods(
            SERVICES_INFO_ENDPOINT + "/deployed-rules" + SERVICES_METHODS_ENDPOINT);
        assertEquals(
            "[{\"name\":\"hello\",\"paramTypes\":[\"IRulesRuntimeContext\",\"String\"],\"returnType\":\"String\"}]",
            serviceMethods1.getBody());
        ResponseEntity<String> serviceMethods2 = fetchServiceMethods(
            SERVICES_INFO_ENDPOINT + "/project1" + SERVICES_METHODS_ENDPOINT);
        assertEquals(
            "[{\"name\":\"getProject2Version\",\"paramTypes\":[],\"returnType\":\"String\"},{\"name\":\"sayHello\",\"paramTypes\":[\"String\"],\"returnType\":\"String\"}]",
            serviceMethods2.getBody());
        ResponseEntity<String> serviceMethods3 = fetchServiceMethods(
            SERVICES_INFO_ENDPOINT + "/yaml_project_project2" + SERVICES_METHODS_ENDPOINT);
        assertEquals(
            "[{\"name\":\"getProject2Version\",\"paramTypes\":[\"IRulesRuntimeContext\"],\"returnType\":\"String\"}]",
            serviceMethods3.getBody());

        String body = pingDeployedService(MULTIPLE_DEPLOYMENT_ENDPOINT);
        // TODO: looks like we have a bug. because response looks wrong. It should return "Hello, Vlad! v1"
        assertEquals("Hello, {\"name\": \"Vlad\"}! v1", body);

        // should not be updated
        response = doDeploy("/multiple-deployment_v2.zip", HttpMethod.POST);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        body = pingDeployedService(MULTIPLE_DEPLOYMENT_ENDPOINT);
        assertEquals("Hello, {\"name\": \"Vlad\"}! v1", body);

        // should not be updated
        response = doDeploy("/multiple-deployment_v2.zip", HttpMethod.PUT);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        body = pingDeployedService(MULTIPLE_DEPLOYMENT_ENDPOINT);
        assertEquals("Hello, Mr. {\"name\": \"Vlad\"}! v2", body);
    }

    @Test
    public void testMissingServiceMethods() {
        ResponseEntity<String> serviceMethods0 = fetchServiceMethods(
            SERVICES_INFO_ENDPOINT + "/missing-name" + SERVICES_METHODS_ENDPOINT);
        assertEquals(HttpStatus.NOT_FOUND, serviceMethods0.getStatusCode());
        assertNull(serviceMethods0.getBody());
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

    private String pingDeployedService(String endpoint) {
        ResponseEntity<String> response = sendHelloRequest(endpoint);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertNotNull(body);
        return body;
    }

    private ResponseEntity<String> sendHelloRequest(String endpoint) {
        return rest
            .exchange(endpoint, HttpMethod.POST, RestClientFactory.request("{\"name\": \"Vlad\"}"), String.class);
    }

    private ResponseEntity<String> doDeploy(String rules, HttpMethod method) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/zip");
        return rest
            .exchange("/rules/deploy", method, new HttpEntity<>(new ClassPathResource(rules), headers), String.class);
    }

    private ResponseEntity<ServiceInfoResponse[]> fetchServices(String endpoint) {
        HttpEntity<?> entity = new HttpEntity<>(new HttpHeaders());
        ResponseEntity<ServiceInfoResponse[]> response = rest
            .exchange(endpoint, HttpMethod.GET, entity, ServiceInfoResponse[].class);
        return response;
    }

    private ResponseEntity<String> fetchServiceMethods(String endpoint) {
        HttpEntity<?> entity = new HttpEntity<>(new HttpHeaders());
        ResponseEntity<String> response = rest.exchange(endpoint, HttpMethod.GET, entity, String.class);
        return response;
    }
}
