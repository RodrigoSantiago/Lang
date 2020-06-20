package logic.stack.expression;

import content.Token;

public class LiteralCall extends Call {
    public LiteralCall(CallGroup group, Token start, Token end) {
        super(group, start, end);
    }
}
