package logic.member;

import content.Token;
import logic.params.Params;
import logic.Pointer;

public class Constructor extends Member {
    Token name;
    Pointer returnType;
    Params params;
    boolean isStatic;
    boolean isDefault;

    public Constructor(Token start, Token end) {

    }
}
