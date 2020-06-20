package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.member.view.ConstructorView;
import logic.member.view.MethodView;
import logic.stack.Stack;
import logic.stack.StackExpansion;

import java.util.ArrayList;

public class InstanceCall extends Call {

    TokenGroup typeToken;
    ConstructorView constructorView;
    ArrayList<Expression> indexArguments = new ArrayList<>();
    ArrayList<Expression> initArguments = new ArrayList<>();
    ArrayList<Expression> arguments = new ArrayList<>();

    StackExpansion initStack;

    public InstanceCall(CallGroup group, Token start, Token end) {
        super(group, start, end);

        System.out.println("INSTANCE : "+ TokenGroup.toString(start, end));
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.NEW) {
                state = 1;
            } else if (state == 1 && token.key == Key.WORD) {
                if (next != end && next.key == Key.GENERIC) {
                    next = next.getNext();
                }
                typeToken = new TokenGroup(token, next);

                while (next != end && next.key == Key.INDEX) {
                    if (next.getChild() != null && !next.isEmptyParent()) {
                        readArguments(getStack(), this.indexArguments, next.getChild(), next.getLastChild());
                    } else {
                        indexArguments.add(null);
                    }
                    next = next.getNext();
                }
                state = 2;
            } else if (state == 2 && token.key == Key.PARAM) {
                if (token.getChild() != null) {
                    readArguments(getStack(), this.arguments, token.getChild(), token.getLastChild());
                } else {
                    cFile.erro(token, "Unexpected end of tokens");
                }
                state = 3;
            } else if ((state == 2 || state == 3) && token.key == Key.BRACE) {
                initStack = new StackExpansion(getStack());
                if (token.getChild() != null) {
                    readArguments(initStack, this.initArguments, token.getChild(), token.getLastChild());
                } else {
                    cFile.erro(token, "Unexpected end of tokens");
                }
                state = 4;
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end && (state != 2 && state != 3 && state != 4)) {
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
        }
    }

    private void readArguments(Stack stack, ArrayList<Expression> arguments, Token start, Token end) {
        Token contentStart = start;
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.COMMA) {
                arguments.add(new Expression(stack, contentStart, token));
                state = 1;
            } else if (state == 1 && token.key != Key.COMMA) {
                contentStart = token;
                state = 0;
            }
            if (next == end) {
                if (state == 0) {
                    arguments.add(new Expression(stack, contentStart, next));
                } else {
                    cFile.erro(token, "Unexpected end of tokens");
                }
            }
            token = next;
        }
    }
}
