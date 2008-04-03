/**
 * Created Feb 11, 2007
 */
package org.openl.rules.validator.dt;

import org.openl.rules.dt.DTOverlapping;
import org.openl.rules.dt.DTUncovered;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.IDTValidationResult;
import org.openl.util.ArrayOfNamedValues;

import com.exigen.ie.constrainer.consistencyChecking.Overlapping;
import com.exigen.ie.constrainer.consistencyChecking.Uncovered;

/**
 * @author snshor
 *
 */
public class DTValidationResult implements IDTValidationResult
{
	
	DecisionTable dt;
	DTOverlapping[] overlappings;
	DTUncovered[] uncovered;

	public DTValidationResult(DecisionTable dt,DTOverlapping[] overlappings, DTUncovered[] uncovered)
	{
		this.dt = dt;
		this.overlappings = overlappings;
		
		this.uncovered = uncovered;
	}

	/**
	 * @param dt2
	 * @param overlappings2
	 * @param uncovereds
	 * @param transformer 
	 */
	public DTValidationResult(DecisionTable dt, Overlapping[] ov, Uncovered[] un, IConditionTransformer transformer)
	{
		this.dt = dt;
		this.overlappings = convertOverlappings(ov, transformer);
		this.uncovered = convertUncovered(un, transformer);
	}

	/**
	 * @param un
	 * @param transformer
	 * @return
	 */
	private DTUncovered[] convertUncovered(Uncovered[] un, IConditionTransformer transformer)
	{
		DTUncovered[] un2 = new DTUncovered[un.length];
		for (int i = 0; i < un.length; i++)
		{
			String[] names = un[i].getSolutionNames();
			Object[] values = new Object[names.length];
			for (int j = 0; j < values.length; j++)
			{
				values[j] = transformer.transformSignatureValueBack(names[j], un[i].getSolutionValues()[j]);
			}
			
			un2[i] = new DTUncovered(new ArrayOfNamedValues(names, values));
		}
		return un2;
	}

	/**
	 * @param ov
	 * @param transformer 
	 * @return
	 */
	private DTOverlapping[] convertOverlappings(Overlapping[] ov, IConditionTransformer transformer)
	{
		DTOverlapping[] ov2 = new DTOverlapping[ov.length];
		for (int i = 0; i < ov.length; i++)
		{
			String[] names = ov[i].getSolutionNames();
			Object[] values = new Object[names.length];
			for (int j = 0; j < values.length; j++)
			{
				values[j] = transformer.transformSignatureValueBack(names[j], ov[i].getSolutionValues()[j]);
			}
			
			ov2[i] = new DTOverlapping(ov[i].getOverlapped(),new ArrayOfNamedValues(names, values));
		}
		return ov2;
	}

	public DecisionTable getDT()
	{
		return dt;
	}

	public DTOverlapping[] getOverlappings()
	{
		return overlappings;
	}

	public DTUncovered[] getUncovered()
	{
		return uncovered;
	}

}
