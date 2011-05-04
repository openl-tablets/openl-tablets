package org.openl;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.service.model.*;
import org.openl.util.ResultAccessor;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class Main {

    private static AbstractXmlApplicationContext context = new ClassPathXmlApplicationContext("beans-context.xml");

    public static void main(String[] args) throws Exception {
        //cxfClient1();
        cxfClient2();
    }

	private static void cxfClient1() throws Exception {
        Client client = (Client) context.getBean("client");
        //String wsdlUrl = "http://localhost:8080/webservicesdeployer/org.openl.tablets.tutorial4/org.openl.tablets.tutorial4.Tutorial_4?wsdl";
        //String wsdlUrl = "http://localhost:8080/webservicesdeployer/test2/rules?wsdl";

        Object[] res = client.invoke("currentYear");
		System.out.println("Echo response: " + res[0]);
	}

	private static void cxfClient2() throws Exception {
        Client client = (Client) context.getBean("client");
//        oneWay(client);
        secondWay(client);
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
        Object calculateComputerPremium = buildCalculateComputerPremium(messagePartTypeClass.newInstance());
        boolean equals = messagePartTypeClass.equals(input);
        System.out.println(messagePartTypeClass.getCanonicalName() + " ? " + calculateComputerPremium.getClass().getCanonicalName());
        System.out.println("equals = " + equals);
        Object[] invokeResult = client.invoke(operation, calculateComputerPremium);
        System.out.println("result: " + invokeResult[0]);
    }

    private static void oneWay(Client client) throws Exception {
        Object[] res = client.invoke("calculateComputerPremium", buildCalculateComputerPremium(newInstance("com.exigen.hldsa.rate.ws.service.CalculateComputerPremium")));
        Object result = res[0];
//        Object text = new ResultAccessor(res[0]).invokeMethod("getText");
        System.out.println("the text is: " + result);
    }

    private static Object newInstance(String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
        Object calculateComputerPremium = clazz.newInstance();
        return calculateComputerPremium;
    }

    private static Object buildCalculateComputerPremium(Object calculateComputerPremium) throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        ResultAccessor accessor = new ResultAccessor(calculateComputerPremium);
        accessor.invokeMethod("setArg0", buildProduct());
        List computers = (List) accessor.invokeMethod("getArg1s");;
        computers.add(buildComputer());
        List policies = (List) accessor.invokeMethod("getArg2s");
        policies.add(buildPolicy());
        return calculateComputerPremium;
    }

    private static Object buildComputer() throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        Object computer = newInstance("com.exigen.hldsa.rate.ws.service.Computer");
        ResultAccessor computerAccessor = new ResultAccessor(computer);
        computerAccessor.invokeMethod("setTotalValue", new Object[]{1}, new Class[]{int.class});
        computerAccessor.invokeMethod("setYear", new Object[]{1}, new Class[]{int.class});
        computerAccessor.invokeMethod("setAgreedValue", true);
        computerAccessor.invokeMethod("setRateEffectiveDate", new XMLGregorianCalendarImpl(), XMLGregorianCalendar.class);
        computerAccessor.invokeMethod("setSumInsured", new Object[]{2011}, new Class[]{int.class});
        return computer;
    }

    private static Object buildPolicy() throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        Object policy = newInstance("com.exigen.hldsa.rate.ws.service.Policy");
        ResultAccessor policyAccessor = new ResultAccessor(policy);
        policyAccessor.invokeMethod("setCarHire", new Object[]{1}, new Class[]{int.class});
        policyAccessor.invokeMethod("setCover", "cover");
        policyAccessor.invokeMethod("setDebitOrderRejectNum", new Object[]{1}, new Class[]{int.class});
        policyAccessor.invokeMethod("setExcludeTheft", true, boolean.class);
        policyAccessor.invokeMethod("setInflationBuster", false, boolean.class);
        policyAccessor.invokeMethod("setPolicyEffectiveDate", new XMLGregorianCalendarImpl(), XMLGregorianCalendar.class);
        policyAccessor.invokeMethod("setProcessCode", "ABC");
        policyAccessor.invokeMethod("setQuoteDate", new XMLGregorianCalendarImpl(), XMLGregorianCalendar.class);
        policyAccessor.invokeMethod("setQuoteYear", new Object[]{2011}, new Class[]{int.class});
        return policy;
    }

    private static Object buildProduct() throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        Object product = newInstance("com.exigen.hldsa.rate.ws.service.Product");
        ResultAccessor productAccessor = new ResultAccessor(product);
        productAccessor.invokeMethod("setProductCode", new Object[]{1}, new Class[]{int.class});
        productAccessor.invokeMethod("setSchemeNo", new Object[]{1}, new Class[]{int.class});
        productAccessor.invokeMethod("setVersionITC", new Object[]{1}, new Class[]{int.class});
        return product;
    }

}

