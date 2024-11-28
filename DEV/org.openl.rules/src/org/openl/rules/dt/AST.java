package org.openl.rules.dt;

import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import org.openl.binding.IBoundNode;
import org.openl.util.text.AbsolutePosition;
import org.openl.util.text.ILocation;
import org.openl.util.text.IPosition;
import org.openl.util.text.TextInfo;
import org.openl.util.text.TextInterval;

public class AST {
    private final IBoundNode boundNode;
    private final Map<IBoundNode, ILocation> extensiveLocationMap;
    private final Map<IBoundNode, ILocation> locationMap;
    private final TextInfo textInfo;

    public AST(IBoundNode boundNode) {
        this.boundNode = boundNode;
        this.textInfo = boundNode != null ? new TextInfo(boundNode.getSyntaxNode().getModule().getCode()) : null;
        if (boundNode != null) {
            this.extensiveLocationMap = new IdentityHashMap<>();
            this.locationMap = new IdentityHashMap<>();
            buildLocationMaps(boundNode, extensiveLocationMap, locationMap);
        } else {
            this.extensiveLocationMap = null;
            this.locationMap = null;
        }
    }

    private Pair<IPosition, IPosition> f(IBoundNode boundNode,
                                         Map<IBoundNode, ILocation> map,
                                         Map<IBoundNode, ILocation> locationMap,
                                         IPosition start,
                                         IPosition end) {
        Pair<IPosition, IPosition> p = buildLocationMaps(boundNode, map, locationMap);
        if (start == null || p.getLeft() != null && start.getAbsolutePosition(textInfo) > p.getLeft()
                .getAbsolutePosition(textInfo)) {
            start = p.getLeft();
        }
        if (end == null || p.getRight() != null && end.getAbsolutePosition(textInfo) < p.getRight()
                .getAbsolutePosition(textInfo)) {
            end = p.getRight();
        }
        return Pair.of(start, end);
    }

    // This method fixes missed bracers in code, because bracers don't have special nodes and build in to tree.
    private TextInterval fixTextInterval(IPosition start, IPosition end) {
        int s = start.getAbsolutePosition(textInfo);
        int e = end.getAbsolutePosition(textInfo);
        String text = textInfo.getText().substring(s, e + 1);
        int m = 0;
        int k = 0;
        for (char c : text.toCharArray()) {
            if (c == '(') {
                m++;
            } else if (c == ')') {
                if (m > 0) {
                    m--;
                } else {
                    k++;
                }
            }
        }
        while (k > 0 && s - 1 >= 0) {
            if (textInfo.getText().charAt(s - 1) == '(') {
                k--;
            }
            s--;
        }
        m = 0;
        k = 0;
        int i = text.length() - 1;
        while (i > 0) {
            if (text.charAt(i) == ')') {
                m++;
            } else if (text.charAt(i) == '(') {
                if (m > 0) {
                    m--;
                } else {
                    k++;
                }
            }
            i--;
        }
        while (k > 0 && e + 1 < textInfo.getText().length()) {
            if (textInfo.getText().charAt(e + 1) == ')') {
                k--;
            }
            e++;
        }
        return new TextInterval(new AbsolutePosition(s), new AbsolutePosition(e));
    }

    private Pair<IPosition, IPosition> buildLocationMaps(IBoundNode boundNode,
                                                         Map<IBoundNode, ILocation> extensiveLocationMap,
                                                         Map<IBoundNode, ILocation> locationMap) {
        ILocation location = boundNode.getSyntaxNode().getSourceLocation();
        IPosition start = location != null ? location.getStart() : null;
        IPosition end = location != null ? location.getEnd() : null;
        IBoundNode x = boundNode;
        while (x.getTargetNode() != null) {
            x = x.getTargetNode();
            Pair<IPosition, IPosition> p1 = f(x, extensiveLocationMap, locationMap, start, end);
            start = p1.getLeft();
            end = p1.getRight();
        }
        for (IBoundNode childNode : boundNode.getChildren()) {
            Pair<IPosition, IPosition> p = f(childNode, extensiveLocationMap, locationMap, start, end);
            start = p.getLeft();
            end = p.getRight();
        }
        if (start != null && end != null) {
            extensiveLocationMap.put(boundNode, fixTextInterval(start, end));
        }
        if (boundNode.getSyntaxNode().getSourceLocation() != null) {
            locationMap.put(boundNode, boundNode.getSyntaxNode().getSourceLocation());
        }
        return Pair.of(start, end);
    }

    public IBoundNode getBoundNode() {
        return boundNode;
    }

    public ILocation getLocation(IBoundNode boundNode) {
        return locationMap.get(boundNode);
    }

    public String getCode() {
        return getCode(boundNode);
    }

    public String getCode(IBoundNode boundNode) {
        if (this.boundNode != null) {
            ILocation location = extensiveLocationMap.get(boundNode);
            int begin = location.getStart().getAbsolutePosition(textInfo);
            int end = location.getEnd().getAbsolutePosition(textInfo);
            return textInfo.getText().substring(begin, end + 1);
        }
        return null;
    }
}
