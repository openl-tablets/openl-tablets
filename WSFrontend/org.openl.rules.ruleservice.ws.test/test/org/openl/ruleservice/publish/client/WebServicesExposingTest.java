package org.openl.ruleservice.publish.client;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.List;

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
        //JAXB 
        /*assertEquals("World, Good Morning!",
                multimoduleClient.invoke("worldHello", new Object[] { new Integer(10) })[0]);
        assertEquals(2, CollectionUtils.size(multimoduleClient.invoke("getData1")[0]));
        assertEquals(3, CollectionUtils.size(multimoduleClient.invoke("getData2")[0]));
        assertEquals(2, CollectionUtils.size(tutorial4Client.invoke("getCoverage")[0]));*/
        
        //Aegis
        assertEquals("World, Good Morning!",
                multimoduleClient.invoke("worldHello", new Object[] { new Integer(10) })[0]);
        Object result = multimoduleClient.invoke("getData1")[0];
        Method m1 = Thread.currentThread().getContextClassLoader().loadClass("multimodule.test2.ArrayOfDomainObject1")
                .getMethod("getDomainObject1s");
        List<?> listResult = (List<?>) m1.invoke(result);
        assertEquals(2, listResult.size());
        result = multimoduleClient.invoke("getData2")[0];
        listResult = (List<?>) m1.invoke(result);
        assertEquals(3, listResult.size());
        result = tutorial4Client.invoke("getCoverage")[0];
        Method m2 = Thread.currentThread().getContextClassLoader().loadClass("org.openl.rules.tutorial4.ArrayOfString")
                .getMethod("getStrings");
        listResult = (List<?>) m2.invoke(result);
        assertEquals(2, listResult.size());
    }
    
    public static void main(String[] args) {
        try{
            JUnitCore.main("org.openl.ruleservice.publish.client.WebServicesExposingTest");
        }catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
