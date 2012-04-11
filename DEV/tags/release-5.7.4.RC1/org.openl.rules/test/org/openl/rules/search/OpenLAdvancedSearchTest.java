package org.openl.rules.search;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.search.ISearchConstants;
import org.openl.rules.search.OpenLAdvancedSearchResult.TableAndRows;

public class OpenLAdvancedSearchTest extends BaseOpenlBuilderHelper{
    
    private static String __src = "test/rules/Tutorial_4_Test.xls";
    
    public OpenLAdvancedSearchTest() {
        super(__src);        
    }    
    private OpenLAdvancedSearch search = new OpenLAdvancedSearch();
    
    @Test
    public void testTableSearch() {      
        SearchConditionElement searchElem =  new SearchConditionElement(ISearchConstants.HEADER);
        searchElem.setNotFlag(false);        
        searchElem.setOpType2("contains");
        searchElem.setElementValue("Driver");
        
        SearchConditionElement[] tableElements = {searchElem};
        search.selectTableType(0, true);
        search.setTableElements(tableElements);
        Object searchResult = search.search(getModuleSuntaxNode());
        if((searchResult != null) && (searchResult instanceof OpenLAdvancedSearchResult)) {
            assertTrue(true);
            assertTrue(8 == ((OpenLAdvancedSearchResult)searchResult).getFoundTableAndRows().length);
            for(TableAndRows tabAndRows : ((OpenLAdvancedSearchResult)searchResult).getFoundTableAndRows()) {
                assertTrue(tabAndRows.getRows().length > 0);
            }
            
        } else {
            fail();
        }        
    }
    
    @Test 
    public void testPropertySearch() {      
        SearchConditionElement searchElem =  new SearchConditionElement(ISearchConstants.PROPERTY);
        searchElem.setNotFlag(false);        
        searchElem.setOpType2("contains");
        searchElem.setElementValue("Driver");
        
        SearchConditionElement[] tableElements = {searchElem};
        search.selectTableType(0, true);
        search.setTableElements(tableElements);
        Object searchResult = search.search(getModuleSuntaxNode());
        if((searchResult != null) && (searchResult instanceof OpenLAdvancedSearchResult)) {
            assertTrue(true);   
            assertTrue(11 == ((OpenLAdvancedSearchResult)searchResult).getFoundTableAndRows().length);
            for(TableAndRows tabAndRows : ((OpenLAdvancedSearchResult)searchResult).getFoundTableAndRows()) {
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
        searchTableElem.setOpType2("contains");
        
        SearchConditionElement columnSearchElem =  new SearchConditionElement(ISearchConstants.COLUMN_PARAMETER);
        columnSearchElem.setNotFlag(false); 
        columnSearchElem.setOpType1("contains");
        columnSearchElem.setOpType2("contains");
        
        SearchConditionElement[] tableElements = {searchTableElem};
        SearchConditionElement[] columnElements = {columnSearchElem};
        search.selectTableType(0, true);
        search.setTableElements(tableElements);
        search.setColumnElements(columnElements);
        Object searchResult = search.search(getModuleSuntaxNode());
        if((searchResult != null) && (searchResult instanceof OpenLAdvancedSearchResult)) {
            assertTrue(true);   
            assertTrue(31 == ((OpenLAdvancedSearchResult)searchResult).getFoundTableAndRows().length);
            for(TableAndRows tabAndRows : ((OpenLAdvancedSearchResult)searchResult).getFoundTableAndRows()) {
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
        columnSearchElem.setOpType2("contains");
        columnSearchElem.setElementValue("Moderate");
        
        
        SearchConditionElement[] columnElements = {columnSearchElem};
        search.selectTableType(0, true);
        
        search.setColumnElements(columnElements);
        Object searchResult = search.search(getModuleSuntaxNode());
        if((searchResult != null) && (searchResult instanceof OpenLAdvancedSearchResult)) {
            assertTrue(true);   
            OpenLAdvancedSearchResult result = ((OpenLAdvancedSearchResult)searchResult);
            assertTrue(31 == result.getFoundTableAndRows().length);
            
            for(int i = 0; i<2; i++) {
                assertTrue(result.getFoundTableAndRows()[i].getRows().length > 0);
                if(i==0) {
                    assertEquals("Theft Rating Table", result.getFoundTableAndRows()[i].getTsn().getTableProperties().getName());
                } else {
                    assertEquals("Injury Rating Table", result.getFoundTableAndRows()[i].getTsn().getTableProperties().getName());
                }
            }
            for(int i = 2; i<result.getFoundTableAndRows().length; i++) {
                assertTrue(((OpenLAdvancedSearchResult)searchResult).getFoundTableAndRows()[i].getRows().length == 0);
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
            assertTrue(31 == result.getFoundTableAndRows().length);
            
            for(int i = 0; i<2; i++) {
                assertTrue(result.getFoundTableAndRows()[i].getRows().length > 0);
                if(i==0) {
                    assertEquals("Injury Rating Table", (String)result.getFoundTableAndRows()[i].getTsn().getTableProperties().getName());
                } else {
                    assertEquals("Vehicle Discounts", (String)result.getFoundTableAndRows()[i].getTsn().getTableProperties().getName());
                }
            }
            for(int i = 2; i<result.getFoundTableAndRows().length; i++) {
                assertTrue(((OpenLAdvancedSearchResult)searchResult).getFoundTableAndRows()[i].getRows().length == 0);
            }
        } else {
            fail();
        }        
    }

}
