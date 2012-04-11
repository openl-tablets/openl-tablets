/**
 * Created Oct 31, 2005
 */
package org.openl.rules.helpers;


/**
 * @author snshor
 *
 */
public class RoundingOperator 
{

	IRoundingOperator operator;
	
	public  RoundingOperator(String op)
	{
		
		operator = findOperator(op);
	}
	
	public double op(double value, double precision)
	{
		return operator.value(value, precision);
	}
	
	static IRoundingOperator findOperator(String op)
	{
		if (op == null)
			throw new RuntimeException("RoundingOperator's name must not be null");
		
		for (int i = 0; i < operators.length; i++) 
		{
			
			
			for (int j = 0; j < operators[i].getNames().length; j++) {
				if (operators[i].getNames()[j].equalsIgnoreCase(op))
					return operators[i];
			}
		}
		
		throw new RuntimeException("Unknown RoundingOperator name: " + op);
		
	}
	
	static interface IRoundingOperator
	{
		public abstract double value(double value, double precision);
		public String[] getNames();
	}
	
	static class OpUp implements IRoundingOperator
	{

		public String[] getNames() {
			return new String[]{"UP"};
		}


		public double value(double value, double precision) {
			return Math.ceil(value/precision)*precision;
		}
	}
	
	static final OpUp OP_UP = new OpUp();
	
	static class OpDown implements IRoundingOperator
	{

		public String[] getNames() {
			return new String[]{"DOWN"};
		}


		public double value(double value, double precision) {
			return Math.floor(value/precision)*precision;
		}
	}

	static final OpDown OP_DOWN = new OpDown();
	
	static class OpRound implements IRoundingOperator
	{

		public String[] getNames() {
			return new String[]{"ROUND"};
		}


		public double value(double value, double precision) {
			return (Math.round(value/precision))*precision;
		}
	}

	static final OpRound OP_ROUND = new OpRound();
	
	static public IRoundingOperator[] operators = {OP_UP, OP_DOWN, OP_ROUND};
	
	public String toString()
	{
		return operator.getNames()[0];
	}
	
	
}
