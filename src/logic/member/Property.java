package logic.member;

import content.Token;
import logic.Pointer;

public class Property extends Member {
    Token tokenName;
    Pointer type;
    boolean isStatic;
    boolean isGetFinal;
    boolean isGetAbstract;
    boolean isSetFinal;
    boolean isSetAbstract;
    boolean isLGetFinal;
    boolean isLGetAbstract;
    boolean isLSetFinal;
    boolean isLSetAbstract;

    public Property(Token start, Token end) {

    }
}
