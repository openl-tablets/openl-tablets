/**
 * 
 */
package com.exigen.le.proba;

import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.exigen.le.project.VersionDesc;
import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.TableDesc;
import com.exigen.le.smodel.Type;
import com.exigen.le.smodel.emulator.SMEmulator;
import com.exigen.le.smodel.emulator.SMEmulator2;
import com.exigen.le.smodel.emulator.SM_Tables_Emulator;
import com.exigen.le.smodel.provider.ServiceModelProvider;

/**
 * @author vabramovs
 *
 */
public class ServiceModelTablesXMLout {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			JAXBContext jc = JAXBContext.newInstance( "com.exigen.le.smodel" );
			
			ServiceModelProvider provider = new  SM_Tables_Emulator();
			ServiceModel sm = provider.create("Tables", new VersionDesc("0")); 
			// marshal to System.out
			Marshaller m = jc.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			m.marshal( sm, System.out );
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
