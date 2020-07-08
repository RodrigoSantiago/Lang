package logic.stack;

import content.Key;
import content.Token;
import builder.CppBuilder;
import builder.Temp;
import logic.Pointer;

public class Field {

    Field copy;

    Stack stack;
    Block source;
    Token token;
    Token nameToken;
    Pointer typePtr;
    boolean isReadyOnly;

    public Field(Stack stack, Token nameToken, Pointer typePtr, boolean isFinal, Block source) {
        this.stack = stack;
        this.source = source;
        this.nameToken = nameToken;
        this.typePtr = typePtr;
        isReadyOnly = isFinal;
    }

    public Field(Stack stack, Token token, Token nameToken, Pointer typePtr, boolean isFinal, Block source) {
        this.stack = stack;
        this.source = source;
        this.token = token;
        this.nameToken = nameToken;
        this.typePtr = typePtr;
        isReadyOnly = isFinal;
    }

    public Field(Stack stack, Field field) {
        this.stack = stack;
        this.copy = field;
        this.source = field.source;
        this.token = field.token;
        this.nameToken = field.nameToken;
        this.typePtr = field.getTypePtr().toLet();
        isReadyOnly = true;
    }

    public Token getName() {
        return nameToken;
    }

    public Pointer getTypePtr() {
        return typePtr;
    }

    public Block getSource() {
        return source;
    }

    public Stack getStack() {
        return stack;
    }

    public boolean isReadOnly(Stack stack) {
        return isReadyOnly || stack != getStack();
    }

    public void build(CppBuilder cBuilder, boolean next) {
        if (nameToken.key == Key.THIS) {
            if (stack.isYieldMode() || stack.isLambda()) {
                if (typePtr.isPointer()) {
                    cBuilder.nameParam("this").add(next, "->");
                } else {
                    cBuilder.nameParam("this").add(next, ".");
                }
            } else if (next) {
                cBuilder.add("this->");
            } else if (typePtr.isPointer()) {
                cBuilder.add("this");
            } else {
                cBuilder.add("(*this)");
            }
        } else if (nameToken.key == Key.BASE) {
            cBuilder.add(typePtr.type.parent.type.pathToken).add(next, "::");
        } else {
            cBuilder.nameParam(nameToken).add(next, typePtr.isPointer() ? "->" : ".");
        }
    }

    public String next() {
        return typePtr.isPointer() ? "->" : ".";
    }

    public boolean isShadow() {
        return copy != null;
    }
}
