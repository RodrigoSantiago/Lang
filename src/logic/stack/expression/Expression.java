package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.ContentFile;
import logic.Pointer;
import logic.stack.Context;
import logic.stack.Line;
import logic.stack.Stack;

import java.util.ArrayList;

public class Expression {

    public final ContentFile cFile;
    public final Stack stack;
    public final Line parent;
    Pointer returnPtr;
    Token start, end;

    ArrayList<CallGroup> groups = new ArrayList<>();

    public Expression(Line line, Token start, Token end) {
        this.cFile = line.cFile;
        this.stack = line.stack;
        this.parent = line;
        this.start = start;
        this.end = end;
        System.out.println("EXPR: "+ TokenGroup.toString(start, end));

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
                    next = TokenGroup.nextType(next.getNext(), end);
                    if (next != end && next.key == Key.PARAM) {
                        next = next.getNext();
                    }
                    if (next != end && next.key == Key.BRACE) {
                        next = next.getNext();
                    }
                    group.add(new InstanceCall(group, token, next));
                    state = 2;
                } else if (next != end) {
                    cFile.erro(next, "Unexpected token", this);
                } else {
                    cFile.erro(token, "Unexpected end of tokens", this);
                    break;
                }
            } else if (state == 0 && token.key == Key.PARAM) {
                if (next == end || next.key == Key.DOT || next.key == Key.INDEX || next.key.isOperator) {
                    // Inner Expression
                    group.add(new InnerCall(group, token, next));
                    state = 2;
                } else if (next.key == Key.LAMBDA) {
                    next = next.getNext();
                    if (next != end && next.key == Key.WORD) {
                        next = next.getNext();
                        next = TokenGroup.nextType(next, end);
                    }
                    if (next != end && next.key == Key.BRACE) {
                        next = next.getNext();
                    }
                    group.add(new LambdaCall(group, token, next));
                    state = 2;
                } else {
                    group.setCastingOperator(token);
                    add(group);
                    group = new CallGroup(this);

                    state = 0;
                }
            } else if (state == 0 && (token.key == Key.DEFAULT || token.key == Key.NULL ||
                    token.key == Key.NUMBER || token.key == Key.STRING ||
                    token.key == Key.TRUE || token.key == Key.FALSE)) {
                // Literal
                group.add(new LiteralCall(group, token, next));
                state = 2;
            } else if (state == 0 && (token.key == Key.WORD || token.key == Key.THIS || token.key == Key.SUPER)) {
                contentStart = token;
                state = 1;
            } else if (state == 1 && token.key == Key.DOT) {
                dot = token;
                group.add(new FieldCall(group, contentStart, contentStart.getNext()));
                state = 3;
            } else if (state == 1 && token.key == Key.PARAM) {
                group.add(new MethodCall(group, contentStart, next));
                state = 2;
            } else if (state == 1 && token.key == Key.INDEX) {
                group.add(new FieldCall(group, contentStart, contentStart.getNext()));
                group.add(new IndexerCall(group, token, next));
                state = 2;
            } else if (state == 2 && token.key == Key.DOT) {
                dot = token;
                state = 3;
            } else if (state == 2 && token.key == Key.INDEX) {
                group.add(new IndexerCall(group, token, next));
                state = 2;
            } else if (state == 3 && token.key == Key.WORD) {
                contentStart = token;
                state = 1;
            } else if (token.key.isOperator || token.key == Key.COLON) {
                if (state == 1) {
                    group.add(new FieldCall(group, contentStart, contentStart.getNext()));
                } else if (state == 3) {
                    cFile.erro(dot, "Unexpected end of tokens", this);
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
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end) {
                if (state == 1) {
                    group.add(new FieldCall(group, contentStart, contentStart.getNext()));
                } else if (state == 3) {
                    cFile.erro(dot, "Unexpected end of tokens", this);
                }
            }
            token = next;
        }
        if (start == end) {
            System.out.println("EMPTY");
        }
    }

    public void load(Context context) {
        for (int i = 0; i < groups.size(); i++) {
            CallGroup group = groups.get(i);
            if (group.isOperator()) {
                // ordem aqui ?
            } else {
                group.load(context);
            }
        }
    }

    public boolean request(Pointer pointer) {
        return false;
    }

    public void add(CallGroup group) {
        groups.add(group);
    }

    public Pointer getReturnType() {
        return returnPtr;
    }
}
