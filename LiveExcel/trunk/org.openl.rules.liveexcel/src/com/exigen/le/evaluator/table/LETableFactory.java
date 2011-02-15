package com.exigen.le.evaluator.table;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.record.formula.eval.BoolEval;
import org.apache.poi.hssf.record.formula.eval.ErrorEval;
import org.apache.poi.hssf.record.formula.eval.NumberEval;
import org.apache.poi.hssf.record.formula.eval.StringEval;
import org.apache.poi.hssf.record.formula.eval.ValueEval;

import com.exigen.le.evaluator.DataPool;
import com.exigen.le.evaluator.LiveExcelEvaluator;
import com.exigen.le.evaluator.ThreadEvaluationContext;
import com.exigen.le.project.ElementFactory;
import com.exigen.le.project.ProjectElement;
import com.exigen.le.project.VersionDesc;
import com.exigen.le.smodel.ServiceModel;
import com.exigen.le.smodel.TableDesc;
import com.exigen.le.smodel.Type;
import com.exigen.le.smodel.TableDesc.ColumnDesc;
import com.exigen.le.smodel.TableDesc.DataType;

public class LETableFactory implements ElementFactory {
	private static final Log LOG = LogFactory.getLog(LETableFactory.class);
	private static final String DB_DIR = "DB";
	private static final String DB_NAME = "LE_TABLE_DB";
	public static final String IMPORT_DATA_FILE_EXT = ".data.txt"; 
	public static final String DB_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

	public ProjectElement create(String projectName, VersionDesc versionDesc,
			String elementFileName, InputStream is, ServiceModel serviceModel,
			Properties configuration) {

		if (serviceModel.getTables() == null || serviceModel.getTables().size() == 0){
			return null; // no tables - no processing
		}

		String tempDir = configuration
				.getProperty(ElementFactory.TEMP_DIR_PROPERTY);

		File dbDir = prepareDirectory(tempDir, projectName, versionDesc
				.getVersion());
		String connectionURL;
		try {
			initDerby(tempDir + File.separator + DB_DIR);
			File importData = copyImportDataFile(is);
			connectionURL = getConnectionURL(dbDir.getCanonicalPath());
			Connection conn = createDB(connectionURL);
			ZipFile zImport = new ZipFile(importData);
			createTables(conn, serviceModel.getTables(), zImport);
			zImport.close();
			importData.delete();
			
			conn.close();
			is.close();
			shutdownDB(connectionURL);
		} catch (Exception e) {
			String err = "db creation failed";
			LOG.error(err, e);
			throw new RuntimeException(err, e);
		}
		
		ProjectElement pe = new ProjectElement(elementFileName, ProjectElement.ElementType.TABLE){
			@Override
			public void dispose(){
				//close DB
				TableElement te = (TableElement)getElement();
				te.shutdownDB();
			}
		};
		pe.setElement(new TableElement(connectionURL, serviceModel.getTables()));
		return pe;
	}
	
	public static String getConnectionURL(String dbDir){
		return "jdbc:derby:" + dbDir + File.separator + DB_NAME;	
	}
	
	// copy stream to temp file
	private File copyImportDataFile(InputStream is) throws IOException {
		File result = File.createTempFile("imp","zip");
		OutputStream os = new FileOutputStream(result);
		long bytes = IOUtils.copyLarge(is, os);
		is.close();
		os.close();
		LOG.debug("Copied bytes: " + bytes + " to: " + result.getCanonicalPath());
		return result;
	}

