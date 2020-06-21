package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.Pointer;
import logic.member.view.MethodView;
import logic.stack.Context;

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
                    cFile.erro(token, "Unexpected end of tokens", this);
                }
                state = 2;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state != 2) {
                cFile.erro(token, "Unexpected end of tokens", this);
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
                arguments.add(new Expression(getLine(), contentStart, token));
                state = 1;
            } else if (state == 1 && token.key != Key.COMMA) {
                contentStart = token;
                state = 0;
            }
            if (next == end) {
                if (state == 0) {
                    arguments.add(new Expression(getLine(), contentStart, next));
                } else {
                    cFile.erro(token, "Unexpected end of tokens", this);
                }
            }
            token = next;
        }
    }

    @Override
    public void load(Context context) {
        if (nameToken == null) {
            context.jumpTo(null);
        } else {

            // TODO - INNER CONSTRUCTOR BEHAVIOR
            ArrayList<MethodView> methods = context.findMethod(nameToken, arguments);
            if (methods.size() == 0) {
                // erro
            } else if (methods.size() > 1) {
                // erro
                methodView = methods.get(0);
            } else {
                methodView = methods.get(0);
            }
            context.jumpTo(methodView == null ? null : methodView.getTypePtr());
        }
    }

    @Override
    public Pointer request(Pointer pointer) {
        if (methodView == null) return null;

        return methodView.getTypePtr();
    }
}
