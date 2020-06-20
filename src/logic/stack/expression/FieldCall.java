package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.member.view.FieldView;
import logic.stack.LocalVar;

public class FieldCall extends Call {

    Token token;
    FieldView field;
    LocalVar local;

    public FieldCall(CallGroup group, Token start, Token end) {
        super(group, start, end);
        System.out.println("FIELD : "+ TokenGroup.toString(start, end));

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && (token.key == Key.WORD || token.key == Key.THIS || token.key == Key.SUPER)) {
                this.token = token;
                state = 1;
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end && state == 0) {
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
        }
    }
}
