package logic.stack.expression;

import content.Token;

public class LambdaCall extends Call {
    public LambdaCall(CallGroup group, Token start, Token end) {
        super(group, start, end);
    }
}
