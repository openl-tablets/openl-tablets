/**
 * Created Feb 7, 2007
 */
package org.openl.rules.validator.dt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openl.IOpenSourceCodeModule;
import org.openl.OpenL;
import org.openl.OpenlTool;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.module.ModuleBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.IDTCondition;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.validator.IValidatedObject;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.AOpenClass;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.OpenIterator;
import org.openl.validate.IValidationResult;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.IntBoolExp;
import com.exigen.ie.constrainer.IntBoolExpConst;
import com.exigen.ie.constrainer.IntExp;
import com.exigen.ie.constrainer.IntExpArray;
import com.exigen.ie.constrainer.IntVar;
import com.exigen.ie.constrainer.consistencyChecking.DTCheckerImpl;
import com.exigen.ie.constrainer.consistencyChecking.Overlapping;
import com.exigen.ie.constrainer.consistencyChecking.Uncovered;
import com.exigen.ie.constrainer.consistencyChecking.DTCheckerImpl.CDecisionTableImpl;

/**
 * @author snshor
 * 
 */
public class DTValidator implements IDTValidator
{

    public static DTValidationResult validateDT(String dtname, Map<String, IDomainDescriptor> domains,
	    IOpenClass type)
    {
	DecisionTable dt = (DecisionTable) AOpenClass.getSingleMethod(dtname,
		type.methods());

	return new DTValidator().validateDT(new DTValidatedObject(dt, domains),
		((XlsModuleOpenClass) type).getOpenl());
    }

    public IValidationResult validate(IValidatedObject ivo, OpenL openl)
    {
	return validateDT( ivo, openl);
    }

    public DTValidationResult validateDT(IValidatedObject ivo, OpenL openl)
    {
	return new ValidationAlgorithm((IDTValidatedObject) ivo, openl)
		.validateDT();
    }

    static class ValidationAlgorithm
    {
	IDTValidatedObject dtvo;

	Constrainer C = new Constrainer("Validation");
	IDTCondition[] cc;
	IOpenMethod[] cmethods;
	IntExpArray vars;

	public ValidationAlgorithm(IDTValidatedObject dtvo, OpenL openl)
	{
	    this.dtvo = dtvo;
	    this.openl = openl;
	}

	public DTValidationResult validateDT()
	{
	    DecisionTable dt = dtvo.getDT();

	    cc = dt.getConditionRows();

	    IConditionSelector sel = dtvo.getSelector();

	    if (sel != null)
		cc = OpenIterator.fromArray(cc).select(sel).asList().toArray(
			new IDTCondition[0]);

	    cmethods = new IOpenMethod[cc.length];

	    for (int i = 0; i < cc.length; i++)
	    {
		cmethods[i] = makeCMethod(cc[i], dt);
	    }

	    vars = makeVars();
	    IntBoolExp[][] exp = makeExpressions();

	    CDecisionTableImpl cdt = new CDecisionTableImpl(exp, vars);
	    DTCheckerImpl dtc = new DTCheckerImpl(cdt);

	    List<Uncovered> completeness = dtc.checkCompleteness();
	    List<Overlapping> overlappings = dtc.checkOverlappings();

	    System.out.println("C: " + completeness);
	    System.out.println("O:" + overlappings);

	    return new DTValidationResult(dtvo.getDT(), overlappings
		    .toArray(new Overlapping[0]), completeness
		    .toArray(new Uncovered[0]), dtvo.getTransformer());
	}

	/**
	 * @return
	 */
	private IntBoolExp[][] makeExpressions()
	{
	    int nrules = dtvo.getDT().getNumberOfRules();
	    IntBoolExp[][] ary = new IntBoolExp[nrules][cmethods.length];

	    for (int i = 0; i < nrules; i++)
	    {
		IntBoolExp[] ruleExp = new IntBoolExp[cmethods.length];
		ary[i] = ruleExp;

		for (int j = 0; j < cmethods.length; j++)
		{
		    ruleExp[j] = makeExpression(i, j);
		}

	    }

	    return ary;
	}

