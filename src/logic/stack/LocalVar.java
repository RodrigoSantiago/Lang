package logic.stack;

import content.Token;
import logic.Pointer;

public class LocalVar {
    public Pointer typePtr;
    public Token name;
    public boolean isSuper;

    public LocalVar(Stack owner, Token name, Pointer typePtr) {

    }

    public Pointer getTypePtr() {
        return typePtr;
    }

    public boolean canGet() {
        return false; // returning a let pointer
    }

    public boolean canSet() {
        return false;
    }

    public boolean canOwn() {
        return false; // returning a ptr pointer
    }

    public boolean canAcess() {
        return false; // Ptr variables inside lambdas are not acessibles
    }
}
