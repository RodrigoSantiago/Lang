package logic.stack.expression;

import content.Token;

public class MethodCall extends Call {
    public MethodCall(CallGroup group, Token start, Token end) {
        super(group, start, end);
    }
}
