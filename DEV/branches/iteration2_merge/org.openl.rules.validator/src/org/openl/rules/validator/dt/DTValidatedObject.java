/**
 * Created Feb 8, 2007
 */
package org.openl.rules.validator.dt;

import java.util.HashMap;
import java.util.Map;

import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.domain.IntRangeDomain;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.IDTCondition;
import org.openl.rules.helpers.IntRange;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.Log;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.IntExp;
import com.exigen.ie.constrainer.IntVar;

/**
 * @author snshor
 * 
 */
public class DTValidatedObject implements IDTValidatedObject,
		IConditionTransformer {

	DecisionTable dt;
	Map<String, IDomainAdaptor> domainMap;

	public DTValidatedObject(DecisionTable dt,
			Map<String, IDomainAdaptor> domainMap) {
		this.dt = dt;
		this.domainMap = domainMap;
	}

	public DTValidatedObject(DecisionTable dt) {
		this.dt = dt;
	}

	public DecisionTable getDT() {
		return dt;
	}

	public IConditionSelector getSelector() {
		return null;
	}

	public IConditionTransformer getTransformer() {
		return this;
	}

	public IOpenClass transformSignatureType(IParameterDeclaration pd) {
		
		Class<?> c = pd.getType().getInstanceClass();
		if (c == String.class) {
			return JavaOpenClass.getOpenClass(IntExp.class);
		}

		if (c == int.class || c == Integer.class) {
			return JavaOpenClass.getOpenClass(IntExp.class);
		}

		return null;
	}

	public IOpenClass transformParameterType(IParameterDeclaration pd) {
		if (pd.getType().getInstanceClass() == String.class) {
			return JavaOpenClass.INT;
		}
		
		if (pd.getType().getInstanceClass() == IntRange.class)
			return JavaOpenClass.getOpenClass(CtrIntRange.class);
		return null;
	}

	public IntVar makeSignatureVar(String parameterName, IOpenClass class1,
			Constrainer c) {
		IDomainAdaptor domain = getDomains().get(parameterName);
		if (domain == null)
			if (class1.getDomain() != null)
				domain = makeDomain(class1.getDomain());

		if (domain != null) {
			return c.addIntVar(domain.getMin(), domain.getMax(), parameterName);
		}

		
		
		Log.warn("Parameter " + parameterName + " has no domain");

		return null;

	}

	private IDomainAdaptor makeDomain(IDomain<?> domain) {
		if (domain instanceof EnumDomain)
			return new EnumDomainAdaptor(((EnumDomain<?>) domain));

		if (domain instanceof IntRangeDomain) {
			IntRangeDomain irange = (IntRangeDomain) domain;
			return new IntRangeDomainAdaptor(irange);

		}

		return null;
	}
	

	public Object transformParameterValue(String name, IDTCondition condition,
			Object value, Constrainer C, DTAnalyzer dtan) 
	{
		
		if (value instanceof IntRange) {
			IntRange intr = (IntRange) value;
			return new CtrIntRange(intr.getMin(), intr.getMax());
			
		}
		
		 IDomain<?> domain = dtan.getParameterDomain(name, condition);
		 if (domain == null)
		 {
			 throw new RuntimeException("Domain is not defined for " + condition.getName() + ":" + name);
//			 return null;
		 } 
		
		IDomainAdaptor domainAsaptor = makeDomain(domain) ;

		if (domain != null) {
			return new Integer(domainAsaptor.getIndex(value));
		}

		return value;
	}

	public Object transformSignatureValueBack(String name, int intValue, DTAnalyzer dtan) 
	{
		IDomain<?> domain = dtan.getSignatureParameterDomain(name);
		
		if (domain == null)
			return intValue;
		
		IDomainAdaptor domainAdaptor = makeDomain(domain);

		if (domain != null) {
			return domainAdaptor.getValue(intValue);
		}

		return intValue;
	}

	public synchronized Map<String, IDomainAdaptor> getDomains() {
		if (domainMap == null)
			domainMap = makeDomains(dt);
		return domainMap;
	}

	private Map<String, IDomainAdaptor> makeDomains(DecisionTable dt2) {
		return new HashMap<String, IDomainAdaptor>();
	}

}
