package org.openl.rules.binding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
}
