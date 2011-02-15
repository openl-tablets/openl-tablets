/**
 * 
 */
package com.exigen.le.proba;

import static junit.framework.Assert.assertEquals;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import com.exigen.le.LE_Value;
import com.exigen.le.LiveExcel;
import com.exigen.le.collections.Collections;
import com.exigen.le.project.VersionDesc;
import com.exigen.le.servicedescr.evaluator.BeanWrapper;
import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.SMHelper;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.Type;
import com.exigen.le.smodel.accessor.ValueHolder;
import com.exigen.le.calculation.CollectionsCalcTest;

/**
 * @author vabramovs
 *
 */
public class CollectionsEvaluator {
	Properties prop;
	String projectName = "Collections";
	List<Object> args;
	public CollectionsEvaluator(Properties prop){
		LiveExcel le = LiveExcel.getInstance();
		le.init(prop);
		ServiceModel sm = le.getServiceModelMakeDefault(projectName, new VersionDesc("d"));
		this.prop =prop;
		Collections context = CollectionsCalcTest.buildContext();

		BeanWrapper bw = new BeanWrapper(context.getDepartament(), sm.getType("Departament"));
		args = new ArrayList<Object>();
		args.add(bw);
		
	}
	public void iterate(){
		LiveExcel le = LiveExcel.getInstance();
		List<Function> servFunc = le.getServiceFunctions(projectName,le.getDefaultVersionDesc(projectName));
        VersionDesc version = new VersionDesc(""); 		
//		for(int i =0;i<1;i++){
		for(int i =0;i<servFunc.size();i++){
//				System.out.println("*******Calculate function(service) "+servFunc.get(i).getName());
			LE_Value[][] answer=le.calculate(projectName,version,servFunc.get(i).getName(),args).getArray();
		}
	
	}
	public void dispose(){
		LiveExcel le = LiveExcel.getInstance();
		// Clean Function pack
		le.clean();
		prop = null;
		args = null;
	}
}