	// verifies if directory exists and creates(delete old)
	// subdirectories needed
	private File prepareDirectory(String tmpDir, String projectName,
			String version) {
		// verify if can read and write
		File dir = new File(tmpDir);
		if (!dir.isDirectory()) {
			String err = "It's not a directory " + tmpDir;
			LOG.error(err);
			throw new RuntimeException(err);
		}
		if (!dir.canRead()) {
			String err = "Can't read temporary directory: " + tmpDir;
			LOG.error(err);
			throw new RuntimeException(err);
		}
		if (!dir.canWrite()) {
			String err = "Can't write into temporary directory: " + tmpDir;
			LOG.error(err);
			throw new RuntimeException(err);
		}
		String pathToDb = pathToDb(tmpDir, projectName, version);
		LOG.debug("Path to db: " + pathToDb);

		// delete the old one
		File dbDir = new File(pathToDb);
		FileUtils.deleteQuietly(dbDir); // exceptions are ignored

		// create new one
		boolean created = dbDir.mkdirs();
		if (!created) {
			String err = "Failed to create directories: "
					+ dbDir.getAbsolutePath();
			LOG.error(err);
			throw new RuntimeException(err);
		}

		return dbDir;

	}
	
	public static String pathToDb(String tmpDir, String projectName, String version){
		return 	tmpDir + File.separator + DB_DIR + File.separator
		+ projectName + File.separator + version;

	}

	// create DB and DB table for each LE table

	synchronized private Connection createDB(String connectionURL) throws Exception {
		String createURL = connectionURL + ";create=true";
		/*
		 * * Load the Derby driver.* When the embedded Driver is used this
		 * action start the Derby engine.* Catch an error and suggest a
		 * CLASSPATH problem
		 */
		Class.forName(DB_DRIVER);
		LOG.debug(DB_DRIVER + " loaded. ");
		Connection conn=DriverManager.getConnection(createURL);
		return conn;

	}

	synchronized public static void shutdownDB(String connectionURL)  {
		String shutdownURL = connectionURL + ";shutdown=true";
		try {
			DriverManager.getConnection(shutdownURL);
		} catch (SQLException se) {
			if (se.getErrorCode() != 45000) {
				throw new RuntimeException("failed to shutdown DB: "
						+ shutdownURL,se);
			}
		}
	}

	private void createTables(Connection conn, List<TableDesc> tables, ZipFile importZip)
			throws SQLException, IOException {
		for (TableDesc td : tables) {
			String createTable = createTableSQL(td);
			Statement s = conn.createStatement();
			LOG.debug("creating table: " + createTable);
			s.execute(createTable);
			importData(conn, td.getName(), importZip);
			String createIndex = createIndexSQL(td);
			s.execute(createIndex);
			s.close();

		}
	}
	
	
	
	private void importData(Connection conn, String tableName, ZipFile importZip) throws IOException, SQLException{
		//Extract from zip - to use import utility
		//I could use read/insert byt I think this one should be most effective
		String entryName = tableName+IMPORT_DATA_FILE_EXT;
		ZipEntry ze = importZip.getEntry(entryName);
		if (ze == null){
			String err = "No import data entry for table: " + entryName;
			LOG.error(err);
			throw new RuntimeException(err);
 		}
		InputStream is = importZip.getInputStream(ze);
		File extracted = File.createTempFile("impext", "txt");
		OutputStream os = new FileOutputStream(extracted);
		IOUtils.copyLarge(is, os);
		is.close();
		os.close();
		
		//import file data to table 
		//CALL SYSCS_UTIL.SYSCS_IMPORT_TABLE (null,'T2','import.data',null,null, null,1);
		PreparedStatement ps=conn.prepareStatement("CALL SYSCS_UTIL.SYSCS_IMPORT_TABLE (?,?,?,?,?,?,?)");
		ps.setString(1,null);
		ps.setString(2, tableName);
		ps.setString(3, extracted.getCanonicalPath());
		ps.setString(4, null);
		ps.setString(5, null);
		ps.setString(6, null);
		ps.setString(7, "1");
		ps.execute();
		ps.close();
		
		extracted.delete();
		
	}

	private String createTableSQL(TableDesc td) {
		String result = "CREATE TABLE " + td.getName() + " (";
		for (int i = 0; i < td.getParamDescs().size(); i++) {
			char letter = getLetter(i);
			String sqlType = getSQLType(td.getParamDescs().get(i));
			if (!td.getParamDescs().get(i).isInterval()) {
				result = result + letter + " " + sqlType + " NOT NULL, ";
			} else {
				result = result + leftName(letter) + " " + sqlType
						+ " NOT NULL, ";
				result = result + rightName(letter) + " " + sqlType
						+ " NOT NULL, ";

			}
		}
		result = result + valueColumnName() + " "
				+ getSQLType(td.getValueDesc()) + " NOT NULL)";
		return result;
	}
	
