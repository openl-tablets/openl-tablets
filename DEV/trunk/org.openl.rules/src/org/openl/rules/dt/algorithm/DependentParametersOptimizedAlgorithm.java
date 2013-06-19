package org.openl.rules.dt.algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.dt.algorithm.evaluator.DefaultConditionEvaluator;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.dt.algorithm.evaluator.RangeIndexedEvaluator;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.type.IRangeAdaptor;
import org.openl.rules.dt.type.ITypeAdaptor;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;

public class DependentParametersOptimizedAlgorithm {

	public static IConditionEvaluator makeEvaluator(ICondition condition,
			IMethodSignature signature) throws SyntaxNodeException {

		EvaluatorFactory evaluatorFactory = determineOptimizedEvaluationFactory(
				condition, signature);

		if (evaluatorFactory == null)
			return null;

		IOpenClass expressionType = evaluatorFactory.getExpressionType();

		IParameterDeclaration[] params = condition.getParams();

		switch (params.length) {

		case 1:
			IOpenClass paramType = params[0].getType();

			if (expressionType.equals(paramType)
					|| expressionType.getInstanceClass().equals(
							paramType.getInstanceClass())) {
				return getSingleRangeEvaluator(evaluatorFactory, paramType);
			}

			if (expressionType instanceof JavaOpenClass
					&& ((JavaOpenClass) expressionType)
							.equalsAsPrimitive(paramType)) {
				return getSingleRangeEvaluator(evaluatorFactory, paramType);
			}

			@SuppressWarnings("unchecked")
			IRangeAdaptor<Object, Object> rangeAdaptor = (IRangeAdaptor<Object, Object>) DecisionTableOptimizedAlgorithm
					.getRangeAdaptor(expressionType, paramType);

			if (rangeAdaptor != null) {
				return new RangeIndexedEvaluator(rangeAdaptor);
			}

			if (JavaOpenClass.BOOLEAN.equals(expressionType)
					|| JavaOpenClass.getOpenClass(Boolean.class).equals(
							expressionType)) {
				return new DefaultConditionEvaluator();

			}

			break;

		case 2:

			IOpenClass paramType0 = params[0].getType();
			IOpenClass paramType1 = params[1].getType();

			if (expressionType == paramType0 && expressionType == paramType1) {

				Class<?> clazz = expressionType.getInstanceClass();

				if (clazz != int.class && clazz != long.class
						&& clazz != double.class && clazz != float.class
						&& !Comparable.class.isAssignableFrom(clazz)) {

					String message = String.format(
							"Type '%s' is not Comparable",
							expressionType.getName());

					throw SyntaxNodeExceptionUtils.createError(message, null,
							null, condition.getSourceCodeModule());
				}

				return new RangeIndexedEvaluator(null);
			}

			break;
		}

		List<String> names = new ArrayList<String>();

		for (IParameterDeclaration parameterDeclaration : params) {

			String name = parameterDeclaration.getType().getName();
			names.add(name);
		}

		String parametersString = StringUtils.join(names, ",");

		String message = String.format(
				"Can not make a Condition Evaluator for parameter %s and [%s]",
				expressionType.getName(), parametersString);

		throw SyntaxNodeExceptionUtils.createError(message, null, null,
				condition.getSourceCodeModule());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static IConditionEvaluator getSingleRangeEvaluator(
			EvaluatorFactory evaluatorFactory, IOpenClass paramType) {

		IRangeAdaptor adaptor = getRangeAdaptor(evaluatorFactory, paramType);

		if (adaptor == null)
			return null;

		RangeIndexedEvaluator rix = new RangeIndexedEvaluator(adaptor);
		
		rix.setOptimizedSourceCode(evaluatorFactory.signatureParam.getName());
		
		return rix;
	}

	private static IRangeAdaptor getRangeAdaptor(
			EvaluatorFactory evaluatorFactory, IOpenClass paramType) {
		
		if (paramType == JavaOpenClass.INT)
		{
			return new RelationRangeAdaptor(evaluatorFactory, ITypeAdaptor.INT);
		}	

		return null;
	}
	
	
	static class RelationRangeAdaptor implements IRangeAdaptor {
		EvaluatorFactory evaluatorFactory;
		ITypeAdaptor typeAdaptor;
		
		
		

		public RelationRangeAdaptor(EvaluatorFactory evaluatorFactory,
				ITypeAdaptor typeAdaptor) {
			super();
			this.evaluatorFactory = evaluatorFactory;
			this.typeAdaptor = typeAdaptor;
		}

		@Override
		public Comparable getMax(Object param) {
			
			if (evaluatorFactory.hasMax())
			{	
				Comparable v  = (Comparable)typeAdaptor.convert(param);
				if (evaluatorFactory.needsIncrement(Bound.UPPER))
					v = (Comparable)typeAdaptor.increment(v);
				return v;
			}	
			
			return (Comparable)typeAdaptor.getMaxBound();
		}

		@Override
		public Comparable getMin(Object param) {
			if (evaluatorFactory.hasMin())
			{	
				Comparable v  = (Comparable)typeAdaptor.convert(param);
				if (evaluatorFactory.needsIncrement(Bound.LOWER))
					v = (Comparable)typeAdaptor.increment(v);
				return v;
			}	
			
			return (Comparable)typeAdaptor.getMinBound();
		}

		@Override
		public Comparable adaptValueType(Object value) {
			return (Comparable)typeAdaptor.convert(value);
		}

		@Override
		public boolean useOriginalSource() {
			return true;
		}

	}

	
	

	final static Pattern regex1 = Pattern
			.compile("\\s*(\\w+)\\s*(<=|<|>|>=)\\s*(\\w+)\\s*");
	final static Pattern regex2 = Pattern
			.compile("\\s*(\\w+)\\s*(<=|<)\\s*(\\w+)\\s*&&\\s*(\\w+)\\s*(<=|<)\\s*(\\w+)\\s*");

	private static EvaluatorFactory determineOptimizedEvaluationFactory(
			ICondition condition, IMethodSignature signature) {
		IParameterDeclaration[] params = condition.getParams();

		String code = condition.getSourceCodeModule().getCode();
		if (code == null)
			return null;

		switch (params.length) {
		case 1:
			Matcher m = regex1.matcher(code);
			if (!m.matches())
				return null;

			OneParameterRangeFactory onepRangefactory = makeOneParameterRangeFactory(
					m, condition, signature);
			return onepRangefactory;

		case 2:
			m = regex2.matcher(code);
			if (!m.matches())
				return null;
			return null; // TODO implement this
		default:
			return null;
		}

	}

	private static OneParameterRangeFactory makeOneParameterRangeFactory(
			Matcher m, ICondition condition, IMethodSignature signature) {

		String p1 = m.group(1);
		String op = m.group(2);
		String p2 = m.group(3);

		IParameterDeclaration signatureParam = getParameter(p1, signature);

		if (signatureParam == null)
			return makeOppositeOneParameterRangeFactory(p1, op, p2, condition,
					signature);

		IParameterDeclaration conditionParam = condition.getParams()[0];

		if (!p2.equals(conditionParam.getName()))
			return null;

		RelationType relation = RelationType.findElement(op);

		if (relation == null)
			throw new RuntimeException("Could not find relation: " + op);

		return new OneParameterRangeFactory(signatureParam, conditionParam,
				relation);

	}

	private static IParameterDeclaration getParameter(String pname,
			IMethodSignature signature) {

		for (int i = 0; i < signature.getNumberOfParameters(); i++) {
			if (pname.equals(signature.getParameterName(i))) {
				return new ParameterDeclaration(signature.getParameterType(i),
						pname);
			}
		}
		return null;
	}

	private static OneParameterRangeFactory makeOppositeOneParameterRangeFactory(
			String p1, String op, String p2, ICondition condition,
			IMethodSignature signature) {

		IParameterDeclaration signatureParam = getParameter(p2, signature);

		if (signatureParam == null)
			return null;

		IParameterDeclaration conditionParam = condition.getParams()[0];

		if (!p1.equals(conditionParam.getName()))
			return null;

		RelationType relation = RelationType.findElement(op);

		if (relation == null)
			throw new RuntimeException("Could not find relation: " + op);

		String oppositeOp = relation.opposite;

		relation = RelationType.findElement(oppositeOp);

		if (relation == null)
			throw new RuntimeException("Could not find relation: " + oppositeOp);

		return new OneParameterRangeFactory(signatureParam, conditionParam,
				relation);
	}

	static class RangeEvaluatorFactory {

		public RangeEvaluatorFactory(String regex, int numberOfparams,
				int minDelta, int maxDelta) {
			super();
			this.regex = regex;
			this.numberOfparams = numberOfparams;
			this.minDelta = minDelta;
			this.maxDelta = maxDelta;
		}

		Pattern pattern;
		String regex;
		int numberOfparams;
		int minDelta, maxDelta;
	}

	RangeEvaluatorFactory[] rangeFactories = { new RangeEvaluatorFactory(null,
			0, 0, 0) };


	enum Bound
	{
		LOWER, UPPER
	}
	
	static abstract class EvaluatorFactory {
		
		IParameterDeclaration signatureParam;

		public EvaluatorFactory(IParameterDeclaration signatureParam) {
			super();
			this.signatureParam = signatureParam;
		}

		public abstract boolean hasMin();

		public abstract boolean hasMax();
		
		
		public abstract boolean needsIncrement(Bound bound);

		public IOpenClass getExpressionType() {
			return signatureParam.getType();
		}

	}

	static class OneParameterRangeFactory extends EvaluatorFactory {
		IParameterDeclaration conditionParam;

		public OneParameterRangeFactory(IParameterDeclaration signatureParam,
				IParameterDeclaration conditionParam, RelationType relation) {
			super(signatureParam);

			this.conditionParam = conditionParam;
			this.relation = relation;
		}

		RelationType relation;

		@Override
		public boolean hasMin() {
			return !relation.isLessThan();
		}

		@Override
		public boolean hasMax() {
			return relation.isLessThan();
		}

		@Override
		public boolean needsIncrement(Bound bound) {
			return relation.getIncBound() == bound;
		}
		
		
		
		
	}

	enum RelationType {

		LT("<", ">", true, null), LE("<=", ">=", true, Bound.UPPER), GE(">=", "<=", false, null), GT(">", "<", false, Bound.LOWER);

		private RelationType(String func, String opposite, boolean lessThan,
				Bound incBound) {
			this.func = func;
			this.opposite = opposite;
			this.lessThan = lessThan;
			this.incBound = incBound;
		}
		
		
		
		public Bound getIncBound() {
			return incBound;
		}

		public boolean isLessThan() {
			return lessThan;
		}

		String func;
		String opposite;

		boolean lessThan;
		Bound incBound;

		static RelationType findElement(String code) {
			RelationType[] all = values();
			for (int i = 0; i < all.length; i++) {
				if (code.equals(all[i].func))
					return all[i];
			}

			return null;
		}

	};

}
