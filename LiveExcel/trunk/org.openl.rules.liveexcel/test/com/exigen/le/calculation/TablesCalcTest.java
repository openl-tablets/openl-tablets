/**
 * 
 */
package com.exigen.le.calculation;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import com.exigen.le.LE_Value;
import com.exigen.le.LiveExcel;
import com.exigen.le.collections.Collections;
import com.exigen.le.collections.Departament;
import com.exigen.le.collections.departament.Person;
import com.exigen.le.evaluator.table.LETableFactory;
import com.exigen.le.evaluator.table.TableFactory;
import com.exigen.le.project.ProjectElement;
import com.exigen.le.project.ProjectManager;
import com.exigen.le.project.VersionDesc;
import com.exigen.le.servicedescr.evaluator.BeanWrapper;
import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.SMHelper;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.Type;
import com.exigen.le.smodel.accessor.ValueHolder;
import com.exigen.le.smodel.emulator.JavaUDF;
import com.exigen.le.smodel.emulator.SMEmulator2;
import com.exigen.le.smodel.provider.ServiceModelJAXB;
import com.exigen.le.smodel.provider.ServiceModelProviderFactory;

import static junit.framework.Assert.*;

/**
 * @author vabramovs
 *
 */
public class TablesCalcTest {
	
	@Test
	public void testSimplestTable(){
		String projectName = "Tables";
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


		LiveExcel le = LiveExcel.getInstance();
		Properties prop = new Properties();
		prop.put("repositoryManager.excelExtension",".xlsm");
		prop.put("repositoryManager.headPath","");
		prop.put("repositoryManager.branchPath","");
		prop.put("functionSelector.className","com.exigen.le.evaluator.selector.FunctionByDateSelector");
		// Restore regular SM provider(from xml), which can be overriden by other tests
		ServiceModelProviderFactory.getInstance().setProvider(new ServiceModelJAXB());
		


//		prop.put("Collections.version","a");
		le.setUnInitialized();
		le.init(prop);
		le.clean();
		ServiceModel sm =le.getServiceModelMakeDefault(projectName,new VersionDesc("0") );
		le.printoutServiceModel(System.out, projectName,le.getDefaultVersionDesc(projectName));
		
//		List<Object> args = new ArrayList<Object>();
//		args.add(new Double(3));
//		args.add("X2");

		String function = "service_calcSimplestTable".toUpperCase();

		DateFormat df = new SimpleDateFormat(Type.DATE_FORMAT);
		Date date=new Date();
		try {
			date = df.parse("2010/05/21-08:00");
		} catch (ParseException e) {
			e.printStackTrace();
		}
        VersionDesc version = new VersionDesc("0",date); 		
		System.out.println("*******Calculate function(service) "+function);
		for(int i=0;i<arg2Set.length;i++){
			String arg2 = arg2Set[i];
			for(int ii=0;ii<arg1Set.length;ii++){
				int arg1= arg1Set[ii];
				List<Object> args = new ArrayList<Object>();
				args.add(new Double(arg1));
				args.add(arg2);
				LE_Value[][] answer=le.calculate(projectName,version,function,args).getArray();
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
		le.clean();

	}

}
