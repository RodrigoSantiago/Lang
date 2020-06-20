package logic.stack.expression;

import content.Token;

public class InstanceCall extends Call {
    public InstanceCall(CallGroup group, Token start, Token end) {
        super(group, start, end);
    }
}
