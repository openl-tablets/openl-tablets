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

package org.apache.poi.ss.formula;

import org.apache.poi.hssf.record.formula.eval.ValueEval;

/**
 * Abstracts a sheet for the purpose of formula evaluation.<br/>
 * 
 * For POI internal use only
 * 
 * @author Josh Micich
 * 
 * June 4, 2009: Added method setCellValue for setting values in cells.
 */
public interface EvaluationSheet {

	/**
	 * @return <code>null</code> if there is no cell at the specified coordinates
	 */
	EvaluationCell getCell(int rowIndex, int columnIndex);
	
	/**
	 * Sets value in specifed cell.
	 * @param rowIndex Row index.
	 * @param columnIndex Column index
	 * @param value The value to set.
	 */
	void setCellValue(int rowIndex, int columnIndex, ValueEval value);
}
