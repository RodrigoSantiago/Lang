package logic.stack.block;

import content.Key;
import content.Token;
import logic.stack.Block;

public class BlockNative extends Block {

    Token paramToken;
    Token contentToken;

    public BlockNative(Block block, Token start, Token end) {
        super(block, start, end);
        System.out.println("NATIVE");

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.NATIVE) {
                state = 1;
            } else if (state == 1 && token.key == Key.PARAM) {
                if (token.getChild() != null) {
                    readSourceToken(token.getChild(), token.getLastChild());
                } else {
                    cFile.erro(token, "Unexpected end of tokens", this);
                }
                state = 2;
            } else if ((state == 1 || state == 2) && token.key == Key.BRACE) {
                if (state == 1) {
                    cFile.erro(token.start, token.start + 1, "Missing parameter", this);
                }
                contentToken = token;
                state = 3;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state != 3) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }
    }

    private void readSourceToken(Token start, Token end) {
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.equals("source")) {
                paramToken = token;
                state = 1;
            } else if (state == 0 && token.equals("header")) {
                paramToken = token;
                cFile.erro(token, "The Header Parameter is not allowed here", this);
                state = 1;
            } else if (state == 0 && token.equals("return")) {
                paramToken = token;
                state = 1;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state != 1) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }
        if (start == end) {
            cFile.erro(token, "The parameter should not be empty", this);
        }
    }
}
