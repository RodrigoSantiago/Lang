package logic.member;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.CppBuilder;
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

    private Constructor constructorTarget;

    public Constructor(Type type, Token start, Token end) {
        super(type);

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

    public void make() {
        if (hasImplementation) {
            Stack stack = new Stack(cFile, token, type.self, Pointer.voidPointer, isStatic() ? null : type, false, isStatic(), true);
            stack.read(contentToken.start, contentToken.end, true);
            stack.addParam(getParams());
            stack.load();

            if (!isStatic()) {
                ConstructorCall call = stack.getConstructorCall();
                if (call != null) {
                    ConstructorView cv = stack.getConstructorCall().getConstructorView();
                    if (cv != null) {
                        constructorTarget = cv.constructor;
                        if (constructorTarget.linkTo(this)) {
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

    public void build(CppBuilder cBuilder) {
        if (isStatic()) {
            cBuilder.idt(1).add("// static initializer");
        } else {
            if (type.isPointer()) {
                cBuilder.toHeader();
                cBuilder.idt(1).path(type.self, false).add("*").add(" create(").add(params).add(");").ln();

                cBuilder.toSource(type.template != null);
                cBuilder.add(type.template)
                        .path(type.self, false).add("*")
                        .add(" ").path(type.self, false).add("::create(").add(params).add(") {").ln()
                        .idt(1).add("return this;").ln()
                        .add("}").ln()
                        .ln();
            } else {
                cBuilder.toHeader();
                if (params.isEmpty()) {
                    cBuilder.idt(1).add(type.pathToken).add("(empty e);").ln();
                } else if (params.getCount() == 1 && params.getTypePtr(0).equals(type.self)) {
                    cBuilder.idt(1).add(type.pathToken).add("(empty e, ").add(type.self).add(" v_").add(params.getNameToken(0)).add(");").ln();
                } else{
                    cBuilder.idt(1).add(type.pathToken).add("(").add(params).add(");").ln();
                }

                cBuilder.toSource(type.template != null);
                cBuilder.add(type.template)
                        .path(type.self, false).add("::").add(type.pathToken);
                if (params.isEmpty()) {
                    cBuilder.add("(empty e) : ").add(type.pathToken).add("() {").ln();
                } else if (params.getCount() == 1 && params.getTypePtr(0).equals(type.self)) {
                    cBuilder.add("(empty e, ").add(type.self)
                            .add(" v_").add(params.getNameToken(0)).add(") : ").add(type.pathToken).add("() {").ln();
                } else {
                    cBuilder.add("(").add(params).add(") : ").add(type.pathToken).add("() {").ln();
                }
                cBuilder.add("}").ln()
                        .ln();
            }
        }

        cBuilder.toHeader();
    }

    public Parameters getParams() {
        return params;
    }

    public void markDefault() {
        isDefault = true;
    }

    public boolean linkTo(Constructor constructor) {
        if (this == constructor) {
            return true;
        } else if (constructorTarget != null) {
            return constructorTarget.linkTo(constructor);
        }
        return false;
    }

    @Override
    public String toString() {
        return (isStatic() ? "static this (" : "this (") + params + ")";
    }
}
