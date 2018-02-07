package org.openl.rules.binding;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openl.binding.impl.NodeUsage;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.ICell;

public class NodeUsagesMetaInfoTest extends BaseOpenlBuilderHelper {
    private static final String SRC = "test/rules/binding/NodeUsagesMetaInfoTest.xls";
    private TableSyntaxNode dataA;
    private TableSyntaxNode dataB;
    private TableSyntaxNode dataC;
    private TableSyntaxNode carType;
    private TableSyntaxNode typeB;
    private TableSyntaxNode typeC;
    private TableSyntaxNode rule1;
    private TableSyntaxNode rule2;
    private TableSyntaxNode convert;
    private TableSyntaxNode method1;
    private TableSyntaxNode assetsCompare;
    private TableSyntaxNode totalAssets;
    private TableSyntaxNode miscAssets;

    public NodeUsagesMetaInfoTest() {
        super(SRC);
    }

    @Before
    public void setUp() throws Exception {
        dataA = findTable("Data String dataA");
        dataB = findTable("Data TypeB dataB");
        dataC = findTable("Data TypeC dataC");
        carType = findTable("Datatype CarType <String>");
        typeB = findTable("Datatype TypeB");
        typeC = findTable("Datatype TypeC extends TypeB");
        rule1 = findTable("Rules TypeB rule1(TypeB typeB, TypeC typeC)");
        rule2 = findTable("Rules TypeB rule2(TypeB typeB, TypeC typeC)");
        convert = findTable("Method TypeB[][] convert(TypeC[][] param)");
        method1 = findTable("Method String method1(TypeC[][] param)");

        assetsCompare = findTable("Spreadsheet SpreadsheetResult AssetsCompare ()");
        totalAssets = findTable("Spreadsheet SpreadsheetResult TotalAssets ()");
        miscAssets = findTable("Spreadsheet SpreadsheetResult MiscAssets (SpreadsheetResultTotalAssets totalAssets1, SpreadsheetResult totalAssets2)");
    }

    @Test
    public void testDataTableReference() {
        // Reference in dataB table points to dataA
        ICell referenceCell = dataB.getGridTable().getCell(0, 2);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(referenceCell));
        assertEquals(dataA.getUri(), referenceCell.getMetaInfo().getUsedNodes().get(0).getUri());

