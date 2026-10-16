package org.openl.rules.xls.merge;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.openxml4j.opc.TargetMode;
import org.apache.poi.ss.formula.FormulaParseException;
import org.apache.poi.ss.formula.FormulaParser;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.Pxg;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.model.ExternalLinksTable;
import org.apache.poi.xssf.usermodel.XSSFEvaluationWorkbook;
import org.apache.poi.xssf.usermodel.XSSFRelation;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.Nullable;

/**
 * Keeps the formulas a merge copies into OUR workbook reading the other workbooks they read.
 *
 * <p>A formula names another workbook by the number of its link. A formula copied into OUR workbook brings the links
 * it reads that OUR workbook lacks, each with the names and the values of the other workbook it keeps, and the links
 * before them, so that each keeps its number. A link that names no workbook, such as a DDE or an OLE link, is not
 * brought, nor any link after it. A link no copied formula reads is never brought.
 *
 * <p>A formula copied into OUR workbook must find under each number it reads the workbook its own workbook links
 * there. Otherwise it would read another workbook, so the merge fails. Workbooks that link different workbooks under
 * one number merge as long as no copied formula reads that link. Only .xlsx workbooks are handled.
 */
final class XlsExternalLinks {

    private XlsExternalLinks() {
    }

    /**
     * Gives the workbook of a cell a formula is copied into the links to other workbooks the formula reads, and checks
     * that it links the same workbooks under their numbers.
     *
     * @param source the cell the formula is copied from
     * @param target the cell the formula is copied into
     * @throws LinkedWorkbookMismatchException when the workbook of the target links another workbook, or none, under
     *                                         a number the formula reads
     * @throws UncheckedIOException            when a link cannot be copied
     */
    static void linkWorkbooks(Cell source, Cell target) {
        if (!(source.getSheet().getWorkbook() instanceof XSSFWorkbook from)
                || !(target.getSheet().getWorkbook() instanceof XSSFWorkbook to)) {
            return;
        }
        for (int number : numbersRead(source, from)) {
            bringUpTo(from, to, number);
            if (!Objects.equals(workbookOf(from, number), workbookOf(to, number))) {
                throw new LinkedWorkbookMismatchException(target.getSheet().getSheetName(),
                        target.getAddress().formatAsString(),
                        number);
            }
        }
    }

    /**
     * The numbers of the links to other workbooks a formula reads. A text that only looks like a link, such as one in
     * quotes or a column of an Excel table, is no link.
     */
    private static Set<Integer> numbersRead(Cell cell, XSSFWorkbook workbook) {
        Ptg[] tokens;
        try {
            tokens = FormulaParser.parse(cell.getCellFormula(),
                    XSSFEvaluationWorkbook.create(workbook),
                    FormulaType.CELL,
                    workbook.getSheetIndex(cell.getSheet()),
                    cell.getRowIndex());
        } catch (FormulaParseException e) {
            // A formula its own workbook cannot read reads no other workbook either.
            return Set.of();
        }
        return Arrays.stream(tokens)
                .filter(Pxg.class::isInstance)
                .map(token -> ((Pxg) token).getExternalWorkbookNumber())
                .filter(number -> number > 0)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Copies into a workbook the links of another workbook it lacks, up to the given number.
     */
    private static void bringUpTo(XSSFWorkbook from, XSSFWorkbook to, int number) {
        var links = linksOf(from);
        var last = Math.min(number, links.size());
        try {
            for (var i = linksOf(to).size(); i < last && namesWorkbook(links.get(i)); i++) {
                copy(links.get(i), to);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The path of the workbook a workbook links under a number, or {@code null} when no link of that number names a
     * workbook.
     */
    private static @Nullable String workbookOf(XSSFWorkbook workbook, int number) {
        var links = linksOf(workbook);
        if (number < 1 || number > links.size()) {
            return null;
        }
        var link = links.get(number - 1);
        return namesWorkbook(link) ? link.getLinkedFileName() : null;
    }

    private static boolean namesWorkbook(ExternalLinksTable link) {
        return link.getCTExternalLink().getExternalBook() != null;
    }

    private static List<ExternalLinksTable> linksOf(XSSFWorkbook workbook) {
        return Objects.requireNonNullElse(workbook.getExternalLinksTables(), List.of());
    }

    /**
     * Adds a copy of a link to the workbook, after its other links: the part of the link, with the path of the other
     * workbook, and the reference the workbook makes to it.
     */
    private static void copy(ExternalLinksTable link, XSSFWorkbook target) throws IOException {
        try {
            var pkg = target.getPackage();
            var number = 0;
            PackagePartName name;
            do {
                number++;
                name = PackagingURIHelper.createPartName(XSSFRelation.EXTERNAL_LINKS.getFileName(number));
            } while (pkg.containPart(name));
            var part = pkg.createPart(name, XSSFRelation.EXTERNAL_LINKS.getContentType());
            var path = link.getPackagePart().getRelationship(link.getCTExternalLink().getExternalBook().getId());
            part.addExternalRelationship(path.getTargetURI().toString(), path.getRelationshipType(), path.getId());
            try (var output = part.getOutputStream()) {
                link.writeTo(output);
            }
            var relation = target.getPackagePart()
                    .addRelationship(name, TargetMode.INTERNAL, XSSFRelation.EXTERNAL_LINKS.getRelation());
            var workbook = target.getCTWorkbook();
            var references = workbook.isSetExternalReferences()
                    ? workbook.getExternalReferences()
                    : workbook.addNewExternalReferences();
            references.addNewExternalReference().setId(relation.getId());
            target.addExternalLinksTable(new ExternalLinksTable(part));
        } catch (InvalidFormatException e) {
            throw new IOException(e);
        }
    }
}
