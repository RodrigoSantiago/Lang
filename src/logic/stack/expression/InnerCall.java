package logic.stack.expression;

import content.Token;

public class InnerCall extends Call {
    public InnerCall(CallGroup group, Token start, Token end) {
        super(group, start, end);
    }
}
