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

package org.apache.poi.hssf.usermodel;

import org.apache.poi.hssf.record.formula.eval.BoolEval;
import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.apache.poi.ss.formula.EvaluationCell;
import org.apache.poi.ss.formula.EvaluationSheet;
import org.apache.poi.ss.usermodel.Cell;

/**
 * HSSF wrapper for a sheet under evaluation
 * 
 * @author Josh Micich
 */
final class HSSFEvaluationSheet implements EvaluationSheet {

	private final HSSFSheet _hs;

	public HSSFEvaluationSheet(HSSFSheet hs) {
		_hs = hs;
	}

	public HSSFSheet getHSSFSheet() {
		return _hs;
	}
	public EvaluationCell getCell(int rowIndex, int columnIndex) {
		HSSFRow row = _hs.getRow(rowIndex);
		if (row == null) {
			return null;
		}
		HSSFCell cell = row.getCell(columnIndex);
		if (cell == null) {
			return null;
		}
		return new HSSFEvaluationCell(cell, this);
	}

    public void setCellValue(int rowIndex, int columnIndex, ValueEval value) {
        // FIXME: move this block to some separate method "getOrCreateCell"(in
        // class Sheet or something like that)
        HSSFRow row = _hs.getRow(rowIndex);
        if (row == null) {
            row = _hs.createRow(rowIndex);
        }
        HSSFCell cell = row.getCell(columnIndex);
        if (cell == null) {
            cell = row.createCell(columnIndex);
        }
        // FIXME: move this block to some separate method "setValueFromEval"(in
        // class Cell or something like that)
        if (value instanceof NumberEval) {
            NumberEval ne = (NumberEval) value;
            cell.setCellType(Cell.CELL_TYPE_NUMERIC);
            cell.setCellValue(ne.getNumberValue());
        }
        if (value instanceof BoolEval) {
            BoolEval be = (BoolEval) value;
            cell.setCellType(Cell.CELL_TYPE_BOOLEAN);
            cell.setCellValue(be.getBooleanValue());
        }
        if (value instanceof StringEval) {
            StringEval se = (StringEval) value;
            cell.setCellType(Cell.CELL_TYPE_STRING);
            cell.setCellValue(se.getStringValue());
        }
        if (value instanceof ErrorEval) {
            ErrorEval ee = (ErrorEval) value;
            cell.setCellType(Cell.CELL_TYPE_ERROR);
            cell.setCellErrorValue((byte) ee.getErrorCode());
        }
    }
}