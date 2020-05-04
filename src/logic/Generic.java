package logic;

import content.Key;
import content.Token;
import data.ContentFile;

import java.util.ArrayList;

public class Generic {

    public Token nameToken;
    public Pointer pointer;

    Token typeToken, end;

    public Generic(Token nameToken, Token typeToken, Token end) {
        this.nameToken = nameToken;
        this.typeToken = typeToken;
        this.end = end;
    }

}
