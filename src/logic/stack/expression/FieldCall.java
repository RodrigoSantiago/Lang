package logic.stack.expression;

import content.Token;

public class FieldCall extends Call {
    public FieldCall(CallGroup group, Token start, Token end) {
        super(group, start, end);
    }
}
