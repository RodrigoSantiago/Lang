package logic.stack.expression;

import content.Token;
import data.ContentFile;
import logic.Pointer;
import logic.stack.Stack;

import java.util.ArrayList;

public class CallGroup {

    public final ContentFile cFile;
    public final Expression parent;
    Pointer returnPtr;

    public ArrayList<Call> calls = new ArrayList<>();

    public CallGroup(Expression expression) {
        this.cFile = expression.cFile;
        this.parent = expression;
    }

    public Stack getStack() {
        return parent.stack;
    }

    public void make() {

    }

    public boolean isEmpty() {
        return calls.isEmpty();
    }

    public void setOperator(Token token) {
        System.out.println("OP : " + token);
    }
    public void setCastingOperator(Token token) {
        System.out.println("CAST : " + token);
    }

    public void add(Call call) {
        calls.add(call);
    }

    public Pointer getReturnType() {
        return returnPtr;
    }
}
