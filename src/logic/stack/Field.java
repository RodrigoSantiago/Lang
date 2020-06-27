package logic.stack;

import content.Token;
import logic.Pointer;

public class Field {

    Stack stack;
    Block source;
    Token nameToken;
    Pointer typePtr;

    boolean hasGet;
    boolean hasSet;
    boolean hasOwn;

    public Field(Stack stack, Token nameToken, Pointer typePtr, boolean isFinal, Block source) {
        this.stack = stack;
        this.source = source;
        this.nameToken = nameToken;
        this.typePtr = typePtr;
        hasGet = true;
        hasSet = !isFinal;
        hasOwn = typePtr.let || !isFinal;
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

    public boolean hasGet() {
        return hasGet;
    }

    public boolean hasSet() {
        return hasSet;
    }

    public boolean hasOwn() {
        return hasOwn;
    }

    public boolean isReadOnly(Stack stack) {
        return false;
    }
}
