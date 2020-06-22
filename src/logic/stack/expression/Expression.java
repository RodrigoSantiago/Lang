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
        int maxLevel = 1;

        // Level [Right Operators]
        for (int i = 0; i < groups.size(); i++) {
            CallGroup group = groups.get(i);
            CallGroup next = i == groups.size() - 1 ? null : groups.get(i + 1);

            if (group.isOperator() && group.getOperatorPriority() > maxLevel) {
                maxLevel = group.getOperatorPriority();
            }
            if (!group.isOperator() && next != null && next.isOperatorRight()) {
                CallGroup groupMerge = new CallGroup(this, group, next);
                groups.remove(i);
                groups.set(i, groupMerge);
                i --;
            }
        }

        // Level [Left Operators]
        for (int i = 0; i < groups.size(); i++) {
            CallGroup prev = i > 0 ? groups.get(i - 1) : null;
            CallGroup group = groups.get(i);
            CallGroup next = i == groups.size() - 1 ? null : groups.get(i + 1);
            if (next != null && !next.isOperator()) {
                if (group.isOperatorLeft() || (group.isOperatorBoth() && (prev == null || prev.isOperator()))) {
                    CallGroup groupMerge = new CallGroup(this, group, next);
                    groups.remove(i);
                    groups.set(i - 1, groupMerge);
                    i -= 1;
                }
            }
        }

        // Level 1-10 [Bi-Operators]
        for (int level = 1; level < maxLevel; level++) {
            int i = 0;
            int state = 0;
            CallGroup init = null, op = null;
            while (i < groups.size()) {
                CallGroup group = groups.get(i);
                if (state == 0 && !group.isOperator()) {
                    init = group;
                    state = 1;
                } else if (state == 1 && group.isOperator()) {
                    op = group;
                    state = 2;
                } else if (state == 2 && !group.isOperator()) {
                    if (op.getOperatorPriority() == level) {
                        CallGroup groupMerge = new CallGroup(this, init, op, group);
                        groups.remove(i - 2);
                        groups.remove(i - 2);
                        groups.set(i - 2, groupMerge);
                        i -= 2;
                        init = groupMerge;
                    } else {
                        init = group;
                        op = null;
                    }
                    state = 1;
                } else if (group.isOperator()) {
                    cFile.erro(group.getToken(), "Unexpected Operator", this);
                    groups.remove(i);
                    i -= 1;
                } else {
                    cFile.erro(group.getToken(), "Unexpected line", this);
                    groups.remove(i);
                    i -= 1;
                }
                i += 1;
                if (state == 2 && i == groups.size()) {
                    cFile.erro(group.getToken(), "Unexpected Operator", this);
                    groups.remove(i);
                    i -= 1;
                }
            }
        }

        if (groups.size() > 1) {
            System.out.println("HOW ?");
        } else if (groups.size() > 0) {
            groups.get(0).load(context);
        }
    }

    public int verify(Pointer pointer) {
        return groups.size() == 0 ? -1 : groups.get(0).verify(pointer);
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
