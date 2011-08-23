package org.openl.ruleservice.publish.client;

import static junit.framework.Assert.assertEquals;

import org.apache.commons.collections.CollectionUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;
import org.junit.Test;
import org.junit.runner.JUnitCore;

public class WebServicesExposingTest {
    private static final String TUTORIAL4_SERVICE_URL = "org.openl.tablets.tutorial4";
    private static final String MULTIMODULE_SERVICE_URL = "multimodule";
    private static final String BASE_URL = "http://localhost:9000/";

    @Test
    public void testExposing() throws Exception {
        JaxWsDynamicClientFactory clientFactory = JaxWsDynamicClientFactory.newInstance();

        Client tutorial4Client = clientFactory.createClient(BASE_URL + TUTORIAL4_SERVICE_URL + "?wsdl");
        Client multimoduleClient = clientFactory.createClient(BASE_URL + MULTIMODULE_SERVICE_URL + "?wsdl");
        
        assertEquals("World, Good Morning!",
                multimoduleClient.invoke("worldHello", new Object[] { new Integer(10) })[0]);
        assertEquals(2, CollectionUtils.size(multimoduleClient.invoke("getData1")[0]));
        assertEquals(3, CollectionUtils.size(multimoduleClient.invoke("getData2")[0]));
        assertEquals(2, CollectionUtils.size(tutorial4Client.invoke("getCoverage")[0]));
    }
    
    public static void main(String[] args) {
        try{
            JUnitCore.main("org.openl.ruleservice.publish.client.WebServicesExposingTest");
        }catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
