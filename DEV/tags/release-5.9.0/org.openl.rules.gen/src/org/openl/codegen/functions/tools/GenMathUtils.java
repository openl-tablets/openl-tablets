package org.openl.codegen.functions.tools;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.openl.codegen.tools.CodeGenTools;
import org.openl.codegen.tools.VelocityTool;
import org.openl.util.math.MathUtils;

/**
 * Generator functions for {@link MathUtils}
 * 
 * @author DLiauchuk
 *
 */
public class GenMathUtils extends GenNumberValueFunctions {
    
    public static void main(String[] arg) throws Exception {
        new GenMathUtils().run();
    }
    
    public void run() throws Exception {        
        generateMathFunctions();
    }
    
    private void generateMathFunctions() throws IOException {
        Map<String, Object> variables = new HashMap<String, Object>();
        
        String sourceFilePath = CodeGenTools.getClassSourcePathInCommonsModule(MathUtils.class);
        variables.put("tool", new VelocityTool());
        variables.put("primitiveTypes", GenNumberValueFunctions.primitiveNumericTypes);
        variables.put("wrapperTypes", GenNumberValueFunctions.wrapperNumericTypes);
        processSourceCode(sourceFilePath, "MathUtils-functions.vm", variables);        
    }

}
