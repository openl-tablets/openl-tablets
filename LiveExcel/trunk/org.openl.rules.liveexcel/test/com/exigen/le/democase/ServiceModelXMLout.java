/**
 * 
 */
package com.exigen.le.democase;

import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.exigen.le.smodel.Function;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.TableDesc;
import com.exigen.le.smodel.Type;
import com.exigen.le.smodel.emulator.DemoCaseEmulator;
import com.exigen.le.smodel.emulator.SMEmulator;
import com.exigen.le.smodel.emulator.SMEmulator2;
import com.exigen.le.smodel.provider.ServiceModelProvider;

/**
 * @author vabramovs
 *
 */
public class ServiceModelXMLout {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			JAXBContext jc = JAXBContext.newInstance( "com.exigen.le.smodel" );
			
			ServiceModelProvider provider = new  DemoCaseEmulator();
			List<Type> types = provider.findTypes();
			List<Function> functions =  provider.findFunctions(types); 
			List<TableDesc> tables =  provider.findTables();
			ServiceModel sm = new ServiceModel(types,functions,tables); 
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
