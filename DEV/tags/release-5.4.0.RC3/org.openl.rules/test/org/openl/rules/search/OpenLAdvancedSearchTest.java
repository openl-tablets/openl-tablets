package org.openl.rules.search;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.BaseOpenlBuilder;
import org.openl.rules.search.ISearchConstants;
import org.openl.rules.search.OpenLAdvancedSearchResult.TableAndRows;

public class OpenLAdvancedSearchTest extends BaseOpenlBuilder{
    
    private String __src = "test/rules/Tutorial_4_Test.xls";
    
    private OpenLAdvancedSearch search = new OpenLAdvancedSearch();
    
    @Before
    public void buildOpenlWrapper() {
        buildXlsModuleSyntaxNode(__src);
    }
    
    @Test 
    public void testTableSearch() {      
        SearchConditionElement searchElem =  new SearchConditionElement(ISearchConstants.HEADER);
        searchElem.setNotFlag(false);        
        searchElem.setElementValueName("--ANY--");
        searchElem.setOpType2("contains");
        searchElem.setElementValue("Driver");
        
        SearchConditionElement[] tableElements = {searchElem};
        search.selectTableType(0, true);
        search.setTableElements(tableElements);
        Object searchResult = search.search(getModuleSuntaxNode());
        if((searchResult != null) && (searchResult instanceof OpenLAdvancedSearchResult)) {
            assertTrue(true);   
            assertTrue(8 == ((OpenLAdvancedSearchResult)searchResult).tablesAndRows().length);
            for(TableAndRows tabAndRows : ((OpenLAdvancedSearchResult)searchResult).tablesAndRows()) {
                assertTrue(tabAndRows.getRows().length>0);
            }
            
        } else {
            fail();
        }        
    }
    
    @Test 
    public void testPropertySearch() {      
        SearchConditionElement searchElem =  new SearchConditionElement(ISearchConstants.PROPERTY);
        searchElem.setNotFlag(false);        
        searchElem.setElementValueName("--ANY--");
        searchElem.setOpType2("contains");
        searchElem.setElementValue("Driver");
        
        SearchConditionElement[] tableElements = {searchElem};
        search.selectTableType(0, true);
        search.setTableElements(tableElements);
        Object searchResult = search.search(getModuleSuntaxNode());
        if((searchResult != null) && (searchResult instanceof OpenLAdvancedSearchResult)) {
            assertTrue(true);   
            assertTrue(11 == ((OpenLAdvancedSearchResult)searchResult).tablesAndRows().length);
            for(TableAndRows tabAndRows : ((OpenLAdvancedSearchResult)searchResult).tablesAndRows()) {
                assertTrue(tabAndRows.getRows().length>0);
            }
            
        } else {
            fail();
        }        
    }
    
    @Test 
    public void testAllSearch() {         
        SearchConditionElement searchTableElem =  new SearchConditionElement(ISearchConstants.HEADER);
        searchTableElem.setNotFlag(false);        
        searchTableElem.setElementValueName("--ANY--");
        searchTableElem.setOpType2("contains");
        searchTableElem.setElementValue("--ANY--");
        
        SearchConditionElement columnSearchElem =  new SearchConditionElement(ISearchConstants.COLUMN_PARAMETER);
        columnSearchElem.setNotFlag(false); 
        columnSearchElem.setOpType1("contains");
        columnSearchElem.setElementValueName("--ANY--");
        columnSearchElem.setOpType2("contains");
        columnSearchElem.setElementValue("--ANY--");
        
        SearchConditionElement[] tableElements = {searchTableElem};
        SearchConditionElement[] columnElements = {columnSearchElem};
        search.selectTableType(0, true);
        search.setTableElements(tableElements);
        search.setColumnElements(columnElements);
        Object searchResult = search.search(getModuleSuntaxNode());
        if((searchResult != null) && (searchResult instanceof OpenLAdvancedSearchResult)) {
            assertTrue(true);   
            assertTrue(31 == ((OpenLAdvancedSearchResult)searchResult).tablesAndRows().length);
            for(TableAndRows tabAndRows : ((OpenLAdvancedSearchResult)searchResult).tablesAndRows()) {
                assertTrue(tabAndRows.getRows().length>0);
            }
        } else {
            fail();
        }        
    }
    
    @Test 
    public void testColumnSearch() {
        SearchConditionElement columnSearchElem =  new SearchConditionElement(ISearchConstants.COLUMN_PARAMETER);
        columnSearchElem.setNotFlag(false); 
        columnSearchElem.setOpType1("contains");
        columnSearchElem.setElementValueName("--ANY--");
        columnSearchElem.setOpType2("contains");
        columnSearchElem.setElementValue("Moderate");
        
        
        SearchConditionElement[] columnElements = {columnSearchElem};
        search.selectTableType(0, true);
        
        search.setColumnElements(columnElements);
        Object searchResult = search.search(getModuleSuntaxNode());
        if((searchResult != null) && (searchResult instanceof OpenLAdvancedSearchResult)) {
            assertTrue(true);   
            OpenLAdvancedSearchResult result = ((OpenLAdvancedSearchResult)searchResult);
            assertTrue(31 == result.tablesAndRows().length);
            
            for(int i = 0; i<2; i++) {
                assertTrue(result.tablesAndRows()[i].getRows().length > 0);
                if(i==0) {
                    assertEquals("Theft Rating Table", result.tablesAndRows()[i].tsn.getTableProperties().getName());
                } else {
                    assertEquals("Injury Rating Table", result.tablesAndRows()[i].tsn.getTableProperties().getName());
                }
            }
            for(int i = 2; i<result.tablesAndRows().length; i++) {
                assertTrue(((OpenLAdvancedSearchResult)searchResult).tablesAndRows()[i].getRows().length == 0);
            }
        } else {
            fail();
        }        
    }
    
    @Test 
    public void testColumnSearch1() {
        SearchConditionElement columnSearchElem =  new SearchConditionElement(ISearchConstants.COLUMN_PARAMETER);
        columnSearchElem.setNotFlag(false); 
        columnSearchElem.setOpType1("contains");
        columnSearchElem.setElementValueName("airbags");
        columnSearchElem.setOpType2("contains");
        columnSearchElem.setElementValue("Driver");
        
        
        SearchConditionElement[] columnElements = {columnSearchElem};
        search.selectTableType(0, true);
        
        search.setColumnElements(columnElements);
        Object searchResult = search.search(getModuleSuntaxNode());
        if((searchResult != null) && (searchResult instanceof OpenLAdvancedSearchResult)) {
            assertTrue(true);   
            OpenLAdvancedSearchResult result = ((OpenLAdvancedSearchResult)searchResult);
            assertTrue(31 == result.tablesAndRows().length);
            
            for(int i = 0; i<2; i++) {
                assertTrue(result.tablesAndRows()[i].getRows().length > 0);
                if(i==0) {
                    assertEquals("Injury Rating Table", (String)result.tablesAndRows()[i].tsn.getTableProperties().getName());
                } else {
                    assertEquals("Vehicle Discounts", (String)result.tablesAndRows()[i].tsn.getTableProperties().getName());
                }
            }
            for(int i = 2; i<result.tablesAndRows().length; i++) {
                assertTrue(((OpenLAdvancedSearchResult)searchResult).tablesAndRows()[i].getRows().length == 0);
            }
        } else {
            fail();
        }        
    }

}
