package com.exigen.le.smodel.table;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;
import org.junit.After;
import org.junit.Test;

import com.exigen.le.evaluator.ThreadEvaluationContext;
import com.exigen.le.evaluator.table.LETableFactory;
import com.exigen.le.evaluator.table.LETableFactory.TableElement;
import com.exigen.le.project.ProjectElement;
import com.exigen.le.project.ProjectLoader;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.TableDesc;
import com.exigen.le.smodel.TableDesc.ColumnDesc;
import com.exigen.le.smodel.TableDesc.DataType;
import static junit.framework.Assert.*;


public class DBInitTest {
	
	@Test
	public void initTest() throws IOException, SQLException, ClassNotFoundException {
		LETableFactory letf = new LETableFactory();
		File zip = prepareImportZip();
		FileInputStream fis = new FileInputStream(zip);
		ProjectElement pe = letf.create(null, fis, new ServiceModel(null,null, findTables()));
		
        //connect to DB
		String connectionURL = ((TableElement)pe.getElement()).getConnectionURL();
		
		Class.forName(LETableFactory.DB_DRIVER);

		Connection conn = DriverManager.getConnection(connectionURL);
		Statement s = conn.createStatement();
		ResultSet rs = s.executeQuery("SELECT A1, A2, B, V FROM TT ORDER BY V");
		int i=0;
		while (rs.next()){
			Double a1 = rs.getDouble("A1");
			Double a2 = rs.getDouble("A2");
			String b = rs.getString("B");
			String v = rs.getString("V");
			assertEquals(1.+i*2, a1, 0.01);
			assertEquals(2.+i*2, a2, 0.01);
			assertEquals("X"+(i+1), b);
			assertEquals("V"+(i+1), v);
			i++;
		}
		rs.close();
		s.close();
		conn.close();
		LETableFactory.shutdownDB(connectionURL);
		
		// search test
		TableElement te = (TableElement)pe.getElement();
		Object[] params = new Object[2];
		params[0] = (Double)1.5;
		params[1] = "X1";
		Object result = te.calculate("TT", params);
		assertEquals("V1", (String)result);
		
		//search test with eval
		ValueEval[] vparams = new ValueEval[2];
		vparams[0] = new NumberEval((Double)1.5);
		vparams[1] = new StringEval("X1");
		ValueEval vresult = te.calculate("TT", vparams);
		assertEquals("V1", ((StringEval)vresult).getStringValue());
		pe.dispose();
		ThreadEvaluationContext.freeEvalContext();
	}
	
	
	

		public static List<TableDesc> findTables() {
			/* table a*/
			
			ColumnDesc cd1 = new ColumnDesc(DataType.DOUBLE, true);
			ColumnDesc cd2 = new ColumnDesc(10);
			List<ColumnDesc> cdl = new LinkedList<ColumnDesc>();
			cdl.add(cd1);
			cdl.add(cd2);
			
			TableDesc td = new TableDesc("TT", cdl, new ColumnDesc(20));
			List<TableDesc> tdl = new LinkedList<TableDesc>();
			tdl.add(td);
			return tdl;
			
		}
		
		private File prepareImportZip() throws IOException{
			File zip = File.createTempFile("imp", "zip");
			zip.deleteOnExit();
			
			System.out.println("Zip File to create: " + zip.getCanonicalPath());
			FileOutputStream fos = new FileOutputStream(zip);
			ZipOutputStream zos = new ZipOutputStream(fos);
			
			ZipEntry ze = new ZipEntry("TT" + LETableFactory.IMPORT_DATA_FILE_EXT);
			zos.putNextEntry(ze);
			
			// write data
//			String[] ss = {"1,2,\"X1\",\"V1\"\n","3,4,\"X2\",\"__LE_REF__Table!E7\"\n","5,6,\"X3\",\"V3\"\n"};
			String[] ss = {"1,2,\"X1\",\"V1\"\n","3,4,\"X2\",\"V2\"\n","5,6,\"X3\",\"V3\"\n"};
			for (String s: ss){
				byte[] b = s.getBytes("US-ASCII");
				zos.write(b);
			}
			
			zos.closeEntry();
			zos.close();
			return zip;
			
		}

	    // We should clear all created temp files manually because JUnit terminates
	    // JVM incorrectly and finalization methods are not executed
	    @After
	    public void finalize() {
	        try {
	            ProjectLoader.reset();
	            FileUtils.deleteDirectory(ProjectLoader.getTempDir());
	        } catch (IOException e) {
	            e.printStackTrace();
	            assertFalse(true);
	        }
	    }
}
