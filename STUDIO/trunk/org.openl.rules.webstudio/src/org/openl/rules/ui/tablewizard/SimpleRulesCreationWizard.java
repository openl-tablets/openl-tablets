package org.openl.rules.ui.tablewizard;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.faces.validator.ValidatorException;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.NotBlank;
import org.openl.base.INamedThing;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.meta.BigDecimalValue;
import org.openl.meta.BigIntegerValue;
import org.openl.meta.ByteValue;
import org.openl.meta.DoubleValue;
import org.openl.meta.FloatValue;
import org.openl.meta.IntValue;
import org.openl.meta.LongValue;
import org.openl.meta.ShortValue;
import org.openl.rules.domaintree.DomainTree;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.CreateTableException;
import org.openl.rules.table.xls.builder.DataTableBuilder;
import org.openl.rules.table.xls.builder.SimpleRulesTableBuilder;
import org.openl.rules.table.xls.builder.TableBuilder;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DomainOpenClass;
import org.richfaces.json.JSONArray;
import org.richfaces.json.JSONException;
import org.richfaces.json.JSONObject;

public class SimpleRulesCreationWizard extends TableCreationWizard {
    @NotBlank(message = "Can not be empty")
    @Pattern(regexp = "([a-zA-Z_][a-zA-Z_0-9]*)?", message = "Invalid name")
    private String tableName;
    private SelectItem[] domainTypes;
    private String jsonTable;
    private JSONObject table;
    private String restoreTable;
    private final String TABLE_TYPE = "xls.dt";

    private String restoreTableFunction = "tableModel.restoreTableFromJSONString";

    List<DomainTypeHolder> typesList;

    private String returnValueType;

    @Valid
    private List<TypeNamePair> parameters = new ArrayList<TypeNamePair>();

    @Override
    protected void onCancel() {
        // TODO Auto-generated method stub
    }

    @Override
    protected void onStart() {
        reset();

        initDomainType();
        initWorkbooks();
    }

    private void initDomainType() {
        List<IOpenClass> types = new ArrayList<IOpenClass>(WizardUtils.getProjectOpenClass().getTypes().values());
        Collection<IOpenClass> importedClasses = WizardUtils.getImportedClasses();
        types.addAll(importedClasses);

        List<String> datatypes = new ArrayList<String>(types.size());
        datatypes.add("");
        for (IOpenClass datatype : types) {
            if (Modifier.isFinal(datatype.getInstanceClass().getModifiers())) {
                // cannot inherit from final class
                continue;
            }

            if (!(datatype instanceof DomainOpenClass)) {
                datatypes.add(datatype.getDisplayName(INamedThing.SHORT));
            }
        }

        Collection<String> allClasses = DomainTree.buildTree(WizardUtils.getProjectOpenClass()).getAllClasses(true);
        for (IOpenClass type : importedClasses) {
            allClasses.add(type.getDisplayName(INamedThing.SHORT));
        }

        domainTypes = FacesUtils.createSelectItems(allClasses);

        Collection<IOpenClass> classTypes =  DomainTree.buildTree(WizardUtils.getProjectOpenClass()).getAllOpenClasses(true);
        this.typesList = new ArrayList<DomainTypeHolder>();

        for (IOpenClass oc : classTypes) {
            typesList.add(new DomainTypeHolder(oc.getDisplayName(INamedThing.SHORT), oc, false));
        }
    }
    
    public int getColumnSize() {
        int size = 0;
        size += this.parameters.size();
        size ++;

        return size;
    }