        // Reference in empty dataC table points to dataA
        ICell referenceCellDataA = dataC.getGridTable().getCell(0, 2);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(referenceCellDataA));
        assertEquals(dataA.getUri(), referenceCellDataA.getMetaInfo().getUsedNodes().get(0).getUri());

        // Reference in empty dataC table points to dataB
        ICell referenceCellDataB = dataC.getGridTable().getCell(1, 2);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(referenceCellDataB));
        assertEquals(dataB.getUri(), referenceCellDataB.getMetaInfo().getUsedNodes().get(0).getUri());
    }

    @Test
    public void testDataTypeNodeInDataTable() throws Exception {
        ICell dataDeclarationCell = dataB.getGridTable().getCell(0, 0);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(dataDeclarationCell));
        assertEquals(typeB.getUri(), dataDeclarationCell.getMetaInfo().getUsedNodes().get(0).getUri());
    }

    @Test
    public void testDataTypeTable() throws Exception {
        ICell dataTypeDeclarationCell = typeC.getGridTable().getCell(0, 0);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(dataTypeDeclarationCell));
        assertEquals(typeB.getUri(), dataTypeDeclarationCell.getMetaInfo().getUsedNodes().get(0).getUri());

        ICell bField = typeC.getGridTable().getCell(0, 1);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(bField));
        assertEquals(typeB.getUri(), bField.getMetaInfo().getUsedNodes().get(0).getUri());

        ICell bArray = typeC.getGridTable().getCell(0, 2);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(bArray));
        NodeUsage typeBNodeUsage = bArray.getMetaInfo().getUsedNodes().get(0);
        assertEquals(typeB.getUri(), typeBNodeUsage.getUri());
        assertEquals(0, typeBNodeUsage.getStart());
        assertEquals(4, typeBNodeUsage.getEnd());

        ICell carTypes = typeC.getGridTable().getCell(0, 3);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(carTypes));
        NodeUsage carTypeNodeUsage = carTypes.getMetaInfo().getUsedNodes().get(0);
        assertEquals(carType.getUri(), carTypeNodeUsage.getUri());
        assertEquals(0, carTypeNodeUsage.getStart());
        assertEquals(6, carTypeNodeUsage.getEnd());
    }

    @Test
    public void testLinksInDecisionTableHeader() throws Exception {
        ICell header = rule1.getGridTable().getCell(0, 0);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(header));

        List<? extends NodeUsage> usedNodes = header.getMetaInfo().getUsedNodes();
        assertEquals(3, usedNodes.size());
        assertEquals(typeB.getUri(), usedNodes.get(0).getUri());
        assertEquals(typeB.getUri(), usedNodes.get(1).getUri());
        assertEquals(typeC.getUri(), usedNodes.get(2).getUri());

        ICell condition2 = rule1.getGridTable().getCell(1, 3);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(condition2));
        usedNodes = condition2.getMetaInfo().getUsedNodes();
        assertEquals(1, usedNodes.size());
        assertEquals(carType.getUri(), usedNodes.get(0).getUri());

        ICell returnCell = rule1.getGridTable().getCell(2, 3);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(returnCell));
        usedNodes = returnCell.getMetaInfo().getUsedNodes();
        assertEquals(1, usedNodes.size());
        assertEquals(typeB.getUri(), usedNodes.get(0).getUri());
    }

    @Test
    public void testLinksInMethodTableHeader() throws Exception {
        ICell header = convert.getGridTable().getCell(0, 0);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(header));

        List<? extends NodeUsage> usedNodes = header.getMetaInfo().getUsedNodes();
        assertEquals(2, usedNodes.size());

        assertEquals(typeB.getUri(), usedNodes.get(0).getUri());
        assertEquals(7, usedNodes.get(0).getStart());
        assertEquals(11, usedNodes.get(0).getEnd());

        assertEquals(typeC.getUri(), usedNodes.get(1).getUri());
        assertEquals(25, usedNodes.get(1).getStart());
        assertEquals(29, usedNodes.get(1).getEnd());
    }

    @Test
    public void testForFieldUsageInDecisionTable() {
        // First condition
        ICell condition1 = rule2.getGridTable().getCell(0, 2);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(condition1));

        List<? extends NodeUsage> usedNodes = condition1.getMetaInfo().getUsedNodes();
        assertEquals(2, usedNodes.size());

        assertEquals(typeC.getUri(), usedNodes.get(0).getUri());
        assertEquals(0, usedNodes.get(0).getStart());
        assertEquals(5, usedNodes.get(0).getEnd());

        assertEquals(typeB.getUri(), usedNodes.get(1).getUri());
        assertEquals(7, usedNodes.get(1).getStart());
        assertEquals(9, usedNodes.get(1).getEnd());

        // Second condition
        ICell condition2 = rule2.getGridTable().getCell(1, 2);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(condition2));

        usedNodes = condition2.getMetaInfo().getUsedNodes();
        assertEquals(1, usedNodes.size());

        assertEquals(typeC.getUri(), usedNodes.get(0).getUri());
        assertEquals(0, usedNodes.get(0).getStart());
        assertEquals(7, usedNodes.get(0).getEnd());
    }

    @Test
    public void testForMixedNodeUsageInMethodTable() {
        // First line of method body
        ICell actionCell = method1.getGridTable().getCell(0, 1);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(actionCell));

        List<? extends NodeUsage> usedNodes = actionCell.getMetaInfo().getUsedNodes();
        assertEquals(2, usedNodes.size());

        assertEquals(dataB.getUri(), usedNodes.get(0).getUri());
        assertEquals(20, usedNodes.get(0).getStart());
        assertEquals(24, usedNodes.get(0).getEnd());

        assertEquals(typeB.getUri(), usedNodes.get(1).getUri());
        assertEquals(29, usedNodes.get(1).getStart());
        assertEquals(31, usedNodes.get(1).getEnd());

        // Return cell
        ICell returnCell = method1.getGridTable().getCell(0, 2);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(returnCell));

        usedNodes = returnCell.getMetaInfo().getUsedNodes();
        assertEquals(4, usedNodes.size());

        assertEquals(typeB.getUri(), usedNodes.get(0).getUri());
        assertEquals(24, usedNodes.get(0).getStart());
        assertEquals(26, usedNodes.get(0).getEnd());

        assertEquals(convert.getUri(), usedNodes.get(1).getUri());
        assertEquals(30, usedNodes.get(1).getStart());
        assertEquals(36, usedNodes.get(1).getEnd());

        assertEquals(typeC.getUri(), usedNodes.get(2).getUri());
        assertEquals(38, usedNodes.get(2).getStart());
        assertEquals(42, usedNodes.get(2).getEnd());

        assertEquals(typeB.getUri(), usedNodes.get(3).getUri());
        assertEquals(51, usedNodes.get(3).getStart());
        assertEquals(53, usedNodes.get(3).getEnd());
    }

    /**
     * This method tests:
     * <ol>
     *     <li>Description and url for CustomSpreadsheetResult type</li>
     *     <li>Description above '=' symbol</li>
     *     <li>Reference to other spreadsheet</li>
     *     <li>Description for the field of <i>other</i> custom spreadsheet result</li>
     *     <li>Description for the field of <i>current</i> spreadsheet referenced by <i>column name</i> only</li>
     *     <li>Description for the field of <i>current</i> spreadsheet referenced by <i>column name and row name</i></li>
     * </ol>
     */
    @Test
    public void testDescriptionInSpreadsheetAssetsCompare() {
        List<? extends NodeUsage> usedNodes;

        // Variable declaration: "AssetsCalc2012 : SpreadsheetResultTotalAssets"
        usedNodes = assetsCompare.getGridTable().getCell(0, 2).getMetaInfo().getUsedNodes();
        assertEquals("Spreadsheet TotalAssets", usedNodes.get(0).getDescription());
        assertEquals(totalAssets.getUri(), usedNodes.get(0).getUri());

        // AssetsCalc2012
        usedNodes = assetsCompare.getGridTable().getCell(1, 2).getMetaInfo().getUsedNodes();
        assertEquals(2, usedNodes.size());
        // '=' symbol
        assertEquals("Cell type: SpreadsheetResultTotalAssets", usedNodes.get(0).getDescription());
        // 'TotalAssets' method
        assertEquals(totalAssets.getUri(), usedNodes.get(1).getUri());
        assertEquals("SpreadsheetResultTotalAssets TotalAssets()", usedNodes.get(1).getDescription());

        // TotalAssets2012
        usedNodes = assetsCompare.getGridTable().getCell(1, 3).getMetaInfo().getUsedNodes();
        assertEquals(3, usedNodes.size());
        assertEquals("Cell type: Long", usedNodes.get(0).getDescription()); // =
        assertEquals("SpreadsheetResultTotalAssets $AssetsCalc2012", usedNodes.get(1).getDescription()); // $AssetsCalc2012
        assertEquals("Spreadsheet TotalAssets\nLong $USDValue$Total", usedNodes.get(2).getDescription()); // $USDValue$Total (other spreadsheet)

        // TotalAssets2011
        assertFalse(CellMetaInfo.isCellContainsNodeUsages(assetsCompare.getGridTable().getCell(1, 4)));

        // Change in %
        usedNodes = assetsCompare.getGridTable().getCell(1, 5).getMetaInfo().getUsedNodes();
        assertEquals(4, usedNodes.size());
        assertEquals("Cell type: Double", usedNodes.get(0).getDescription()); // =
        assertEquals("Long $TotalAssets2012", usedNodes.get(1).getDescription()); // $TotalAssets2012
        assertEquals("Double $TotalAssets2011", usedNodes.get(2).getDescription()); // $TotalAssets2011
        assertEquals("Double $Value$TotalAssets2011", usedNodes.get(3).getDescription()); // $Value$TotalAssets2011
    }

    /**
     * This method tests:
     * <ol>
     *     <li>Description for the field of <i>current</i> spreadsheet referenced by <i>row name</i> only</li>
     *     <li>Description for cell ranges</li>
     * </ol>
     */
    @Test
    public void testDescriptionInSpreadsheetTotalAssets() {
        List<? extends NodeUsage> usedNodes;

        // USD
        usedNodes = totalAssets.getGridTable().getCell(3, 2).getMetaInfo().getUsedNodes();
        assertEquals(3, usedNodes.size());
        assertEquals("Cell type: Long", usedNodes.get(0).getDescription()); // =
        assertEquals("Double $Amount", usedNodes.get(1).getDescription()); // Amount
        assertEquals("Double $Exchange Rate", usedNodes.get(2).getDescription()); // $Exchange Rate

        // Total
        usedNodes = totalAssets.getGridTable().getCell(3, 7).getMetaInfo().getUsedNodes();
        assertEquals(2, usedNodes.size());
        assertEquals("Cell type: Long", usedNodes.get(0).getDescription()); // =
        assertEquals("Long[] $USD:$GLD", usedNodes.get(1).getDescription()); // $USD:$GLD (cell range)
    }

    /**
     * This method tests:
     * <ol>
     *     <li>Link to other custom spreadsheet table from the field exists</li>
     *     <li>Description for the field of other non-custom spreadsheet result</li>
     * </ol>
     */
    @Test
    public void testDescriptionInSpreadsheetMiscAssets() {
        List<? extends NodeUsage> usedNodes;

        // TotalAssets1
        usedNodes = miscAssets.getGridTable().getCell(1, 2).getMetaInfo().getUsedNodes();
        assertEquals(3, usedNodes.size());
        assertEquals("Cell type: Long", usedNodes.get(0).getDescription()); // =
        assertNull(usedNodes.get(0).getUri());
        assertEquals("SpreadsheetResultTotalAssets totalAssets1", usedNodes.get(1).getDescription()); // $AssetsCalc2012
        assertEquals(totalAssets.getUri(), usedNodes.get(1).getUri());
        assertEquals("Spreadsheet TotalAssets\nLong $USDValue$Total", usedNodes.get(2).getDescription()); // $USDValue$Total (other spreadsheet)
        assertEquals(totalAssets.getUri(), usedNodes.get(2).getUri());

        // TotalAssets2
        usedNodes = miscAssets.getGridTable().getCell(1, 3).getMetaInfo().getUsedNodes();
        assertEquals(3, usedNodes.size());
        assertEquals("Cell type: Object", usedNodes.get(0).getDescription()); // =
        assertEquals("SpreadsheetResult totalAssets2", usedNodes.get(1).getDescription()); // totalAssets2
        assertEquals("Spreadsheet\nObject $USDValue$Total", usedNodes.get(2).getDescription()); // $USDValue$Total (other spreadsheet)
    }
}
