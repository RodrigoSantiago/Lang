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

    public Call(CallGroup group, Token start, Token end) {
        this.cFile = group.cFile;
        this.group = group;
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

    public void make() {

    }

    public void load(Context context) {

    }

    public boolean verify(Pointer pointer) {
        return false;
    }

    public Pointer request(Pointer pointer) {
        return null;
    }

    public Pointer getReturnType() {
        return null;
    }
}
