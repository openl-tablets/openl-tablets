/**
 * 
 */
package com.exigen.le.calculation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.exigen.le.LE_Value;
import com.exigen.le.LiveExcel;
import com.exigen.le.evaluator.selector.FunctionByDateSelector;
import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.SMHelper;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.provider.ServiceModelJAXB;

import static junit.framework.Assert.*;

/**
 * @author vabramovs
 *
 */
public class TablesCalcTestXLS {
	
	@Test
	public void testSimplestTable(){
		int[] arg1Set = new int[]{1,2,3,4,5,6,};
		String[] arg2Set = new String[]{"X1","X2","X3"};
		
		String[][] etalons = {
				new String[]{"V1"},
				new String[]{"V1"},
				new String[]{"42"},
				new String[]{"42"},
				new String[]{"42"},
				new String[]{"42"},
				new String[]{"42"},
				new String[]{"42"},
				new String[]{"7.0"},
				new String[]{"7.0"},
				new String[]{"42"},
				new String[]{"42"},
				new String[]{"42"},
				new String[]{"42"},
				new String[]{"42"},
				new String[]{"42"},
				new String[]{"V3"},
				new String[]{"V3"},
		};

		// Set regular SM provider, which get SM from xml
        ServiceModelJAXB provider = new ServiceModelJAXB(new File("./test-resources/LERepository/Tables/XLS/"));
        LiveExcel le = new LiveExcel(new FunctionByDateSelector(), provider);
		


//		prop.put("Collections.version","a");
		ServiceModel sm =le.getServiceModel();
		le.printoutServiceModel(System.out);
		
//		List<Object> args = new ArrayList<Object>();
//		args.add(new Double(3));
//		args.add("X2");

		String function = "service_calcSimplestTable".toUpperCase();

        Map<String, String> envProps = new HashMap<String, String>();
		envProps.put(Function.EFFECTIVE_DATE, "2010/05/21-08:00");
		System.out.println("*******Calculate function(service) "+function);
		for(int i=0;i<arg2Set.length;i++){
			String arg2 = arg2Set[i];
			for(int ii=0;ii<arg1Set.length;ii++){
				int arg1= arg1Set[ii];
				List<Object> args = new ArrayList<Object>();
				args.add(new Double(arg1));
				args.add(arg2);
				LE_Value[][] answer=le.calculate(function,args,envProps).getArray();
				for(int j=0;j<answer.length;j++){
					for(int jj=0;jj<answer[j].length;jj++){
						System.out.println("Answer Value["+j+"]["+jj+"]="+answer[j][jj].getValue());
						if(answer[j][jj].getType()== LE_Value.Type.VALUE_HOLDER){
							SMHelper.printoutValues(System.out, "", answer[j][jj].getValueHolder());
						}
						else{  // 
							assertEquals(etalons[i*arg1Set.length+ii][j*answer[j].length+jj], answer[j][jj].getValue());
						}
					}
				}
			}
				
		}
	}

}
