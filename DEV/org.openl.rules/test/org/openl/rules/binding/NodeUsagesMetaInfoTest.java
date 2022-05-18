package org.openl.rules.binding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;
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
    private TableSyntaxNode rule3;
    private TableSyntaxNode rule4;
    private TableSyntaxNode convert;
    private TableSyntaxNode method1;
    private TableSyntaxNode method3;
    private TableSyntaxNode method4;
    private TableSyntaxNode assetsCompare;
    private TableSyntaxNode totalAssets;
    private TableSyntaxNode miscAssets;
    private TableSyntaxNode constructors;
    private TableSyntaxNode arrayNodeHints;
    private TableSyntaxNode ternaryOpHints;
    private TableSyntaxNode tab;
    private TableSyntaxNode tabs;
    private TableSyntaxNode tabs1;
    private TableSyntaxNode tabs1t;
    private TableSyntaxNode tabs2;
    private TableSyntaxNode tabs2t;
    private TableSyntaxNode tabs4;
    private TableSyntaxNode tabs4t;

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
        rule3 = findTable("Rules TypeB rule3(TypeB typeB, TypeC typeC)");
        rule4 = findTable("Rules TypeB rule4(TypeB typeB, TypeC typeC)");
        convert = findTable("Method TypeB[][] convert(TypeC[][] param)");
        method1 = findTable("Method String method1(TypeC[][] param)");
        method3 = findTable("Method String method3(TypeC[][] param)");
        method4 = findTable("Method String method4(TypeC[][] param)");

        assetsCompare = findTable("Spreadsheet SpreadsheetResult AssetsCompare ()");
        totalAssets = findTable("Spreadsheet SpreadsheetResult TotalAssets ()");
        miscAssets = findTable(
            "Spreadsheet SpreadsheetResult MiscAssets (SpreadsheetResultTotalAssets totalAssets1, SpreadsheetResult totalAssets2)");
        constructors = findTable("Spreadsheet SpreadsheetResult constructorHints()");
        arrayNodeHints = findTable("Spreadsheet SpreadsheetResult arrayNodeHints()");
        ternaryOpHints = findTable("Spreadsheet SpreadsheetResult ternaryOp (String spreadsheetResult)");
        tab = findTable("SmartRules String Tab(String componentID)");
        tabs = findTable("SmartRules Tabs Tabs(String componentID)");
        tabs1 = findTable("SmartRules Tabs Tabs1(String componentID)");
        tabs1t = findTable("SmartRules Tabs Tabs1t(String componentID)");
        tabs2 = findTable("SmartRules Tabs Tabs2(String componentID)");
        tabs2t = findTable("SmartRules Tabs Tabs2t(String componentID)");
        tabs4 = findTable("SmartRules Tabs Tabs4(String componentID)");
        tabs4t = findTable("SmartRules Tabs Tabs4t(String componentID)");
    }

    @Test
    public void testDataTableReference() {
        // Reference in dataB table points to dataA
        List<? extends NodeUsage> nodeUsages = assertMetaInfo(dataB, 0, 2, 1);

        assertNodeUsage(dataA.getUri(), nodeUsages.get(0), "Data String dataA", 1, 6);

        // Reference in empty dataC table points to dataA
        nodeUsages = assertMetaInfo(dataC, 0, 2, 1);

        assertNodeUsage(dataA.getUri(), nodeUsages.get(0), "Data String dataA", 1, 6);

        // Reference in empty dataC table points to dataB
        nodeUsages = assertMetaInfo(dataC, 1, 2, 1);

        assertNodeUsage(dataB.getUri(), nodeUsages.get(0), "Data TypeB dataB", 1, 6);
    }

    @Test
    public void testDataTypeNodeInDataTable() {
        List<? extends NodeUsage> nodeUsages = assertMetaInfo(dataB, 0, 0, 1);

        assertNodeUsage(typeB.getUri(), nodeUsages.get(0), "Datatype TypeB", 5, 10);
    }

    @Test
    public void testDataTypeTable() {
        List<? extends NodeUsage> nodeUsages = assertMetaInfo(typeC, 0, 0, 1);

        assertNodeUsage(typeB.getUri(), nodeUsages.get(0), "Datatype TypeB", 23, 28);

        nodeUsages = assertMetaInfo(typeC, 0, 1, 1);

        assertNodeUsage(typeB.getUri(), nodeUsages.get(0), "Datatype TypeB", 0, 5);

        nodeUsages = assertMetaInfo(typeC, 0, 2, 1);

        assertNodeUsage(typeB.getUri(), nodeUsages.get(0), "Datatype TypeB", 0, 5);

        nodeUsages = assertMetaInfo(typeC, 0, 3, 1);

        assertNodeUsage(carType.getUri(), nodeUsages.get(0), "Datatype CarType <String>", 0, 7);
    }

    @Test
    public void testLinksInDecisionTableHeader() {
        List<? extends NodeUsage> usedNodes = assertMetaInfo(rule1, 0, 0, 3);

        assertNodeUsage(typeB.getUri(), usedNodes.get(0), "Datatype TypeB", 6, 11);

        assertNodeUsage(typeB.getUri(), usedNodes.get(1), "Datatype TypeB", 18, 23);

        assertNodeUsage(typeC.getUri(), usedNodes.get(2), "Datatype TypeC extends TypeB", 31, 36);

        usedNodes = assertMetaInfo(rule1, 1, 3, 1);

        assertNodeUsage(carType.getUri(), usedNodes.get(0), "Datatype CarType <String>", 0, 7);

        usedNodes = assertMetaInfo(rule1, 2, 3, 1);

        assertNodeUsage(typeB.getUri(), usedNodes.get(0), "Datatype TypeB", 0, 5);

        assertCellType(rule1, 1, 5, "CarType");
    }



    @Test
    public void testLinksInMethodTableHeader() {
        List<? extends NodeUsage> nodeUsages = assertMetaInfo(convert, 0, 0, 2);

        assertNodeUsage(typeB.getUri(), nodeUsages.get(0), "Datatype TypeB", 7, 12);

        assertNodeUsage(typeC.getUri(), nodeUsages.get(1), "Datatype TypeC extends TypeB", 25, 30);
    }

    @Test
    public void testForFieldUsageInDecisionTable() {
        // First condition
        List<? extends NodeUsage> usedNodes = assertMetaInfo(rule2, 0, 2, 2);

        assertNodeUsage(typeC.getUri(), usedNodes.get(0), "Datatype TypeC extends TypeB\nTypeB bField", 0, 6);

        assertNodeUsage(typeB.getUri(), usedNodes.get(1), "Datatype TypeB\nString aaa", 7, 10);

        // Second condition
        usedNodes = assertMetaInfo(rule2, 1, 2, 1);

        assertNodeUsage(typeC.getUri(), usedNodes.get(0), "Datatype TypeC extends TypeB\nCarType[] carTypes", 0, 8);

        // test return cell
        usedNodes = assertMetaInfo(rule2, 2, 2, 2);

        assertNodeUsage(typeB.getUri(), usedNodes.get(0), "TypeB (String aaa)", 4, 9);

        assertNodeUsage(usedNodes.get(1), "String res", 10, 13);
    }

    @Test
    public void testForFieldUsageInDecisionTable3() {
        // First condition
        List<? extends NodeUsage> usedNodes = assertMetaInfo(rule3, 0, 2, 2);

        assertNodeUsage(typeC.getUri(), usedNodes.get(0), "Datatype TypeC extends TypeB\nTypeB bField", 0, 6);

        assertNodeUsage(typeB.getUri(), usedNodes.get(1), "Datatype TypeB\nString aaa", 7, 10);

        // Second condition
        usedNodes = assertMetaInfo(rule3, 1, 2, 1);

        assertNodeUsage(typeC.getUri(), usedNodes.get(0), "Datatype TypeC extends TypeB\nCarType[] carTypes", 0, 8);

        // test return cell
        usedNodes = assertMetaInfo(rule3, 2, 2, 2);

        assertNodeUsage(typeB.getUri(), usedNodes.get(0), "TypeB (String aaa)", 0, 5);

        assertNodeUsage(usedNodes.get(1), "String res", 6, 9);
    }

    @Test
    public void testForFieldUsageInDecisionTable4() {
        // First condition
        List<? extends NodeUsage> usedNodes = assertMetaInfo(rule4, 0, 2, 2);

        assertNodeUsage(typeC.getUri(), usedNodes.get(0), "Datatype TypeC extends TypeB\nTypeB bField", 0, 6);

        assertNodeUsage(typeB.getUri(), usedNodes.get(1), "Datatype TypeB\nString aaa", 7, 10);

        // Second condition
        usedNodes = assertMetaInfo(rule4, 1, 2, 1);

        assertNodeUsage(typeC.getUri(), usedNodes.get(0), "Datatype TypeC extends TypeB\nCarType[] carTypes", 0, 8);

        // test return cell
        usedNodes = assertMetaInfo(rule4, 2, 2, 3);

        assertNodeUsage(typeB.getUri(), usedNodes.get(0), "TypeB (String aaa)", 0, 5);

        assertNodeUsage(typeB.getUri(), usedNodes.get(1), "Datatype TypeB\nString aaa", 6, 9);

        assertNodeUsage(usedNodes.get(2), "String res", 10, 13);
    }

    @Test
    public void testForMixedNodeUsageInMethod1Table() {
        // Header line
        List<? extends NodeUsage> usedNodes = assertMetaInfo(method1, 0, 0, 1);

        assertNodeUsage(typeC.getUri(), usedNodes.get(0), "Datatype TypeC extends TypeB", 22, 27);

        // First line of method body
       usedNodes = assertMetaInfo(method1, 0, 1, 2);

        assertNodeUsage(dataB.getUri(), usedNodes.get(0), "Data TypeB dataB", 20, 25);

        assertNodeUsage(typeB.getUri(), usedNodes.get(1), "Datatype TypeB\nString aaa", 29, 32);

        // Return cell
        usedNodes = assertMetaInfo(method1, 0, 2, 5);

        assertNodeUsage(typeB.getUri(), usedNodes.get(0), "TypeB <init>(String aaa)", 11, 16);

        assertNodeUsage(typeB.getUri(), usedNodes.get(1), "Datatype TypeB\nString aaa", 24, 27);

        assertNodeUsage(convert.getUri(), usedNodes.get(2), "TypeB[][] convert(TypeC[][] param)", 30, 37);

        assertNodeUsage(typeC.getUri(), usedNodes.get(3), "TypeC[][] param", 38, 43);

        assertNodeUsage(typeB.getUri(), usedNodes.get(4), "Datatype TypeB\nString aaa", 51, 54);
    }

    @Test
    public void testForMixedNodeUsageInMethod3Table() {
        // First line of method body
        List<? extends NodeUsage> usedNodes = assertMetaInfo(method3, 0, 1, 2);

        assertNodeUsage(dataB.getUri(), usedNodes.get(0), "Data TypeB dataB", 20, 25);

        assertNodeUsage(typeB.getUri(), usedNodes.get(1), "Datatype TypeB\nString aaa", 29, 32);

        // Return cell
        usedNodes = assertMetaInfo(method3, 0, 2, 5);

        assertNodeUsage(typeB.getUri(), usedNodes.get(0), "TypeB <init>(String aaa)", 7, 12);

        assertNodeUsage(typeB.getUri(), usedNodes.get(1), "Datatype TypeB\nString aaa", 20, 23);

        assertNodeUsage(convert.getUri(), usedNodes.get(2), "TypeB[][] convert(TypeC[][] param)", 26, 33);

        assertNodeUsage(typeC.getUri(), usedNodes.get(3), "TypeC[][] param", 34, 39);

        assertNodeUsage(typeB.getUri(), usedNodes.get(4), "Datatype TypeB\nString aaa", 47, 50);
    }

    @Test
    public void testForMixedNodeUsageInMethod4Table() {
        // First line of method body
        List<? extends NodeUsage> usedNodes = assertMetaInfo(method4, 0, 1, 2);

        assertNodeUsage(dataB.getUri(), usedNodes.get(0), "Data TypeB dataB", 20, 25);

        assertNodeUsage(typeB.getUri(), usedNodes.get(1), "Datatype TypeB\nString aaa", 29, 32);

        // Return cell
        usedNodes = assertMetaInfo(method4, 0, 2, 6);

        assertNodeUsage(typeB.getUri(), usedNodes.get(0), "TypeB <init>()", 8, 13);

        assertNodeUsage(typeB.getUri(), usedNodes.get(1), "Datatype TypeB\nString aaa", 14, 17);

        assertNodeUsage(typeB.getUri(), usedNodes.get(2), "Datatype TypeB\nString aaa", 25, 28);

        assertNodeUsage(convert.getUri(), usedNodes.get(3), "TypeB[][] convert(TypeC[][] param)", 31, 38);

        assertNodeUsage(typeC.getUri(), usedNodes.get(4), "TypeC[][] param", 39, 44);

        assertNodeUsage(typeB.getUri(), usedNodes.get(5), "Datatype TypeB\nString aaa", 52, 55);
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
        // Variable declaration: "AssetsCalc2012 : SpreadsheetResultTotalAssets"
        List<? extends NodeUsage> nodeUsages = assertMetaInfo(assetsCompare, 0, 2, 1);

        assertNodeUsage(totalAssets.getUri(), nodeUsages.get(0), "Spreadsheet TotalAssets", 17, 45);

        // AssetsCalc2012
        nodeUsages = assertMetaInfo(assetsCompare, 1, 2, 2);
        // '=' symbol
        assertNodeUsage(nodeUsages.get(0), "Cell type: SpreadsheetResultTotalAssets", 0, 1);

        assertNodeUsage(totalAssets.getUri(), nodeUsages.get(1), "SpreadsheetResultTotalAssets TotalAssets()", 2, 13);

        // TotalAssets2012
        nodeUsages = assertMetaInfo(assetsCompare, 1, 3, 3);

        assertNodeUsage(nodeUsages.get(0), "Cell type: Long", 0, 1);

        assertNodeUsage(nodeUsages.get(1), "SpreadsheetResultTotalAssets $AssetsCalc2012", 2, 17);

        assertNodeUsage(totalAssets.getUri(), nodeUsages.get(2), "Spreadsheet TotalAssets\nLong $USDValue$Total", 18, 33);

        // TotalAssets2011
        assertCellType(assetsCompare, 1, 4, "java.lang.String");

        // Change in %
        nodeUsages = assertMetaInfo(assetsCompare, 1, 5, 4);

        assertNodeUsage(nodeUsages.get(0), "Cell type: Double", 0, 1);

        assertNodeUsage(nodeUsages.get(1), "Long $TotalAssets2012", 9, 25);

        assertNodeUsage(nodeUsages.get(2), "Double $TotalAssets2011", 28, 44);

        assertNodeUsage(nodeUsages.get(3), "Double $Value$TotalAssets2011", 48, 70);
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
        usedNodes = assertMetaInfo(totalAssets, 3, 2, 3);

        assertNodeUsage(usedNodes.get(0), "Cell type: Long", 0, 1);

        assertNodeUsage(usedNodes.get(1), "Double $Amount", 9, 16);

        assertNodeUsage(usedNodes.get(2), "Double $Exchange Rate", 19, 33);

        // Total
        usedNodes = assertMetaInfo(totalAssets, 3, 7, 2);

        assertNodeUsage(usedNodes.get(0), "Cell type: Long", 0, 1);

        assertNodeUsage(usedNodes.get(1), "Long[] $USD:$GLD", 6, 15);
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
        usedNodes = assertMetaInfo(miscAssets, 1, 2, 3);

        assertNodeUsage(usedNodes.get(0), "Cell type: Long", 0, 1);

        assertNodeUsage(totalAssets.getUri(), usedNodes.get(1), "SpreadsheetResultTotalAssets totalAssets1", 2, 14);

        assertNodeUsage(totalAssets.getUri(), usedNodes.get(2), "Spreadsheet TotalAssets\nLong $USDValue$Total", 15, 30);

        // TotalAssets2
        usedNodes = assertMetaInfo(miscAssets, 1, 3, 3);

        assertNodeUsage(usedNodes.get(0), "Cell type: Long", 0, 1);

        assertNodeUsage(usedNodes.get(1), "SpreadsheetResult totalAssets2", 2, 14);

        assertNodeUsage(totalAssets.getUri(), usedNodes.get(2), "Spreadsheet TotalAssets\nLong $USDValue$Total", 15, 30);
    }

    @Test
    public void testTransposedDataTypeTable() {
        List<? extends NodeUsage> nodeUsages = assertMetaInfo(typeCTransposed, 0, 0, 1);

        assertNodeUsage(typeB.getUri(), nodeUsages.get(0), "Datatype TypeB", 33, 38);

        nodeUsages = assertMetaInfo(typeCTransposed, 0, 1, 1);

        assertNodeUsage(typeB.getUri(), nodeUsages.get(0), "Datatype TypeB", 0, 5);

        nodeUsages = assertMetaInfo(typeCTransposed, 1, 1, 1);

        assertNodeUsage(typeB.getUri(), nodeUsages.get(0), "Datatype TypeB", 0, 5);

        nodeUsages = assertMetaInfo(typeCTransposed, 2, 1, 1);

        assertNodeUsage(carType.getUri(), nodeUsages.get(0), "Datatype CarType <String>", 0, 7);
    }

    @Test
    public void testAliasTable() {
        MetaInfoReader metaInfoReader = carType.getMetaInfoReader();
        IGridTable gridTable = carType.getGridTable();

        ICell cell = gridTable.getCell(0, 0);
        assertNull(metaInfoReader.getMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn()));

        assertCellType(carType, 0, 1, "java.lang.String");
    }

    @Test
    public void testConstructorsMetaInformation_FormulaColumnForFirstStep() {
        List<? extends NodeUsage> usedNodes = assertMetaInfo(constructors, 1, 2, 6);

        assertNodeUsage(usedNodes.get(0), "Cell type: TypeC", 0, 1);

        assertNodeUsage(typeC.getUri(), usedNodes.get(1), "TypeC (String aaa, TypeB bField, TypeB[] bArray, CarType[] carTypes)", 6, 11);

        assertNodeUsage(typeB.getUri(), usedNodes.get(2), "TypeB (String aaa)", 23, 28);

        assertNodeUsage(typeB.getUri(), usedNodes.get(3), "Datatype TypeB", 38, 43);

        assertNodeUsage(typeB.getUri(), usedNodes.get(4), "TypeB ()", 51, 56);

        assertNodeUsage(usedNodes.get(5), "java.lang\nclass String", 61, 67);
    }

    @Test
    public void testConstructorsMetaInformation_FormulaColumnForSecondStep() {
        List<? extends NodeUsage> usedNodes = assertMetaInfo(constructors, 1, 3, 2);

        assertNodeUsage(usedNodes.get(0), "Cell type: CarType[]", 0, 1);

        assertNodeUsage(usedNodes.get(1), "TypeC $TypeC", 2, 8);
    }

    @Test
    public void testConstructorsMetaInformation_FormulaColumnForThirdStep() {
        List<? extends NodeUsage> usedNodes = assertMetaInfo(constructors, 1, 4, 3);

        assertNodeUsage(usedNodes.get(0), "Cell type: CarType[]", 0, 1);

        assertNodeUsage(usedNodes.get(1), "TypeC $TypeC", 2, 8);

        assertNodeUsage(typeC.getUri(), usedNodes.get(2), "Datatype TypeC extends TypeB\nCarType[] carTypes", 9, 17);
    }

    @Test
    public void testConstructorsMetaInformation_FormulaColumnForFourthStep() {
        List<? extends NodeUsage> usedNodes = assertMetaInfo(constructors, 1, 5, 2);

        assertNodeUsage(usedNodes.get(0), "Cell type: BigDecimal", 0, 1);

        assertNodeUsage(usedNodes.get(1), "java.math\nBigDecimal (double p0)", 6, 16);
    }

    @Test
    public void testConstructorsMetaInformation_FormulaColumnForFifthStep() {
        List<? extends NodeUsage> usedNodes = assertMetaInfo(constructors, 1, 6, 2);

        assertNodeUsage(usedNodes.get(0), "Cell type: SimpleDateFormat", 0, 1);

        assertNodeUsage(usedNodes.get(1), "java.text\nSimpleDateFormat (String p0)", 6, 32);
    }

    @Test
    public void testConstructorsMetaInformation_FormulaColumnForS6Step() {
        List<? extends NodeUsage> usedNodes = assertMetaInfo(constructors, 1, 7, 2);

        assertNodeUsage(usedNodes.get(0), "Cell type: TypeB", 0, 1);

        assertNodeUsage(typeB.getUri(), usedNodes.get(1), "TypeB ()", 2, 7);
    }

    @Test
    public void testConstructorsMetaInformation_FormulaColumnForS7Step() {

        List<? extends NodeUsage> usedNodes = assertMetaInfo(constructors, 1, 8, 2);

        assertNodeUsage(usedNodes.get(0), "Cell type: TypeB", 0, 1);

        assertNodeUsage(typeB.getUri(), usedNodes.get(1), "TypeB (String aaa)", 2, 7);
    }

    @Test
    public void testConstructorsMetaInformation_FormulaColumnForS8Step() {

        List<? extends NodeUsage> usedNodes = assertMetaInfo(constructors, 1, 9, 3);

        assertNodeUsage(usedNodes.get(0), "Cell type: TypeB", 0, 1);

        assertNodeUsage(typeB.getUri(), usedNodes.get(1), "TypeB (String aaa)", 2, 7);

        assertNodeUsage(typeB.getUri(), usedNodes.get(2), "Datatype TypeB\nString aaa", 8, 11);
    }

    @Test
    public void testConstructorsMetaInformation_FormulaColumnForS9Step() {
        List<? extends NodeUsage> usedNodes = assertMetaInfo(constructors, 1, 10, 8);

        assertNodeUsage(usedNodes.get(0), "Cell type: TypeCTransposed", 0, 1);

        assertNodeUsage(typeCTransposed.getUri(), usedNodes.get(1), "TypeCTransposed (TypeB bField, TypeB[] bArray, String aaa)", 8, 23);

        assertNodeUsage(typeCTransposed.getUri(), usedNodes.get(2), "Datatype TypeCTransposed extends TypeB\nTypeB bField", 24, 30);

        assertNodeUsage(typeB.getUri(), usedNodes.get(3), "TypeB (String aaa)", 33, 38);

        assertNodeUsage(typeB.getUri(), usedNodes.get(4), "Datatype TypeB\nString aaa", 39, 42);

        assertNodeUsage(typeCTransposed.getUri(), usedNodes.get(5), "Datatype TypeCTransposed extends TypeB\nTypeB[] bArray", 52, 58);

        assertNodeUsage(typeB.getUri(), usedNodes.get(6), "Datatype TypeB", 65, 70);

        assertNodeUsage(typeB.getUri(), usedNodes.get(7), "Datatype TypeB\nString aaa", 75, 78);
    }

    @Test
    public void testConstructorsMetaInformation_FormulaColumnForS10Step() {
        List<? extends NodeUsage> usedNodes = assertMetaInfo(constructors, 1, 11, 3);

        assertNodeUsage(usedNodes.get(0), "Cell type: Object", 0, 1);

        assertNodeUsage(usedNodes.get(1), "java.lang\n" + "class String", 11, 17);

        assertNodeUsage(usedNodes.get(2), "java.lang\n" + "class Integer", 30, 37);
    }

    @Test
    public void testArrayBoundNodeMetaInformation() {
        List<? extends NodeUsage> usedNodes = assertMetaInfo(arrayNodeHints, 1, 2, 2);

        assertNodeUsage(typeC.getUri(), usedNodes.get(1), "Datatype TypeC extends TypeB", 6, 11);

        usedNodes = assertMetaInfo(arrayNodeHints, 1, 3, 2);

        assertNodeUsage(usedNodes.get(1), "org.openl.generated.beans\nclass TypeC", 6, 37);

        usedNodes = assertMetaInfo(arrayNodeHints, 1, 4, 2);

        assertNodeUsage(usedNodes.get(1), "java.text\nclass SimpleDateFormat", 6, 32);

        usedNodes = assertMetaInfo(arrayNodeHints, 1, 5, 3);

        assertNodeUsage(typeC.getUri(), usedNodes.get(1), "Datatype TypeC extends TypeB", 6, 11);

        usedNodes = assertMetaInfo(arrayNodeHints, 1, 6, 3);

        assertNodeUsage(usedNodes.get(1), "java.lang\nclass String", 6, 12);

        usedNodes = assertMetaInfo(arrayNodeHints, 1, 7, 2);

        assertNodeUsage(usedNodes.get(1), "java.lang\n@interface Override", 6, 14);

        usedNodes = assertMetaInfo(arrayNodeHints, 1, 8, 2);

        assertNodeUsage(usedNodes.get(1), "java.lang\ninterface Runnable", 6, 14);

        usedNodes = assertMetaInfo(arrayNodeHints, 1, 9, 3);

        assertNodeUsage(typeC.getUri(), usedNodes.get(1), "Datatype TypeC extends TypeB", 3, 8);
    }

    @Test
    public void testTernaryOp() {
        List<? extends NodeUsage> usedNodes = assertMetaInfo(ternaryOpHints, 1, 2, 4);

        assertNodeUsage(usedNodes.get(1), "org.openl.rules.calc\nclass SpreadsheetResult",16, 33);

        usedNodes = assertMetaInfo(ternaryOpHints, 1, 3, 4);

        assertNodeUsage(usedNodes.get(1), "org.openl.rules.calc\nclass SpreadsheetResult",9, 26);
    }

    @Test
    public void testDTArrays() {
        // Tab
        List<? extends NodeUsage> usedNodes = assertMetaInfo(tabs, 1, 2, 1);
        assertNodeUsage(tab.getUri(), usedNodes.get(0), "String Tab(String componentID)", 1, 4);
        usedNodes = assertMetaInfo(tabs, 1, 3, 1);
        assertNodeUsage(tab.getUri(), usedNodes.get(0), "String Tab(String componentID)", 1, 4);
        assertMetaInfo(tabs, 2, 2, 0);
        assertMetaInfo(tabs, 2, 3, 0);

        // Tab1
        usedNodes = assertMetaInfo(tabs1, 1, 2, 1);
        assertNodeUsage(tab.getUri(), usedNodes.get(0), "String Tab(String componentID)", 1, 4);
        usedNodes = assertMetaInfo(tabs1, 2, 2, 1);
        assertNodeUsage(tab.getUri(), usedNodes.get(0), "String Tab(String componentID)", 1, 4);
        assertMetaInfo(tabs1, 3, 2, 0);

        // Tab1t
        usedNodes = assertMetaInfo(tabs1t, 1, 2, 1);
        assertNodeUsage(tab.getUri(), usedNodes.get(0), "String Tab(String componentID)", 1, 4);
        usedNodes = assertMetaInfo(tabs1t, 1, 3, 1);
        assertNodeUsage(tab.getUri(), usedNodes.get(0), "String Tab(String componentID)", 1, 4);
        assertMetaInfo(tabs1t, 1, 4, 0);

        // Tab2
        usedNodes = assertMetaInfo(tabs2, 1, 2, 1);
        assertNodeUsage(tab.getUri(), usedNodes.get(0), "String Tab(String componentID)", 1, 4);
        usedNodes = assertMetaInfo(tabs2, 1, 3, 1);
        assertNodeUsage(tab.getUri(), usedNodes.get(0), "String Tab(String componentID)", 1, 4);
        usedNodes = assertMetaInfo(tabs2, 2, 2, 1);
        assertNodeUsage(tab.getUri(), usedNodes.get(0), "String Tab(String componentID)", 1, 4);
        usedNodes = assertMetaInfo(tabs2, 2, 3, 1);
        assertNodeUsage(tab.getUri(), usedNodes.get(0), "String Tab(String componentID)", 1, 4);
        assertMetaInfo(tabs2, 3, 2, 0);
        assertMetaInfo(tabs2, 3, 3, 0);

        // Tab2t
        usedNodes = assertMetaInfo(tabs2t, 1, 2, 1);
        assertNodeUsage(tab.getUri(), usedNodes.get(0), "String Tab(String componentID)", 1, 4);
        usedNodes = assertMetaInfo(tabs2t, 1, 3, 1);
        assertNodeUsage(tab.getUri(), usedNodes.get(0), "String Tab(String componentID)", 1, 4);
        assertMetaInfo(tabs2t, 1, 4, 0);
        assertMetaInfo(tabs2t, 1, 5, 0);

        // Tab4
        usedNodes = assertMetaInfo(tabs4, 1, 2, 1);
        assertNodeUsage(tab.getUri(), usedNodes.get(0), "String Tab(String componentID)", 1, 4);
        usedNodes = assertMetaInfo(tabs4, 2, 2, 1);
        assertNodeUsage(tab.getUri(), usedNodes.get(0), "String Tab(String componentID)", 1, 4);
        usedNodes = assertMetaInfo(tabs4, 3, 2, 1);
        assertNodeUsage(tab.getUri(), usedNodes.get(0), "String Tab(String componentID)", 1, 4);
        usedNodes = assertMetaInfo(tabs4, 4, 2, 1);
        assertNodeUsage(tab.getUri(), usedNodes.get(0), "String Tab(String componentID)", 1, 4);
        assertMetaInfo(tabs4, 5, 2, 0);

        // Tab4t
        usedNodes = assertMetaInfo(tabs4t, 1, 2, 1);
        assertNodeUsage(tab.getUri(), usedNodes.get(0), "String Tab(String componentID)", 1, 4);
        usedNodes = assertMetaInfo(tabs4t, 1, 3, 1);
        assertNodeUsage(tab.getUri(), usedNodes.get(0), "String Tab(String componentID)", 1, 4);
        usedNodes = assertMetaInfo(tabs4t, 1, 4, 1);
        assertNodeUsage(tab.getUri(), usedNodes.get(0), "String Tab(String componentID)", 1, 4);
        usedNodes = assertMetaInfo(tabs4t, 1, 5, 1);
        assertNodeUsage(tab.getUri(), usedNodes.get(0), "String Tab(String componentID)", 1, 4);
        assertMetaInfo(tabs4t, 1, 6, 0);

    }

    private static void assertCellType(TableSyntaxNode node, int column, int row, String type) {
        MetaInfoReader metaInfoReader = node.getMetaInfoReader();
        ICell cell = node.getGridTable().getCell(column, row);
        CellMetaInfo cellMetaInfo = metaInfoReader.getMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
        assertNotNull(cellMetaInfo);
        assertFalse(CellMetaInfo.isCellContainsNodeUsages(cellMetaInfo));
        assertEquals(type, cellMetaInfo.getDataType().getName());
        assertFalse(cellMetaInfo.isMultiValue());
    }

    private static List<? extends NodeUsage> assertMetaInfo(TableSyntaxNode node, int column, int row, int size) {
        MetaInfoReader metaInfoReader = node.getMetaInfoReader();
        ICell cell = node.getGridTable().getCell(column, row).getTopLeftCellFromRegion();
        CellMetaInfo cellMetaInfo = metaInfoReader.getMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
        if (size > 0) {
            assertNotNull(cellMetaInfo);
        }
        List<? extends NodeUsage> usedNodes = cellMetaInfo != null ? cellMetaInfo.getUsedNodes()
                                                                   : Collections.emptyList();
        if (usedNodes == null) {
            usedNodes = Collections.emptyList();
        }
        assertEquals(size, usedNodes.size());
        return usedNodes;
    }

    private static void assertNodeUsage(NodeUsage nodeUsage, String descr, int start, int end) {
        assertNull(nodeUsage.getUri());
        assertEquals(descr, nodeUsage.getDescription());
        assertEquals(start, nodeUsage.getStart());
        assertEquals(end, nodeUsage.getEnd());
    }

    private static void assertNodeUsage(String uri, NodeUsage nodeUsage, String descr, int start, int end) {
        assertEquals(uri, nodeUsage.getUri());
        assertEquals(descr, nodeUsage.getDescription());
        assertEquals(start, nodeUsage.getStart());
        assertEquals(end, nodeUsage.getEnd());
    }

}
