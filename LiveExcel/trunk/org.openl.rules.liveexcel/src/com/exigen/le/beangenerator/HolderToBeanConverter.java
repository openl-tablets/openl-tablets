/**
 * 
 */
package com.exigen.le.beangenerator;

import java.lang.reflect.Array;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtils;

import com.exigen.le.smodel.MappedProperty;
import com.exigen.le.smodel.Type;
import com.exigen.le.smodel.accessor.CollectionValueHolder;
import com.exigen.le.smodel.accessor.ValueHolder;

/**
 * @author zsulkins
 * Utility Class
 * Converts ValueHolder to corresponding bean
 * 
 */
public class HolderToBeanConverter  {
	
	/**
	 * Convert ValueHolder to Bean
	 * @param type
	 * @param vh
	 * @param cl
	 * @return
	 * @throws Exception
	 */
	public static Object convert(Type type, ValueHolder vh, ClassLoader cl) throws Exception {
		// get class of the object
		String className = BeanGenerator.getQualifiedType(type);
		Class root = cl.loadClass(className);
		Object result = root.newInstance();
		for(MappedProperty p: type.getChilds()){
			String name = p.getName();
			String beanName = name;
			// Workaround - according https://issues.apache.org/jira/browse/BEANUTILS-369
			// The Java Bean Specification states in section "8.8 Capitalization of inferred names" that when the first character is converted to lowercase unless the first 
			// two characters are both uppercase then the property name is "unchanged".
			if(!Character.isUpperCase(beanName.charAt(1))){ // second letter is not Upper case, so need transformation 
				beanName = beanName.substring(0,1).toLowerCase()+beanName.substring(1);
			}
			
			if (!p.getType().isComplex()){ // simple property
				// primitive
				Class<?> clazz = Type.Primary.getPrimary(p.getType()).getClass();
				if (p.isCollection()){
					clazz = (Array.newInstance(clazz, 1)).getClass();
				}
				Object v = ConvertUtils.convert(vh.getValue(name), clazz);
			    BeanUtils.setProperty(result, beanName, v);
			} else { // complex
				if (!p.isCollection()){
					Object v = convert(p.getType(),(ValueHolder)vh.getValue(name), cl);
				    BeanUtils.setProperty(result,beanName, v);
				} else {
					// collection of complex type
					CollectionValueHolder cvh = (CollectionValueHolder)(vh.getValue(name));
					int size = cvh.size();
					String pClassName = BeanGenerator.getQualifiedType(p.getType());
					Class pClass = cl.loadClass(pClassName); // property element class
					Object arr = Array.newInstance(pClass, size);
					for (int i=0; i<size; i++){
						Array.set(arr, i, convert(p.getType(),(ValueHolder)(cvh.getValue(i)), cl));
					}
					BeanUtils.setProperty(result, beanName, arr);
				}
			}
		}
		
		
		return result;
	}
}
