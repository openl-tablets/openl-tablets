package org.openl.rules.helpers;

import java.util.Collection;

import org.openl.OpenL;
import org.openl.engine.OpenLManager;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessages;
import org.openl.message.OpenLMessagesUtils;
import org.openl.source.SourceType;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.util.RangeWithBounds;

final public class BExGrammarParser implements RangeParser {
    private final SourceType sourceType;

    public BExGrammarParser(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    @Override
    public RangeWithBounds parse(String range) {
        // TODO: Correct tokenizing in grammar.
        OpenL openl = OpenL.getInstance(OpenL.OPENL_J_NAME);
        RangeWithBounds res;

        // Save current openl messages before range parser invocation to
        // avoid populating messages list with errors what are not refer to
        // appropriate table. Reason: input string doesn't contain required
        // information about source.
        //
        Collection<OpenLMessage> oldMessages = OpenLMessages.getCurrentInstance().getMessages();

        try {
            OpenLMessages.getCurrentInstance().clear();
            res = (RangeWithBounds) OpenLManager
                    .run(openl, new StringSourceCodeModule(range, ""), sourceType);
        } finally {
            // Load old openl messages list.
            //
            OpenLMessages.getCurrentInstance().clear();
            for (OpenLMessage message : oldMessages) {
                OpenLMessagesUtils.addMessage(message);
            }
        }
        return res;
    }
}
