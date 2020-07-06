package logic.member;

import content.Key;
import content.Token;
import content.TokenGroup;
import builder.CppBuilder;
import logic.Pointer;
import logic.member.view.ConstructorView;
import logic.params.Parameters;
import logic.stack.Stack;
import logic.stack.expression.ConstructorCall;
import logic.typdef.Type;

public class Constructor extends Member {

    private Parameters params;
    private TokenGroup contentToken;
    private boolean hasImplementation;

    private Stack stack;
    private Constructor constructorTarget;

    public Constructor(Type type, Token start, Token end) {
        super(type, type.cFile);

        int state = 0;
        Token next;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();

            if (state == 0 && token.key.isAttribute) {
                readModifier(cFile, token, true, true, false, false, true, false, true);
            } else if (state == 0 && token.key == Key.THIS) {
                this.token = token;
                state = 1;
            } else if (state == 1 && token.key == Key.PARAM && token.getChild() != null) {
                params = new Parameters(cFile, token);
                state = 2;
            } else if (state == 2 && token.key == Key.BRACE) {
                hasImplementation = true;
                if (token.getChild() == null) {
                    if (next != end) {
                        contentToken = new TokenGroup(next, end);
                        next = end;
                    }
                    cFile.erro(token, "Brace closure expected", this);
                } else {
                    if (token.isOpen()) cFile.erro(token, "Brace closure expected", this);
                    contentToken = new TokenGroup(token.getChild(), token.getLastChild());
                }
                state = 3;
            } else if (state == 2 && token.key == Key.SEMICOLON) {
                contentToken = new TokenGroup(token, next);
                state = 3;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state != 3) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }
    }

    @Override
    public boolean load() {
        if (!hasImplementation) {
            cFile.erro(contentToken.start, "A Constructor should implement", this);
        }

        if (token != null && params != null) {
            params.load(isStatic() ? null : type);

            if (isStatic() && params.getCount() > 0) {
                cFile.erro(token, "Static constructors cannot have parameters", this);
            }

            return true;
        }
        return false;
    }

    @Override
    public void make() {
        if (hasImplementation) {
            stack = new Stack(cFile, token, type.self, Pointer.voidPointer, isStatic() ? null : type,
                    false, isStatic(), true, getParams(), null);
            stack.read(contentToken.start, contentToken.end, true);
            stack.load();

            if (!isStatic()) {
                ConstructorCall call = stack.getConstructorCall();
                if (call != null) {
                    ConstructorView cv = stack.getConstructorCall().getConstructorView();
                    if (cv != null) {
                        constructorTarget = cv.constructor;
                        if (constructorTarget.setTarget(this)) {
                            constructorTarget = null;
                            cFile.erro(call.token, "Cyclic constructor call", this);
                        }
                    }
                } else {
                    constructorTarget = type.getParentEmptyConstructor();
                    if (constructorTarget == null && type.parent != null && type.isClass()) {
                        cFile.erro(token, "The constructor must call a local or parent constructor", this);
                    }
                }
            }
        }
    }

    @Override
    public void build(CppBuilder cBuilder) {
        if (isStatic()) {
            cBuilder.add(stack, 1);
        } else {
            if (type.isPointer()) {
                cBuilder.toHeader();
                cBuilder.idt(1).path(type.self, false).add("*").add(" create(").add(params).add(");").ln();

                cBuilder.toSource(type.template != null);
                cBuilder.add(type.template)
                        .path(type.self, false).add("*")
                        .add(" ").path(type.self, false).add("::create(").add(params).add(") ").in(1);
                if (stack.getConstructorCall() == null && constructorTarget != null) {
                    cBuilder.idt(1).add(type.parent.type.pathToken).add("::create();").ln();
                }
                if (stack.getConstructorCall() == null && type.hasInstanceInit()) {
                    cBuilder.idt(1).add("init();").ln();
                }
                cBuilder.add(stack, 1)
                        .idt(1).add("return this;").ln()
                        .out().ln()
                        .ln();
            } else {
                cBuilder.toHeader();
                if (params.isEmpty()) {
                    cBuilder.idt(1).add("explicit ").add(type.pathToken).add("(empty e);").ln();
                } else if (params.getCount() == 1 && params.getTypePtr(0).equals(type.self)) {
                    cBuilder.idt(1).add("explicit ").add(type.pathToken).add("(empty e, ").add(type.self).add(" v_").add(params.getNameToken(0)).add(");").ln();
                } else{
                    cBuilder.idt(1).add("explicit ").add(type.pathToken).add("(").add(params).add(");").ln();
                }

                cBuilder.toSource(type.template != null);
                cBuilder.add(type.template)
                        .path(type.self, false).add("::").add(type.pathToken);
                if (params.isEmpty()) {
                    cBuilder.add("(empty e) : ");
                } else if (params.getCount() == 1 && params.getTypePtr(0).equals(type.self)) {
                    cBuilder.add("(empty e, ").add(type.self).add(" v_").add(params.getNameToken(0)).add(") : ");
                } else {
                    cBuilder.add("(").add(params).add(") : ");
                }
                if (constructorTarget == null) {
                    cBuilder.add(type.pathToken).add("() ").in(1);
                    if (type.hasInstanceInit()) {
                        cBuilder.idt(1).add("init();").ln();
                    }
                } else {
                    stack.getConstructorCall().buildDirect(cBuilder, 0);
                    cBuilder.in(1);
                }
                cBuilder.add(stack, 1)
                        .out().ln()
                        .ln();
            }
        }
    }

    public void buildEmpty(Type type, CppBuilder cBuilder) {
        cBuilder.toHeader();
        cBuilder.idt(1).path(type.self, false).add("*").add(" create();").ln();

        cBuilder.toSource(type.template != null);
        cBuilder.add(type.template)
                .path(type.self, false).add("*")
                .add(" ").path(type.self, false).add("::create() ").in(1)
                .idt(1).path(type.parent, false).add("::create();").ln()
                .idt(1).add("return this;").ln()
                .out().ln()
                .ln();
    }

    public void toDefault() {
        isDefault = true;
    }

    public boolean setTarget(Constructor constructor) {
        if (this == constructor) {
            return true;
        } else if (constructorTarget != null) {
            return constructorTarget.setTarget(constructor);
        }
        return false;
    }

    public Parameters getParams() {
        return params;
    }

    public Stack getStack() {
        return stack;
    }

    @Override
    public String toString() {
        return (isStatic() ? "static this (" : "this (") + params + ")";
    }
}
