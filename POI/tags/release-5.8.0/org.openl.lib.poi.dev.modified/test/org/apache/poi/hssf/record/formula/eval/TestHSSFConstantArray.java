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

import org.apache.poi.ss.formula.eval.BaseConstantArrayTest;

/**
 * <i>ConstantArray.xls</i> formulas calculation testing class. 
 */
public class TestHSSFConstantArray extends BaseConstantArrayTest {

    /**
     * Creates test case instance.
     */
    public TestHSSFConstantArray() {
        super();
    }

    /**
     * Creates named test case instance.
     * @param name The test case name.
     */
    public TestHSSFConstantArray(String name) {
        super(name);
    }

    @Override
    protected int getTestSheetIndex() {
        return 0;
    }

    @Override
    protected int getTestStartingRow() {
        return 5;
    }

    @Override
    protected String getResourceName() {
        return "ConstantArray.xls";
    }

    @Override
    protected String getFunctionEndString() {
        return "<END-OF-FUNCTIONS>";
    }
}
