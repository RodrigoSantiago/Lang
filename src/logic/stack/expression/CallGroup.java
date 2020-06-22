package logic.stack.expression;

import content.Key;
import content.Token;
import data.ContentFile;
import logic.Pointer;
import logic.stack.Context;
import logic.stack.Stack;

import java.util.ArrayList;

public class CallGroup {

    public final ContentFile cFile;
    public final Expression parent;
    private Token operatorToken;

    public ArrayList<Call> calls = new ArrayList<>();
    public ArrayList<CallGroup> groups = new ArrayList<>();
    Pointer returnPtr;
    Pointer castPtr;

    public CallGroup(Expression expression) {
        this.cFile = expression.cFile;
        this.parent = expression;
    }

    public CallGroup(Expression expression, CallGroup left, CallGroup right) {
        this.cFile = expression.cFile;
        this.parent = expression;
    }

    public CallGroup(Expression expression, CallGroup left, CallGroup center, CallGroup right) {
        this.cFile = expression.cFile;
        this.parent = expression;
    }

    public Token getToken() {
        return operatorToken != null ? operatorToken :
                groups.size() > 0 ? groups.get(groups.size() - 1).getToken() :
                        calls.size() > 0 ? calls.get(calls.size() - 1).getToken() : parent.start;
    }

    public Stack getStack() {
        return parent.stack;
    }

    public void make() {

    }

    public void load(Context context) {
        if (false) {

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

    public int verify(Pointer pointer) {
        return -1;
    }

    public Pointer request(Pointer pointer) {
        if (calls.size() == 0) return Pointer.voidPointer;

        return calls.get(calls.size() - 1).request(pointer);
    }

    public boolean isEmpty() {
        return calls.isEmpty();
    }

    public void setOperator(Token token) {
        operatorToken = token;
    }

    public void setCastingOperator(Token token) {
        operatorToken = token;
    }

    public boolean isOperator() {
        return operatorToken != null;
    }

    public boolean isOperatorRight() {
        return operatorToken.key == Key.INC || operatorToken.key == Key.DEC;
    }

    public boolean isOperatorLeft() {
        return castPtr != null || operatorToken.key == Key.INC || operatorToken.key == Key.DEC ||
                operatorToken.key == Key.NOT || operatorToken.key == Key.BITNOT;
    }

    public boolean isOperatorBoth() {
        return operatorToken.key == Key.ADD || operatorToken.key == Key.SUB;
    }

    public boolean isOperatorCenter() {
        return !isOperatorLeft();
    }

    public int getOperatorPriority() {
        return operatorToken.key.priority;
    }

    public boolean isField() {
        return calls.size() == 1 && calls.get(0) instanceof FieldCall;
    }

    public void add(Call call) {
        calls.add(call);
    }

    public Pointer getReturnType() {
        return returnPtr;
    }
}