	private String createIndexSQL(TableDesc td){
		String result = "CREATE INDEX "+getIndex(td.getName()) + " ON " + td.getName() +
						"(";
		for (int i = 0; i < td.getParamDescs().size(); i++) {
			if (i!=0){
				result=result+", ";
			}
			char letter = getLetter(i);
			if (!td.getParamDescs().get(i).isInterval()){
				result = result+letter;
			} else {
				result = result + leftName(letter)+" DESC, ";
				result = result + rightName(letter)+" ASC";
				
			}
		}
		result = result + ")";
		return result;
	}
	
	private static String getIndex(String tableName){
		return "I"+tableName;
	}

	public static char getLetter(int i) {
		return (char) ('A' + i);
	}

	public static String valueColumnName() {
		return "V";
	}

	public static String getSQLType(ColumnDesc cd) {
		switch (cd.getType()) {
		case BOOLEAN:
			return "SMALLINT";
		case DATE:
			return "BIGINT";
		case DOUBLE:
			return "FLOAT";
		case STRING:
			return "VARCHAR(" + cd.getMaxLength() + ")";
		default:
			throw new RuntimeException("unexpected type");

		}

	}

	public static String leftName(char letter) {
		return "" + letter + "1";
	}

	public static String rightName(char letter) {
		return "" + letter + "2";
	}

	public static void initDerby(String homedir) {
		// Derby system-wide properties are set here
		// should be called before any call to derby
		String home = System.getProperty("derby.system.home");
		if (home == null) {
			System.setProperty("derby.system.home", homedir); // if not exist -
																// derby creates
		}
		System.setProperty("derby.stream.error.method",
		"com.exigen.le.evaluator.table.LETableFactory.disableDerbyLogFile");
	}

	public static void shutdownDerby() {
//		try {
//			DriverManager.getConnection("jdbc:derby:;shutdown=true");
//		} catch (SQLException se) {
//			if (se.getSQLState()!=null && se.getSQLState().equals("XJ015")) {
//				// OK
//			} else {
//				LOG.warn("failed to shutdown Derby", se);
//			}
//		} 
	}
	
	public static class TableElement  {
		
		String connectionURL;
		Map<String, TableDesc> tables = new HashMap<String, TableDesc>(); // <tableName, table desc>
		Map<String, String> selectSQLs = new HashMap<String, String>();
		
		TableElement(){  // Dummy constructor for table emulators
			
		}
		
		TableElement(String connectionURL,  List<TableDesc> tds ){
			this.connectionURL = connectionURL;
			for (TableDesc td: tds){
				tables.put(td.getName(), td);
				selectSQLs.put(td.getName(), selectSQL(td));
			}
			
		}
		
		//select  v from t2   --DERBY-PROPERTIES index=It2
		//where a1<=71 and a2>=71 and b1 <=3 and b2>=3 order by a1 desc, a2 asc, b1 desc, b2 asc FETCH FIRST ROW ONLY;
		
