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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import org.apache.poi.hssf.record.formula.NamePtg;
import org.apache.poi.hssf.record.formula.NameXPtg;
import org.apache.poi.hssf.record.formula.Ptg;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.PackageRelationshipCollection;
import org.apache.poi.openxml4j.opc.TargetMode;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.EvaluationCell;
import org.apache.poi.ss.formula.EvaluationName;
import org.apache.poi.ss.formula.EvaluationSheet;
import org.apache.poi.ss.formula.EvaluationWorkbook;
import org.apache.poi.ss.formula.FormulaParser;
import org.apache.poi.ss.formula.FormulaParsingWorkbook;
import org.apache.poi.ss.formula.FormulaRenderingWorkbook;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.IExternalWorkbookResolver;
import org.apache.poi.ss.formula.UpdatableEvaluationCell;
import org.apache.poi.ss.formula.WorkbookEvaluator;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.PathUtils;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDefinedName;

/**
 * Internal POI use only
 * 
 * Modified 09/07/09 by Petr Udalau - added methods for searching for UDFs of this Workbook. 
 *
 * @author Josh Micich
 */
public final class XSSFEvaluationWorkbook implements FormulaRenderingWorkbook, EvaluationWorkbook, FormulaParsingWorkbook {

	private final XSSFWorkbook _uBook;

	public static XSSFEvaluationWorkbook create(XSSFWorkbook book) {
		if (book == null) {
			return null;
		}
		return new XSSFEvaluationWorkbook(book);
	}

	private XSSFEvaluationWorkbook(XSSFWorkbook book) {
		_uBook = book;
	}

	private int convertFromExternalSheetIndex(int externSheetIndex) {
		return externSheetIndex;
	}
	/**
	 * @return the sheet index of the sheet with the given external index.
	 */
	public int convertFromExternSheetIndex(int externSheetIndex) {
		return externSheetIndex;
	}
	/**
	 * @return  the external sheet index of the sheet with the given internal
	 * index. Used by some of the more obscure formula and named range things.
	 * Fairly easy on XSSF (we think...) since the internal and external
	 * indicies are the same
	 */
	private int convertToExternalSheetIndex(int sheetIndex) {
		return sheetIndex;
	}

	public int getExternalSheetIndex(String sheetName) {
		int sheetIndex = _uBook.getSheetIndex(sheetName);
		return convertToExternalSheetIndex(sheetIndex);
	}

	public EvaluationName getName(String name, int sheetIndex) {
		for (int i = 0; i < _uBook.getNumberOfNames(); i++) {
			XSSFName nm = _uBook.getNameAt(i);
			String nameText = nm.getNameName();
			if (name.equalsIgnoreCase(nameText) && nm.getSheetIndex() == sheetIndex) {
				return new Name(_uBook.getNameAt(i), i, this);
			}
		}
		return sheetIndex == -1 ? null : getName(name, -1);
	}

	public int getSheetIndex(EvaluationSheet evalSheet) {
		XSSFSheet sheet = ((XSSFEvaluationSheet)evalSheet).getXSSFSheet();
		return _uBook.getSheetIndex(sheet);
	}

	public String getSheetName(int sheetIndex) {
		return _uBook.getSheetName(sheetIndex);
	}
	
	public ExternalName getExternalName(int externSheetIndex, int externNameIndex) {
	   throw new RuntimeException("Not implemented yet");
	}

	public NameXPtg getNameXPtg(String name) {
		// may require to return null to make tests pass
        throw new NotImplementedException("Not implemented yet");
	}

	public EvaluationSheet getSheet(int sheetIndex) {
		return new XSSFEvaluationSheet(_uBook.getSheetAt(sheetIndex));
	}

	public ExternalSheet getExternalSheet(int externSheetIndex) {
        return _uBook.getExternalSheet(externSheetIndex);
    }
    public int getExternalSheetIndex(String workbookName, String sheetName) {
        return _uBook.getExternalSheetIndex(workbookName, sheetName);
	}
    
	public int getSheetIndex(String sheetName) {
		return _uBook.getSheetIndex(sheetName);
	}

	/**
	 * TODO - figure out what the hell this methods does in
	 *  HSSF...
	 */
	public String resolveNameXText(NameXPtg n) {
		throw new RuntimeException("method not implemented yet");
	}

	public String getSheetNameByExternSheet(int externSheetIndex) {
		int sheetIndex = convertFromExternalSheetIndex(externSheetIndex);
		return _uBook.getSheetName(sheetIndex);
	}

	public String getNameText(NamePtg namePtg) {
		return _uBook.getNameAt(namePtg.getIndex()).getNameName();
	}
	public EvaluationName getName(NamePtg namePtg) {
		int ix = namePtg.getIndex();
		return new Name(_uBook.getNameAt(ix), ix, this);
	}
	public Ptg[] getFormulaTokens(EvaluationCell evalCell) {
		XSSFCell cell = ((XSSFEvaluationCell)evalCell).getXSSFCell();
		XSSFEvaluationWorkbook frBook = XSSFEvaluationWorkbook.create(_uBook);
		return FormulaParser.parse(cell.getCellFormula(), frBook, FormulaType.CELL, _uBook.getSheetIndex(cell.getSheet()));
	}

