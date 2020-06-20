package logic.stack.expression;

import content.Token;
import data.ContentFile;
import logic.Pointer;
import logic.stack.Stack;

public class Call {
    public static final int FIELD = 0;
    public static final int METHOD = 1;
    public static final int INDEXER = 2;
    public static final int INSTANCE = 3;
    public static final int LAMBDA = 4;
    public static final int CASTING = 5;
    public static final int INNER = 6;
    public static final int LITERAL = 7;

    public final ContentFile cFile;
    public final CallGroup group;

    Pointer returnPtr;

    public Call(CallGroup group, Token start, Token end, int type) {
        this.cFile = group.cFile;
        this.group = group;
        System.out.println("TYPE : " + type);
    }

    public Stack getStack() {
        return group.getStack();
    }

    public Expression getExpression() {
        return group.parent;
    }

    public void make() {

    }

    public Pointer getReturnType() {
        return returnPtr;
    }
}
