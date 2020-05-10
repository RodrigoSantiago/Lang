package logic;

import content.Key;
import content.Token;
import data.ContentFile;

import java.util.ArrayList;

public class Generic {

    public Template owner;
    public int index;

    public Token nameToken;
    public Pointer pointer;

    Token typeToken, end;

    public Generic(Template owner, int index, Token nameToken, Token typeToken, Token end) {
        this.owner = owner;
        this.index = index;
        this.nameToken = nameToken;
        this.typeToken = typeToken;
        this.end = end;
    }

}