	/**
	 * @param i
	 * @param j
	 * @return
	 */
	private IntBoolExp makeExpression(int rule, int cnum)
	{
	    IDTCondition cond = cc[cnum];

	    Object[] values = cond.getParamValues()[rule];

	    if (values == null)
		return new IntBoolExpConst(C, true);

	    int nargs = cmethods[cnum].getSignature().getNumberOfArguments();
	    // /make params from vars and values

	    Object[] args = new Object[nargs];

	    int ndtArgs = dtvo.getDT().getSignature().getNumberOfArguments();

	    for (int i = 0; i < nargs; i++)
	    {
		String name = cmethods[cnum].getSignature().getParameterName(i);
		if (i < ndtArgs)
		{
		    args[i] = findVar(vars, name);
		} else
		{
		    Object[][] params = cond.getParamValues();
		    args[i] = transformValue(name, cc[cnum], params[rule][i
			    - ndtArgs]);
		}

	    }

	    Object instance = getInstance();
	    return (IntBoolExp) cmethods[cnum].invoke(instance, args, openl
		    .getVm().getRuntimeEnv());
	}

	/**
	 * @return
	 */
	private Object getInstance()
	{
	    // TODO Auto-generated method stub
	    return null;
	}

	/**
	 * @param name
	 * @param condition
	 * @param object
	 * @return
	 */
	private Object transformValue(String name, IDTCondition condition,
		Object value)
	{
	    return dtvo.getTransformer().transformParameterValue(name,
		    condition, value, C);
	}

	/**
	 * @param vars2
	 * @param name
	 * @return
	 */
	private Object findVar(IntExpArray vars2, String name)
	{
	    for (int i = 0; i < vars2.size(); i++)
	    {
		if (name.equals(vars2.elementAt(i).name()))
		    return vars2.elementAt(i);
	    }

	    return null;
	}

	/**
	 * @return
	 */
	private IntExpArray makeVars()
	{

	    List<IntExp> v = new ArrayList<IntExp>();

	    IMethodSignature dtsign = dtvo.getDT().getSignature();

	    int nargs = dtsign.getParameterTypes().length;

	    for (int i = 0; i < nargs; i++)
	    {
		IntVar var = dtvo.getTransformer().makeSignatureVar(
			dtsign.getParameterName(i),
			dtsign.getParameterTypes()[i], C);
		if (var != null)
		    v.add(var);
	    }

	    IntExpArray iary = new IntExpArray(C, v);
	    return iary;
	}

	/**
	 * @param condition
	 * @param dt
	 * @return
	 */
	private IOpenMethod makeCMethod(IDTCondition condition, DecisionTable dt)
	{

	    IOpenSourceCodeModule src = condition.getSourceCodeModule();

	    IParameterDeclaration[] pd = condition.getParams();

	    IMethodSignature dtsignature = dt.getSignature();

	    IMethodSignature newSignature = makeNewSignature(pd, dtsignature);

	    IOpenClass methodType = JavaOpenClass
		    .getOpenClass(IntBoolExp.class);

	    IOpenClass declaringClass = dt.getDeclaringClass();

	    OpenMethodHeader methodHeader = new OpenMethodHeader(null,
		    methodType, newSignature, declaringClass);

	    IBindingContext cxt = new ModuleBindingContext(openl.getBinder()
		    .makeBindingContext(), (ModuleOpenClass) declaringClass);
	    return OpenlTool.makeMethod(src, openl, methodHeader, cxt);

	}

	/**
	 * @param pd
	 * @param dtsignature
	 * @return
	 */
	private IMethodSignature makeNewSignature(IParameterDeclaration[] pd,
		IMethodSignature dtsignature)
	{
	    IOpenClass[] ptypes = dtsignature.getParameterTypes();

	    IParameterDeclaration[] pdd = new ParameterDeclaration[ptypes.length
		    + pd.length];

	    for (int i = 0; i < ptypes.length; i++)
	    {
		IOpenClass newType = dtvo.getTransformer()
			.transformSignatureType(ptypes[i],
				dtsignature.getParameterName(i),
				dtsignature.getParameterDirection(i));
		if (newType == null)
		    newType = ptypes[i];
		pdd[i] = new ParameterDeclaration(newType, dtsignature
			.getParameterName(i), dtsignature
			.getParameterDirection(i));
	    }

	    for (int i = 0; i < pd.length; i++)
	    {
		IOpenClass newType = dtvo.getTransformer()
			.transformParameterType(pd[i]);
		pdd[i + ptypes.length] = newType == null ? pd[i]
			: new ParameterDeclaration(newType, pd[i].getName(),
				pd[i].getDirection());
	    }

	    return new MethodSignature(pdd);
	}

	OpenL openl;

    }
}
