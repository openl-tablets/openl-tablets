package org.openl.rules.binding;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openl.binding.impl.NodeUsage;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;

public class NodeUsagesMetaInfoTest extends BaseOpenlBuilderHelper {
    private static final String SRC = "test/rules/binding/NodeUsagesMetaInfoTest.xls";
    private TableSyntaxNode dataA;
    private TableSyntaxNode dataB;
    private TableSyntaxNode dataC;
    private TableSyntaxNode carType;
    private TableSyntaxNode typeB;
    private TableSyntaxNode typeC;
    private TableSyntaxNode typeCTransposed;
    private TableSyntaxNode rule1;
    private TableSyntaxNode rule2;
    private TableSyntaxNode convert;
    private TableSyntaxNode method1;
    private TableSyntaxNode assetsCompare;
    private TableSyntaxNode totalAssets;
    private TableSyntaxNode miscAssets;
    private MetaInfoReader dataBMetaReader;
    private MetaInfoReader dataCMetaReader;
    private MetaInfoReader typeCMetaReader;
    private MetaInfoReader typeCTransposedMetaReader;
    private MetaInfoReader rule1MetaReader;
    private MetaInfoReader convertMetaReader;
    private MetaInfoReader rule2MetaReader;
    private MetaInfoReader method1MetaReader;
    private MetaInfoReader assetsCompareMetaReader;

    public NodeUsagesMetaInfoTest() {
        super(SRC);
    }

    @Before
    public void setUp() {
        dataA = findTable("Data String dataA");
        dataB = findTable("Data TypeB dataB");
        dataC = findTable("Data TypeC dataC");
        carType = findTable("Datatype CarType <String>");
        typeB = findTable("Datatype TypeB");
        typeC = findTable("Datatype TypeC extends TypeB");
        typeCTransposed = findTable("Datatype TypeCTransposed extends TypeB");
        rule1 = findTable("Rules TypeB rule1(TypeB typeB, TypeC typeC)");
        rule2 = findTable("Rules TypeB rule2(TypeB typeB, TypeC typeC)");
        convert = findTable("Method TypeB[][] convert(TypeC[][] param)");
        method1 = findTable("Method String method1(TypeC[][] param)");

        assetsCompare = findTable("Spreadsheet SpreadsheetResult AssetsCompare ()");
        totalAssets = findTable("Spreadsheet SpreadsheetResult TotalAssets ()");
        miscAssets = findTable(
            "Spreadsheet SpreadsheetResult MiscAssets (SpreadsheetResultTotalAssets totalAssets1, SpreadsheetResult totalAssets2)");

        dataBMetaReader = dataB.getMetaInfoReader();
        dataCMetaReader = dataC.getMetaInfoReader();
        typeCMetaReader = typeC.getMetaInfoReader();
        typeCTransposedMetaReader = typeCTransposed.getMetaInfoReader();
        rule1MetaReader = rule1.getMetaInfoReader();
        rule2MetaReader = rule2.getMetaInfoReader();
        convertMetaReader = convert.getMetaInfoReader();
        method1MetaReader = method1.getMetaInfoReader();
        assetsCompareMetaReader = assetsCompare.getMetaInfoReader();
    }

    @Test
    public void testDataTableReference() {
        // Reference in dataB table points to dataA
        ICell referenceCell = dataB.getGridTable().getCell(0, 2);
        CellMetaInfo referenceCellMeta = getMetaInfo(dataBMetaReader, referenceCell);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(referenceCellMeta));
        assertEquals(dataA.getUri(), referenceCellMeta.getUsedNodes().get(0).getUri());

