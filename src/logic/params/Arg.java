package logic.params;

import content.Token;
import content.TokenGroup;
import logic.Pointer;

public class Arg {

    public boolean isLet;
    public TokenGroup typeToken;
    public Token nameToken;

    public Pointer type;

    public Arg(Token nameToken, TokenGroup typeToken, boolean isLet) {
        this.isLet = isLet;
        this.typeToken = typeToken;
        this.nameToken = nameToken;
    }

    @Override
    public String toString() {
        return (isLet ? "let " : "") + typeToken + " " + nameToken;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (obj instanceof Arg) {
            Arg other = (Arg) obj;

        }
        return false;
    }
}
