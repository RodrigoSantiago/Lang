package logic.member;

import content.Token;
import logic.member.params.Params;
import logic.typdef.Generic;
import logic.typdef.Pointer;

public class Method {
    Token name;
    Pointer returnType;
    Params params;
    Generic[] generics;
    boolean isFinal;
    boolean isStatic;
    boolean isAbstract;
}
