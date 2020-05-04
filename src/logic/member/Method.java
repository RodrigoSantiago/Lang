package logic.member;

import content.Token;
import logic.params.Params;
import logic.Generic;
import logic.Pointer;

public class Method extends Member {
    Token name;
    Pointer returnType;
    Params params;
    Generic[] generics;
    boolean isFinal;
    boolean isStatic;
    boolean isAbstract;

    public Method(Token start, Token end) {

    }
}
