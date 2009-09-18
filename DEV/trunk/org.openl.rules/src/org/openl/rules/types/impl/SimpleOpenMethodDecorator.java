package org.openl.rules.types.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.openl.rules.lang.xls.binding.TableProperties;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.runtime.IContext;
import org.openl.types.IOpenMethod;
import org.openl.types.OpenMethodDecorator;

public class SimpleOpenMethodDecorator extends OpenMethodDecorator {
	
	/**
	 * Creates new instance of class.
	 * 
	 * @param delegate
	 *            delegate method
	 */
	public SimpleOpenMethodDecorator(IOpenMethod delegate) {
		
		super();
		decorate(delegate);
	}
	
	/**
	 * Temporal implementation.
	 * 
	 * In this implementation of this method supported the following hard-coded
	 * variables: - context variable with name "date" and type
	 * {@link java.util.Date}, - dimension property with name "effectiveDate"
	 * and type {@link java.util.Date}, - dimension property with name
	 * "expirationDate" and type {@link java.util.Date}
	 */
	@Override
	protected IOpenMethod findMatchingMethod(List<IOpenMethod> candidates, IContext context) {
		
		List<IOpenMethod> filteredMethods = new ArrayList<IOpenMethod>();
		
		Date userDate = (Date) context.getValue("date", Date.class);
		
		if (userDate == null) {
			return null;
		}
		
		for (IOpenMethod candidate : candidates) {
			
			if (candidate.getInfo() == null || candidate.getInfo().getSyntaxNode() == null) {
				continue;
			}
			
			TableSyntaxNode syntaxNode = (TableSyntaxNode) candidate.getInfo().getSyntaxNode();
			TableProperties tableProperties = syntaxNode.getTableProperties();
			
			//String effectiveDateString = tableProperties.getPropertyValueAsString("effectiveDate");
			//String expirationDateString = tableProperties.getPropertyValueAsString("expirationDate");
			
			Date effectiveDate;
			Date expirationDate;
			
			try {
			    effectiveDate = (Date)tableProperties.getPropertyValue("effectiveDate");
			    expirationDate = (Date)tableProperties.getPropertyValue("expirationDate");
			    Calendar calendar = Calendar.getInstance();
			    if(effectiveDate == null) {
			        calendar.set(1900, 0, 1, 0, 0, 0);
                    effectiveDate = calendar.getTime();
			    }
			    if(expirationDate == null) {
			        calendar.set(2999, 11, 31, 23, 59, 59);
                    expirationDate = calendar.getTime();
                }
			} catch (Exception e) {
			    throw new RuntimeException(e);
            }   
			    
			    
				/*SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", new Locale("en"));
				Calendar calendar = Calendar.getInstance();
				
				if (effectiveDateString == null || "".equals(effectiveDateString)) {
					calendar.set(1900, 0, 1, 0, 0, 0);
					effectiveDate = calendar.getTime();
				} else {
					effectiveDate = dateFormat.parse((Date)tableProperties.getPropertyValue("effectiveDate"));
				}
				
				if (expirationDateString == null || "".equals(expirationDateString)) {
					calendar.set(2999, 11, 31, 23, 59, 59);
					expirationDate = calendar.getTime();
				} else {
					expirationDate = dateFormat.parse(tableProperties.getPropertyValueAsString("expirationDate"));
				}
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}*/
			
			if (userDate.after(effectiveDate) && userDate.before(expirationDate)) {
//				System.out.println("\n+++++++++++++++++++++++++++++++");
//				System.out.println("CONTEXT: " + context);
//				System.out.println("USERDATE: " + userDate);
//				System.out.println("CANDIDATE:");
//				System.out.println("DATE: " + 
//						((TableSyntaxNode) candidate.getInfo().getSyntaxNode()).getTableProperties().getPropertyValue("effectiveDate") 
//						+ " - " + 
//						((TableSyntaxNode) candidate.getInfo().getSyntaxNode()).getTableProperties().getPropertyValue("expirationDate"));
//				System.out.println("+++++++++++++++++++++++++++++++\n");
				filteredMethods.add(candidate);
			}
		}
		
		if (filteredMethods.size() == 1) {
			return filteredMethods.get(0);
		}
		
		if (filteredMethods.size() > 1) {
			throw new RuntimeException("Ambiguous method parameters");
		}
		
		return null;
	}
}
