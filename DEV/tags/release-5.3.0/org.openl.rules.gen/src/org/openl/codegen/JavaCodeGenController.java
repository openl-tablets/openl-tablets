/**
 * 
 */
package org.openl.codegen;

import java.util.HashMap;
import java.util.Map;

import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor Created Jul 23, 2009
 * 
 */
public class JavaCodeGenController implements ICodeGenController {
	
	private Map<Class<?>, Processor> map = new HashMap<Class<?>, Processor>();
	
	public JavaCodeGenController() {
		init();
	}

	private void init() {

		map.put(String.class, new Processor() {
			
			public StringBuilder processValue(Object value, ICodeGen gen, StringBuilder sb) {
				return gen.genLiteralString((String) value, sb);
			}
		});
		
		map.put(Integer.class, new Processor() {
			
			public StringBuilder processValue(Object value, ICodeGen gen, StringBuilder sb) {
				return gen.genLiteralInt((Integer) value, sb);
			}
		});
		
		map.put(Boolean.class, new Processor() {
			
			public StringBuilder processValue(Object value, ICodeGen gen, StringBuilder sb) {
				return gen.genLiteralBool((Boolean) value, sb);
			}
		});
		
		map.put(Character.class, new Processor() {
			
			public StringBuilder processValue(Object value, ICodeGen gen, StringBuilder sb) {
				return gen.genLiteralChar((Character) value, sb);
			}
		});
		
		map.put(Double.class, new Processor() {
			
			public StringBuilder processValue(Object value, ICodeGen gen, StringBuilder sb) {
				return gen.genLiteralDouble((Double) value, sb);
			}
		});
		
		map.put(Long.class, new Processor() {
			
			public StringBuilder processValue(Object value, ICodeGen gen, StringBuilder sb) {
				return gen.genLiteralLong((Long) value, sb);
			}
		});
		
		map.put(JavaOpenClass.class, new Processor() {
			
			public StringBuilder processValue(Object value, ICodeGen gen, StringBuilder sb) {
				return gen.genLiteralJavaOpenClass((JavaOpenClass) value, sb);
			}
		});
    }
	
	public StringBuilder processLiteralValue(Object value, ICodeGen gen, StringBuilder sb) {
		
		if (value == null) {
			return gen.genLiteralNull(sb);
		}
		
		Class<?> c = value.getClass();
		
		if (c.isArray()) {
			return gen.genLiteralArray(value, this, sb);
		}
		
		Processor p = map.get(c);
		
		if (p == null) {
			throw new RuntimeException("Can not process literal class: " + c.getName());
		}
		
		return p.processValue(value, gen, sb);
	}
	
	public static interface Processor {
		public StringBuilder processValue(Object value, ICodeGen gen, StringBuilder sb);
	}
}
