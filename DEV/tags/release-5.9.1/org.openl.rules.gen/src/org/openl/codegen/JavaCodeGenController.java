/**
 * 
 */
package org.openl.codegen;

import java.util.HashMap;
import java.util.Map;

import org.openl.message.Severity;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.table.constraints.Constraints;
import org.openl.rules.table.properties.def.TablePropertyDefinition.SystemValuePolicy;
import org.openl.rules.table.properties.expressions.match.MatchingExpression;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenEnum;

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
		// FIXME: copied code for JavaOpenClass.class, refactor to support processing of successor classes
		map.put(JavaOpenEnum.class, new Processor() {
            
            public StringBuilder processValue(Object value, ICodeGen gen, StringBuilder sb) {
                return gen.genLiteralJavaOpenClass((JavaOpenClass) value, sb);
            }
        });
		map.put(Constraints.class, new Processor() {
            
            public StringBuilder processValue(Object value, ICodeGen gen, StringBuilder sb) {
                return gen.genLiteralConstraints((Constraints) value, sb);
            }
        });
		
		map.put(SystemValuePolicy.class, new Processor() {            
            public StringBuilder processValue(Object value, ICodeGen gen, StringBuilder sb) {
                return gen.genLiteralSystemValuePolicy((SystemValuePolicy) value, sb);
            }
        });
		
		map.put(InheritanceLevel.class, new Processor() {            
            public StringBuilder processValue(Object value, ICodeGen gen, StringBuilder sb) {
                return gen.genLiteralLevelInheritance((InheritanceLevel) value, sb);
            }
        });
		
		map.put(MatchingExpression.class, new Processor() {            
            public StringBuilder processValue(Object value, ICodeGen gen, StringBuilder sb) {
                return gen.genLiteralMatchingExpression((MatchingExpression) value, sb);
            }
        });
		
		map.put(XlsNodeTypes.class, new Processor() {            
            public StringBuilder processValue(Object value, ICodeGen gen, StringBuilder sb) {
                return gen.genLiteralTableType((XlsNodeTypes) value, sb);
            }
        });
		
		map.put(Severity.class, new Processor() {            
            public StringBuilder processValue(Object value, ICodeGen gen, StringBuilder sb) {
                return gen.genLiteralErrorSeverity((Severity) value, sb);
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

	public interface Processor {
	    StringBuilder processValue(Object value, ICodeGen gen, StringBuilder sb);
	}
}
