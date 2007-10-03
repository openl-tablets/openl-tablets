package com.exigen.openl.importer;

import java.util.Iterator;

import org.apache.commons.lang.ClassUtils;
import org.openl.CompiledOpenClass;
import org.openl.IOpenSourceCodeModule;
import org.openl.OpenL;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.syntax.IParsedCode;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.FileSourceCodeModule;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;

import com.exigen.openl.component.ErrorListener;
import com.exigen.openl.model.openl.OpenlFactory;
import com.exigen.openl.model.openl.RuleSet;
import com.exigen.openl.model.openl.RuleSetFile;
import com.exigen.openl.model.openl.RuleSetParameter;
import com.exigen.openl.model.openl.RuleSetReturn;

public class OpenLImporter {
	private static final String EXCEL_FILE_LANGUAGE = "org.openl.xls";

	@SuppressWarnings("unchecked")
	public RuleSetFile importExcelFile(String excelFileName,
			ErrorListener errorListener) throws RuntimeException {
		UserContext userContext = new UserContext(Thread.currentThread()
				.getContextClassLoader(), ".");

		IOpenSourceCodeModule src = new FileSourceCodeModule(excelFileName,
				null);

		OpenClassJavaWrapper wrapper = OpenClassJavaWrapper.createWrapper(
				EXCEL_FILE_LANGUAGE, userContext, src);

		CompiledOpenClass compiledClass = wrapper.getCompiledClass();
		IOpenClass openClass = compiledClass.getOpenClassWithErrors();

		if (compiledClass.hasErrors()) {
			for (ISyntaxError syntaxError : compiledClass.getParsingErrors()) {
				errorListener.parsingError(syntaxError.getMessage(),
						syntaxError.getThrowable(), syntaxError.getLocation()
								.toString());
			}
			for (ISyntaxError syntaxError : compiledClass.getBindingErrors()) {
				errorListener.bindingError(syntaxError.getMessage(),
						syntaxError.getThrowable(), syntaxError.getLocation()
								.toString());
			}
		}

		if (openClass != null) {
			RuleSetFile ruleSetFile = OpenlFactory.eINSTANCE
					.createRuleSetFile();
			for (Iterator methodIter = openClass.methods(); methodIter
					.hasNext();) {
				IOpenMethod method = (IOpenMethod) methodIter.next();
				RuleSet ruleSet = OpenlFactory.eINSTANCE.createRuleSet();
				ruleSetFile.getRuleSets().add(ruleSet);

				ruleSet.setName(method.getName());
				ruleSet.setDescription("Generated from Excel file");

				int numberOfArguments = method.getSignature()
						.getNumberOfArguments();
				IOpenClass[] argumentTypes = method.getSignature()
						.getParameterTypes();
				for (int index = 0; index < numberOfArguments; index++) {
					RuleSetParameter ruleSetParameter = OpenlFactory.eINSTANCE
							.createRuleSetParameter();
					ruleSet.getMethodParameters().add(ruleSetParameter);

					ruleSetParameter.setName(method.getSignature()
							.getParameterName(index));
					IOpenClass argumentOpenClass = argumentTypes[index];
					// argumentOpenClass.
					ruleSetParameter.setType(ClassUtils.primitiveToWrapper(
							argumentOpenClass.getInstanceClass()).getName());
					// ruleSetParameter.setMany(argumentTypes[index].is)
					ruleSetParameter.setRequired(true);

				}

				IOpenClass returnType = method.getType();
				Class returnClass = ClassUtils.primitiveToWrapper(returnType
						.getInstanceClass());
				if (returnClass != null) {
					RuleSetReturn ruleSetReturn = OpenlFactory.eINSTANCE
							.createRuleSetReturn();
					ruleSet.setReturn(ruleSetReturn);

					ruleSetReturn.setType(returnClass.getName());
					// ruleSetParameter.setMany(argumentTypes[index].is)
					ruleSetReturn.setRequired(true);
				}
			}

			return ruleSetFile;
		} else {
			return null;
		}
	}

	public String getCellRegionFor(String excelFileName,
			String decisionTableName) {
		IOpenSourceCodeModule src = new FileSourceCodeModule(excelFileName,
				null);
		OpenL openlExcel = OpenL.getInstance(EXCEL_FILE_LANGUAGE);
		IParsedCode module = openlExcel.getParser().parseAsModule(src);
		openlExcel.getBinder().bind(module);
		XlsModuleSyntaxNode topNode = (XlsModuleSyntaxNode) module.getTopNode();
		for (ISyntaxNode syntaxNode : topNode.getNodes()) {
			if (syntaxNode instanceof TableSyntaxNode) {
				TableSyntaxNode tableSyntaxNode = (TableSyntaxNode) syntaxNode;
				IOpenMember member = tableSyntaxNode.getMember();
				if ((member != null)
						&& decisionTableName.equals(member.getName())) {
					return tableSyntaxNode.getXlsSheetSourceCodeModule()
							.getSheetName()
							+ "!"
							+ tableSyntaxNode.getLocation().getStart()
									.toString()
							+ ":"
							+ tableSyntaxNode.getLocation().getEnd().toString();
				}
			}
		}

		return null;
	}

}