		private String selectSQL(TableDesc td){
			String tableName = td.getName();
			String result = "SELECT V FROM " + tableName + " --DERBY-PROPERTIES index=" + getIndex(tableName) + "\n"; 
			List<ColumnDesc> cds = td.getParamDescs();
			String where = " WHERE ";
			String orderBy = " ORDER BY ";
			for (int i=0; i< cds.size(); i++){
				if (i!=0){
					where+=" AND ";
					orderBy+=", ";
				}
				ColumnDesc cd = cds.get(i);
				char letter = getLetter(i);
				if (!cd.isInterval()){
					where += letter + "=?";
					orderBy += letter;
				} else {
					where += leftName(letter)+"<" + (cd.isLeftIncluded() ? "=" : "")+"? AND " ;
					where += rightName(letter)+">" + (cd.isRightIncluded() ? "=" : "")+"? ";
					orderBy += leftName(letter)+" DESC, " + rightName(letter)+" ASC";
				}
			}
			result += where + orderBy + " FETCH FIRST ROW ONLY";
			return result;
		}
		
				
		public Object calculate(String tableName, Object[] params){
			TableDesc td = tables.get(tableName);
			List<ColumnDesc> cds = td.getParamDescs();
			if (params.length != cds.size()){
				String err = "Table: parameters count doesn't match. Expected: " + cds.size() + " , received: "+ params.length;
				LOG.error(err);
				throw new RuntimeException(err);
			}
			try {
				
				Connection conn = ThreadEvaluationContext.getConnection();
				if (conn == null){ // first access in thread
					conn = DriverManager.getConnection(connectionURL);
					conn.setReadOnly(true);
					conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED); // I do not need any locks 
					ThreadEvaluationContext.setConnection(conn);
				}
				PreparedStatement ps = conn.prepareStatement(selectSQLs.get(tableName));
				int paramIndex = 1;
				for (int i=0; i<params.length; i++){
					Object param = convertValue(cds.get(i).getType(), params[i]);
					int pCntr = cds.get(i).isInterval() ? 2:1;
					while (pCntr-- > 0){
						setParam(ps, paramIndex, cds.get(i).getType(),param);
						paramIndex++;
					}
				}
				ResultSet rs = ps.executeQuery();
				Object result = null;
				if (rs.next()){
					result = getResult(td.getValueDesc().getType(), rs);
				}else {
					// nothing found - try default value
					if (td.getDefaultValue() != null){
						result = convertValue(td.getValueDesc().getType(), td.getDefaultValue());
					}
				}
				rs.close();
				ps.close();
//				conn.close(); don't need to be closed - kept in context
				return result;
			
			} catch (SQLException se){
				throw new RuntimeException ("failed to calculate table: "+ tableName, se);
			}
			
		}
		
		public ValueEval calculate(String tableName, ValueEval[] params){
			DataPool pool = ThreadEvaluationContext.getDataPool();
			int total = params.length;
			Object[] oparams = new Object[total];
			for (int i=0; i<total; i++){
				if(params[i] instanceof StringEval && pool.isPoolObject(((StringEval)params[i]).getStringValue())){
						oparams[i] = pool.get(((StringEval)params[i]).getStringValue());
				}
				else {
					oparams[i] = LiveExcelEvaluator.createObjectForEval(params[i]);
				}
			}
			Object result = calculate(tableName, oparams);
			if (result == null){
				return ErrorEval.NA;
			}
			if (result instanceof Number){
				return new NumberEval(((Number) result).doubleValue());
			}
			if (result instanceof Boolean) {
	            return BoolEval.valueOf((Boolean) result);
			}
			if (result instanceof String) {
	            return new StringEval((String) result);
			}
			//should never happen
			throw new RuntimeException("Unsupported object type: " + result.getClass().getCanonicalName());
		}
		
		public void shutdownDB(){
			LETableFactory.shutdownDB(connectionURL);
		}
		
		private void setParam(PreparedStatement ps, int paramIndex, DataType type, Object param) throws SQLException{
			switch (type){
			case BOOLEAN:
				// for boolean - convert  to 0/1 - no boolean supported
				if ( ((Boolean)param).booleanValue()){
					ps.setShort(paramIndex, (short)1);
				} else {
					ps.setShort(paramIndex, (short)0);
				}
				return;
			case DATE:
				ps.setLong(paramIndex, (Long)param);
				return;
			case DOUBLE:
				ps.setDouble(paramIndex, (Double)param);
				return;
			case STRING:
				ps.setString(paramIndex, (String)param);
				return;
			default:	
			}
			throw new RuntimeException("unsupported type:" + type);
		}
		
