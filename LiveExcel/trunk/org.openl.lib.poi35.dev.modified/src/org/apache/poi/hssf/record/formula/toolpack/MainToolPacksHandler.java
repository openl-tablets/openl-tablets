package org.apache.poi.hssf.record.formula.toolpack;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.record.formula.atp.AnalysisToolPak;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;

public class MainToolPacksHandler{

    private DefaultToolPack defaultToolPack;

    private List<ToolPack> usedToolPacks = new ArrayList<ToolPack>();

    private static MainToolPacksHandler instance;

    public static MainToolPacksHandler instance() {
        if (instance == null) {
            instance = new MainToolPacksHandler();
        }
        return instance;
    }

    private MainToolPacksHandler() {
        defaultToolPack = new DefaultToolPack();
        usedToolPacks.add(defaultToolPack);
        usedToolPacks.add(new AnalysisToolPak());
    }

    public boolean containsFunction(String name) {
        for (ToolPack pack : usedToolPacks) {
            if (pack.containsFunction(name)) {
                return true;
            }
        }
        return false;
    }

    public FreeRefFunction findFunction(String name) {
        FreeRefFunction evaluatorForFunction;
        for (ToolPack pack : usedToolPacks) {
            evaluatorForFunction = pack.findFunction(name);
            if (evaluatorForFunction != null) {
                return evaluatorForFunction;
            }
        }
        return null;
    }

    public void addToolPack(ToolPack pack) {
        usedToolPacks.add(pack);
    }
}