        // Reference in empty dataC table points to dataA
        ICell referenceCellDataA = dataC.getGridTable().getCell(0, 2);
        CellMetaInfo referenceCellDataAMeta = getMetaInfo(dataCMetaReader, referenceCellDataA);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(referenceCellDataAMeta));
        assertEquals(dataA.getUri(), referenceCellDataAMeta.getUsedNodes().get(0).getUri());

        // Reference in empty dataC table points to dataB
        ICell referenceCellDataB = dataC.getGridTable().getCell(1, 2);
        CellMetaInfo referenceCellDataBMeta = getMetaInfo(dataCMetaReader, referenceCellDataB);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(referenceCellDataBMeta));
        assertEquals(dataB.getUri(), referenceCellDataBMeta.getUsedNodes().get(0).getUri());
    }

    @Test
    public void testDataTypeNodeInDataTable() {
        ICell dataDeclarationCell = dataB.getGridTable().getCell(0, 0);
        CellMetaInfo dataDeclarationCellMeta = getMetaInfo(dataBMetaReader, dataDeclarationCell);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(dataDeclarationCellMeta));
        assertEquals(typeB.getUri(), dataDeclarationCellMeta.getUsedNodes().get(0).getUri());
    }

    @Test
    public void testDataTypeTable() {
        ICell dataTypeDeclarationCell = typeC.getGridTable().getCell(0, 0);
        CellMetaInfo dataTypeDeclarationCellMeta = getMetaInfo(typeCMetaReader, dataTypeDeclarationCell);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(dataTypeDeclarationCellMeta));
        assertEquals(typeB.getUri(), dataTypeDeclarationCellMeta.getUsedNodes().get(0).getUri());

        ICell bField = typeC.getGridTable().getCell(0, 1);
        CellMetaInfo bFieldMeta = getMetaInfo(typeCMetaReader, bField);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(bFieldMeta));
        assertEquals(typeB.getUri(), bFieldMeta.getUsedNodes().get(0).getUri());

        ICell bArray = typeC.getGridTable().getCell(0, 2);
        CellMetaInfo bArrayMeta = getMetaInfo(typeCMetaReader, bArray);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(bArrayMeta));
        NodeUsage typeBNodeUsage = bArrayMeta.getUsedNodes().get(0);
        assertEquals(typeB.getUri(), typeBNodeUsage.getUri());
        assertEquals(0, typeBNodeUsage.getStart());
        assertEquals(4, typeBNodeUsage.getEnd());

        ICell carTypes = typeC.getGridTable().getCell(0, 3);
        CellMetaInfo carTypesMeta = getMetaInfo(typeCMetaReader, carTypes);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(carTypesMeta));
        NodeUsage carTypeNodeUsage = carTypesMeta.getUsedNodes().get(0);
        assertEquals(carType.getUri(), carTypeNodeUsage.getUri());
        assertEquals(0, carTypeNodeUsage.getStart());
        assertEquals(6, carTypeNodeUsage.getEnd());
    }

    @Test
    public void testLinksInDecisionTableHeader() {
        ICell header = rule1.getGridTable().getCell(0, 0);
        CellMetaInfo headerMeta = getMetaInfo(rule1MetaReader, header);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(headerMeta));

        List<? extends NodeUsage> usedNodes = headerMeta.getUsedNodes();
        assertEquals(3, usedNodes.size());
        assertEquals(typeB.getUri(), usedNodes.get(0).getUri());
        assertEquals(typeB.getUri(), usedNodes.get(1).getUri());
        assertEquals(typeC.getUri(), usedNodes.get(2).getUri());

        ICell condition2 = rule1.getGridTable().getCell(1, 3);
        CellMetaInfo condition2Meta = getMetaInfo(rule1MetaReader, condition2);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(condition2Meta));
        usedNodes = condition2Meta.getUsedNodes();
        assertEquals(1, usedNodes.size());
        assertEquals(carType.getUri(), usedNodes.get(0).getUri());

        ICell returnCell = rule1.getGridTable().getCell(2, 3);
        CellMetaInfo returnCellMeta = getMetaInfo(rule1MetaReader, returnCell);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(returnCellMeta));
        usedNodes = returnCellMeta.getUsedNodes();
        assertEquals(1, usedNodes.size());
        assertEquals(typeB.getUri(), usedNodes.get(0).getUri());

        ICell ruleCondition2 = rule1.getGridTable().getCell(1, 5);
        CellMetaInfo ruleCondition2Meta = getMetaInfo(rule1MetaReader, ruleCondition2);
        assertFalse(CellMetaInfo.isCellContainsNodeUsages(ruleCondition2Meta));
        assertEquals("CarType", ruleCondition2Meta.getDataType().getName());
        assertFalse(ruleCondition2Meta.isMultiValue());
    }

    @Test
    public void testLinksInMethodTableHeader() {
        ICell header = convert.getGridTable().getCell(0, 0);
        CellMetaInfo headerMeta = getMetaInfo(convertMetaReader, header);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(headerMeta));

        List<? extends NodeUsage> usedNodes = headerMeta.getUsedNodes();
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
        CellMetaInfo condition1Meta = getMetaInfo(rule2MetaReader, condition1);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(condition1Meta));

        List<? extends NodeUsage> usedNodes = condition1Meta.getUsedNodes();
        assertEquals(2, usedNodes.size());

        assertEquals(typeC.getUri(), usedNodes.get(0).getUri());
        assertEquals(0, usedNodes.get(0).getStart());
        assertEquals(5, usedNodes.get(0).getEnd());

        assertEquals(typeB.getUri(), usedNodes.get(1).getUri());
        assertEquals(7, usedNodes.get(1).getStart());
        assertEquals(9, usedNodes.get(1).getEnd());

        // Second condition
        ICell condition2 = rule2.getGridTable().getCell(1, 2);
        CellMetaInfo condition2Meta = getMetaInfo(rule2MetaReader, condition2);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(condition2Meta));

        usedNodes = condition2Meta.getUsedNodes();
        assertEquals(1, usedNodes.size());

        assertEquals(typeC.getUri(), usedNodes.get(0).getUri());
        assertEquals(0, usedNodes.get(0).getStart());
        assertEquals(7, usedNodes.get(0).getEnd());

        // test return cell
        ICell ret = rule2.getGridTable().getCell(2, 2);
        CellMetaInfo retMeta = getMetaInfo(rule2MetaReader, ret);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(retMeta));

        usedNodes = retMeta.getUsedNodes();
        assertEquals(2, usedNodes.size());

        assertEquals(typeB.getUri(), usedNodes.get(0).getUri());
        assertEquals("TypeB <init>(String aaa)", usedNodes.get(0).getDescription());
        assertEquals(4, usedNodes.get(0).getStart());
        assertEquals(8, usedNodes.get(0).getEnd());

        assertNull(usedNodes.get(1).getUri());
        assertEquals("String res", usedNodes.get(1).getDescription());
        assertEquals(10, usedNodes.get(1).getStart());
        assertEquals(12, usedNodes.get(1).getEnd());
    }

    @Test
    public void testForMixedNodeUsageInMethodTable() {
        // First line of method body
        ICell actionCell = method1.getGridTable().getCell(0, 1);
        CellMetaInfo actionCellMeta = getMetaInfo(method1MetaReader, actionCell);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(actionCellMeta));

        List<? extends NodeUsage> usedNodes = actionCellMeta.getUsedNodes();
        assertEquals(2, usedNodes.size());

        assertEquals(dataB.getUri(), usedNodes.get(0).getUri());
        assertEquals(20, usedNodes.get(0).getStart());
        assertEquals(24, usedNodes.get(0).getEnd());

        assertEquals(typeB.getUri(), usedNodes.get(1).getUri());
        assertEquals(29, usedNodes.get(1).getStart());
        assertEquals(31, usedNodes.get(1).getEnd());

        // Return cell
        ICell returnCell = method1.getGridTable().getCell(0, 2);
        CellMetaInfo returnCellMeta = getMetaInfo(method1MetaReader, returnCell);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(returnCellMeta));

        usedNodes = returnCellMeta.getUsedNodes();
        assertEquals(5, usedNodes.size());

        // the first node it's constructor
        assertEquals(typeB.getUri(), usedNodes.get(0).getUri());
        assertEquals("TypeB <init>(String aaa)", usedNodes.get(0).getDescription());
        assertEquals(11, usedNodes.get(0).getStart());
        assertEquals(15, usedNodes.get(0).getEnd());

        assertEquals(typeB.getUri(), usedNodes.get(1).getUri());
        assertEquals("Datatype TypeB\nString aaa", usedNodes.get(1).getDescription());
        assertEquals(24, usedNodes.get(1).getStart());
        assertEquals(26, usedNodes.get(1).getEnd());

        assertEquals(convert.getUri(), usedNodes.get(2).getUri());
        assertEquals(30, usedNodes.get(2).getStart());
        assertEquals(36, usedNodes.get(2).getEnd());

        assertEquals(typeC.getUri(), usedNodes.get(3).getUri());
        assertEquals(38, usedNodes.get(3).getStart());
        assertEquals(42, usedNodes.get(3).getEnd());

        assertEquals(typeB.getUri(), usedNodes.get(4).getUri());
        assertEquals(51, usedNodes.get(4).getStart());
        assertEquals(53, usedNodes.get(4).getEnd());
    }

    /**
     * This method tests:
     * <ol>
     * <li>Description and url for CustomSpreadsheetResult type</li>
     * <li>Description above '=' symbol</li>
     * <li>Reference to other spreadsheet</li>
     * <li>Description for the field of <i>other</i> custom spreadsheet result</li>
     * <li>Description for the field of <i>current</i> spreadsheet referenced by <i>column name</i> only</li>
     * <li>Description for the field of <i>current</i> spreadsheet referenced by <i>column name and row name</i></li>
     * </ol>
     */
    @Test
    public void testDescriptionInSpreadsheetAssetsCompare() {
        List<? extends NodeUsage> usedNodes;

        // Variable declaration: "AssetsCalc2012 : SpreadsheetResultTotalAssets"
        usedNodes = getMetaInfo(assetsCompareMetaReader, assetsCompare.getGridTable().getCell(0, 2)).getUsedNodes();
        assertEquals("Spreadsheet TotalAssets", usedNodes.get(0).getDescription());
        assertEquals(totalAssets.getUri(), usedNodes.get(0).getUri());

        // AssetsCalc2012
        usedNodes = getMetaInfo(assetsCompareMetaReader, assetsCompare.getGridTable().getCell(1, 2)).getUsedNodes();
        assertEquals(2, usedNodes.size());
        // '=' symbol
        assertEquals("Cell type: SpreadsheetResultTotalAssets", usedNodes.get(0).getDescription());
        // 'TotalAssets' method
        assertEquals(totalAssets.getUri(), usedNodes.get(1).getUri());
        assertEquals("SpreadsheetResultTotalAssets TotalAssets()", usedNodes.get(1).getDescription());

        // TotalAssets2012
        usedNodes = getMetaInfo(assetsCompareMetaReader, assetsCompare.getGridTable().getCell(1, 3)).getUsedNodes();
        assertEquals(3, usedNodes.size());
        assertEquals("Cell type: Long", usedNodes.get(0).getDescription()); // =
        assertEquals("SpreadsheetResultTotalAssets $AssetsCalc2012", usedNodes.get(1).getDescription()); // $AssetsCalc2012
        assertEquals("Spreadsheet TotalAssets\nLong $USDValue$Total", usedNodes.get(2).getDescription()); // $USDValue$Total
        // (other
        // spreadsheet)

        // TotalAssets2011
        assertFalse(CellMetaInfo.isCellContainsNodeUsages(
            getMetaInfo(assetsCompareMetaReader, assetsCompare.getGridTable().getCell(1, 4))));

        // Change in %
        usedNodes = getMetaInfo(assetsCompareMetaReader, assetsCompare.getGridTable().getCell(1, 5)).getUsedNodes();
        assertEquals(4, usedNodes.size());
        assertEquals("Cell type: Double", usedNodes.get(0).getDescription()); // =
        assertEquals("Long $TotalAssets2012", usedNodes.get(1).getDescription()); // $TotalAssets2012
        assertEquals("Double $TotalAssets2011", usedNodes.get(2).getDescription()); // $TotalAssets2011
        assertEquals("Double $Value$TotalAssets2011", usedNodes.get(3).getDescription()); // $Value$TotalAssets2011
    }

    /**
     * This method tests:
     * <ol>
     * <li>Description for the field of <i>current</i> spreadsheet referenced by <i>row name</i> only</li>
     * <li>Description for cell ranges</li>
     * </ol>
     */
    @Test
    public void testDescriptionInSpreadsheetTotalAssets() {
        List<? extends NodeUsage> usedNodes;

        // USD
        usedNodes = getMetaInfo(totalAssets.getMetaInfoReader(), totalAssets.getGridTable().getCell(3, 2))
            .getUsedNodes();
        assertEquals(3, usedNodes.size());
        assertEquals("Cell type: Long", usedNodes.get(0).getDescription()); // =
        assertEquals("Double $Amount", usedNodes.get(1).getDescription()); // Amount
        assertEquals("Double $Exchange Rate", usedNodes.get(2).getDescription()); // $Exchange Rate

        // Total
        usedNodes = getMetaInfo(totalAssets.getMetaInfoReader(), totalAssets.getGridTable().getCell(3, 7))
            .getUsedNodes();
        assertEquals(2, usedNodes.size());
        assertEquals("Cell type: Long", usedNodes.get(0).getDescription()); // =
        assertEquals("Long[] $USD:$GLD", usedNodes.get(1).getDescription()); // $USD:$GLD (cell range)
    }

    /**
     * This method tests:
     * <ol>
     * <li>Link to other custom spreadsheet table from the field exists</li>
     * <li>Description for the field of other non-custom spreadsheet result</li>
     * </ol>
     */
    @Test
    public void testDescriptionInSpreadsheetMiscAssets() {
        List<? extends NodeUsage> usedNodes;

        // TotalAssets1
        usedNodes = getMetaInfo(miscAssets.getMetaInfoReader(), miscAssets.getGridTable().getCell(1, 2)).getUsedNodes();
        assertEquals(3, usedNodes.size());
        assertEquals("Cell type: Long", usedNodes.get(0).getDescription()); // =
        assertNull(usedNodes.get(0).getUri());
        assertEquals("SpreadsheetResultTotalAssets totalAssets1", usedNodes.get(1).getDescription()); // $AssetsCalc2012
        assertEquals(totalAssets.getUri(),
            usedNodes.stream().filter(e -> e.getUri() != null).findFirst().get().getUri());
        assertEquals("Spreadsheet TotalAssets\nLong $USDValue$Total", usedNodes.get(2).getDescription()); // $USDValue$Total
        // (other
        // spreadsheet)
        assertEquals(totalAssets.getUri(), usedNodes.get(2).getUri());

        // TotalAssets2
        usedNodes = getMetaInfo(miscAssets.getMetaInfoReader(), miscAssets.getGridTable().getCell(1, 3)).getUsedNodes();
        assertEquals(3, usedNodes.size());
        assertEquals("Cell type: Object", usedNodes.get(0).getDescription()); // =
        assertEquals("SpreadsheetResult totalAssets2", usedNodes.get(1).getDescription()); // totalAssets2
        assertEquals("Spreadsheet\nObject $USDValue$Total", usedNodes.get(2).getDescription()); // $USDValue$Total
        // (other spreadsheet)
    }

    @Test
    public void testTransposedDataTypeTable() {
        ICell dataTypeDeclarationCell = typeCTransposed.getGridTable().getCell(0, 0);
        CellMetaInfo dataTypeDeclarationCellMeta = getMetaInfo(typeCTransposedMetaReader, dataTypeDeclarationCell);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(dataTypeDeclarationCellMeta));
        assertEquals(typeB.getUri(), dataTypeDeclarationCellMeta.getUsedNodes().get(0).getUri());

        ICell bField = typeCTransposed.getGridTable().getCell(0, 1);
        CellMetaInfo bFieldMeta = getMetaInfo(typeCTransposedMetaReader, bField);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(bFieldMeta));
        assertEquals(typeB.getUri(), bFieldMeta.getUsedNodes().get(0).getUri());

        ICell bArray = typeCTransposed.getGridTable().getCell(1, 1);
        CellMetaInfo bArrayMeta = getMetaInfo(typeCTransposedMetaReader, bArray);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(bArrayMeta));
        NodeUsage typeBNodeUsage = bArrayMeta.getUsedNodes().get(0);
        assertEquals(typeB.getUri(), typeBNodeUsage.getUri());
        assertEquals(0, typeBNodeUsage.getStart());
        assertEquals(4, typeBNodeUsage.getEnd());

        ICell carTypes = typeCTransposed.getGridTable().getCell(2, 1);
        CellMetaInfo carTypesMeta = getMetaInfo(typeCTransposedMetaReader, carTypes);
        assertTrue(CellMetaInfo.isCellContainsNodeUsages(carTypesMeta));
        NodeUsage carTypeNodeUsage = carTypesMeta.getUsedNodes().get(0);
        assertEquals(carType.getUri(), carTypeNodeUsage.getUri());
        assertEquals(0, carTypeNodeUsage.getStart());
        assertEquals(6, carTypeNodeUsage.getEnd());
    }

    @Test
    public void testAliasTable() {
        MetaInfoReader metaInfoReader = carType.getMetaInfoReader();
        IGridTable gridTable = carType.getGridTable();

        assertNull(getMetaInfo(metaInfoReader, gridTable.getCell(0, 0)));

        CellMetaInfo firstValueMeta = getMetaInfo(metaInfoReader, gridTable.getCell(0, 1));
        assertNotNull(firstValueMeta);
        assertFalse(CellMetaInfo.isCellContainsNodeUsages(firstValueMeta));
        assertEquals(String.class, firstValueMeta.getDataType().getInstanceClass());
        assertFalse(firstValueMeta.isMultiValue());
    }

    private CellMetaInfo getMetaInfo(MetaInfoReader metaInfoReader, ICell cell) {
        return metaInfoReader.getMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
    }

}
