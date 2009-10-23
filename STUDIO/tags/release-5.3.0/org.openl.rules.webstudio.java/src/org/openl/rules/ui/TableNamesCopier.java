package org.openl.rules.ui;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.lang.xls.binding.TableProperties.Property;
import org.openl.rules.table.xls.builder.TableBuilder;

public class TableNamesCopier extends TableCopier {
    
    public TableNamesCopier() {
        start();
        initUri();
    }

    public TableNamesCopier(String elementUri1) {        
        start();
        this.elementUri = elementUri1;
        initTableNames();        
    }  
   
    @Override
    protected Map<String, Object> buildProperties(Property[] properties) {
        Map<String, Object> newProperties = new LinkedHashMap<String, Object>();
        if (properties != null) {
            for (int i = 0; i < properties.length; i++) {
                String key = properties[i].getKey().getValue();
                Object value = properties[i].getValue().getValue();
                newProperties.put(key.trim(), value);
            }
        }
        if (StringUtils.isBlank(tableBusinessName) && newProperties.containsKey(TableBuilder.TABLE_PROPERTIES_NAME)) {
            newProperties.remove(TableBuilder.TABLE_PROPERTIES_NAME);
        } else if (StringUtils.isNotBlank(tableBusinessName)) {
            newProperties.put(TableBuilder.TABLE_PROPERTIES_NAME, tableBusinessName);
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
