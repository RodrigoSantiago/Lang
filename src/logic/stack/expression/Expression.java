package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.ContentFile;
import builder.CppBuilder;
import logic.Pointer;
import logic.stack.Context;
import logic.stack.Line;
import logic.stack.Stack;

import java.util.ArrayList;

public class Expression {

    public final ContentFile cFile;
    public final Stack stack;
    public final Line parent;
    private TokenGroup tokenGroup;
    Token start, end;

    ArrayList<CallGroup> groups = new ArrayList<>();

    public Expression(Line line, Token start, Token end) {
        this(line, start, end, null);
    }

    public Expression(Line line, Token start, Token end, Pointer arrayInit) {
        this.cFile = line.cFile;
        this.stack = line.stack;
        this.parent = line;
        this.start = start;
        this.end = end;
        tokenGroup = new TokenGroup(start, end);

        Token dot = null;
        Token contentStart = null;
        Token token = start;
        Token next;
        int state = 0;

        CallGroup group = new CallGroup(this);

        // Inner Array Init
        if (start != null && start.key == Key.BRACE && arrayInit != null) {
            group.add(new InitCall(group, start, start.getNext(), arrayInit));
            add(group);

            token = start.getNext();
            while (token != null && token != end) {
                cFile.erro(token, "Unexpected token", this);
                token = token.getNext();
            }
        }

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
                    if (next != end && next.key == Key.LET) {
                        next = next.getNext();
                    }
                    if (next != end && next.key == Key.WORD) {
                        next = next.getNext();
                        next = TokenGroup.nextType(next, end);
                    } else if (next != end && next.key == Key.VOID) {
                        next = next.getNext();
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
                group.add(new LiteralCall(group, token, next));
                state = 2;
            } else if (state == 0 && (token.key == Key.WORD || token.key == Key.THIS || token.key == Key.BASE)) {
                contentStart = token;
                state = 1;
            } else if (state == 1 && token.key == Key.DOT) {
                dot = token;
                group.add(new FieldCall(group, contentStart, contentStart.getNext()));
                state = 3;
            } else if (state == 1 && token.key == Key.PARAM) {
                if (contentStart.key == Key.WORD) {
                    group.add(new MethodCall(group, contentStart, next));
                } else {
                    group.add(new ConstructorCall(group, contentStart, next));
                }
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

                TokenGroup typeGroup = null;
                if (token.key == Key.IS || token.key == Key.ISNOT) {
                    if (next != end && next.key == Key.WORD) {
                        Token it = next;
                        typeGroup = new TokenGroup(it, next = TokenGroup.nextType(next.getNext(), end));
                    } else {
                        cFile.erro(token, "Type identifier expected", this);
                    }
                }
                if (typeGroup != null) {
                    group.add(new TypeCall(group, typeGroup.start, typeGroup.end));
                    add(group);

                    group = new CallGroup(this);
                    state = 4;
                } else {
                    state = 0;
                }
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end) {
                if (state == 1) {
                    group.add(new FieldCall(group, contentStart, contentStart.getNext()));
                } else if (state == 3) {
                    cFile.erro(dot, "Unexpected end of tokens", this);
                }
                if (!group.isEmpty()) {
                    add(group);
                }
            }
            token = next;
        }
    }

    public TokenGroup getTokenGroup() {
        return tokenGroup;
    }

    public void add(CallGroup group) {
        groups.add(group);
    }

    public CallGroup  getGroup() {
        return groups.get(0);
    }

    public Pointer getRequestPtr() {
        return groups.size() == 1 ? groups.get(0).getRequestPtr() : null;
    }

    public boolean isLiteral() {
        return groups.size() == 1 && groups.get(0).isLiteral();
    }

    public LiteralCall getLiteral() {
        return groups.size() == 1 ? groups.get(0).getLiteral() : null;
    }

    public void load(Context context) {
        boolean ternary = false;
        int maxLevel = 1;

        // Level [Right Operators]
        for (int i = 0; i < groups.size(); i++) {
            CallGroup group = groups.get(i);
            CallGroup next = i == groups.size() - 1 ? null : groups.get(i + 1);

            if (group.isOperator()) {
                if (group.getOperatorPriority() > maxLevel) {
                    maxLevel = group.getOperatorPriority();
                }
                if (group.getOperatorToken().key == Key.COLON || group.getOperatorToken().key == Key.QUEST) {
                    ternary = true;
                }
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
                    groups.set(i, groupMerge);
                    i --;
                    if (i > -1) i--;
                }
            }
        }

        // Level 1-10 [Bi-Operators]
        for (int level = 1; level < maxLevel + 1 && level < 11; level++) {
            readOperators(level, 0, groups.size());
        }

        // entre [?](...)[?] e entre [?](...)[:]
        boolean preview = ternary;
        while (preview) {
            preview = false;
            int i = 0;
            int state = 0;
            int init = 0;
            while (i < groups.size()) {
                CallGroup group = groups.get(i);
                if (state == 0 && group.isOperator() && group.getOperatorToken().key == Key.QUEST) {
                    init = i;
                    state = 1;
                } else if (state == 1 && group.isOperator() && (group.getOperatorToken().key == Key.QUEST ||
                        group.getOperatorToken().key == Key.COLON)) {
                    if (i - (init + 1) > 1) {
                        readCompositeOperators(init + 1, i);
                        preview = true;
                        break;
                    } else if (group.getOperatorToken().key == Key.QUEST) {
                        init = i;
                    } else {
                        state = 0;
                    }
                } else if (state == 1) {
                    // SAY nothing
                }
                i++;
            }
        }

        // Ternary Expression
        while (ternary) {
            ternary = false;
            int i = groups.size() - 1;
            int state = 0;
            CallGroup option = null, colon = null, second = null, quest = null;
            while (i >= 0) {
                CallGroup group = groups.get(i);
                if (state == 0 && !group.isOperator()) {
                    option = group;
                    state = 1;
                } else if (state == 1 && group.isOperator() && group.getOperatorToken().key == Key.COLON) {
                    colon = group;
                    state = 2;
                } else if (state == 1 && group.isOperator() && group.getOperatorToken().key != Key.COLON) {
                    state = 0;
                } else if (state == 2 && !group.isOperator()) {
                    second = group;
                    state = 3;
                } else if (state == 3 && group.isOperator() && group.getOperatorToken().key == Key.COLON) {
                    option = second;
                    colon = group;
                    ternary = true;
                    state = 2;
                }  else if (state == 3 && group.isOperator() && group.getOperatorToken().key == Key.QUEST) {
                    quest = group;
                    state = 4;
                } else if (state == 4 && !group.isOperator()) {
                    CallGroup groupMerge = new CallGroup(this, group, quest, second, colon, option);
                    groups.remove(i + 4);
                    groups.remove(i + 3);
                    groups.remove(i + 2);
                    groups.remove(i + 1);
                    groups.set(i, groupMerge);
                    option = groupMerge;
                    state = 1;
                    if (ternary) break;
                } else if (group.isOperator()) {
                    cFile.erro(group.getTokenGroup(), "Unexpected Operator", this);
                    groups.remove(i);
                } else {
                    cFile.erro(group.getTokenGroup(), "Unexpected line", this);
                    groups.remove(i);
                }
                i --;
                if (i < 0) {
                    if (state == 2) {
                        cFile.erro(group.getTokenGroup(), "Unexpected Operator", this);
                        groups.remove(0);
                    } else if (state == 3) {
                        cFile.erro(group.getTokenGroup(), "Incomplete Ternary Operator", this);
                        groups.remove(0);
                        groups.remove(0);
                    } else if (state == 4) {
                        cFile.erro(group.getTokenGroup(), "Incomplete Ternary Operator", this);
                        groups.remove(0);
                        groups.remove(0);
                        groups.remove(0);
                    }
                }
            }
        }

        // Set and Composite Set
        if (maxLevel == 11) {
            readCompositeOperators(0, groups.size());
        }

        if (groups.size() != 1) {
            System.out.println("INVALID MULT GROUP EXPRESSION ?");
        } else {
            groups.get(0).load(context);
        }
    }

    private void readOperators(int level, int start, int end) {
        int i = start;
        int state = 0;
        CallGroup init = null, op = null;
        while (i < end) {
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
                    end -= 2;
                    init = groupMerge;
                } else {
                    init = group;
                    op = null;
                }
                state = 1;
            } else if (group.isOperator()) {
                cFile.erro(group.getTokenGroup(), "Unexpected Operator", this);
                groups.remove(i);
                i -= 1;
                end -= 1;
            } else {
                cFile.erro(group.getTokenGroup(), "Unexpected line", this);
                groups.remove(i);
                i -= 1;
                end -= 1;
            }
            i += 1;
            if (state == 2 && i == end) {
                cFile.erro(group.getTokenGroup(), "Unexpected Operator", this);
                groups.remove(i);
            }
        }
    }

    private void readCompositeOperators(int start, int end) {
        int i = end - 1;
        int state = 0;
        CallGroup init = null, op = null;
        while (i >= start) {
            CallGroup group = groups.get(i);
            if (state == 0 && !group.isOperator()) {
                init = group;
                state = 1;
            } else if (state == 1 && group.isOperator()) {
                op = group;
                state = 2;
            } else if (state == 2 && !group.isOperator()) {
                if (op.getOperatorPriority() == 11) {
                    CallGroup groupMerge = new CallGroup(this, group, op, init);
                    groups.remove(i + 2);
                    groups.remove(i + 1);
                    groups.set(i, groupMerge);
                    init = groupMerge;
                } else {
                    init = group;
                    op = null;
                }
                state = 1;
            } else if (group.isOperator()) {
                cFile.erro(group.getTokenGroup(), "Unexpected Operator", this);
                groups.remove(i);
            } else {
                cFile.erro(group.getTokenGroup(), "Unexpected line", this);
                groups.remove(i);
            }
            i --;
            if (state == 2 && i < start) {
                cFile.erro(group.getTokenGroup(), "Unexpected Operator", this);
                groups.remove(i);
            }
        }
    }

    public int verify(Pointer pointer) {
        return groups.size() == 0 ? 0 : groups.get(0).verify(pointer);
    }

    public Pointer getNaturalPtr(Pointer convertFlag) {
        return groups.size() > 0 ? groups.get(0).getNaturalPtr(convertFlag) : Pointer.voidPointer;
    }

    public Pointer getNaturalPtr() {
        return groups.size() > 0 ? groups.get(0).getNaturalPtr() : Pointer.voidPointer;
    }

    public void request() {
        if (groups.size() > 0) groups.get(0).request();
    }

    public void requestGet(Pointer pointer) {
        if (groups.size() > 0) groups.get(0).requestGet(pointer);
    }

    public void requestOwn(Pointer pointer) {
        if (groups.size() > 0) groups.get(0).requestOwn(pointer);
    }

    public void build(CppBuilder cBuilder, int idt) {
        groups.get(0).build(cBuilder, idt);
    }

    public void markArgument() {
        if (groups.size() > 0) groups.get(0).markArgument();
    }

    public void markLine() {
        if (groups.size() > 0) groups.get(0).markLine();
    }
}
