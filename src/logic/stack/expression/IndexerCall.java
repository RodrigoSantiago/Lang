package logic.stack.expression;

import content.Token;

public class IndexerCall extends Call {
    public IndexerCall(CallGroup group, Token start, Token end) {
        super(group, start, end);
    }
}
