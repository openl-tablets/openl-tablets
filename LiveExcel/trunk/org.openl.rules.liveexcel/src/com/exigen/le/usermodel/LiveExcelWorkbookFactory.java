package com.exigen.le.usermodel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Vector;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.POIXMLDocument;
import org.apache.poi.hssf.record.formula.function.FunctionMetadataRegistry;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Workbook;

import com.exigen.le.LiveExcel;
import com.exigen.le.evaluator.ThreadEvaluationContext;
import com.exigen.le.evaluator.ValueGetter;
import com.exigen.le.evaluator.function.DeclaredFunction;
import com.exigen.le.evaluator.function.UDFFinderLE;
import com.exigen.le.evaluator.table.TableLooker;
import com.exigen.le.project.ElementFactory;
import com.exigen.le.project.ProjectElement;
import com.exigen.le.project.ProjectLoader;
import com.exigen.le.smodel.Property;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.TableDesc;
import com.exigen.le.smodel.Type;
import com.exigen.le.usermodel.hssf.LiveExcelHSSFWorkbook;
import com.exigen.le.usermodel.xssf.LiveExcelXSSFWorkbook;

/**
 * Factory to create LiveExcel workbook according ElementFactory rules, that
 * 
 * @author spetrakovsky
 * @author vabramovs 
 * 
 * @see org.apache.poi.ss.usermodel.WorkbookFactory
 *
 */
public class LiveExcelWorkbookFactory implements ElementFactory{

	private static final Log LOG = LogFactory.getLog(LiveExcelWorkbookFactory.class);

	private static LiveExcelWorkbookFactory INSTANCE = new LiveExcelWorkbookFactory();
	
	private LiveExcelWorkbookFactory(){
	}
    
	public static LiveExcelWorkbookFactory getInstance(){
		return INSTANCE;
	}
    public static LiveExcelWorkbook create(InputStream inp) throws IOException, InvalidFormatException {
        
    	LiveExcelWorkbook result = null;
        if(! inp.markSupported()) {
            inp = new PushbackInputStream(inp, 8);
        }
        
        if(POIFSFileSystem.hasPOIFSHeader(inp)) {
        	result = new LiveExcelHSSFWorkbook(inp);
        	registerServiceModelUDFs(result);
            return result;
        }
        
        if(POIXMLDocument.hasOOXMLHeader(inp)) {
        	
        	File temp = File.createTempFile("poi", ".tmp", ProjectLoader.getTempDir());
        	FileOutputStream fout = new FileOutputStream(temp);
        	IOUtils.copy(inp,fout);
        	inp.close();
        	fout.close();
        	result = new LiveExcelXSSFWorkbook(OPCPackage.open(temp.getAbsolutePath()));
        	registerServiceModelUDFs(result);
            return result;
        }
        
        throw new IllegalArgumentException("Your InputStream was neither an OLE2 stream, nor an OOXML stream");
    }

	public ProjectElement create(String elementFileName, InputStream is, ServiceModel serviceModel) {
		try {
			Workbook wb = create(is);
			ProjectElement result = new ProjectElement(elementFileName,ProjectElement.ElementType.WORKBOOK){
				@Override
				public void dispose(){
					Workbook wb = (Workbook)this.getElement();
					if(wb instanceof LiveExcelXSSFWorkbook){  
						// Only XSFF workbook use temporary file that 
						// can not be deleted without wb revert (closing without saving)
						((LiveExcelXSSFWorkbook)wb).getPackage().revert();
					}
				}
			};
			result.setElement((Object)wb);
			return result;
		} catch (InvalidFormatException e) {
			String msg = "Error during LiveExcelWorbook creation for "+elementFileName;
			LOG.error(msg);
			throw new RuntimeException(msg,e);
		} catch (IOException e) {
			String msg = "Error during LiveExcelWorbook creation for "+elementFileName;
			LOG.error(msg);
			throw new RuntimeException(msg,e);
		}
	}
    public static void registerServiceModelUDFs(LiveExcelWorkbook lewb) {
    	// Functions 
    	UDFFinderLE finder = lewb.getUDFFinder();
    	ServiceModel serviceModel = ThreadEvaluationContext.getServiceModel();
        for (String functionName : serviceModel.getUniqueFunctionName()) {
            finder.addUDF( functionName, new DeclaredFunction(functionName));
            LOG.trace("Register function -"+functionName);
        }
        // ServiceModel Types 
        for (Type type :serviceModel.getTypes()) {
    		finder.addUDF( type.getName(), new ValueGetter(type.getName()));
        	LOG.trace("Register function to get value -"+type.getName());
        	registerGetterDown(type, finder,new Vector<Type>());
        }
        // TODO  review after Table real definition
        for (TableDesc table : serviceModel.getTables()) {
            finder.addUDF( table.getName(), new TableLooker(table));
        	LOG.trace("Register function to lookup table -"+table.getName());
        }
        
        // Register all "global" java UDF
		for(Entry<String,FreeRefFunction> javaUDF: LiveExcel.getJavaUDF().entrySet()){
	        finder.addUDF(javaUDF.getKey(), javaUDF.getValue());
	        FunctionMetadataRegistry.removeFunction(javaUDF.getKey()); // Overwrite standard function definition
		}

        
    }
    protected static void registerGetterDown(Type type, UDFFinderLE finder,Vector<Type> done){
    	if(done.contains(type)){
    		return;
    	}
    	done.add(type);
    	for(Property prop :type.getChilds()){
    		finder.addUDF( prop.getName(), new ValueGetter(prop.getName()));
        	LOG.trace("Register function to get value -"+prop.getName());
        	registerGetterDown(prop.getType(), finder, done);
    	}
    	
    }

}
