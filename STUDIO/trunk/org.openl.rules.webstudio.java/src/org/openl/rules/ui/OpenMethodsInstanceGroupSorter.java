package org.openl.rules.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.binding.TableProperties;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.AOpenClass.MethodKey;
import org.openl.util.ITreeElement;

public class OpenMethodsInstanceGroupSorter extends ATableTreeSorter {

    private String[] getTableDisplayValue(TableSyntaxNode tsn, int i) {
        TableProperties tp = tsn.getTableProperties();
        String display = null;
        String name = null;

        if (tp != null) {

            name = tp.getPropertyValueAsString("name");
            display = tp.getPropertyValueAsString("display");
            if (display == null) {
                display = name;
            }
        }

        if (name == null) {
            name = str2name(tsn.getTable().getGridTable().getCell(0, 0).getStringValue(), tsn.getType());
        }

        if (display == null) {
            display = str2display(tsn.getTable().getGridTable().getCell(0, 0).getStringValue(), tsn.getType());
        }

        String sfx = (i < 2 ? "" : "(" + i + ")");
        return new String[] { name + sfx, display + sfx, display + sfx };
    }

    private String str2display(String src, String type) {
        return src;
    }

    private String str2name(String src, String type) {
        if (src == null) {
            src = "NO NAME";
        } else if (type.equals(ITableNodeTypes.XLS_DT) || type.equals(ITableNodeTypes.XLS_SPREADSHEET)
                || type.equals(ITableNodeTypes.XLS_TBASIC) || type.equals(ITableNodeTypes.XLS_COLUMN_MATCH)
                || type.equals(ITableNodeTypes.XLS_DATA) || type.equals(ITableNodeTypes.XLS_DATATYPE)
                || type.equals(ITableNodeTypes.XLS_METHOD) || type.equals(ITableNodeTypes.XLS_TEST_METHOD)
                || type.equals(ITableNodeTypes.XLS_RUN_METHOD)) {
            String[] tokens = StringUtils.split(src.replaceAll("\\(.*\\)", ""));
            src = tokens[tokens.length - 1].trim();
        }
        return src;
    }

    @Override
    public String[] getDisplayValue(Object sorterObject, int i) {
        TableSyntaxNode tsn = (TableSyntaxNode) sorterObject;
        return getTableDisplayValue(tsn, i);
    }

    @Override
    public String getName() {
        return "Table Group Instance";
    }

    @Override
    public String getType(Object sorterObject) {
        return IProjectTypes.PT_TABLE_GROUP;
    }

    @Override
    public String getUrl(Object sorterObject) {
        return null;
    }

    @Override
    public int getWeight(Object sorterObject) {
        return 0;
    }

    @Override
    public Object makeSorterObject(TableSyntaxNode tsn) {
        return tsn;
    }

    @Override
    public ITreeElement makeElement(Object obj, int i, String name) {

        TableSyntaxNode node = (TableSyntaxNode) obj;

        if (node.getMember() instanceof IOpenMethod) {

            IOpenMethod method = (IOpenMethod) node.getMember();

            IOpenMethodGroupsDictionary openMethodGroupsDictionary = getOpenMethodGroupsDictionary();
            
            if (openMethodGroupsDictionary.contains(method)) {
                List<IOpenMethod> groupMethods = openMethodGroupsDictionary.getGroup(method);

                // If group of methods size is over then 1 create the tree
                // element (folder); otherwise - method is unique and additional
                // element will not be created.
                // author: Alexey Gamanovich
                //
                if (groupMethods != null && groupMethods.size() > 1) {

                    String folderName = getMajorityName(groupMethods);
                    return makeFolder(folderName);
                }
            }
        }

        return null;
    }

    @Override
    public Comparable makeKey(Object obj, int i) {
        
        TableSyntaxNode node = (TableSyntaxNode) obj;
        
        if (node.getMember() instanceof IOpenMethod) {

            IOpenMethod method = (IOpenMethod) node.getMember();
            MethodKey methodKey = new MethodKey(method);
           
            int hash = methodKey.hashCode();
            String hashString = String.valueOf(hash);

            Object sorterObject = makeSorterObject(node);
            
            return new Key(getWeight(sorterObject), new String[]{hashString, hashString, hashString});
        }
        
        return null;
    }
    
    private String getMajorityName(List<IOpenMethod> methods) {
        
        Map<String, Integer> map = new HashMap<String, Integer>();
        
        for (IOpenMethod method : methods) {
            String[] names = getDisplayValue(method.getInfo().getSyntaxNode(), 0);
            String name = names[0];
            
            Integer value = map.get(name);
            
            if (value == null) {
                value = 0;
            }
            
            value += 1;
            map.put(name, value);
        }
        

        Integer maxNameWeight = 0;
        String majorName = StringUtils.EMPTY;
        
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        
        for (Map.Entry<String, Integer> entry : entries) {
            
            if(maxNameWeight.compareTo(entry.getValue()) < 0) {
                maxNameWeight = entry.getValue();
                majorName = entry.getKey();
            }
        }
        
        return majorName;
    }
}