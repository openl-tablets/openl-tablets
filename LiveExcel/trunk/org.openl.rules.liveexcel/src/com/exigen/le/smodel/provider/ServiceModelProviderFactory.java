/**
 * 
 */
package com.exigen.le.smodel.provider;

/**
 * @author vabramovs
 *
 */
public class ServiceModelProviderFactory {
	
	private static final ServiceModelProviderFactory INSTANCE = new ServiceModelProviderFactory();
//	private ServiceModelProvider provider = new SMEmulator();
	private ServiceModelProvider provider = new ServiceModelJAXB();
	private ServiceModelProviderFactory (){
		
	}
		
	public	static ServiceModelProviderFactory getInstance(){
		return INSTANCE;
	}	
	public 	ServiceModelProvider getProvider(){
		return provider;
	}
	public 	void setProvider(ServiceModelProvider provider){
		this.provider=provider;
	}
}
