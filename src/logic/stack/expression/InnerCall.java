package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.Pointer;
import logic.stack.Context;

public class InnerCall extends Call {

    Expression innerExpression;

    public InnerCall(CallGroup group, Token start, Token end) {
        super(group, start, end);
        System.out.println("INNER : ("+ TokenGroup.toString(start, end)+")");
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.PARAM && token.getChild() != null) {
                this.token = token;
                // TODO - EMPTY CALL
                innerExpression = new Expression(getLine(), token.getChild(), token.getLastChild());
                state = 1;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state == 0) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }
    }

    @Override
    public void load(Context context) {
        if (innerExpression == null) {
            context.jumpTo(null);
        } else {
            Context internal = new Context(getStack());
            innerExpression.load(internal);
            context.jumpToContext(internal);
        }
    }

    @Override
    public int verify(Pointer pointer) {
        return innerExpression == null ? 0 : innerExpression.verify(pointer);
    }

    @Override
    public Pointer request(Pointer pointer) {
        if (innerExpression == null) return null;
        returnPtr = innerExpression.request(pointer);
        return returnPtr;
    }

    @Override
    public Pointer requestSet(Pointer pointer) {
        request(pointer);
        return null;
    }
}
