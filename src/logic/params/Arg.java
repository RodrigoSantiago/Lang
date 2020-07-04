package logic.params;

import content.Token;
import content.TokenGroup;
import logic.Pointer;

public class Arg {

    public boolean isLet;
    public Token nameToken;
    public Pointer typePtr;
    public TokenGroup typeToken;

    public Arg(Token nameToken, TokenGroup typeToken, boolean isLet) {
        this.isLet = isLet;
        this.typeToken = typeToken;
        this.nameToken = nameToken;
    }

    @Override
    public String toString() {
        return typePtr + " " + nameToken;
    }
}
