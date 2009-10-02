package org.openl.rules.table.properties;

/**
 * 
 * @author snshor
 * Created Jul 21, 2009 
 *
 */
public class DefaultPropertyDefinitions 
{

	public static TablePropertyDefinition[] getDefaultDefinitions()
	{		
	    TablePropertyDefinition[] definitions = null;
	    // <<< INSERT TablePropertiesDefinition >>>
		definitions = new TablePropertyDefinition[23];
		definitions[0] = new TablePropertyDefinition();
		definitions[0].setBusinessSearch(true);
		definitions[0].setConstraints("unique in:module");
		definitions[0].setDescription("The name of the table, must be unique");
		definitions[0].setDimensional(false);
		definitions[0].setDisplayName("Name");
		definitions[0].setGroup("Info");
		definitions[0].setInheritable("no");
		definitions[0].setName("name");
		definitions[0].setPrimaryKey(false);
		definitions[0].setSecurityFilter("no");
		definitions[0].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[1] = new TablePropertyDefinition();
		definitions[1].setBusinessSearch(true);
		definitions[1].setConstraints("no");
		definitions[1].setDescription("The category of the table, could be two-level, in this case use format: <categor"
		 + "y>-<subcategory>");
		definitions[1].setDimensional(false);
		definitions[1].setDisplayName("Category");
		definitions[1].setGroup("Info");
		definitions[1].setInheritable("worksheet");
		definitions[1].setName("category");
		definitions[1].setPrimaryKey(false);
		definitions[1].setSecurityFilter("yes (coma separated filter specification by user role: category/role pairs)");
		definitions[1].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[2] = new TablePropertyDefinition();
		definitions[2].setBusinessSearch(true);
		definitions[2].setConstraints("no");
		definitions[2].setDescription("The description of the table component");
		definitions[2].setDimensional(false);
		definitions[2].setDisplayName("Description");
		definitions[2].setGroup("Info");
		definitions[2].setInheritable("no");
		definitions[2].setName("description");
		definitions[2].setPrimaryKey(false);
		definitions[2].setSecurityFilter("no");
		definitions[2].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[3] = new TablePropertyDefinition();
		definitions[3].setBusinessSearch(true);
		definitions[3].setConstraints("no");
		definitions[3].setDescription("Add any number of comma-separated tags, could be used for search");
		definitions[3].setDimensional(false);
		definitions[3].setDisplayName("Tags");
		definitions[3].setFormat("comma separated");
		definitions[3].setGroup("Info");
		definitions[3].setInheritable("no");
		definitions[3].setName("tags");
		definitions[3].setPrimaryKey(false);
		definitions[3].setSecurityFilter("no");
		definitions[3].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[4] = new TablePropertyDefinition();
		definitions[4].setBusinessSearch(true);
		definitions[4].setConstraints("effectiveDate < expirationDate");
		definitions[4].setDescription("The table becomes active on this date and inactive on the expiration date. There"
		 + " can be multiple copies of the same table in the same module");
		definitions[4].setDimensional(true);
		definitions[4].setDisplayName("Rate Effective Date");
		definitions[4].setExpression("le(currentDate)");
		definitions[4].setFormat("MM/dd/yyyy");
		definitions[4].setGroup("Business Dimension");
		definitions[4].setInheritable("worksheet,workbook");
		definitions[4].setName("effectiveDate");
		definitions[4].setPrimaryKey(true);
		definitions[4].setSecurityFilter("no");
		definitions[4].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.util.Date.class));
		definitions[5] = new TablePropertyDefinition();
		definitions[5].setBusinessSearch(true);
		definitions[5].setConstraints("effectiveDate < expirationDate");
		definitions[5].setDescription("See effectiveDate");
		definitions[5].setDimensional(true);
		definitions[5].setDisplayName("Rate Expiration Date");
		definitions[5].setExpression("gt(currentDate)");
		definitions[5].setFormat("MM/dd/yyyy");
		definitions[5].setGroup("Business Dimension");
		definitions[5].setInheritable("worksheet,workbook");
		definitions[5].setName("expirationDate");
		definitions[5].setPrimaryKey(false);
		definitions[5].setSecurityFilter("no");
		definitions[5].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.util.Date.class));
		definitions[6] = new TablePropertyDefinition();
		definitions[6].setBusinessSearch(true);
		definitions[6].setConstraints("no");
		definitions[6].setDescription("User Name");
		definitions[6].setDimensional(false);
		definitions[6].setDisplayName("Created By");
		definitions[6].setGroup("Info");
		definitions[6].setInheritable("worksheet,workbook, module");
		definitions[6].setName("createdBy");
		definitions[6].setPrimaryKey(false);
		definitions[6].setSecurityFilter("no");
		definitions[6].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[7] = new TablePropertyDefinition();
		definitions[7].setBusinessSearch(true);
		definitions[7].setConstraints("no");
		definitions[7].setDescription("The date of the table creation");
		definitions[7].setDimensional(false);
		definitions[7].setDisplayName("Created On");
		definitions[7].setFormat("MM/dd/yyyy");
		definitions[7].setGroup("Info");
		definitions[7].setInheritable("worksheet,workbook, module");
		definitions[7].setName("createdOn");
		definitions[7].setPrimaryKey(false);
		definitions[7].setSecurityFilter("no");
		definitions[7].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.util.Date.class));
		definitions[8] = new TablePropertyDefinition();
		definitions[8].setBusinessSearch(false);
		definitions[8].setConstraints("no");
		definitions[8].setDescription("User Name");
		definitions[8].setDimensional(false);
		definitions[8].setDisplayName("Modified By");
		definitions[8].setGroup("Info");
		definitions[8].setInheritable("no");
		definitions[8].setName("modifiedBy");
		definitions[8].setPrimaryKey(false);
		definitions[8].setSecurityFilter("no");
		definitions[8].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[9] = new TablePropertyDefinition();
		definitions[9].setBusinessSearch(false);
		definitions[9].setConstraints("no");
		definitions[9].setDescription("The date of the last table modification");
		definitions[9].setDimensional(false);
		definitions[9].setDisplayName("Modified On");
		definitions[9].setFormat("MM/dd/yyyy");
		definitions[9].setGroup("Info");
		definitions[9].setInheritable("no");
		definitions[9].setName("modifyOn");
		definitions[9].setPrimaryKey(false);
		definitions[9].setSecurityFilter("no");
		definitions[9].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.util.Date.class));
		definitions[10] = new TablePropertyDefinition();
		definitions[10].setBusinessSearch(false);
		definitions[10].setConstraints("one of: common, vocabulary[N], main[N]");
		definitions[10].setDescription("Used to manage dependencies between build phases");
		definitions[10].setDimensional(false);
		definitions[10].setDisplayName("Build Phase");
		definitions[10].setGroup("Dev");
		definitions[10].setInheritable("worksheet, workbook");
		definitions[10].setName("buildPhase");
		definitions[10].setPrimaryKey(false);
		definitions[10].setSecurityFilter("no");
		definitions[10].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[11] = new TablePropertyDefinition();
		definitions[11].setBusinessSearch(false);
		definitions[11].setConstraints("one of: on, off, gaps, overlaps");
		definitions[11].setDescription("Defines validation mode for DT");
		definitions[11].setDimensional(false);
		definitions[11].setDisplayName("Validate DT");
		definitions[11].setGroup("Dev");
		definitions[11].setInheritable("worksheet, workbook, module");
		definitions[11].setName("validateDT");
		definitions[11].setPrimaryKey(false);
		definitions[11].setSecurityFilter("no");
		definitions[11].setTableType("xls.dt");
		definitions[11].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[12] = new TablePropertyDefinition();
		definitions[12].setBusinessSearch(true);
		definitions[12].setConstraints("list: Defined by method getLob()");
		definitions[12].setDescription("Defines the list of active LOBs for this table");
		definitions[12].setDimensional(true);
		definitions[12].setDisplayName("LOB");
		definitions[12].setExpression("eq(lob)");
		definitions[12].setGroup("Business Dimension");
		definitions[12].setInheritable("worksheet,workbook, module");
		definitions[12].setName("lob");
		definitions[12].setPrimaryKey(false);
		definitions[12].setSecurityFilter("yes (coma separated filter specification by user role: category/role pairs)");
		definitions[12].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[13] = new TablePropertyDefinition();
		definitions[13].setBusinessSearch(true);
		definitions[13].setConstraints("list: East, West, Midwest, South");
		definitions[13].setDescription("US Region");
		definitions[13].setDimensional(true);
		definitions[13].setDisplayName("US Region");
		definitions[13].setExpression("eq(usRegion)");
		definitions[13].setGroup("Business Dimension");
		definitions[13].setInheritable("worksheet,workbook");
		definitions[13].setName("usregion");
		definitions[13].setPrimaryKey(false);
		definitions[13].setSecurityFilter("yes (coma separated filter specification by user role: category/role pairs)");
		definitions[13].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[14] = new TablePropertyDefinition();
		definitions[14].setBusinessSearch(false);
		definitions[14].setConstraints("list: countries");
		definitions[14].setDefaultValue("US");
		definitions[14].setDescription("Country");
		definitions[14].setDimensional(true);
		definitions[14].setDisplayName("Country");
		definitions[14].setExpression("eq(country)");
		definitions[14].setGroup("Business Dimension");
		definitions[14].setInheritable("worksheet,workbook");
		definitions[14].setName("country");
		definitions[14].setPrimaryKey(false);
		definitions[14].setSecurityFilter("yes (coma separated filter specification by user role: category/role pairs)");
		definitions[14].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[15] = new TablePropertyDefinition();
		definitions[15].setBusinessSearch(false);
		definitions[15].setConstraints("list: currencies");
		definitions[15].setDefaultValue("USD");
		definitions[15].setDescription("Currency");
		definitions[15].setDimensional(false);
		definitions[15].setDisplayName("Currency");
		definitions[15].setGroup("Business Dimension");
		definitions[15].setInheritable("worksheet,workbook");
		definitions[15].setName("currency");
		definitions[15].setPrimaryKey(false);
		definitions[15].setSecurityFilter("no");
		definitions[15].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[16] = new TablePropertyDefinition();
		definitions[16].setBusinessSearch(false);
		definitions[16].setConstraints("list: languages");
		definitions[16].setDefaultValue("en");
		definitions[16].setDescription("Language");
		definitions[16].setDimensional(false);
		definitions[16].setDisplayName("Language");
		definitions[16].setGroup("Business Dimension");
		definitions[16].setInheritable("worksheet,workbook");
		definitions[16].setName("lang");
		definitions[16].setPrimaryKey(false);
		definitions[16].setSecurityFilter("no");
		definitions[16].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[17] = new TablePropertyDefinition();
		definitions[17].setBusinessSearch(true);
		definitions[17].setConstraints("list: US States");
		definitions[17].setDescription("US State");
		definitions[17].setDimensional(true);
		definitions[17].setDisplayName("US State");
		definitions[17].setExpression("eq(usState)");
		definitions[17].setGroup("Business Dimension");
		definitions[17].setInheritable("worksheet,workbook");
		definitions[17].setName("state");
		definitions[17].setPrimaryKey(false);
		definitions[17].setSecurityFilter("yes (coma separated filter specification by user role: category/role pairs)");
		definitions[17].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[18] = new TablePropertyDefinition();
		definitions[18].setBusinessSearch(true);
		definitions[18].setConstraints("list: US, EU, Asia Pacific, North America");
		definitions[18].setDescription("Economic Region");
		definitions[18].setDimensional(false);
		definitions[18].setDisplayName("Region");
		definitions[18].setGroup("Business Dimension");
		definitions[18].setInheritable("worksheet,workbook");
		definitions[18].setName("region");
		definitions[18].setPrimaryKey(false);
		definitions[18].setSecurityFilter("yes (coma separated filter specification by user role: category/role pairs)");
		definitions[18].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[19] = new TablePropertyDefinition();
		definitions[19].setBusinessSearch(false);
		definitions[19].setConstraints("NN.NN[.NN]");
		definitions[19].setDescription("Version is a dimension with a specific constraint - one and only one version mus"
		 + "t be active(per dimension), the dispatch is done automatically to the active ver"
		 + "sion");
		definitions[19].setDimensional(false);
		definitions[19].setDisplayName("Version");
		definitions[19].setGroup("Version");
		definitions[19].setName("version");
		definitions[19].setPrimaryKey(false);
		definitions[19].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[20] = new TablePropertyDefinition();
		definitions[20].setBusinessSearch(false);
		definitions[20].setConstraints("only one active version per dimension");
		definitions[20].setDefaultValue("true");
		definitions[20].setDescription("Indicates an active version");
		definitions[20].setDimensional(false);
		definitions[20].setDisplayName("Active");
		definitions[20].setGroup("Version");
		definitions[20].setName("active");
		definitions[20].setPrimaryKey(false);
		definitions[20].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.Boolean.class));
		definitions[21] = new TablePropertyDefinition();
		definitions[21].setBusinessSearch(false);
		definitions[21].setDefaultValue("true");
		definitions[21].setDescription("Raises an error if no rules were matched. The error will display at least parame"
		 + "ter set, if possible trace(not complete)");
		definitions[21].setDimensional(false);
		definitions[21].setDisplayName("Fail On Miss");
		definitions[21].setGroup("Dev");
		definitions[21].setName("failOnMiss");
		definitions[21].setPrimaryKey(false);
		definitions[21].setTableType("xls.dt");
		definitions[21].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.Boolean.class));
		definitions[22] = new TablePropertyDefinition();
		definitions[22].setBusinessSearch(false);
		definitions[22].setDescription("Value to return if no rules were matched. The type is compatible with table retu"
		 + "rn type");
		definitions[22].setDimensional(false);
		definitions[22].setDisplayName("Return On Miss");
		definitions[22].setGroup("Dev");
		definitions[22].setName("returnOnMiss");
		definitions[22].setPrimaryKey(false);
		definitions[22].setTableType("xls.dt");
		definitions[22].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.Boolean.class));
        // <<< END INSERT TablePropertiesDefinition >>>
	    
		return definitions;
	}
	
	/**
	 * Gets the name of the property by the given display name
	 * @param displayName
	 * @return name
	 */
	public static String getPropertyName(String displayName) {
	    String result = null;
	    for(TablePropertyDefinition tablPropDef : getDefaultDefinitions()){
	        if(tablPropDef.getDisplayName().equals(displayName)){
	            result = tablPropDef.getName();
	        }
	    }
	    return result;
	}
	
	/**
     * Gets the display name of the property by the given name
     * @param name
     * @return diplayName
     */
	public static String getPropertyDisplayName(String name) {
        String result = null;
        for(TablePropertyDefinition tablPropDef : getDefaultDefinitions()){
            if(tablPropDef.getName().equals(name)){
                result = tablPropDef.getDisplayName();
            }
        }
        return result;
    }
	/**
     * Gets the property by its given name
     * @param name
     * @return property definition
     */
	public static TablePropertyDefinition getPropertyByName(String name) {
	    TablePropertyDefinition result = null;
        for(TablePropertyDefinition tablPropDef : getDefaultDefinitions()){
            if(tablPropDef.getName().equals(name)){
                result = tablPropDef;
            }
        }
        return result;
    }
}
