package logic.stack.expression;

import content.Key;
import content.Token;
import builder.CppBuilder;
import logic.Pointer;
import logic.stack.Context;

public class InnerCall extends Call {

    Expression innerExpression;

    public InnerCall(CallGroup group, Token start, Token end) {
        super(group, start, end);

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.PARAM && token.getChild() != null) {
                this.token = token;
                if (token.getChild() != token.getLastChild()) {
                    innerExpression = new Expression(getLine(), token.getChild(), token.getLastChild());
                } else {
                    cFile.erro(token, "Empty expression not allowed", this);
                }
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
            innerExpression.load(new Context(context));
        }
    }

    @Override
    public int verify(Pointer pointer) {
        return innerExpression == null ? 0 : innerExpression.verify(pointer);
    }

    @Override
    public Pointer getNaturalPtr(Pointer convertFlag) {
        naturalPtr = innerExpression == null ? null : innerExpression.getNaturalPtr(convertFlag);
        return naturalPtr;
    }

    @Override
    public void requestGet(Pointer pointer) {
        if (pointer != null) pointer = pointer.toLet();
        if (innerExpression != null) {
            innerExpression.requestGet(pointer);
            requestPtr = innerExpression.getRequestPtr();
        }
    }

    @Override
    public void requestOwn(Pointer pointer) {
        if (innerExpression != null) {
            innerExpression.requestOwn(pointer);
            requestPtr = innerExpression.getRequestPtr();
        }
    }

    @Override
    public void requestSet() {
        cFile.erro(getToken(), "SET not allowed", this);
    }

    @Override
    public void build(CppBuilder cBuilder, int idt, boolean next) {
        cBuilder.add("(").add(innerExpression, idt).add(")");
        if (next) {
            cBuilder.add(requestPtr.isPointer() ? "->" : ".");
        }
    }
}
