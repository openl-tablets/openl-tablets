package org.openl.rules.ui.copy;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.xls.builder.TableBuilder;

public class TableNamesCopier extends TableCopier {
    
    public TableNamesCopier() {
        start();
        initUri();
    }

    public TableNamesCopier(String elementUri) {        
        start();
        setElementUri(elementUri);
        initTableNames();        
    }  
   
    @Override
    protected Map<String, Object> buildProperties() {
        Map<String, Object> newProperties = new LinkedHashMap<String, Object>();
        newProperties.putAll(buildSystemProperties());
        TableSyntaxNode node = getCopyingTable();
        if (node != null) {
            ITableProperties tableProperties = node.getTableProperties();
            if (tableProperties != null) {
                Map<String, Object> properties = tableProperties.getPropertiesDefinedInTableIgnoreSystem();
                if (properties != null) {
                    for (Map.Entry<String, Object> property : properties.entrySet()) {
                        String propertyName = property.getKey();
                        Object propertyValue = property.getValue();
                        newProperties.put(propertyName.trim(), propertyValue);
                    }   
                }
            }
        }
        if (StringUtils.isBlank(getTableBusinessName()) && newProperties.containsKey(TableBuilder.TABLE_PROPERTIES_NAME)) {
            newProperties.remove(TableBuilder.TABLE_PROPERTIES_NAME);
        } else if (StringUtils.isNotBlank(getTableBusinessName())) {
            newProperties.put(TableBuilder.TABLE_PROPERTIES_NAME, getTableBusinessName());
        }
        return newProperties;
    }
    
        



//    private void validateTechnicalName(TableSyntaxNode node) throws CreateTableException {
//        String[] headerStr = node.getHeaderLineValue().getValue().split(" ");
//        if (headerStr.length >=3) {
//            String existingTechnicalName = headerStr[2].substring(0, headerStr[2].indexOf("("));
//            if (tableTechnicalName.equalsIgnoreCase(existingTechnicalName)) {
//                throw new CreateTableException("Table with the same technical name already exists");
//            }
//        }
//        
//    }
    
    @Override
    public String getName() {
        return "changeName";
    }

}
