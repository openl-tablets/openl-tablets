package org.openl.rules.lang.xls;

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.record.formula.eval.BoolEval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.RefEvalBase;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.openl.rules.liveexcel.formula.FunctionParam;
import org.openl.rules.liveexcel.formula.ParsedDeclaredFunction;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.IParameterDeclaration;
import org.openl.types.java.JavaOpenClass;

public class LiveExcelMethodHeader implements IOpenMethodHeader{
	
private static class LiveExcelMethodSignature implements IMethodSignature {
		
		private ParsedDeclaredFunction declaredFunction;
		
		public LiveExcelMethodSignature(ParsedDeclaredFunction declaredFunction) {
			this.declaredFunction = declaredFunction;
		}

		public int getNumberOfArguments() {
			return declaredFunction.getParameters().size();
		}

		public int getParameterDirection(int i) {
			return IParameterDeclaration.IN;
		}

		public String getParameterName(int i) {
			return convertName(declaredFunction.getParameters().get(i).getParamName());
		}

		public IOpenClass[] getParameterTypes() {
			IOpenClass[] params = new IOpenClass[declaredFunction.getParameters().size()];
			for (int i = 0; i < params.length; i ++) {
			    params[i] = getParameterClass(declaredFunction.getParameters().get(i));
			}
			//Arrays.fill(params, JavaOpenClass.OBJECT);
			return params;
		}
		
	}
	
	private ParsedDeclaredFunction declaredFunction;
	
	private IMethodSignature methodSignature;
	
	private IOpenClass clazz;
	
	public LiveExcelMethodHeader(ParsedDeclaredFunction declaredFunction, IOpenClass clazz) {
		this.declaredFunction = declaredFunction;
		methodSignature = new LiveExcelMethodSignature(declaredFunction);
		this.clazz = clazz;
	}
	
	public IMethodSignature getSignature() {
		return methodSignature;
	}

	public IOpenClass getDeclaringClass() {
		return clazz;
	}

	public IMemberMetaInfo getInfo() {
		return null;
	}

	public IOpenClass getType() {
	    return getParameterClass(declaredFunction.getReturnCell());
	}

	public boolean isStatic() {
		return false;
	}

	public String getDisplayName(int mode) {
		return convertName(declaredFunction.getDeclFuncName());
	}

	public String getName() {
		return convertName(declaredFunction.getDeclFuncName());
	}
	
	private static IOpenClass getParameterClass(FunctionParam functionParam) {
	    ValueEval innerValueEval = ((RefEvalBase)functionParam.getParamCell()).getInnerValueEval();
	    if (innerValueEval instanceof NumberEval) {
	        return JavaOpenClass.DOUBLE;
	    } else if (innerValueEval instanceof BoolEval) {
	        return JavaOpenClass.BOOLEAN;
	    } else if (innerValueEval instanceof StringEval){
	        return JavaOpenClass.STRING;
	    }
        return JavaOpenClass.OBJECT;
	}
	
	private static String convertName(String name) {
        if (name != null) {
            String[] parts = name.split(" ");
            parts[0] = StringUtils.uncapitalize(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                parts[i] = StringUtils.capitalize(parts[i]);
            }
            return StringUtils.join(parts);
        }
        return null;
    }

}
