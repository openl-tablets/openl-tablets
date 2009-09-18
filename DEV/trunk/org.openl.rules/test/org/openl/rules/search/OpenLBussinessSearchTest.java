package org.openl.rules.search;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
import org.openl.meta.ObjectValue;
import org.openl.meta.StringValue;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.TableProperties.Property;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;

public class OpenLBussinessSearchTest {
    
    private String __src = "test/rules/Tutorial_4_Test.xls";
    
    private OpenLBussinessSearch search = new OpenLBussinessSearch();
    
    private XlsModuleSyntaxNode getTables() {        
        UserContext ucxt = new UserContext(Thread.currentThread().getContextClassLoader(), ".");
        OpenClassJavaWrapper wrapper = OpenClassJavaWrapper.createWrapper("org.openl.xls", ucxt, __src);
        XlsMetaInfo xmi = (XlsMetaInfo) wrapper.getOpenClass().getMetaInfo();
        XlsModuleSyntaxNode xsn = xmi.getXlsModuleNode();
        return xsn;
    }
    
    private void initSearchCondition(List<Property> propList) {
        BussinessSearchCondition searchCondition = new BussinessSearchCondition();        
        searchCondition.setPropToSearch(propList);
        search.setBusSearchCondit(searchCondition);
    }
    
    @Test 
    public void testTableByName() {
        List<Property> propList = new ArrayList<Property>(){{
            add(new Property(new StringValue("name"), new ObjectValue("Vehicle Discounts"))); 
        }};
        initSearchCondition(propList);
        Object searchResult = search.search(getTables());
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
        List<Property> propList = new ArrayList<Property>(){{
            add(new Property(new StringValue("name"), new ObjectValue("Vehicle Score Processing Sequence")));
            add(new Property(new StringValue("category"), new ObjectValue("Auto-Scoring")));
        }};
        initSearchCondition(propList);
        Object searchResult = search.search(getTables());
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
        XlsModuleSyntaxNode xls = getTables();
        List<Property> propList = new ArrayList<Property>(){{
            add(new Property(new StringValue("name"), new ObjectValue("Vehicle Score Processing Sequence")));            
        }};
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
            if(table.getProperty("name") != null && table.getPropertyValue("name").equals(nameProp) && 
                    table.getProperty("category") == null) {
                listTables[0] = table;
            }
        }
        return listTables;
    }
                                               

}
