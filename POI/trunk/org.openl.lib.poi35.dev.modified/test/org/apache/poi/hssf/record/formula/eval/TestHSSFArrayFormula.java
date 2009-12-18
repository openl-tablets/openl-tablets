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

package org.apache.poi.hssf.record.formula.eval;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.eval.BaseFormulaEvaluationTest;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * <i>ArrayFormula.xls</i> formulas calculation testing class. 
 */
public class TestHSSFArrayFormula extends BaseFormulaEvaluationTest {

    /**
     * Creates test case instance.
     */
    public TestHSSFArrayFormula() {
        super();
    }

    /**
     * Creates named test case instance.
     * @param name The test case name.
     */
    public TestHSSFArrayFormula(String name) {
        super(name);
    }

    @Override
    /**
     * Returns <i>ArrayFormula.xls</i> as test cases resource name.
     */
    protected String getResourceName() {
        return "ArrayFormula.xls";
    }

    @Override
    protected Workbook createWorkbook(File file) throws IOException {
        if (file != null) {
            return new HSSFWorkbook( new FileInputStream(file) );
        } else {
            return new HSSFWorkbook();
        }
    }
}
