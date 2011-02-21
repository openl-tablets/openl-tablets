/**
 * 
 */
package com.exigen.le.proba;

import static junit.framework.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.exigen.le.LE_Value;
import com.exigen.le.LiveExcel;
import com.exigen.le.calculation.CollectionsCalcTest;
import com.exigen.le.collections.Collections;
import com.exigen.le.evaluator.selector.FunctionSelector;
import com.exigen.le.servicedescr.evaluator.BeanWrapper;
import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.SMHelper;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.provider.ServiceModelJAXB;

/**
 * @author vabramovs
 *
 */
public class TableEvaluator {
    File projectLocation = new File("./test-resources/LERepository/Tables/0");
	String function = "service_calcSimplestTable";
	int[] arg1Set = new int[]{1,2,3,4,5,6,};
	String[] arg2Set = new String[]{"X1","X2","X3"};
	LiveExcel le;
	
	public TableEvaluator(FunctionSelector functionSelector){
        le = new LiveExcel(functionSelector, new ServiceModelJAXB(projectLocation));
		ServiceModel sm = le.getServiceModel();
		Collections context = CollectionsCalcTest.buildContext();

		
	}
	public void iterate(){
		for(int i=0;i<arg2Set.length;i++){
			String arg2 = arg2Set[i];
			for(int ii=0;ii<arg1Set.length;ii++){
				int arg1= arg1Set[ii];
				List<Object> args = new ArrayList<Object>();
				args.add(new Double(arg1));
				args.add(arg2);
				LE_Value[][] answer=le.calculate(function,args).getArray();
//				for(int j=0;j<answer.length;j++){
//					for(int jj=0;jj<answer[j].length;jj++){
//						System.out.println("Answer Value["+j+"]["+jj+"]="+answer[j][jj].getValue());
//						if(answer[j][jj].getType()== LE_Value.Type.VALUE_HOLDER){
//							SMHelper.printoutValues(System.out, "", answer[j][jj].getValueHolder());
//						}
//						else{  // 
////							assertEquals(etalons[i*arg1Set.length+ii][j*answer[j].length+jj], answer[j][jj].getValue());
//						}
//					}
//				}
			}
				
		}
	
	}
	public void dispose(){
		// Clean Function pack
	}

}
