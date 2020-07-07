package logic.member;

import content.Key;
import content.Token;
import content.TokenGroup;
import builder.CppBuilder;
import logic.params.Parameters;
import logic.Pointer;
import logic.stack.Stack;
import logic.typdef.Type;

public class Operator extends Member {

    private TokenGroup typeToken;
    private Pointer typePtr;
    private Parameters params;

    private Token operator;
    private Key op = Key.NOONE;

    private TokenGroup contentToken;
    private boolean hasImplementation;
    private Stack stack;

    public Operator(Type type, Token start, Token end) {
        super(type, type.cFile, start);

        int state = 0;
        Token next;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key.isAttribute) {
                readModifier(cFile, token, true, true, false, true, true, true, false);
            } else if (state == 0 && token.key == Key.WORD) {
                typeToken = new TokenGroup(token, next = TokenGroup.nextType(next, end));
                state = 1;
            } else if (state == 1 && token.key == Key.OPERATOR) {
                state = 2;
            } else if (state == 2 && token.key.isOperator) {
                this.token = operator = token;
                if (!token.key.isOverridable) {
                    cFile.erro(token, "This operator cannot be overriden", this);
                }
                op = token.key;
                state = 3;
            } else if (state == 2 && token.equals("cast")) {
                this.token = operator = token;
                op = Key.CAST;
                state = 3;
            } else if (state == 2 && token.equals("auto")) {
                this.token = operator = token;
                op = Key.AUTO;
                state = 3;
            } else if (state == 3 && token.key == Key.PARAM && token.getChild() != null) {
                params = new Parameters(cFile, token);
                state = 4;
            } else if (state == 4 && token.key == Key.BRACE) {
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
                hasImplementation = true;
                state = 5;
            } else if (state == 4 && token.key == Key.SEMICOLON) {
                contentToken = new TokenGroup(token, next);
                state = 5;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state != 5) {
                cFile.erro(token, "Unexpected end of token", this);
            }
            token = next;
        }

    }

    @Override
    public boolean load() {
        if (contentToken != null && contentToken.start.key == Key.SEMICOLON) {
            cFile.erro(contentToken.start, "A Operator should implement", this);
        }

        if (typeToken != null) {
            typePtr = cFile.getPointer(typeToken.start, typeToken.end, null, type, isLet());
            if (typePtr == null) {
                typePtr = cFile.langObjectPtr(isLet());
            } else if (!typePtr.equals(cFile.langBoolPtr())) {
                if (getOp() == Key.EQUAL || getOp() == Key.DIF || getOp() == Key.LESS || getOp() == Key.ELESS ||
                        getOp() == Key.MORE || getOp() == Key.EMORE) {
                    cFile.erro(operator, "The comparing operators must return bool", this);
                    return false;
                }
            }

            if (params != null) {
                params.load(type);

                if (params.getCount() == 1) {
                    if (!params.getTypePtr(0).equals(type.self)) {
                        cFile.erro(operator, "The first parameter must be the current Type", this);

                        return false;
                    } else if (getOp() != Key.INC && getOp() != Key.DEC &&
                                getOp() != Key.ADD && getOp() != Key.SUB &&
                                getOp() != Key.CAST && getOp() != Key.AUTO &&
                                getOp() != Key.NOT && getOp() != Key.BITNOT && getOp() != Key.SETVAL) {

                        cFile.erro(operator, "The operator must have two parameters", this);
                        return false;
                    }
                } else if (params.getCount() == 2) {
                    if (!params.getTypePtr(0).equals(type.self) && !params.getTypePtr(1).equals(type.self)) {
                        cFile.erro(operator, "The first or second parameter must be the current Type", this);
                        return false;
                    } else if (getOp() == Key.INC || getOp() == Key.DEC ||
                            getOp() == Key.CAST || getOp() == Key.AUTO ||
                            getOp() == Key.NOT || getOp() == Key.BITNOT || getOp() == Key.SETVAL) {
                        cFile.erro(operator, "The operator must have a single parameter", this);
                        return false;
                    }
                } else {
                    cFile.erro(operator, "The operator must have one or two parameters", this);
                    return false;
                }

                if (typePtr.equals(type.self) && (getOp() == Key.CAST || getOp() == Key.AUTO)) {
                    cFile.erro(params.token, "The casting operators cannot return the current type", this);
                    return false;
                }

                if (typePtr.isPointer() && (getOp() == Key.CAST || getOp() == Key.AUTO)) {
                    cFile.erro(params.token, "The casting operators cannot return a Class neither an Interface", this);
                    return false;
                }

                return true;
            }
        }
        return false;
    }

    public void make() {
        if (hasImplementation) {
            stack = new Stack(cFile, token, type.self, typePtr, type, false, true, false, getParams(), null);
            stack.read(contentToken.start, contentToken.end, true);
            stack.load();
        }
    }

    public void build(CppBuilder cBuilder) {
        cBuilder.toHeader();
        cBuilder.idt(1).add(type.template, 1);
        cBuilder.add("static ")
                .add(typePtr)
                .add(" ").nameOp(getOp(), typePtr).add("(").add(params).add(");").ln();
        cBuilder.toSource(type.template != null);
        cBuilder.add(type.template)
                .add(typePtr)
                .add(" ").path(type.self, false).add("::").nameOp(getOp(), typePtr)
                .add("(").add(params).add(") ").in(1)
                .add(stack, 1)
                .out().ln()
                .ln();
    }

    public void buildOperator(CppBuilder cBuilder) {
        if (op == Key.EQUAL || op == Key.DIF) {
            cBuilder.toHeader();
            cBuilder.add("inline bool operator ").add(op.string).add("(const ").path(params.getTypePtr(0), false).add("& left, const ")
                    .path(params.getTypePtr(1), false).add("& right) ").in(1);
            cBuilder.idt(1).add("return ").path(type.self, false).add("::").nameOp(op, typePtr).add("(left, right);").ln();
            cBuilder.out().ln()
                    .ln();
        } else if (isCasting()) {
            cBuilder.toHeader();
            Pointer in = getParams().getTypePtr(0);
            cBuilder.add("template<>").ln()
                    .add("struct cast<").add(type.self).add(", ").add(getTypePtr()).add("> {").ln()
                    .idt(1).add("inline static ").add(getTypePtr()).add(" as(const ").add(in).add("& from) {").ln()
                    .idt(2).add("return ").path(type.self, false).add("::").nameOp(op, typePtr).add("(from);").ln()
                    .idt(1).add("}").ln()
                    .idt(1).add("inline static bool is(const ").path(type.self, false).add("& from) { return true; }").ln()
                    .idt(1).add("inline static bool is(const ")
                    .path(type.self, false).add("& from, ").path(getTypePtr(), false).add("& result) {").ln()
                    .idt(2).add("result = as(from);").ln()
                    .idt(2).add("return true;").ln()
                    .idt(1).add("}").ln()
                    .add("};").ln()
                    .ln();
        }
    }

    public static void buildAutomatic(CppBuilder cBuilder, Type type, Operator equal, Operator dif) {
        if (equal == null && dif == null) {
            cBuilder.toHeader();
            cBuilder.idt(1).add(type.template, 1);
            cBuilder.add("static ")
                    .add(type.cFile.langBoolPtr())
                    .add(" equal(").add(type.self).add(" left,").add(type.self).add(" right);").ln();

            cBuilder.toSource(type.template != null);
            cBuilder.add(type.template)
                    .add(type.cFile.langBoolPtr())
                    .add(" ").path(type.self, false).add("::equal(")
                    .add(type.self).add(" left,").add(type.self).add(" right) ").in(1);
            boolean val = false;
            cBuilder.idt(1).add("return ");
            for (Variable variable : type.variables) {
                if (!variable.isStatic()) {
                    for (int i = 0; i < variable.getCount(); i++) {
                        if (val) {
                            cBuilder.add(" && ").ln().idt(2);
                        }
                        val = true;
                        cBuilder.add("left.").nameField(variable.getName(i)).add(" == right.")
                                .nameField(variable.getName(i));
                    }
                }
            }
            if (!val) {
                cBuilder.add("true");
            }
            cBuilder.add(";").ln();
            cBuilder.out().ln()
                    .ln();
        }
    }

    public static void buildAutomaticOperator(CppBuilder cBuilder, Type type, Operator equal, Operator dif) {
        if (equal == null) {
            cBuilder.toHeader();
            cBuilder.add("inline bool operator ==(const ").path(type.self, false).add("& left, const ")
                    .path(type.self, false).add("& right) ").in(1);
            if (dif != null) {
                cBuilder.idt(1).add("return !").path(type.self, false).add("::dif(left, right);").ln();
            } else {
                cBuilder.idt(1).add("return ").path(type.self, false).add("::equal(left, right);").ln();
            }
            cBuilder.out().ln()
                    .ln();
        }

        if (dif == null) {
            cBuilder.toHeader();
            cBuilder.add("inline bool operator !=(const ").path(type.self, false).add("& left, const ")
                    .path(type.self, false).add("& right) ").in(1);
            cBuilder.idt(1).add("return !").path(type.self, false).add("::equal(left, right);").ln();
            cBuilder.out().ln()
                    .ln();
        }
    }
    public Token getOperator() {
        return operator;
    }

    public Key getOp() {
        return op;
    }

    public Pointer getTypePtr() {
        return typePtr;
    }

    public Parameters getParams() {
        return params;
    }

    public Stack getStack() {
        return stack;
    }

    public boolean isCasting() {
        return getOp() == Key.CAST || getOp() == Key.AUTO;
    }

    @Override
    public String toString() {
        return "operator " + getOp() + " (" + params + ") : " + typePtr;
    }
}
