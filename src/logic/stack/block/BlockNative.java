package logic.stack.block;

import content.Key;
import content.Parser;
import content.Token;
import content.TokenGroup;
import logic.stack.Block;
import logic.stack.Line;
import logic.stack.Stack;

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
                paramToken = token;
                state = 2;
            } else if ((state == 1 || state == 2) && token.key == Key.BRACE) {
                if (state == 1) {
                    cFile.erro(token.start, token.start + 1, "Missing parameter");
                }
                contentToken = token;
                state = 3; // completed
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end && state != 3) {
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
        }
    }
}
