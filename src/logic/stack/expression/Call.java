package logic.stack.expression;

import content.Token;
import data.ContentFile;
import logic.Pointer;
import logic.member.Constructor;
import logic.member.view.*;
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

    public void readField() {

    }

    public void readMethod() {

    }

    public void readIndexer() {

    }

    public void readInstance() {

    }

    public void readLambda() {

    }

    public void readCasting() {

    }

    public void readLiteral() {

    }

    public void make() {

    }

    public Pointer getReturnType() {
        return null;
    }
}