    public List<SelectItem> getPropertyList() {
        List<SelectItem> propertyNames = new ArrayList<SelectItem>();
        TablePropertyDefinition[] propDefinitions = TablePropertyDefinitionUtils
                .getDefaultDefinitionsForTable(TABLE_TYPE, InheritanceLevel.TABLE, true);

        SelectItem selectItem = new SelectItem("");
        selectItem.setLabel("");
        propertyNames.add(selectItem);

        for (TablePropertyDefinition propDefinition : propDefinitions) {
            if(propDefinition.getDeprecation() == null) {
                String propName = propDefinition.getName();
                selectItem = new SelectItem(propName);
                selectItem.setLabel(propDefinition.getDisplayName());
                propertyNames.add(selectItem);
            }
        }

        return propertyNames;
    }

    public DomainTypeHolder getReturnValue() {
        return getTypedParameterByName(this.returnValueType);
    }

    private DomainTypeHolder getTypedParameterByName(String name) {
        DomainTypeHolder dth = null;

        for (DomainTypeHolder type :  typesList) {
            if (type.name.equals(name)) {
                dth = type.clone();
                break;
            }
        }

        if (dth == null) {
            dth = new DomainTypeHolder(name, "STRING", false);
        }

        return dth;
    }

    public List<DomainTypeHolder> getTypedParameters() {
        List<DomainTypeHolder> typedParameters = new ArrayList<DomainTypeHolder>();

        for (TypeNamePair tnp : this.parameters) {
            DomainTypeHolder gth = getTypedParameterByName(tnp.getType());
            gth.setTypeName(tnp.getName());
            gth.setIterable(tnp.isIterable());

            typedParameters.add(gth);
        }

        return typedParameters;
    }

    @Override
    protected void onFinish() throws Exception {
        XlsSheetSourceCodeModule sheetSourceModule = getDestinationSheet();
        String newTableUri = buildTable(sheetSourceModule);
        setNewTableUri(newTableUri);
        getModifiedWorkbooks().add(sheetSourceModule.getWorkbookSource());
        super.onFinish();
    }

