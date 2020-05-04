package logic.member;

import content.Token;
import logic.Pointer;

public class Field extends Member {
    Token name;
    Pointer type;
    boolean isFinal;
    boolean isStatic;
    boolean isAbstract;

    public Field(Token start, Token end) {

    }
}
