package org.openl.rules.lang.xls;

import java.util.ArrayDeque;
import java.util.Queue;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 * Internal Util class for working with table syntax node relations
 * 
 * @author Marat Kamalov
 *
 */
final class TableSyntaxNodeRelationsUtils {

    private TableSyntaxNodeRelationsUtils() {
    }

    public static boolean[][] buildRelationsMatrix(TableSyntaxNode[] tableSyntaxNodes,
            TableSyntaxNodeRelationsDeterminer tableSyntaxNodeRelationsDeterminer) {
        boolean[][] matrix = new boolean[tableSyntaxNodes.length][tableSyntaxNodes.length];
        for (int i = 0; i < tableSyntaxNodes.length; i++) {
            for (int j = 0; j < tableSyntaxNodes.length; j++) {
                if (i != j && tableSyntaxNodeRelationsDeterminer.determine(tableSyntaxNodes[i], tableSyntaxNodes[j])) {
                    matrix[j][i] = true;
                }
            }
        }
        return matrix;
    }

    public static TableSyntaxNode[] sort(TableSyntaxNode[] tableSyntaxNodes,
            TableSyntaxNodeRelationsDeterminer tableSyntaxNodeRelationsDeterminer) throws TableSyntaxNodeCircularDependencyException {
        boolean[][] matrix = buildRelationsMatrix(tableSyntaxNodes, tableSyntaxNodeRelationsDeterminer);
        return sort(tableSyntaxNodes, matrix);
    }

    public static TableSyntaxNode[] sort(TableSyntaxNode[] tableSyntaxNodes,
            boolean[][] matrix) throws TableSyntaxNodeCircularDependencyException {
        TableSyntaxNode[] result = new TableSyntaxNode[tableSyntaxNodes.length];

        int[] countArray = new int[tableSyntaxNodes.length];
        for (int i = 0; i < tableSyntaxNodes.length; i++) {
            for (int j = 0; j < tableSyntaxNodes.length; j++) {
                if (i != j && matrix[j][i]) {
                    countArray[i]++;
                }
            }
        }

        int n = 0;
        Queue<Integer> queue = new ArrayDeque<Integer>();
        for (int i = 0; i < tableSyntaxNodes.length; i++) {
            if (countArray[i] == 0) {
                queue.add(i);
            }
        }
        while (!queue.isEmpty()) {
            int t = queue.poll();
            result[n++] = tableSyntaxNodes[t];
            for (int i = 0; i < tableSyntaxNodes.length; i++) {
                if (matrix[t][i]) {
                    countArray[i]--;
                    if (countArray[i] == 0) {
                        queue.add(i);
                    }
                }
            }
        }
        if (n < tableSyntaxNodes.length) {
            TableSyntaxNode[] invalidTableSyntaxNodes = new TableSyntaxNode[tableSyntaxNodes.length - n];
            int j = 0;
            for (int i = 0; i < tableSyntaxNodes.length; i++) {
                if (countArray[i] > 0) {
                    invalidTableSyntaxNodes[j++] = tableSyntaxNodes[i];
                }
            }
            throw new TableSyntaxNodeCircularDependencyException(invalidTableSyntaxNodes);
        }
        return result;
    }

}