    private String getHeaderStr() {
        try {
            String tableName = this.table.getJSONObject("header").getString("name");
            JSONArray inParam = new JSONArray(table.getJSONObject("header").get("inParam").toString());
            JSONObject returnObj = table.getJSONObject("header").getJSONObject("returnType");

            String paramStr = "";
            for (int i = 0; i < inParam.length(); i++) {
                JSONObject param = (JSONObject) inParam.get(i);
                
                paramStr += ((i > 0)? ", " : "") + param.getString("type") +((param.getBoolean("iterable"))? "[]" : "")+ " " + param.getString("name");
            }

            return returnObj.getString("type") +((returnObj.getBoolean("iterable"))? "[]" : "")+ " " +tableName + "("+paramStr+")";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    protected String buildTable(XlsSheetSourceCodeModule sourceCodeModule) throws CreateTableException {
        XlsSheetGridModel gridModel = new XlsSheetGridModel(sourceCodeModule);
        SimpleRulesTableBuilder builder = new SimpleRulesTableBuilder(gridModel);

        JSONArray dataRow = null;
        int rowSize = 0;
        if (!this.table.isNull("dataRows")) {
            try {
                dataRow = new JSONArray(table.get("dataRows").toString());
                rowSize = dataRow.length();
            } catch (Exception e) {
                
            }
        }

        Map<String, Object> properties = buildProperties();

        int width = DataTableBuilder.MIN_WIDTH;
        if (!properties.isEmpty()) {
            width = TableBuilder.PROPERTIES_MIN_WIDTH;
        }

        width = Math.max(getFieldsCount(), width);
        int height = TableBuilder.HEADER_HEIGHT + properties.size() + rowSize;

        builder.beginTable(width, height);
        builder.writeHeader(getHeaderStr());
        builder.writeProperties(properties, null);

        if (dataRow != null) {
            try {
                for (int i = 0; i < dataRow.length(); i++) {
                    JSONArray rowElements = new JSONArray(dataRow.get(i).toString());

                    List<Object> row = new ArrayList<Object>();
                    for (int j= 0; j < rowElements.length(); j++) {
                        JSONObject dataCell = ((JSONObject)rowElements.get(j));
                        if (dataCell.getString("valueType").equals("DATE")) {
                            String dateString = dataCell.getString("value");
                            String dateFormat = "yyyy-MM-dd'T'HH:mm:ss";

                            SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
                            formatter.setLenient(false);
                            formatter.setTimeZone(TimeZone.getDefault());
                            try{
                                row.add(formatter.parse(dateString));
                            } catch (Exception e) {
                                row.add(dateString);
                            }
                        } else {
                            row.add(((JSONObject)rowElements.get(j)).getString("value"));
                        }
                    }

                    builder.writeTableBodyRow(row);
                }
            } catch (Exception e) {
                
            }
        }

        String uri = gridModel.getRangeUri(builder.getTableRegion());

        builder.endTable();
        
        return uri;
    }
    
    private int getFieldsCount() {
        try {
            JSONArray inParam = new JSONArray(table.getJSONObject("header").get("inParam").toString());
            return  inParam.length() + 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    protected Map<String, Object> buildProperties() {
        Map<String, Object> properties = new LinkedHashMap<String, Object>();

        // Put system properties.
        if (WebStudioUtils.getWebStudio().isUpdateSystemProperties()) {
            Map<String, Object> systemProperties = buildSystemProperties();
            properties.putAll(systemProperties);
        }

        try {
            if (!table.isNull("properties")) {
                JSONArray propertiesJSON = new JSONArray(table.get("properties").toString());;
                for (int i = 0; i < propertiesJSON.length(); i++) {
                    JSONObject prop = (JSONObject) propertiesJSON.get(i);
                    properties.put(prop.getString("type"), prop.getString("value"));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
            

        return properties;
    }

    public void addParameter() {
        parameters.add(new TypeNamePair());
    }

    public void removeParameter(TypeNamePair parameter) {
        parameters.remove(parameter);
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public SelectItem[] getDomainTypes() {
        return domainTypes;
    }

    public void setDomainTypes(SelectItem[] domainTypes) {
        this.domainTypes = domainTypes;
    }

    public String getReturnValueType() {
        return returnValueType;
    }

    public void setReturnValueType(String returnValueType) {
        this.returnValueType = returnValueType;
    }

    public List<TypeNamePair> getParameters() {
        return parameters;
    }

    public void setParameters(List<TypeNamePair> parameters) {
        this.parameters = parameters;
    }

    public List<DomainTypeHolder> getTypesList() {
        return typesList;
    }

    public void setTypesList(List<DomainTypeHolder> typesList) {
        this.typesList = typesList;
    }
    
    public class DomainTypeHolder{
        private String name;
        private String type;
        private boolean iterable = false;
        private String typeName;
        
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getType() {
            return type;
        }
        public void setType(String type) {
            this.type = type;
        }
        
        DomainTypeHolder(String name, IOpenClass openClass, boolean iterable) {
            this.name = name;
            this.iterable = iterable;

            if (openClass != null) {
                if(openClass.isArray()) {
                    this.type = "ARRAY";
                } else if (openClass.toString().equals(Date.class.getCanonicalName().toString())) {
                    this.type = "DATE";
                } else if (openClass.toString().equals(boolean.class.getCanonicalName().toString()) ||
                        openClass.toString().equals(Boolean.class.getCanonicalName().toString())) {
                    this.type = "BOOLEAN";
                } else if (openClass.toString().equals(BigDecimal.class.getCanonicalName().toString()) ||
                        openClass.toString().equals(BigDecimalValue.class.getCanonicalName().toString()) ||
                        openClass.toString().equals(BigInteger.class.getCanonicalName().toString()) ||
                        openClass.toString().equals(BigIntegerValue.class.getCanonicalName().toString()) ||
                        openClass.toString().equals(ByteValue.class.getCanonicalName().toString()) ||
                        openClass.toString().equals(LongValue.class.getCanonicalName().toString()) ||
                        openClass.toString().equals(ShortValue.class.getCanonicalName().toString()) ||
                        openClass.toString().equals(DoubleValue.class.getCanonicalName().toString()) ||
                        openClass.toString().equals(FloatValue.class.getCanonicalName().toString()) ||
                        openClass.toString().equals(byte.class.getCanonicalName().toString()) ||
                        openClass.toString().equals(double.class.getCanonicalName().toString()) ||
                        openClass.toString().equals(float.class.getCanonicalName().toString()) ||
                        openClass.toString().equals(long.class.getCanonicalName().toString()) ||
                        openClass.toString().equals(short.class.getCanonicalName().toString()) ||
                        openClass.toString().equals(int.class.getCanonicalName().toString()) ||
                        openClass.toString().equals(IntValue.class.getCanonicalName().toString())) {
                    this.type = "INT";
                } else {
                    this.type = "STRING";
                }
            }
        }

        DomainTypeHolder(String name, String type, boolean iterable) {
            this.name = name;
            this.iterable = iterable;
            this.type = type;
        }

        public DomainTypeHolder clone(){
            return new DomainTypeHolder(this.name, this.type, this.iterable);
        }

        public DomainTypeHolder(TypeNamePair tnp) {
            // TODO Auto-generated constructor stub
        }

        public String getTypeName() {
            return typeName;
        }
        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }
        public boolean isIterable() {
            return iterable;
        }
        public void setIterable(boolean iterable) {
            this.iterable = iterable;
        }
    }

    public String getJsonTable() {
        return jsonTable;
    }

    public void setJsonTable(String jsonTable) {
        this.jsonTable = jsonTable;
        try {
            this.table = new JSONObject(jsonTable);
            this.restoreTable = getTableInitFunction(jsonTable);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private String getTableInitFunction(String jsonStr) {
        return this.restoreTableFunction + "("+jsonStr+")";
    }

    public String getRestoreTable() {
        return restoreTable;
    }

    public void setRestoreTable(String restoreTable) {
        this.restoreTable = restoreTable;
    }

    @Override
    protected void reset() {
        jsonTable = null;
        table = null;
        restoreTable = null;

        super.reset();
    }

    /**
     * Validation for properties name
     */
    public void validatePropsName(FacesContext context, UIComponent toValidate, Object value) {
        FacesMessage message = new FacesMessage();
        ValidatorException validEx = null;
        int paramId = this.getParamId(toValidate.getClientId());

        try {
            String name = ((String) value).toUpperCase();

            int i = 0;
            for (TypeNamePair param : parameters) {
                if (paramId != i && param.getName() != null && param.getName().toUpperCase().equals(name)){
                    message.setDetail("Parameter with such name already exists");
                    validEx = new ValidatorException(message);
                    throw validEx;
                }

                i++;
            }

        } catch (Exception e) {
            throw new ValidatorException(message);
        }
    }

    public void pNameListener(ValueChangeEvent valueChangeEvent) {
        int paramId = this.getParamId(valueChangeEvent.getComponent().getClientId());

        this.parameters.get(paramId).setName(valueChangeEvent.getNewValue().toString()); 
    }

    public void pTypeListener(ValueChangeEvent valueChangeEvent) {
        int paramId = this.getParamId(valueChangeEvent.getComponent().getClientId());

        this.parameters.get(paramId).setType(valueChangeEvent.getNewValue().toString()); 
    }

    public void pIterableListener(ValueChangeEvent valueChangeEvent) {
        int paramId = this.getParamId(valueChangeEvent.getComponent().getClientId());

        this.parameters.get(paramId).setIterable(Boolean.getBoolean(valueChangeEvent.getNewValue().toString())); 
    }

    private int getParamId(String componentId) {
        if(componentId != null) {
            String[] elements = componentId.split(":");
            
            if (elements.length > 3) {
                return Integer.parseInt(elements[2]);
            }
        }
        
        return 0;
    }

}
