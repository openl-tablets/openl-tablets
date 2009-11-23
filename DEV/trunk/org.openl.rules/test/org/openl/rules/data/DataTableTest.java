package org.openl.rules.data;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
import org.openl.rules.data.binding.DataTableBoundNode.DataOpenField;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;

public class DataTableTest {
    
private String __src = "test/rules/Tutorial_2_Test.xls";
    
    private XlsModuleSyntaxNode getTables() {        
        OpenClassJavaWrapper wrapper = getJavaWrapper();
        XlsMetaInfo xmi = (XlsMetaInfo) wrapper.getOpenClassWithErrors().getMetaInfo();
        XlsModuleSyntaxNode xsn = xmi.getXlsModuleNode();
        return xsn;
    }

    private OpenClassJavaWrapper getJavaWrapper() {
        UserContext ucxt = new UserContext(Thread.currentThread().getContextClassLoader(), ".");
        OpenClassJavaWrapper wrapper = OpenClassJavaWrapper.createWrapper("org.openl.xls", ucxt, __src);
        return wrapper;
    }
    
    @Test
    public void testSimpleStringArray() {
        XlsModuleSyntaxNode module = getTables();
        TableSyntaxNode[] tsns = module.getXlsTableSyntaxNodes();
        for (TableSyntaxNode tsn : tsns) {
            if ("Data ArrayList simpleStringArray".equals(tsn.getDisplayName())) {
                DataOpenField member = (DataOpenField)tsn.getMember();
                assertNotNull(member);
                String[] stringData = (String[])member.getTable().getDataArray();
                assertTrue(stringData.length == 5);
                List<String> dataList = new ArrayList<String>();
                for (String data : stringData) {
                    dataList.add(data);
                }                
                assertTrue(dataList.contains("StringValue1"));
                assertTrue(dataList.contains("StringValue2"));
                assertTrue(dataList.contains("StringValue3"));
                assertTrue(dataList.contains("StringValue4"));
                assertTrue(dataList.contains("StringValue5"));                
            }        
        }
    }
    
    @Test
    public void testTypeWithArrayColumns() {
        XlsModuleSyntaxNode module = getTables();
        TableSyntaxNode[] tsns = module.getXlsTableSyntaxNodes();
        for (TableSyntaxNode tsn : tsns) {
            if ("Data TypeWithArray testTypeWithArrayColumns".equals(tsn.getDisplayName())) {
                DataOpenField member = (DataOpenField)tsn.getMember();
                assertNotNull(member);
                TypeWithArray[] typeWitharray = (TypeWithArray[])member.getTable().getDataArray();
                assertTrue(typeWitharray[0].getIntArray().length == 4);
                List<Integer> dataList = new ArrayList<Integer>();
                for (int i=0; i< typeWitharray[0].getIntArray().length; i++) {                    
                    dataList.add(Integer.valueOf(typeWitharray[0].getIntArray()[i]));
                }                                
                assertTrue(dataList.contains(111));
                assertTrue(dataList.contains(23));
                assertTrue(dataList.contains(5));
                assertTrue(dataList.contains(67));  
            }        
        }
    }
    
    @Test
    public void testTypeWithArrayRows() {
        XlsModuleSyntaxNode module = getTables();
        TableSyntaxNode[] tsns = module.getXlsTableSyntaxNodes();
        for (TableSyntaxNode tsn : tsns) {
            if ("Data TypeWithArray testTypewithArray2".equals(tsn.getDisplayName())) {
                DataOpenField member = (DataOpenField)tsn.getMember();
                assertNotNull(member);
                TypeWithArray[] typeWitharray = (TypeWithArray[])member.getTable().getDataArray();
                assertTrue(typeWitharray[0].getIntArray().length == 5);
                List<Integer> dataList = new ArrayList<Integer>();
                for (int i=0; i< typeWitharray[0].getIntArray().length; i++) {                    
                    dataList.add(Integer.valueOf(typeWitharray[0].getIntArray()[i]));
                }                                
                assertTrue(dataList.contains(12));
                assertTrue(dataList.contains(13));
                assertTrue(dataList.contains(14));
                assertTrue(dataList.contains(15));
                assertTrue(dataList.contains(16));
            }        
        }
    }
    
