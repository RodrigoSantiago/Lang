package logic.stack;

import content.Token;
import logic.Pointer;

public class Field {

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
}
