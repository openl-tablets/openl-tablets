/**
 * 
 */
package com.exigen.le.proba;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.SMHelper;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.TableDesc;
import com.exigen.le.smodel.Type;
import com.exigen.le.smodel.emulator.SMEmulator2;
import com.exigen.le.smodel.provider.ServiceModelProvider;

/**
 * @author vabramovs
 *
 */
public class ServiceModelXMLin {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			JAXBContext jc = JAXBContext.newInstance( "com.exigen.le.smodel" );
			
			Unmarshaller u = jc.createUnmarshaller();
			 
			File descFile = new File( "e:/temp/servicemodel.xml");
			
			FileInputStream is = new FileInputStream(descFile);  
			 
			ServiceModel sm = (ServiceModel)u.unmarshal( is ); 
			
			SMHelper.printoutStructure(System.out, "", sm.getTypes());
			
			SMHelper.printoutFunctions(System.out, "", sm.getFunctions());

			SMHelper.printoutTables(System.out, "", sm.getTables());
			
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
