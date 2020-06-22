package logic.stack.line;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.stack.Block;
import logic.stack.Context;
import logic.stack.Line;
import logic.stack.expression.Expression;

public class LineReturn extends Line {

    TokenGroup returnToken;
    Expression returnExp;

    public LineReturn(Block block, Token start, Token end) {
        super(block, start, end);
        System.out.println("RETURN");

        Token init = start;
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.RETURN) {
                init = next;
                state = 1;
            } else if (state == 1 && (token.key == Key.SEMICOLON || next == end)) {
                if (token.key != Key.SEMICOLON) {
                    cFile.erro(token, "Semicolon expected", this);
                }
                if (init != (token.key == Key.SEMICOLON ? token : next)) {
                    returnToken = new TokenGroup(init, token.key == Key.SEMICOLON ? token : next);
                    returnExp = new Expression(this, returnToken.start, returnToken.end);
                }
                state = 2;
            } else if (state != 1) {
                cFile.erro(token, "Unexpected token", this);
            }
            if (state == 0 && next == end) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }
    }

    @Override
    public void load() {
        if (returnExp != null) {
            returnExp.load(new Context(stack));
            // TODO -REQUEST STACK RETURN TYPE
        }
        super.load();
    }
}
