package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.ContentFile;
import logic.Pointer;
import logic.stack.Line;
import logic.stack.Stack;

import java.util.ArrayList;

public class Expression {

    public final ContentFile cFile;
    public final Stack stack;
    Pointer returnPtr;
    Token start, end;

    ArrayList<CallGroup> groups = new ArrayList<>();

    // Never recives [;] -> just mark error
    public Expression(Line line, Token start, Token end) {
        this.cFile = line.cFile;
        this.stack = line.stack;
        this.start = start;
        this.end = end;

        CallGroup group = new CallGroup(this);

        Token dot = null;
        Token contentStart = null;
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.NEW) {
                if (next != end && next.key == Key.WORD) {
                    next = TokenGroup.nextType(next, end);
                    if (next != end && next.key == Key.PARAM) {
                        next = next.getNext();
                    }
                    if (next != end && next.key == Key.BRACE) {
                        next = next.getNext();
                    }
                    group.add(new Call(group, token, next, Call.INSTANCE));
                    state = 2;
                } else if (next != end) {
                    cFile.erro(next, "Unexpected token");
                } else {
                    cFile.erro(token, "Unexpected end of tokens");
                    break;
                }
            } else if (state == 0 && token.key == Key.PARAM) {
                if (next == end || next.key == Key.DOT || next.key == Key.INDEX || next.key.isOperator) {
                    // Inner Expression
                    group.add(new Call(group, token, next, Call.INNER));
                    state = 2;
                } else if (next.key == Key.LAMBDA) {
                    next = next.getNext();
                    if (next != end && next.key == Key.WORD) {
                        next = TokenGroup.nextType(next, end);
                    }
                    if (next != end && next.key == Key.BRACE) {
                        next = next.getNext();
                    }
                    group.add(new Call(group, token, next, Call.LAMBDA));
                    state = 2;
                } else {
                    group.add(new Call(group, token, next, Call.CASTING));
                    state = 0;
                }
            } else if (state == 0 && (token.key == Key.DEFAULT || token.key == Key.NULL ||
                    token.key == Key.NUMBER || token.key == Key.STRING ||
                    token.key == Key.TRUE || token.key == Key.FALSE)) {
                // Literal
                group.add(new Call(group, token, next, Call.LITERAL));
                state = 2;
            } else if (state == 0 && (token.key == Key.WORD || token.key == Key.THIS || token.key == Key.SUPER)) {
                contentStart = token;
                state = 1;
            } else if (state == 1 && token.key == Key.DOT) {
                dot = token;
                group.add(new Call(group, contentStart, contentStart.getNext(), Call.FIELD));
                state = 3;
            } else if (state == 1 && token.key == Key.PARAM) {
                group.add(new Call(group, contentStart, next, Call.METHOD));
                state = 2;
            } else if (state == 1 && token.key == Key.INDEX) {
                group.add(new Call(group, contentStart, contentStart.getNext(), Call.FIELD));
                group.add(new Call(group, token, next, Call.INDEXER));
                state = 2;
            } else if (state == 2 && token.key == Key.DOT) {
                dot = token;
                state = 3;
            } else if (state == 2 && token.key == Key.INDEX) {
                group.add(new Call(group, token, next, Call.INDEXER));
                state = 2;
            } else if (state == 3 && token.key == Key.WORD) {
                contentStart = token;
                state = 1;
            } else if (token.key.isOperator) {
                if (state == 1) {
                    group.add(new Call(group, contentStart, contentStart.getNext(), Call.FIELD));
                } else if (state == 3) {
                    cFile.erro(dot, "Unexpected end of tokens");
                }
                if (group.isEmpty()) {
                    group.setOperator(token);
                } else {
                    add(group);
                    group = new CallGroup(this);
                    group.setOperator(token);
                }
                add(group);
                group = new CallGroup(this);
                state = 0;
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end) {
                if (state == 1) {
                    group.add(new Call(group, contentStart, contentStart.getNext(), Call.FIELD));
                } else if (state == 3) {
                    cFile.erro(dot, "Unexpected end of tokens");
                }
            }
            token = next;
        }
    }

    public void make() {
        // load group types
        // load operators
    }

    public void add(CallGroup group) {
        groups.add(group);
    }

    public Pointer getReturnType() {
        return returnPtr;
    }
}
