package test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.openl.rules.examples.hello.Customer;
import org.openl.rules.examples.hello.Response;

import com.exigen.common.component.CommonComponentBuilderFactory;
import com.exigen.common.component.CommonExecutorFactory;
import com.exigen.common.component.ComponentBuilder;
import com.exigen.common.component.ComponentBuilderFactory;
import com.exigen.common.component.ComponentInstantiationException;
import com.exigen.common.component.ComponentReference;
import com.exigen.common.component.Executor;
import com.exigen.common.component.ExecutorFactory;
import com.exigen.common.component.OperationExecutionException;
import com.exigen.common.component.ParameterValue;
import com.exigen.common.component.util.ComponentContextHelper;
import com.exigen.common.model.components.java.JavaComponentsFactory;
import com.exigen.common.model.components.java.JavaMethodParameter;
import com.exigen.openl.component.ExcelUtil;
import com.exigen.openl.component.OpenInstance;
import com.exigen.openl.model.openl.OpenlFactory;
import com.exigen.openl.model.openl.RuleSet;
import com.exigen.openl.model.openl.RuleSetFile;

public class SimpleTestComponent {
	public static void main(String[] args)
			throws ComponentInstantiationException, OperationExecutionException {
		URI uri = URI.createFileURI("test/HelloCustomer.openl");
		Resource res = new XMIResourceImpl(uri);
		RuleSetFile ruleSetFile = OpenlFactory.eINSTANCE.createRuleSetFile();
		res.getContents().add(ruleSetFile);

		String fileName = ExcelUtil.getFileName(ruleSetFile);
		System.out.println(fileName + "");

		RuleSet ruleSet = OpenlFactory.eINSTANCE.createRuleSet();
		ruleSet.setName("helloCustomer");
		ruleSetFile.getRuleSets().add(ruleSet);

		JavaMethodParameter param1 = JavaComponentsFactory.eINSTANCE
				.createJavaMethodParameter();
		param1.setName("customer");
		param1.setType(Customer.class.getName());
		ruleSet.getMethodParameters().add(param1);

		JavaMethodParameter param2 = JavaComponentsFactory.eINSTANCE
				.createJavaMethodParameter();
		param2.setName("response");
		param2.setType(Response.class.getName());
		ruleSet.getMethodParameters().add(param2);

		ComponentBuilderFactory factory = new CommonComponentBuilderFactory();
		ComponentBuilder builder = factory.createComponentBuilder(ruleSetFile);

		Map<String, Map<String, Object>> context = ComponentContextHelper
				.createContext();

		ComponentReference<OpenInstance> componentReference = builder.createComponent(
				ruleSetFile, Collections.EMPTY_LIST, context);

		
		
		Customer customer = new Customer();
		customer.setName("Robinson");
		customer.setGender("Female");
		customer.setMaritalStatus("Married");

		Response response = new Response();

		List<ParameterValue> params = new ArrayList<ParameterValue>();
		params.add(new ParameterValue(param1,customer));
		params.add(new ParameterValue(param2,response));

		ExecutorFactory executorFactory = new CommonExecutorFactory();
		Executor executor = executorFactory.createExecutor(ruleSet);
		executor
				.execute(ruleSet, componentReference.getObject(), params, Collections.EMPTY_LIST, context);

		
		
		System.out.println("Response: " + response.getMap().get("greeting")
				+ ", " + response.getMap().get("salutation")
				+ customer.getName() + "!");
		
		componentReference.release();

	}

}
