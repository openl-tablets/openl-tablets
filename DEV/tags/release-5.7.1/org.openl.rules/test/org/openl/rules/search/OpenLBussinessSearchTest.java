package org.openl.rules.search;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;

public class OpenLBussinessSearchTest extends BaseOpenlBuilderHelper{
    
    private static String __src = "test/rules/Tutorial_4_Test.xls";
    
    public OpenLBussinessSearchTest() {
        super(__src);        
    }
    
    private OpenLBussinessSearch search = new OpenLBussinessSearch();
    
    private void initSearchCondition(Map<String, Object> propList) {
        BussinessSearchCondition searchCondition = new BussinessSearchCondition();        
        searchCondition.setPropToSearch(propList);
        search.setBusSearchCondit(searchCondition);
    }
    
    @Test 
    public void testTableByName() {
        Map<String, Object> propList = new HashMap<String, Object>();
        propList.put("name", "Vehicle Discounts");
        initSearchCondition(propList);
        Object searchResult = search.search(getModuleSuntaxNode());
        if((searchResult != null) && (searchResult instanceof OpenLBussinessSearchResult)) {
            List<TableSyntaxNode> foundTables = ((OpenLBussinessSearchResult) searchResult).getFoundTables();
            assertTrue("There is only one table for this cryteria",foundTables.size()==1);
            assertEquals("Display names are identical", "Rules DoubleValue vehicleDiscount(Vehicle vehicle, String vehicleTheftRating)" ,
                    foundTables.get(0).getDisplayName());            
        } else {
            fail();
        }        
    }
    
    @Test 
    public void testTableByNameAndCategory() {        
        Map<String, Object> propList = new HashMap<String, Object>();
        propList.put("name", "Vehicle Score Processing Sequence");
        propList.put("category", "Auto-Scoring");
        initSearchCondition(propList);
        Object searchResult = search.search(getModuleSuntaxNode());
        if((searchResult != null) && (searchResult instanceof OpenLBussinessSearchResult)) {
            List<TableSyntaxNode> foundTables = ((OpenLBussinessSearchResult) searchResult).getFoundTables();
            assertTrue("There is only one table for this cryteria",foundTables.size()==1);
            assertEquals("Display names are identical", "Rules void vehicleScore1(VehicleCalc vc)" ,
                    foundTables.get(0).getDisplayName());
            //System.out.println("name: "+ foundTables.get(0).getDisplayName());
        } else {
            fail();
        }        
    }
    
    @Test 
    public void testWithConsists() {
        XlsModuleSyntaxNode xls = getModuleSuntaxNode();        
        Map<String, Object> propList = new HashMap<String, Object>();
        propList.put("name", "Vehicle Score Processing Sequence");
        initSearchCondition(propList);
        search.getBusSearchCondit().setTablesContains(getTableConsists(xls, "Vehicle Score Processing Sequence"));
        Object searchResult = search.search(xls);
        if((searchResult != null) && (searchResult instanceof OpenLBussinessSearchResult)) {
            List<TableSyntaxNode> foundTables = ((OpenLBussinessSearchResult) searchResult).getFoundTables();
            assertTrue("There is only one table for this cryteria",foundTables.size()==1);
            assertEquals("Display names are identical", "Rules void vehicleScore(VehicleCalc vc)" ,
                    foundTables.get(0).getDisplayName());            
        } else {
            fail();
        }        
    }
    
    private TableSyntaxNode[] getTableConsists(XlsModuleSyntaxNode xls, String nameProp) {
        TableSyntaxNode[] listTables = new TableSyntaxNode[1];
        TableSyntaxNode result = null;
        
        for(TableSyntaxNode table : xls.getXlsTableSyntaxNodes()) {
            ITableProperties tableProp = table.getTableProperties();
            if(tableProp != null) {
                if(tableProp.getName() != null && tableProp.getName().equals(nameProp) && 
                        tableProp.getCategory() == null) {
                    listTables[0] = table;
                }
            }
            
        }
        return listTables;
    }
                                               

}
