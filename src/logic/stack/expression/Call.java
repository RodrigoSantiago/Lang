package logic.stack.expression;

import content.Token;
import data.ContentFile;
import data.CppBuilder;
import logic.Pointer;
import logic.stack.Context;
import logic.stack.Line;
import logic.stack.Stack;

public abstract class Call {
    public final ContentFile cFile;
    public final CallGroup group;
    public Token token;

    Pointer naturalPtr;
    Pointer requestPtr;

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

    public Pointer getNaturalPtr(Pointer convertFlag) {
        return naturalPtr;
    }

    public boolean isTypeCall() {
        return false;
    }

    public boolean isLiteral() {
        return false;
    }

    public boolean isMethodSetter() {
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

    public abstract void requestGet(Pointer pointer);

    public abstract void requestOwn(Pointer pointer);

    public abstract void requestSet();

    public void build(CppBuilder cBuilder, int idt, boolean next) {

    }

    public void buildSet(CppBuilder cBuilder, int idt) {
        build(cBuilder, idt, false);
    }

    public String next() {
        return requestPtr.isPointer() ? "->" : ".";
    }

    public boolean isDirectCall() {
        return false;
    }
}
