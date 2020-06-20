package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.member.view.MethodView;

import java.util.ArrayList;

public class MethodCall extends Call {

    Token nameToken;
    MethodView methodView;
    ArrayList<Expression> arguments = new ArrayList<>();

    public MethodCall(CallGroup group, Token start, Token end) {
        super(group, start, end);

        System.out.println("METHOD : "+ TokenGroup.toString(start, end));
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.WORD) {
                this.nameToken = token;
                state = 1;
            } else if (state == 1 && token.key == Key.PARAM) {
                if (token.getChild() != null) {
                    readArguments(token.getChild(), token.getLastChild());
                } else {
                    cFile.erro(token, "Unexpected end of tokens");
                }
                state = 2;
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end && state != 2) {
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
        }
    }

    private void readArguments(Token start, Token end) {
        Token contentStart = start;
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.COMMA) {
                arguments.add(new Expression(getStack(), contentStart, token));
                state = 1;
            } else if (state == 1 && token.key != Key.COMMA) {
                contentStart = token;
                state = 0;
            }
            if (next == end) {
                if (state == 0) {
                    arguments.add(new Expression(getStack(), contentStart, next));
                } else {
                    cFile.erro(token, "Unexpected end of tokens");
                }
            }
            token = next;
        }
    }
}
