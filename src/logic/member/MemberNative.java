package logic.member;

import content.Key;
import content.Token;
import data.ContentFile;
import data.CppBuilder;
import logic.typdef.Type;

public class MemberNative extends Member {

    private boolean isHeader;   // in Header Implementation
    private boolean isSource;   // in Source Implementation OR internal non returning block
    private boolean isReturn;   // in Source internal with returning block

    private Token sourceToken;
    private Token contentToken;

    public MemberNative(Type type, Token start, Token end) {
        super(type);

        int state = 0;
        Token next;
        Token last = start;
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

            last = token;
            token = next;
        }

        if (state != 4) {
            cFile.erro(last, "Unexpected end of tokens");
        }
    }

    private void readSourceToken(Token init, Token end) {

        int state = 0;
        Token next;
        Token last = init;
        Token token = init;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.equals("source")) {
                sourceToken = token;
                isSource = true;
                state = 1;
            } else if (state == 0 && token.equals("header")) {
                sourceToken = token;
                isHeader = true;
                state = 1;
            } else if (state == 0 && token.equals("return")) {
                sourceToken = token;
                isReturn = true;
                state = 1;
            } else {
                cFile.erro(token, "Unexpected token");
            }

            last = token;
            token = next;
        }

        if (state != 1) {
            cFile.erro(last, "Unexpected end of tokens");
        }
    }

    @Override
    public boolean load() {
        if (sourceToken != null && isReturn) {
            cFile.erro(sourceToken, "Returning block are not allowed here");
        }
        return sourceToken != null && !isReturn;
    }

    public void build(CppBuilder cBuilder) {

    }

    public boolean isHeader() {
        return isHeader;
    }

    public boolean isSource() {
        return isSource;
    }

    public boolean isReturn() {
        return isReturn;
    }
}
