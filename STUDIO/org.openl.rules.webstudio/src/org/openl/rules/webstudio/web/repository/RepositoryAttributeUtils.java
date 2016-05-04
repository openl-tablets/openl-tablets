package org.openl.rules.webstudio.web.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.tableeditor.renderkit.TableProperty;
import org.openl.rules.webstudio.web.tableeditor.PropertyRow;
import org.openl.rules.webstudio.web.tableeditor.PropertyRowType;
import org.openl.util.StringUtils;

public class RepositoryAttributeUtils {
    private Map<String, String> attribs;
    private String dateFormat = "MM/dd/yyyy";//data.format.date
    
    private ArrayList<String> dateAttrs = new ArrayList<String>(Arrays.asList("attribute6", "attribute7", "attribute8", "attribute9", "attribute10"));
    private ArrayList<String> doubleAttrs = new ArrayList<String>(Arrays.asList("attribute11", "attribute12", "attribute13", "attribute14", "attribute15"));
    
    public RepositoryAttributeUtils() {
        RepositoryArtefactPropsHolder rap = new RepositoryArtefactPropsHolder();
        attribs = rap.getProps();
    }

    public PropertyRow getPropertyRowByAttrName(String attrName) {
        if (StringUtils.isNotBlank(attrName) && attribs.containsKey(attrName)) {
            TablePropertyDefinition tpd = new TablePropertyDefinition();
            setAttrType(attrName, tpd);
            
            tpd.setName(attrName);
            tpd.setDefaultValue("");
            tpd.setDisplayName(attribs.get(attrName));
            
            TableProperty tp = new TableProperty(tpd);
            
            return new PropertyRow(PropertyRowType.PROPERTY,tp);
        }
        
        return null;
    }

    private void setAttrType(String attrName, TablePropertyDefinition tpd) {
        if (dateAttrs.contains(attrName)) {
            tpd.setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.util.Date.class));
            tpd.setFormat(dateFormat);
        } else if (doubleAttrs.contains(attrName)) {
            tpd.setType(org.openl.types.java.JavaOpenClass.DOUBLE);
        } else {
            tpd.setType(org.openl.types.java.JavaOpenClass.getOpenClass(java.lang.String.class));
        }
    }
    
    public Map<String, String> getActiveAttribs() {
        /*return sorted map*/
        if (attribs != null) {
            return new TreeMap<String, String>(attribs);
        }
        
        return new HashMap<String, String>();
    }
    
}
