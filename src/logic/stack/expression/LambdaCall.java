package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.params.Parameters;
import logic.stack.StackExpansion;

public class LambdaCall extends Call {

    Parameters params;
    TokenGroup typeToken;
    StackExpansion innerStack;

    public LambdaCall(CallGroup group, Token start, Token end) {
        super(group, start, end);

        System.out.println("LAMBDA : "+ TokenGroup.toString(start, end));
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.PARAM) {
                params = new Parameters(cFile, token);
                state = 1;
            } else if (state == 1 && token.key == Key.LAMBDA) {
                state = 2;
            } else if (state == 2 && token.key == Key.WORD) {
                typeToken = new TokenGroup(token, next = TokenGroup.nextType(next, end));
                state = 3;
            } else if ((state == 2 || state == 3) && token.key == Key.BRACE) {
                innerStack = new StackExpansion(getStack());
                if (token.getChild() != null) {
                    innerStack.read(token.getChild(), token.getLastChild());
                } else {
                    cFile.erro(token, "Unexpected end of tokens");
                }
                state = 4;
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end && state != 2) {
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
        }
    }
}
