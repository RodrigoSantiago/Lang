package logic.stack.expression;

import content.Token;
import data.ContentFile;
import logic.Pointer;
import logic.member.Constructor;
import logic.member.view.*;
import logic.stack.Context;
import logic.stack.Line;
import logic.stack.Stack;

public abstract class Call {
    public final ContentFile cFile;
    public final CallGroup group;
    public Token token;
    Pointer returnPtr;

    public Call(CallGroup group, Token start, Token end) {
        this.cFile = group.cFile;
        this.group = group;
        this.token = start;
    }

    public Stack getStack() {
        return group.getStack();
    }

    public Expression getExpression() {
        return group.parent;
    }

    public Line getLine() {
        return group.parent.parent;
    }

    public Token getToken() {
        return token;
    }

    public Pointer getReturnType() {
        return returnPtr;
    }

    public boolean isTypeCall() {
        return false;
    }

    public boolean isLiteral() {
        return false;
    }

    public int getLiteralType() {
        return 0;
    }

    public Pointer getTypePtr() {
        return null;
    }

    public abstract void load(Context context);

    public abstract int verify(Pointer pointer);

    public abstract Pointer request(Pointer pointer);

    public abstract boolean requestSet(Pointer pointer);
}
