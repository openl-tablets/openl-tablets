package org.openl.rules.table.properties;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinition.InheritanceLevel;
import org.openl.rules.table.properties.def.TablePropertyDefinition.SystemValuePolicy;

/**
 * Definitions of supported properties.
 * @author snshor
 * Created Jul 21, 2009 
 *
 */
public class DefaultPropertyDefinitions 
{
    private static final TablePropertyDefinition[] definitions;  
    
    static {  
        // <<< INSERT TablePropertiesDefinition >>>
		definitions = new TablePropertyDefinition[24];
		definitions[0] = new TablePropertyDefinition();
		definitions[0].setBusinessSearch(false);
		definitions[0].setConstraints(new org.openl.rules.table.constraints.Constraints("data:countries"));
		definitions[0].setDescription("Country");
		definitions[0].setDimensional(true);
		definitions[0].setDisplayName("Country");
		definitions[0].setExpression("contains(country)");
		definitions[0].setGroup("Business Dimension");
		definitions[0].setInheritanceLevel(new InheritanceLevel[] {InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE});
		definitions[0].setName("country");
		definitions[0].setPrimaryKey(false);
		definitions[0].setSecurityFilter("yes (coma separated filter specification by user role: category/role pairs)");
		definitions[0].setSystem(false);
		definitions[0].setType(org.openl.types.java.JavaOpenClass.getOpenClass(org.openl.rules.enumeration.CountriesEnum[].class));
		definitions[1] = new TablePropertyDefinition();
		definitions[1].setBusinessSearch(true);
		definitions[1].setConstraints(new org.openl.rules.table.constraints.Constraints("unique in:module"));
		definitions[1].setDescription("The name of the table, must be unique");
		definitions[1].setDimensional(false);
		definitions[1].setDisplayName("Name");
		definitions[1].setGroup("Info");
		definitions[1].setInheritanceLevel(new InheritanceLevel[] {InheritanceLevel.TABLE});
		definitions[1].setName("name");
		definitions[1].setPrimaryKey(false);
		definitions[1].setSecurityFilter("no");
		definitions[1].setSystem(false);
		definitions[1].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[2] = new TablePropertyDefinition();
		definitions[2].setBusinessSearch(true);
		definitions[2].setConstraints(new org.openl.rules.table.constraints.Constraints("no"));
		definitions[2].setDescription("The category of the table, could be two-level, in this case use format: <categor"
		 + "y>-<subcategory>");
		definitions[2].setDimensional(false);
		definitions[2].setDisplayName("Category");
		definitions[2].setGroup("Info");
		definitions[2].setInheritanceLevel(new InheritanceLevel[] {InheritanceLevel.CATEGORY, InheritanceLevel.TABLE});
		definitions[2].setName("category");
		definitions[2].setPrimaryKey(false);
		definitions[2].setSecurityFilter("yes (coma separated filter specification by user role: category/role pairs)");
		definitions[2].setSystem(false);
		definitions[2].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[3] = new TablePropertyDefinition();
		definitions[3].setBusinessSearch(true);
		definitions[3].setConstraints(new org.openl.rules.table.constraints.Constraints("no"));
		definitions[3].setDescription("The description of the table component");
		definitions[3].setDimensional(false);
		definitions[3].setDisplayName("Description");
		definitions[3].setGroup("Info");
		definitions[3].setInheritanceLevel(new InheritanceLevel[] {InheritanceLevel.TABLE});
		definitions[3].setName("description");
		definitions[3].setPrimaryKey(false);
		definitions[3].setSecurityFilter("no");
		definitions[3].setSystem(false);
		definitions[3].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[4] = new TablePropertyDefinition();
		definitions[4].setBusinessSearch(true);
		definitions[4].setConstraints(new org.openl.rules.table.constraints.Constraints("no"));
		definitions[4].setDescription("Add any number of comma-separated tags, could be used for search");
		definitions[4].setDimensional(false);
		definitions[4].setDisplayName("Tags");
		definitions[4].setFormat("comma separated");
		definitions[4].setGroup("Info");
		definitions[4].setInheritanceLevel(new InheritanceLevel[] {InheritanceLevel.TABLE});
		definitions[4].setName("tags");
		definitions[4].setPrimaryKey(false);
		definitions[4].setSecurityFilter("no");
		definitions[4].setSystem(false);
		definitions[4].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[5] = new TablePropertyDefinition();
		definitions[5].setBusinessSearch(true);
		definitions[5].setConstraints(new org.openl.rules.table.constraints.Constraints("< expirationDate"));
		definitions[5].setDescription("The table becomes active on this date and inactive on the expiration date. There"
		 + " can be multiple copies of the same table in the same module");
		definitions[5].setDimensional(true);
		definitions[5].setDisplayName("Rate Effective Date");
		definitions[5].setExpression("le(currentDate)");
		definitions[5].setFormat("MM/dd/yyyy");
		definitions[5].setGroup("Business Dimension");
		definitions[5].setInheritanceLevel(new InheritanceLevel[] {InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE});
		definitions[5].setName("effectiveDate");
		definitions[5].setPrimaryKey(true);
		definitions[5].setSecurityFilter("no");
		definitions[5].setSystem(false);
		definitions[5].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.util.Date.class));
		definitions[6] = new TablePropertyDefinition();
		definitions[6].setBusinessSearch(true);
		definitions[6].setConstraints(new org.openl.rules.table.constraints.Constraints("> effectiveDate"));
		definitions[6].setDescription("See effectiveDate");
		definitions[6].setDimensional(true);
		definitions[6].setDisplayName("Rate Expiration Date");
		definitions[6].setExpression("gt(currentDate)");
		definitions[6].setFormat("MM/dd/yyyy");
		definitions[6].setGroup("Business Dimension");
		definitions[6].setInheritanceLevel(new InheritanceLevel[] {InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE});
		definitions[6].setName("expirationDate");
		definitions[6].setPrimaryKey(false);
		definitions[6].setSecurityFilter("no");
		definitions[6].setSystem(false);
		definitions[6].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.util.Date.class));
		definitions[7] = new TablePropertyDefinition();
		definitions[7].setBusinessSearch(true);
		definitions[7].setConstraints(new org.openl.rules.table.constraints.Constraints("no"));
		definitions[7].setDescription("User Name");
		definitions[7].setDimensional(false);
		definitions[7].setDisplayName("Created By");
		definitions[7].setGroup("Info");
		definitions[7].setInheritanceLevel(new InheritanceLevel[] {InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE});
		definitions[7].setName("createdBy");
		definitions[7].setPrimaryKey(false);
		definitions[7].setSecurityFilter("no");
		definitions[7].setSystem(true);
		definitions[7].setSystemValueDescriptor("currentUser");
		definitions[7].setSystemValuePolicy(SystemValuePolicy.IF_BLANK_ONLY);
		definitions[7].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[8] = new TablePropertyDefinition();
		definitions[8].setBusinessSearch(true);
		definitions[8].setConstraints(new org.openl.rules.table.constraints.Constraints("no"));
		definitions[8].setDescription("The date of the table creation");
		definitions[8].setDimensional(false);
		definitions[8].setDisplayName("Created On");
		definitions[8].setFormat("MM/dd/yyyy");
		definitions[8].setGroup("Info");
		definitions[8].setInheritanceLevel(new InheritanceLevel[] {InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE});
		definitions[8].setName("createdOn");
		definitions[8].setPrimaryKey(false);
		definitions[8].setSecurityFilter("no");
		definitions[8].setSystem(true);
		definitions[8].setSystemValueDescriptor("currentDate");
		definitions[8].setSystemValuePolicy(SystemValuePolicy.IF_BLANK_ONLY);
		definitions[8].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.util.Date.class));
		definitions[9] = new TablePropertyDefinition();
		definitions[9].setBusinessSearch(false);
		definitions[9].setConstraints(new org.openl.rules.table.constraints.Constraints("no"));
		definitions[9].setDescription("User Name");
		definitions[9].setDimensional(false);
		definitions[9].setDisplayName("Modified By");
		definitions[9].setGroup("Info");
		definitions[9].setInheritanceLevel(new InheritanceLevel[] {InheritanceLevel.TABLE});
		definitions[9].setName("modifiedBy");
		definitions[9].setPrimaryKey(false);
		definitions[9].setSecurityFilter("no");
		definitions[9].setSystem(true);
		definitions[9].setSystemValueDescriptor("currentUser");
		definitions[9].setSystemValuePolicy(SystemValuePolicy.ON_EACH_EDIT);
		definitions[9].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[10] = new TablePropertyDefinition();
		definitions[10].setBusinessSearch(false);
		definitions[10].setConstraints(new org.openl.rules.table.constraints.Constraints("no"));
		definitions[10].setDescription("The date of the last table modification");
		definitions[10].setDimensional(false);
		definitions[10].setDisplayName("Modified On");
		definitions[10].setFormat("MM/dd/yyyy");
		definitions[10].setGroup("Info");
		definitions[10].setInheritanceLevel(new InheritanceLevel[] {InheritanceLevel.TABLE});
		definitions[10].setName("modifyOn");
		definitions[10].setPrimaryKey(false);
		definitions[10].setSecurityFilter("no");
		definitions[10].setSystem(true);
		definitions[10].setSystemValueDescriptor("currentDate");
		definitions[10].setSystemValuePolicy(SystemValuePolicy.ON_EACH_EDIT);
		definitions[10].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.util.Date.class));
		definitions[11] = new TablePropertyDefinition();
		definitions[11].setBusinessSearch(false);
		definitions[11].setConstraints(new org.openl.rules.table.constraints.Constraints("one of: common, vocabulary[N], main[N]"));
		definitions[11].setDescription("Used to manage dependencies between build phases");
		definitions[11].setDimensional(false);
		definitions[11].setDisplayName("Build Phase");
		definitions[11].setGroup("Dev");
		definitions[11].setInheritanceLevel(new InheritanceLevel[] {InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE});
		definitions[11].setName("buildPhase");
		definitions[11].setPrimaryKey(false);
		definitions[11].setSecurityFilter("no");
		definitions[11].setSystem(false);
		definitions[11].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[12] = new TablePropertyDefinition();
		definitions[12].setBusinessSearch(false);
		definitions[12].setConstraints(new org.openl.rules.table.constraints.Constraints("one of: on, off, gaps, overlaps"));
		definitions[12].setDescription("Defines validation mode for DT");
		definitions[12].setDimensional(false);
		definitions[12].setDisplayName("Validate DT");
		definitions[12].setGroup("Dev");
		definitions[12].setInheritanceLevel(new InheritanceLevel[] {InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE});
		definitions[12].setName("validateDT");
		definitions[12].setPrimaryKey(false);
		definitions[12].setSecurityFilter("no");
		definitions[12].setSystem(false);
		definitions[12].setTableType("xls.dt");
		definitions[12].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[13] = new TablePropertyDefinition();
		definitions[13].setBusinessSearch(true);
		definitions[13].setConstraints(new org.openl.rules.table.constraints.Constraints("list: Defined by method getLob()"));
		definitions[13].setDescription("Defines the list of active LOBs for this table");
		definitions[13].setDimensional(true);
		definitions[13].setDisplayName("LOB");
		definitions[13].setExpression("eq(lob)");
		definitions[13].setGroup("Business Dimension");
		definitions[13].setInheritanceLevel(new InheritanceLevel[] {InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE});
		definitions[13].setName("lob");
		definitions[13].setPrimaryKey(false);
		definitions[13].setSecurityFilter("yes (coma separated filter specification by user role: category/role pairs)");
		definitions[13].setSystem(false);
		definitions[13].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[14] = new TablePropertyDefinition();
		definitions[14].setBusinessSearch(true);
		definitions[14].setConstraints(new org.openl.rules.table.constraints.Constraints("list: East, West, Midwest, South"));
		definitions[14].setDescription("US Region");
		definitions[14].setDimensional(true);
		definitions[14].setDisplayName("US Region");
		definitions[14].setExpression("eq(usRegion)");
		definitions[14].setGroup("Business Dimension");
		definitions[14].setInheritanceLevel(new InheritanceLevel[] {InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE});
		definitions[14].setName("usregion");
		definitions[14].setPrimaryKey(false);
		definitions[14].setSecurityFilter("yes (coma separated filter specification by user role: category/role pairs)");
		definitions[14].setSystem(false);
		definitions[14].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[15] = new TablePropertyDefinition();
		definitions[15].setBusinessSearch(false);
		definitions[15].setConstraints(new org.openl.rules.table.constraints.Constraints("list: currencies"));
		definitions[15].setDefaultValue("USD");
		definitions[15].setDescription("Currency");
		definitions[15].setDimensional(false);
		definitions[15].setDisplayName("Currency");
		definitions[15].setGroup("Business Dimension");
		definitions[15].setInheritanceLevel(new InheritanceLevel[] {InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE});
		definitions[15].setName("currency");
		definitions[15].setPrimaryKey(false);
		definitions[15].setSecurityFilter("no");
		definitions[15].setSystem(false);
		definitions[15].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[16] = new TablePropertyDefinition();
		definitions[16].setBusinessSearch(false);
		definitions[16].setConstraints(new org.openl.rules.table.constraints.Constraints("list: languages"));
		definitions[16].setDefaultValue("en");
		definitions[16].setDescription("Language");
		definitions[16].setDimensional(false);
		definitions[16].setDisplayName("Language");
		definitions[16].setGroup("Business Dimension");
		definitions[16].setInheritanceLevel(new InheritanceLevel[] {InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE});
		definitions[16].setName("lang");
		definitions[16].setPrimaryKey(false);
		definitions[16].setSecurityFilter("no");
		definitions[16].setSystem(false);
		definitions[16].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[17] = new TablePropertyDefinition();
		definitions[17].setBusinessSearch(true);
		definitions[17].setConstraints(new org.openl.rules.table.constraints.Constraints("list: US States"));
		definitions[17].setDescription("US State");
		definitions[17].setDimensional(true);
		definitions[17].setDisplayName("US State");
		definitions[17].setExpression("eq(usState)");
		definitions[17].setGroup("Business Dimension");
		definitions[17].setInheritanceLevel(new InheritanceLevel[] {InheritanceLevel.MODULE, InheritanceLevel.CATEGORY, InheritanceLevel.TABLE});
		definitions[17].setName("state");
		definitions[17].setPrimaryKey(false);
		definitions[17].setSecurityFilter("yes (coma separated filter specification by user role: category/role pairs)");
		definitions[17].setSystem(false);
		definitions[17].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[18] = new TablePropertyDefinition();
		definitions[18].setBusinessSearch(true);
		definitions[18].setConstraints(new org.openl.rules.table.constraints.Constraints("list: US, EU, Asia Pacific, North America"));
		definitions[18].setDescription("Economic Region");
		definitions[18].setDimensional(false);
		definitions[18].setDisplayName("Region");
		definitions[18].setGroup("Business Dimension");
		definitions[18].setInheritanceLevel(new InheritanceLevel[] {InheritanceLevel.MODULE, InheritanceLevel.CATEGORY});
		definitions[18].setName("region");
		definitions[18].setPrimaryKey(false);
		definitions[18].setSecurityFilter("yes (coma separated filter specification by user role: category/role pairs)");
		definitions[18].setSystem(false);
		definitions[18].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[19] = new TablePropertyDefinition();
		definitions[19].setBusinessSearch(false);
		definitions[19].setConstraints(new org.openl.rules.table.constraints.Constraints("NN.NN[.NN]"));
		definitions[19].setDescription("Version is a dimension with a specific constraint - one and only one version mus"
		 + "t be active(per dimension), the dispatch is done automatically to the active ver"
		 + "sion");
		definitions[19].setDimensional(false);
		definitions[19].setDisplayName("Version");
		definitions[19].setGroup("Version");
		definitions[19].setInheritanceLevel(new InheritanceLevel[] {InheritanceLevel.TABLE});
		definitions[19].setName("version");
		definitions[19].setPrimaryKey(false);
		definitions[19].setSystem(false);
		definitions[19].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
		definitions[20] = new TablePropertyDefinition();
		definitions[20].setBusinessSearch(false);
		definitions[20].setConstraints(new org.openl.rules.table.constraints.Constraints("only one active version per dimension"));
		definitions[20].setDefaultValue("true");
		definitions[20].setDescription("Indicates an active version");
		definitions[20].setDimensional(false);
		definitions[20].setDisplayName("Active");
		definitions[20].setGroup("Version");
		definitions[20].setInheritanceLevel(new InheritanceLevel[] {InheritanceLevel.TABLE});
		definitions[20].setName("active");
		definitions[20].setPrimaryKey(false);
		definitions[20].setSystem(false);
		definitions[20].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.Boolean.class));
		definitions[21] = new TablePropertyDefinition();
		definitions[21].setBusinessSearch(false);
		definitions[21].setDefaultValue("true");
		definitions[21].setDescription("Raises an error if no rules were matched. The error will display at least parame"
		 + "ter set, if possible trace(not complete)");
		definitions[21].setDimensional(false);
		definitions[21].setDisplayName("Fail On Miss");
		definitions[21].setGroup("Dev");
		definitions[21].setInheritanceLevel(new InheritanceLevel[] {InheritanceLevel.TABLE});
		definitions[21].setName("failOnMiss");
		definitions[21].setPrimaryKey(false);
		definitions[21].setSystem(false);
		definitions[21].setTableType("xls.dt");
		definitions[21].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.Boolean.class));
		definitions[22] = new TablePropertyDefinition();
		definitions[22].setBusinessSearch(false);
		definitions[22].setDescription("Value to return if no rules were matched. The type is compatible with table retu"
		 + "rn type");
		definitions[22].setDimensional(false);
		definitions[22].setDisplayName("Return On Miss");
		definitions[22].setGroup("Dev");
		definitions[22].setInheritanceLevel(new InheritanceLevel[] {InheritanceLevel.TABLE});
		definitions[22].setName("returnOnMiss");
		definitions[22].setPrimaryKey(false);
		definitions[22].setSystem(false);
		definitions[22].setTableType("xls.dt");
		definitions[22].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.Boolean.class));
		definitions[23] = new TablePropertyDefinition();
		definitions[23].setBusinessSearch(true);
		definitions[23].setConstraints(new org.openl.rules.table.constraints.Constraints("Worksheet, Workbook, Module"));
		definitions[23].setDescription("Use in properties table to provide scope of the properties");
		definitions[23].setDimensional(false);
		definitions[23].setDisplayName("Scope");
		definitions[23].setGroup("Dev");
		definitions[23].setInheritanceLevel(new InheritanceLevel[] {InheritanceLevel.MODULE, InheritanceLevel.CATEGORY});
		definitions[23].setName("scope");
		definitions[23].setPrimaryKey(false);
		definitions[23].setSystem(false);
		definitions[23].setTableType("xls.props");
		definitions[23].setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
        // <<< END INSERT TablePropertiesDefinition >>>
    }

    /**
     * 
     * @return
     */
    public static TablePropertyDefinition[] getDefaultDefinitions(){
        return definitions;
    }
    
	/**
	 * Gets the name of the property by the given display name
	 * @param displayName
	 * @return name
	 */
	public static String getPropertyName(String displayName) {
	    String result = null;
	    for(TablePropertyDefinition propDefinition : getDefaultDefinitions()){
	        if(propDefinition.getDisplayName().equals(displayName)){
	            result = propDefinition.getName();
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
        for(TablePropertyDefinition propDefinition : getDefaultDefinitions()){
            if(propDefinition.getName().equals(name)){
                result = propDefinition.getDisplayName();
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
        for(TablePropertyDefinition propDefinition : getDefaultDefinitions()){
            if(propDefinition.getName().equals(name)){
                result = propDefinition;
            }
        }
        return result;
    }
	
	/**
	 * Gets list of properties that must me set for every table by default.
	 *
	 * @return list of properties.
	 */
	public static List<TablePropertyDefinition> getPropertiesToBeSetByDefault() {
        List<TablePropertyDefinition> result = new ArrayList<TablePropertyDefinition>();
        for(TablePropertyDefinition propDefinition : getDefaultDefinitions()){
            if(propDefinition.getDefaultValue() != null){
                result.add(propDefinition);
            }
        }
        return result;
    }
	
	/**
	 * Gets list of properties that are marked as system.
	 *  
	 * @return list of properties.
	 */
	public static List<TablePropertyDefinition> getSystemProperties() {	    
	    List<TablePropertyDefinition> result = new ArrayList<TablePropertyDefinition>();
	        for(TablePropertyDefinition propDefinition : getDefaultDefinitions()){
	            if(propDefinition.isSystem()){	                
	                result.add(propDefinition);
	            }
	        }
        return result;
	}
	
}
