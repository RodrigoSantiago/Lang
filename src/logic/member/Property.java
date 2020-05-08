package logic.member;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.ContentFile;
import logic.member.view.FieldView;
import logic.typdef.Type;

import java.util.ArrayList;

public class Property extends Member {

    boolean isGetFinal, isGetAbstract, isGetPublic, isGetPrivate;
    boolean isLetFinal, isLetAbstract, isLetPublic, isLetPrivate;
    boolean isSetFinal, isSetAbstract, isSetPublic, isSetPrivate;

    Token nameToken;
    Token contentToken;
    TokenGroup typeToken;
    TokenGroup initToken;

    public Property(Type type, Token start, Token end) {
        super(type);

        int state = 0;
        Token next;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key.isAttribute) {
                readModifier(cFile, token, true, true, true, true, true, true, false);
            } else if (state == 0 && token.key == Key.WORD) {
                if (next != null && next != end && (next.key == Key.GENERIC)) {
                    next = next.getNext();
                }
                while (next != null && next != end && next.key == Key.INDEX) {
                    next = next.getNext();
                }
                typeToken = new TokenGroup(token, next);
                state = 1;
            } else if (state == 1 && token.key == Key.WORD) {
                this.token = token;
                nameToken = token;
                state = 2;
            } else if (token.key == Key.BRACE) {
                if (state != 2 || contentToken != null) {
                    cFile.erro(token, "Unexpected token");
                } else {
                    state = 3;
                }
                if (contentToken == null) {
                    contentToken = token;
                }
            } else if (state == 3 && token.key == Key.SETVAL) {
                while (next != null && next != end && next.key != Key.SEMICOLON) {
                    next = next.getNext();
                }
                if (next != end && next != null) {
                    next = next.getNext();
                } else {
                    cFile.erro(token, "Unexpected end of tokens");
                }
                initToken = new TokenGroup(token, next);
                state = 4;
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end && state < 3) {
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
        }
    }

    private void readBlocks(Token init, Token end) {
        boolean isPrivate, isPublic, isAbstract, isFinal, isLet, isGet;

        int state = 0;
        Token next;
        Token token = init;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key.isAttribute) {
                // add atts {private|public|abstract|final}
            } else if (state == 0 && token.equals("get")) {
                state = 1;
            } else if (state == 0 && token.equals("set")) {
                state = 1;
            } else if (state == 0 && token.key == Key.LET) {
                state = 1;
            } else if (token.key == Key.BRACE || token.key == Key.SEMICOLON) {
                if (state != 1) {

                }
                state = 0;
            } else {

            }
            token = next;
        }
    }

    @Override
    public boolean load() {
        return true;
    }

    public FieldView getField() {
        return new FieldView(nameToken, null, this);
    }

    @Override
    public String toString() {
        return nameToken+" : "+ typeToken+" {} ";
    }
}
