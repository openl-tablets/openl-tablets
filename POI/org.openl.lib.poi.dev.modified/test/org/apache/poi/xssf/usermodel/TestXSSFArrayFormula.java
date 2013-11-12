/* ====================================================================    
   Licensed to the Apache Software Foundation (ASF) under one or more      
   contributor license agreements.  See the NOTICE file distributed with   
   this work for additional information regarding copyright ownership.     
   The ASF licenses this file to You under the Apache License, Version 2.0 
   (the "License"); you may not use this file except in compliance with    
   the License.  You may obtain a copy of the License at                   
                                                                           
       http://www.apache.org/licenses/LICENSE-2.0                          
                                                                           
   Unless required by applicable law or agreed to in writing, software     
   distributed under the License is distributed on an "AS IS" BASIS,       
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and     
   limitations under the License.                                          
==================================================================== */

package org.apache.poi.xssf.usermodel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.poi.ss.formula.eval.BaseFormulaEvaluationTest;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * <i>ArrayFormula.xlsm</i> formulas calculation testing class. 
 */
public class TestXSSFArrayFormula extends BaseFormulaEvaluationTest {

    /**
     * Creates test case instance.
     */
    public TestXSSFArrayFormula() {
        super();
    }

    /**
     * Creates named test case instance.
     * @param name The test case name.
     */
    public TestXSSFArrayFormula(String name) {
        super(name);
    }

    @Override
    /**
     * Returns <i>ArrayFormula.xlsm</i> as test cases resource name.
     */
    protected String getResourceName() {
        return "ArrayFormula.xlsm";
    }

    @Override
    protected Workbook createWorkbook(File file) throws IOException {
        if (file != null) {
            return new XSSFWorkbook( new FileInputStream(file) );
        } else {
            return new XSSFWorkbook();
        }
    }
}
