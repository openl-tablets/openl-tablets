package org.openl;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.service.model.*;
import org.openl.util.ClassHelper;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class Main {

    private static AbstractXmlApplicationContext context = new ClassPathXmlApplicationContext("beans-context.xml");

    public static void main(String[] args) throws Exception {
        cxfClient();
    }

	private static void cxfClient() throws Exception {
        Client client = (Client) context.getBean("client");
        secondWay(client);
        oneWay(client);
	}

    private static void secondWay(Client client) throws Exception, InstantiationException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException {
        ClientImpl clientImpl = (ClientImpl) client;
        Endpoint endpoint = clientImpl.getEndpoint();
        ServiceInfo serviceInfo = endpoint.getService().getServiceInfos().get(0);
        QName bindingName = new QName("http://service.ws.rate.hldsa.exigen.com/", "PremiumServiceImplServiceSoapBinding");
        BindingInfo binding = serviceInfo.getBinding(bindingName);
        QName bindingOperationName = new QName("http://service.ws.rate.hldsa.exigen.com/", "calculateComputerPremium");
        BindingOperationInfo operation = binding.getOperation(bindingOperationName);
        BindingMessageInfo input = operation.getInput();
        MessagePartInfo messagePart = input.getMessageParts().get(0);
        Class<?> messagePartTypeClass = messagePart.getTypeClass();
        Object calculateComputerPremium = buildCalculateComputerPremium(newInstance("com.exigen.hldsa.rate.ws.service.CalculateComputerPremium"));
//        Object calculateComputerPremium = buildCalculateComputerPremium(messagePartTypeClass.newInstance());
        Object[] invokeResult = client.invoke(operation, calculateComputerPremium);
        System.out.println("result: " + invokeResult[0]);
    }

    private static void oneWay(Client client) throws Exception {
        BindingOperationInfo calculateComputerPremium = client.getEndpoint().getEndpointInfo().getBinding().getOperation(new QName(client.getEndpoint().getService().getName().getNamespaceURI(), "calculateComputerPremium"));
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

