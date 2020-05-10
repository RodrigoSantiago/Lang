package logic.member;

import content.Key;
import content.Token;
import data.ContentFile;
import logic.typdef.Type;

public class MemberNative extends Member {

    boolean isSource;

    Token sourceToken;
    Token contentToken;

    public MemberNative(Type type, Token start, Token end) {
        super(type);

        int state = 0;
        Token next;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key.isAttribute) {
                cFile.erro(token, "Unexpected modifier");
            } else if (state == 0 && token.key == Key.NATIVE) {
                this.token = token;
                state = 2;
            } else if (state == 2 && token.key == Key.PARAM) {
                readSourceToken(token.getChild(), token.getLastChild());
                state = 3;
            } else if (state == 3 && token.key == Key.BRACE) {
                contentToken = token;
                state = 4;
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end && state != 4) {
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
        }
    }

    private void readSourceToken(Token init, Token end) {

        int state = 0;
        Token next;
        Token token = init;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && (token.equals("source") || token.equals("header") || token.equals("return"))) {
                sourceToken = token;
                state = 1;
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end && state != 1) {
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
        }
    }

    @Override
    public boolean load() {
        return true;
    }
}