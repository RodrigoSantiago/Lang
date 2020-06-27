package logic.stack.line;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.Pointer;
import logic.stack.Block;
import logic.stack.Context;
import logic.stack.Line;
import logic.stack.expression.Expression;

import java.util.ArrayList;

public class LineVar extends Line {

    TokenGroup typeToken;
    ArrayList<Token> nameTokens = new ArrayList<>();
    ArrayList<Expression> expresions = new ArrayList<>();
    ArrayList<Pointer> typePtrs = new ArrayList<>();

    public boolean isFinal, isLet;
    public Pointer typePtr;

    public LineVar(Block block, Token start, Token end) {
        this(block, start, end, false);
    }

    public LineVar(Block block, Token start, Token end, boolean foreachVar) {
        super(block, start, end);
        System.out.println("VAR");

        Token init = null;
        Token nameToken = null;
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.FINAL && !isFinal) {
                isFinal = true;
            } else if (state == 0 && token.key == Key.LET) {
                isLet = true;
                state = 1;
            } else if (state == 0 && token.key == Key.VAR) {
                state = 1;
            } else if (state == 0 && token.key == Key.WORD) {
                typeToken = new TokenGroup(token, next = TokenGroup.nextType(next, end));
                state = 2;
            } else if (state == 1 && token.key == Key.WORD) {
                if (isLet) {
                    if (next == end || next.key == Key.SEMICOLON || next.key == Key.SETVAL || next.key == Key.COMMA) {
                        if (token.isComplex()) cFile.erro(token, "Complex names are not allowed", this);
                        nameToken = token;
                        state = 3;
                    } else {
                        typeToken = new TokenGroup(token, next = TokenGroup.nextType(next, end));
                        state = 2;
                    }
                } else {
                    if (token.isComplex()) cFile.erro(token, "Complex names are not allowed", this);
                    nameToken = token;
                    state = 3;
                }
            } else if (state == 2 && token.key == Key.WORD) {
                if (token.isComplex()) cFile.erro(token, "Complex names are not allowed", this);
                nameToken = token;
                state = 3;
            } else if (!foreachVar && state == 3 && token.key == Key.SETVAL) {
                init = next;
                while (next != end && next.key != Key.COMMA && next.key != Key.SEMICOLON) {
                    next = next.getNext();
                }
                state = 4;
            } else if (!foreachVar && (state == 3 || state == 4) && token.key == Key.COMMA) {
                nameTokens.add(nameToken);
                expresions.add(state == 4 ? new Expression(this, init, token) : null);
                nameToken = null;
                init = null;
                state = 2;
            } else if ((state == 3 || state == 4) && (token.key == Key.SEMICOLON || next == end)) {
                if (token.key != Key.SEMICOLON && !foreachVar) {
                    cFile.erro(token, "Semicolon expected", this);
                }

                nameTokens.add(nameToken);
                expresions.add(state == 4 ? new Expression(this, init, token.key == Key.SEMICOLON ? token : next) : null);
                nameToken = null;
                init = null;
                state = 5;
            } else if (state != 1) {
                cFile.erro(token, "Unexpected token", this);
            }
            if (state != 5 && next == end && (state != 3 || foreachVar)) {
                if (nameToken != null) {
                    nameTokens.add(nameToken);
                    expresions.add(null);
                }
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }
    }

    @Override
    public void load() {
        if (typeToken != null) {
            typePtr = stack.getPointer(typeToken, isLet);
            if (typePtr == null) {
                typePtr = cFile.langObjectPtr(isLet);
            }
        }

        for (int i = 0; i < expresions.size(); i++) {
            Expression expresion = expresions.get(i);
            if (expresion != null) {
                expresion.load(new Context(stack));

                Pointer naturalPtr = typePtr;
                if (typePtr == null) {
                    naturalPtr = expresion.getNaturalPtr(null);
                    if (naturalPtr == null) naturalPtr = cFile.langObjectPtr(isLet);
                    else if (isLet) naturalPtr = naturalPtr.toLet();
                    typePtrs.add(naturalPtr);
                }
                expresion.requestOwn(naturalPtr);
            } else if (typePtr == null) {
                typePtrs.add(cFile.langObjectPtr());
                cFile.erro(nameTokens.get(i), "Cannot determine a Type without an initialization", this);
            }
        }

        for (int i = 0; i < nameTokens.size(); i++) {
            Token nameToken = nameTokens.get(i);
            if (!stack.addField(nameToken, typePtr == null ? typePtrs.get(i) : typePtr, isFinal, parent)) {
                cFile.erro(nameToken, "Repeated field name", this);
            }
        }
    }
}