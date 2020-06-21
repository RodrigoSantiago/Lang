package logic.stack.expression;

import content.Token;
import data.ContentFile;
import logic.Pointer;
import logic.stack.Context;
import logic.stack.Stack;

import java.util.ArrayList;

public class CallGroup {

    public final ContentFile cFile;
    public final Expression parent;
    Pointer returnPtr;
    private boolean isOperator;

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

    public void load(Context context) {
        if (isOperator) {

        } else {
            for (int i = 0; i < calls.size(); i++) {
                Call call = calls.get(i);

                call.load(context);
                if (!context.isIncorrect() && i < calls.size() - 1) {
                    call.request(null);
                }
            }
        }
    }

    public Pointer request(Pointer pointer) {
        if (calls.size() == 0) return Pointer.voidPointer;

        return calls.get(calls.size() - 1).request(pointer);
    }

    public boolean isEmpty() {
        return calls.isEmpty();
    }

    public void setOperator(Token token) {
        isOperator = true;
    }

    public void setCastingOperator(Token token) {
        isOperator = true;
    }

    public boolean isOperator() {
        return isOperator;
    }

    public void add(Call call) {
        calls.add(call);
    }

    public Pointer getReturnType() {
        return returnPtr;
    }
}