	private static final class Name implements EvaluationName {

		private final XSSFName _nameRecord;
		private final int _index;
		private final FormulaParsingWorkbook _fpBook;

		public Name(XSSFName name, int index, FormulaParsingWorkbook fpBook) {
			_nameRecord = name;
			_index = index;
			_fpBook = fpBook;
		}

		public Ptg[] getNameDefinition() {

			return FormulaParser.parse(_nameRecord.getRefersToFormula(), _fpBook, FormulaType.NAMEDRANGE, _nameRecord.getSheetIndex());
		}

		public String getNameText() {
			return _nameRecord.getNameName();
		}

		public boolean hasFormula() {
			// TODO - no idea if this is right
			CTDefinedName ctn = _nameRecord.getCTName();
			String strVal = ctn.getStringValue();
			return !ctn.getFunction() && strVal != null && strVal.length() > 0;
		}

		public boolean isFunctionName() {
			return _nameRecord.isFunctionName();
		}

		public boolean isRange() {
			return hasFormula(); // TODO - is this right?
		}
		public NamePtg createPtg() {
			return new NamePtg(_index);
		}
	}

	public SpreadsheetVersion getSpreadsheetVersion(){
		return SpreadsheetVersion.EXCEL2007;
	}

    public Workbook getWorkbook() {
        return _uBook;
    }

    public UpdatableEvaluationCell getOrCreateUpdatableCell(String sheetName, int rowIndex, int columnIndex) {
        return getSheet(getSheetIndex(sheetName)).getOrCreateUpdatableCell(rowIndex, columnIndex);
    }
    
    private URI getExternalReferenceURI(String referenceName){
        URI answer = null;
        try {
            OPCPackage pkg =  _uBook.getPackage();
            String contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.externalLink+xml";
            ArrayList<PackagePart> array = pkg.getPartsByContentType(contentType );
            int index = Integer.parseInt(referenceName);
            PackagePart p = array.get(index - 1);
            PackageRelationshipCollection rels = p.getRelationships();
            for (PackageRelationship rel : rels) {
                if(rel.getTargetMode() == TargetMode.EXTERNAL){
                    answer = rel.getTargetURI();
                    break;
                }
            }
        } catch (NumberFormatException e) {
        } catch (InvalidFormatException e) {
        }
        return answer;
    }
    

    public WorkbookEvaluator createExternalWorkbookEvaluator(String workbookName, IExternalWorkbookResolver resolver) {
        Workbook externalWorkbook = createExternalWorkbook(workbookName, resolver);
        EvaluationWorkbook ebook = (new XSSFCreationHelper((XSSFWorkbook)externalWorkbook)).createEvaluationWorkbook();
        return new WorkbookEvaluator(ebook,null,null);
    }
    
    public Workbook createExternalWorkbook(String workbookName, IExternalWorkbookResolver resolver) {
        Workbook result = null;
        URI targetUri = getExternalReferenceURI(workbookName);
        if(targetUri != null){
            try {
                result = resolver.resolveExternalWorkbook(targetUri.getPath());
                if(result == null ){
                    result = new XSSFWorkbook(resolver.resolveExternalExcel(targetUri.getPath()));
                }
                    return result;

            } catch (IOException e) {
            }
            
        }
        else{
            try {
                result = new XSSFWorkbook(resolver.resolveExternalExcel(workbookName));
            } catch (IOException e) {
                throw new RuntimeException("ExternalWorkbook "+workbookName+" not found",e);
            }
        }
        return result;
    }

    public String translateExternalWorkbookRef(String refWorkbookName) {
        try {
            OPCPackage pkg =  _uBook.getPackage();
            String contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.externalLink+xml";
            ArrayList<PackagePart> array = pkg.getPartsByContentType(contentType );
            for(PackagePart part : array){
                for(PackageRelationship rel:part.getRelationships()){
                    String filePath = rel.getTargetURI().getRawPath();
                    if(PathUtils.hewExtension(filePath).endsWith(PathUtils.hewExtension(refWorkbookName))){
                        return ""+(array.indexOf(part)+1);
                    }
                }
            }
        } catch (NumberFormatException e) {
        } catch (InvalidFormatException e) {
        }
        return refWorkbookName;
    }
    

    /* (non-Javadoc)
     * @see org.apache.poi.ss.formula.EvaluationWorkbook#restoreExternalWorkbookName(java.lang.String)
     */
    public String restoreExternalWorkbookName(String refWorkbookName) {
        URI targetURI =  getExternalReferenceURI(refWorkbookName);
        if(targetURI != null){
            return targetURI.getPath();
        }
        return refWorkbookName;
    }
}
