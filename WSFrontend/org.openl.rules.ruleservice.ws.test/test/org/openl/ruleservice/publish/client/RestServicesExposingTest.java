package org.openl.ruleservice.publish.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.apache.commons.collections.CollectionUtils;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.openl.generated.beans.Driver;
import org.openl.rules.tutorial4.Tutorial4Interface;

public class RestServicesExposingTest {
    private static final String TUTORIAL4_SERVICE_URL = "org.openl.tablets.tutorial4";
    private static final String BASE_URL = "http://localhost:9000/";

    private Tutorial4Interface service;

    @Before
    public void setUp() throws Exception {
        service = JAXRSClientFactory.create(BASE_URL + TUTORIAL4_SERVICE_URL, Tutorial4Interface.class,
                Arrays.asList(new JSONProvider()));
    }

    @Test
    public void testJsonSerivice() throws Exception {
        String[] coverageArray = service.getCoverage().getArray();
        assertEquals(2, CollectionUtils.size(coverageArray));
        assertNotNull(coverageArray[0]);

        Driver driver = new Driver("test", "gender", 1, "maritalStatus", "state", 2, 3, 4, true);
        assertNotNull(service.driverAgeType(driver));

//        assertNotNull(service.driverAgeType(driver).getName());
    }

    public static void main(String[] args) {
        try {
            JUnitCore.main(RestServicesExposingTest.class.getName());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