		private Object getResult(DataType type, ResultSet rs) throws SQLException{
			switch (type){
			case BOOLEAN:
				short b = rs.getShort(1);
				if (b==0) {
					return Boolean.FALSE;
				}
				return Boolean.TRUE;
			case DATE:
				return (Long)rs.getLong(1);
			case DOUBLE:
				return (Double)rs.getDouble(1);
			case STRING:
				return rs.getString(1);
			default:	
			}
			throw new RuntimeException("Get result not supported: " + type);
		}

		// convert value to type required
		static Object convertValue(DataType type, Object value){
			switch (type){
				case BOOLEAN:
					return convertToBoolean(value);
				case DOUBLE:
					return convertToDouble(value);
				case STRING:
					// TODO - to take in account other than "integer" format by fractal length
					int fractalLen =0;
					return convertToString(value,fractalLen);
				case DATE:
					return convertToLong(value);
				default:
					
			}
			throw new IllegalArgumentException("Type value is wrong:" + type);
		}
		
		// we assume date as long
		static Long convertToLong(Object value){
			if (value instanceof Long){
				return (Long)value;
			}
			
			if (value instanceof Date){
				return ((Date)value).getTime();
			}
			
			if (value instanceof Double){
				Double d = (Double)value;
				d.longValue();
			}
			if (value instanceof String){ // Date as String 
				try {
					DateFormat df = new SimpleDateFormat(Type.DATE_FORMAT);
					Date date = df.parse((String)value);
					return date.getTime();
				} catch (ParseException e) {
					throw new IllegalArgumentException("String value "+(String)value + " can not be converted to Long: only Date format "+Type.DATE_FORMAT+" is supported");	
				}
			}
			
			throw new IllegalArgumentException("Class " + value.getClass().getCanonicalName() + " can not be converted to Long: only Date and Double are supported");
		}
		
		static Double convertToDouble(Object value){
			if (value instanceof Double){
				return (Double)value;
			}
			if (value instanceof Long){
				Long l = (Long)value;
				return l.doubleValue();
			}
			if (value instanceof Date){
				Long l = ((Date)value).getTime();
				return l.doubleValue();
			}
			
			if (value instanceof String){
				String s = (String)value;
				return Double.valueOf(s);
			}
			
			throw new IllegalArgumentException("Class " + value.getClass().getCanonicalName() + " can not be converted to Double: only Date, String and Long are supported");
			
		}
		
		static Boolean convertToBoolean(Object value){
			if (value instanceof Boolean){
				return (Boolean)value;
			}
			
			if (value instanceof Long){
				Long l = (Long)value;
				if (l == 0){
					return false;
				}
				return true;
			}
			
			if (value instanceof Double){
				Double d = (Double)value;
				if (d == 0){
					return false;
				}
				return true;
			}
			
			if (value instanceof String){
				String s = (String)value;
				s=s.trim();
				if (s.equalsIgnoreCase("true")||s.equals("1")){
					return true;
				} 
				if (s.equalsIgnoreCase("false")||s.equals("0")){
					return false;
				}
				throw new IllegalArgumentException("String: " + s + " cannot be converted to Boolean");
			}
			
			throw new IllegalArgumentException("Class " + value.getClass().getCanonicalName() + " can not be converted to Boolean: only String, Double and Long are supported");
			
		}
		
		static String convertToString(Object value,int fractalLen){
			if (value instanceof String){
				return (String)value;
			}
			
			if (value instanceof Long || value instanceof Double || value instanceof Boolean){
				String str = value.toString();
				if(str.contains(".")){
					// TODO - to take in account other than "integer" format by fractal length
					int index = str.lastIndexOf(".");
					str=str.substring(0,index+fractalLen);
				}
				return str;
			}
			
			throw new IllegalArgumentException("Class " + value.getClass().getCanonicalName() + " can not be converted to String: only Boolean, Double and Long are supported");
		}

	}
	
	public static java.io.OutputStream disableDerbyLogFile() {
		return new java.io.OutputStream() {
			public void write(int b) throws IOException {
				// Ignore all log messages
			}
		};
	}

}
