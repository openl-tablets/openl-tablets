package org.openl;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.service.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.util.ClassHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/beans-context.xml")
public class ClientServiceTest {

    //TODO: the remote service should be replaced to local
    private static final String NAMESPACEURI = "http://service.ws.rate.hldsa.exigen.com/";
    private static final String SERVICE_BINDING = "PremiumServiceImplServiceSoapBinding";
    private static final String OPERATION_NAME = "calculateComputerPremium";

    private Client client;

    @Test
    public void test() throws Exception {
        cxfClient();
    }


	private void cxfClient() throws Exception {
        secondWay(client);
        oneWay(client);
	}

    private static void secondWay(Client client) throws Exception, InvocationTargetException, ClassNotFoundException, NoSuchMethodException {
        ClientImpl clientImpl = (ClientImpl) client;
        Endpoint endpoint = clientImpl.getEndpoint();
        ServiceInfo serviceInfo = endpoint.getService().getServiceInfos().get(0);
        QName bindingName = new QName(NAMESPACEURI, SERVICE_BINDING);
        BindingInfo binding = serviceInfo.getBinding(bindingName);
        QName bindingOperationName = new QName(NAMESPACEURI, OPERATION_NAME);
        BindingOperationInfo operation = binding.getOperation(bindingOperationName);
        BindingMessageInfo input = operation.getInput();
        MessagePartInfo messagePart = input.getMessageParts().get(0);
        Class<?> messagePartTypeClass = messagePart.getTypeClass();
        Object calculateComputerPremium = buildCalculateComputerPremium(messagePartTypeClass);
        Object[] invokeResult = client.invoke(operation, calculateComputerPremium);
        System.out.println("result: " + invokeResult[0]);
    }

    private static void oneWay(Client client) throws Exception {
        BindingOperationInfo calculateComputerPremium = client.getEndpoint().getEndpointInfo().getBinding().getOperation(new QName(client.getEndpoint().getService().getName().getNamespaceURI(), OPERATION_NAME));
        Object[] res = client.invoke(calculateComputerPremium, buildCalculateComputerPremium(newInstance("com.exigen.hldsa.rate.ws.service.CalculateComputerPremium")));
        System.out.println("result: " + res[0]);
    }

    private static Object newInstance(String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
        Object calculateComputerPremium = clazz.newInstance();
        return calculateComputerPremium;
    }

    private static Object buildCalculateComputerPremium(Object calculateComputerPremium) throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        ClassHelper accessor = new ClassHelper(calculateComputerPremium);
        accessor.invokeMethod("setArg0", buildProduct());
        List computers = (List) accessor.invokeMethod("getArg1s");;
        computers.add(buildComputer());
        List policies = (List) accessor.invokeMethod("getArg2s");
        policies.add(buildPolicy());
        return calculateComputerPremium;
    }

    private static Object buildComputer() throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        Object computer = newInstance("com.exigen.hldsa.rate.ws.service.Computer");
        ClassHelper computerAccessor = new ClassHelper(computer);
        computerAccessor.invokeMethod("setAgreedValue", false);
        XMLGregorianCalendarImpl rateEffectiveDate = new XMLGregorianCalendarImpl();
        rateEffectiveDate.setYear(2011);
        rateEffectiveDate.setMonth(5);
        rateEffectiveDate.setDay(4);
        rateEffectiveDate.setTime(1, 1, 1);
        computerAccessor.invokeMethod("setRateEffectiveDate", rateEffectiveDate, XMLGregorianCalendar.class);
        computerAccessor.invokeMethod("setSumInsured", 5000, int.class);
        computerAccessor.invokeMethod("setTotalValue", 5000, int.class);
        computerAccessor.invokeMethod("setYear", 2010, int.class);
        return computer;
    }

    private static Object buildPolicy() throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        Object policy = newInstance("com.exigen.hldsa.rate.ws.service.Policy");
        ClassHelper policyAccessor = new ClassHelper(policy);
        policyAccessor.invokeMethod("setCarHire", 0, int.class);
//        policyAccessor.invokeMethod("setCover", "cover");
        policyAccessor.invokeMethod("setDebitOrderRejectNum", 0, int.class);
        policyAccessor.invokeMethod("setExcludeTheft", true, boolean.class);
        policyAccessor.invokeMethod("setInflationBuster", true, boolean.class);
        XMLGregorianCalendarImpl quoteDate = new XMLGregorianCalendarImpl();
        quoteDate.setYear(2011);
        quoteDate.setMonth(5);
        quoteDate.setDay(4);
        quoteDate.setTime(1, 1, 1);
        policyAccessor.invokeMethod("setPolicyEffectiveDate", quoteDate, XMLGregorianCalendar.class);
        policyAccessor.invokeMethod("setProcessCode", "New Business");
//        policyAccessor.invokeMethod("setQuoteDate", new XMLGregorianCalendarImpl(), XMLGregorianCalendar.class);
        policyAccessor.invokeMethod("setQuoteYear", 2011, int.class);
        return policy;
    }

    private static Object buildProduct() throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        Object product = newInstance("com.exigen.hldsa.rate.ws.service.Product");
        ClassHelper productAccessor = new ClassHelper(product);
        productAccessor.invokeMethod("setProductCode", 1001, int.class);
        productAccessor.invokeMethod("setSchemeNo", 203, int.class);
        productAccessor.invokeMethod("setVersionITC", 111, int.class);
        return product;
    }
}
