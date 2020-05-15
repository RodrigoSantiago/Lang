package logic.templates;

import content.Token;
import content.TokenGroup;
import logic.Pointer;

public class Generic {

    public Template owner;
    public int index;

    public Token nameToken;
    public Pointer basePtr;
    public Pointer typePtr;

    public TokenGroup typeToken;

    public Generic(Template owner, int index, Token nameToken, TokenGroup typeToken) {
        this.owner = owner;
        this.index = index;
        this.nameToken = nameToken;
        this.typeToken = typeToken;
    }

}
