/**
 * Created Apr 4, 2007
 */
package org.openl.rules.dt;

import org.openl.base.INamedThing;
import org.openl.rules.table.IGridTable;
import org.openl.types.IParameterDeclaration;
import org.openl.util.StringTool;
import org.openl.util.print.Formatter;

/**
 * @author snshor
 *
 */
public class DTRule
{
	DecisionTable dt;
	int ruleRow;
	public DTRule(DecisionTable dt, int row)
	{
		this.dt = dt;
		this.ruleRow = row;
	}

	
	public IGridTable getGridTable()
	{
		return dt.getRuleTable(ruleRow).getGridTable();
	}
	
	
	
	static String KEYWORD_CLASS = "rule-keyword";
	static String PRESENTATION_CLASS = "rule-presentation";
	static String PARAMETER_CLASS = "rule-parameter";
	
	
	class Printer 
	{
		StringBuffer buf;

		public Printer(StringBuffer buf)
		{
			this.buf = buf;
		}

		void printConditionOrAction(FunctionalRow ca)
		{
			IParameterDeclaration[] params = ca.getParams();
			
			
			for (int i = 0; i < params.length; i++)
			{
				DTParameterInfo pi = ca.getParameterInfo(i);
				
				if (i > 0)
					buf.append(' ');
				presentation(pi.getPresentation());
				buf.append(' ');
				Object param = pi.getValue(ruleRow);
				if (param == null)
					param = "ANY";
				parameter(param);
			}
			
		}
		
		void print()
		{
			IDTCondition[] cc = dt.getConditionRows();
			if (cc.length > 0)
				keyword("IF ");
			for (int j = 0; j < cc.length; j++)
			{
				IDTCondition dtc = cc[j];
				FunctionalRow ca = (FunctionalRow)dtc;
				if (j > 0)
					keyword(" AND ");

				printConditionOrAction(ca);
				
			}
			
			IDTAction[] aa = dt.getActionRows();
			if (cc.length > 0 && aa.length > 0)
				keyword(" THEN ");
			for (int j = 0; j < aa.length; j++)
			{
				IDTAction dta = aa[j];
				FunctionalRow ca = (FunctionalRow)dta;
				if (j > 0)
					keyword(" ALSO ");

				printConditionOrAction(ca);
				
			}
			
			
		}	
		
		void keyword(String key)
		{
			buf.append(key);
		}

		void presentation(String presentation)
		{
			buf.append(presentation);
		}

		void parameter(Object param)
		{
			Formatter.format(param, INamedThing.SHORT, buf);			
		}
		
		
		
	}
	
	
	class HtmlPrinter extends Printer
	{

		/**
		 * @param buf
		 */
		public HtmlPrinter(StringBuffer buf)
		{
			super(buf);
		}

		void keyword(String key)
		{
			buf.append("<span class=\"").append(KEYWORD_CLASS).append("\">");
			super.keyword(key);
			buf.append("</span>");
		}

		void parameter(Object param)
		{
			buf.append("<span class=\"").append(PARAMETER_CLASS).append("\">");
			StringBuffer buf1 = new StringBuffer();
			String pStr = Formatter.format(param, INamedThing.SHORT, buf1).toString();			
			StringTool.encodeHTMLBody(pStr, buf);
			buf.append("</span>");
		}

		void presentation(String presentation)
		{
			buf.append("<span class=\"").append(PRESENTATION_CLASS).append("\">");
			super.presentation(presentation);
			buf.append("</span>");
		}
		
	}
	
	
	public String getUri()
	{
		return dt.getRuleTable(ruleRow).getGridTable().getUri();
	}
	
	
	
	
	public StringBuffer display(StringBuffer buf, String outMode)
	{
		
		Printer p = "html".equalsIgnoreCase(outMode) ? new HtmlPrinter(buf) : new Printer(buf);
	
		p.print();
		
		
		return buf;
	}




	public int getRuleRow()
	{
		return this.ruleRow;
	}




	/**
	 * @return
	 */
	public DecisionTable getDecisionTable()
	{
		return dt;
	}
	
	
}