    @Test
    public void testTypeWithArrayRowsOneElement() {
        XlsModuleSyntaxNode module = getTables();
        TableSyntaxNode[] tsns = module.getXlsTableSyntaxNodes();
        for (TableSyntaxNode tsn : tsns) {
            if ("Data TypeWithArray testTypewithArray3".equals(tsn.getDisplayName())) {
                DataOpenField member = (DataOpenField)tsn.getMember();
                assertNotNull(member);
                TypeWithArray[] typeWitharray = (TypeWithArray[])member.getTable().getDataArray();
                assertTrue(typeWitharray[0].getIntArray().length == 1);
                List<Integer> dataList = new ArrayList<Integer>();
                for (int i=0; i< typeWitharray[0].getIntArray().length; i++) {                    
                    dataList.add(Integer.valueOf(typeWitharray[0].getIntArray()[i]));
                }                                
                assertTrue(dataList.contains(12));               
            }        
        }
    }
    
    @Test
    public void testCommaSeparated() {
        XlsModuleSyntaxNode module = getTables();
        TableSyntaxNode[] tsns = module.getXlsTableSyntaxNodes();
        for (TableSyntaxNode tsn : tsns) {
            if ("Data TypeWithArray testCommaSeparated".equals(tsn.getDisplayName())) {
                DataOpenField member = (DataOpenField)tsn.getMember();
                assertNotNull(member);
                TypeWithArray[] typeWitharray = (TypeWithArray[])member.getTable().getDataArray();
                assertTrue(typeWitharray[0].getIntArray().length == 5);
                List<Integer> dataList = new ArrayList<Integer>();
                for (int i=0; i< typeWitharray[0].getIntArray().length; i++) {                    
                    dataList.add(Integer.valueOf(typeWitharray[0].getIntArray()[i]));
                }                                
                assertTrue(dataList.contains(1));
                assertTrue(dataList.contains(56));
                assertTrue(dataList.contains(78));
                assertTrue(dataList.contains(45));
                assertTrue(dataList.contains(99));
            }        
        }
    }
    
//    @Test
//    public void testDoubleArray() {
//        XlsModuleSyntaxNode module = getTables();
//        TableSyntaxNode[] tsns = module.getXlsTableSyntaxNodes();
//        for (TableSyntaxNode tsn : tsns) {
//            if ("Data TypeWithArray testDoubleArray".equals(tsn.getDisplayName())) {
//                DataOpenField member = (DataOpenField)tsn.getMember();
//                assertNotNull(member);
//                TypeWithArray[] typeWitharray = (TypeWithArray[])member.getTable().getDataArray();
//                assertTrue(typeWitharray[0].getIntArray().length == 5);
//                List<Integer> dataList = new ArrayList<Integer>();
//                for (int i=0; i< typeWitharray[0].getIntArray().length; i++) {                    
//                    dataList.add(Integer.valueOf(typeWitharray[0].getIntArray()[i]));
//                }                                
//                assertTrue(dataList.contains(1));
//                assertTrue(dataList.contains(56));
//                assertTrue(dataList.contains(78));
//                assertTrue(dataList.contains(45));
//                assertTrue(dataList.contains(99));
//            }        
//        }
//    }
    
    @Test
    public void testStringArray() {
        XlsModuleSyntaxNode module = getTables();
        TableSyntaxNode[] tsns = module.getXlsTableSyntaxNodes();
        for (TableSyntaxNode tsn : tsns) {
            if ("Data TypeWithArray testStringArray".equals(tsn.getDisplayName())) {
                DataOpenField member = (DataOpenField)tsn.getMember();
                assertNotNull(member);
                TypeWithArray[] typeWitharray = (TypeWithArray[])member.getTable().getDataArray();
                assertTrue(typeWitharray[0].getStringArray().length == 2);
                List<String> dataList = new ArrayList<String>();
                for (String token : typeWitharray[0].getStringArray()) {                    
                    dataList.add(token);
                }                                
                assertTrue(dataList.contains("Hello Denis! My name is vova."));
                assertTrue(dataList.contains("Yeah you are right."));                
            }        
        }
    }
    
    @Test
    public void testStringArrayWithEscaper() {
        XlsModuleSyntaxNode module = getTables();
        TableSyntaxNode[] tsns = module.getXlsTableSyntaxNodes();
        for (TableSyntaxNode tsn : tsns) {
            if ("Data TypeWithArray testStringArrayWithEscaper".equals(tsn.getDisplayName())) {
                DataOpenField member = (DataOpenField)tsn.getMember();
                assertNotNull(member);
                TypeWithArray[] typeWitharray = (TypeWithArray[])member.getTable().getDataArray();
                assertTrue(typeWitharray[0].getStringArray().length == 4);
                List<String> dataList = new ArrayList<String>();
                for (String token : typeWitharray[0].getStringArray()) {                    
                    dataList.add(token);
                }                                
                assertTrue(dataList.contains("One"));
                assertTrue(dataList.contains("two"));
                assertTrue(dataList.contains("three,continue this"));
                assertTrue(dataList.contains("four"));
            }        
        }
    }
    
    
    
    

}
