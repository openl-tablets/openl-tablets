/**
 * 
 */
package com.exigen.le.proba;

import static junit.framework.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.exigen.le.LE_Value;
import com.exigen.le.LiveExcel;
import com.exigen.le.calculation.CollectionsCalcTest;
import com.exigen.le.collections.Collections;
import com.exigen.le.project.VersionDesc;
import com.exigen.le.servicedescr.evaluator.BeanWrapper;
import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.SMHelper;
import com.exigen.le.smodel.ServiceModel;

/**
 * @author vabramovs
 *
 */
public class TableEvaluator {
	Properties prop;
	String projectName = "Tables";
	String function = "service_calcSimplestTable";
	int[] arg1Set = new int[]{1,2,3,4,5,6,};
	String[] arg2Set = new String[]{"X1","X2","X3"};

	public TableEvaluator(Properties prop){
		LiveExcel le = LiveExcel.getInstance();
		le.init(prop);
		ServiceModel sm = le.getServiceModelMakeDefault(projectName, new VersionDesc("0"));
		this.prop =prop;
		Collections context = CollectionsCalcTest.buildContext();

		
	}
	public void iterate(){
		LiveExcel le = LiveExcel.getInstance();
        VersionDesc version = new VersionDesc("0"); 		
		for(int i=0;i<arg2Set.length;i++){
			String arg2 = arg2Set[i];
			for(int ii=0;ii<arg1Set.length;ii++){
				int arg1= arg1Set[ii];
				List<Object> args = new ArrayList<Object>();
				args.add(new Double(arg1));
				args.add(arg2);
				LE_Value[][] answer=le.calculate(projectName,version,function,args).getArray();
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
		LiveExcel le = LiveExcel.getInstance();
		// Clean Function pack
		le.clean();
		prop = null;
	}

}
