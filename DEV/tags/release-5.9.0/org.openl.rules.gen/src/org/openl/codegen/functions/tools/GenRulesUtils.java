package org.openl.codegen.functions.tools;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.openl.codegen.tools.CodeGenTools;
import org.openl.codegen.tools.VelocityTool;
import org.openl.rules.helpers.RulesUtils;

/**
 * Functions generator for {@link RulesUtils}
 * 
 * @author DLiauchuk
 *
 */
public class GenRulesUtils extends GenNumberValueFunctions {
    
    private static Class<?>[] ALL_WRAPPER_TYPES;
    private static Class<?>[] ALL_TYPES;
    
    public static void main(String[] arg) throws Exception {
        new GenRulesUtils().run();
    }
    
    public void run() throws Exception {
        ALL_WRAPPER_TYPES = (Class<?>[]) ArrayUtils.addAll(GenNumberValueFunctions.wrapperNumericTypes, GenNumberValueFunctions.BIG_NUMERIC_TYPES);
        ALL_TYPES = (Class<?> []) ArrayUtils.addAll(ALL_WRAPPER_TYPES, GenNumberValueFunctions.primitiveNumericTypes);
        generateRulesFunctions();
    }
    
    private void generateRulesFunctions() throws IOException {
        Map<String, Object> variables = new HashMap<String, Object>();
        
        String sourceFilePath = CodeGenTools.getClassSourcePathInRulesModule(RulesUtils.class);
        variables.put("tool", new VelocityTool());                
        variables.put("allTypes", ALL_TYPES);

        processSourceCode(sourceFilePath, "RulesUtils-functions.vm", variables);        
    }
}
