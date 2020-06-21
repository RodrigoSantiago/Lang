package logic.stack;

import content.Token;
import content.TokenGroup;
import logic.Pointer;
import logic.member.view.*;
import logic.stack.expression.Expression;
import logic.typdef.Type;

import java.util.ArrayList;

public class Context {
    public Type type;
    public Pointer pointer;
    private boolean isStatic;
    private boolean isIncorrect;

    public Context(Type type) {
        this.type = type;
        this.pointer = type.self;
        this.isStatic = true;
    }

    public Context(Pointer pointer) {
        this.type = pointer.type;
        this.pointer = pointer;
        this.isStatic = false;
    }

    public void jumpTo(Pointer pointer) {
        if (pointer == null) {
            isIncorrect = true;
        } else {
            this.isStatic = false;
            this.type = pointer.type;
            this.pointer = pointer;
        }
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isIncorrect() {
        return isIncorrect;
    }

    public Pointer getPointer(TokenGroup typeToken) {
        return null;
    }

    public void resolve(Expression expression) {

    }

    public FieldView findField(Token nameToken) {
        return null;
    }

    public ArrayList<MethodView> findMethod(Token nameToken, ArrayList<Expression> arguments) {
        return null;
    }

    public ArrayList<IndexerView> findIndexer(ArrayList<Expression> arguments) {
        return null;
    }

    public ArrayList<OperatorView> findOperator(Token opToken, ArrayList<Expression> arguments) {
        return null;
    }

    public ArrayList<ConstructorView> findConstructor(Pointer typePtr, ArrayList<Expression> arguments) {
        return null;
    }
}
