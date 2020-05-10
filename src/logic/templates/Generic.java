package logic.templates;

import content.Token;
import logic.Pointer;

public class Generic {

    public Template owner;
    public int index;

    public Token nameToken;
    public Pointer type;

    Token typeToken, end;

    public Generic(Template owner, int index, Token nameToken, Token typeToken, Token end) {
        this.owner = owner;
        this.index = index;
        this.nameToken = nameToken;
        this.typeToken = typeToken;
        this.end = end;
    }

}
