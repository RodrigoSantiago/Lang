package logic.member;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.CppBuilder;
import logic.params.Parameters;
import logic.Pointer;
import logic.typdef.Type;

public class Operator extends Member {

    public Parameters params;

    public Token operator;
    public Key op = Key.NOONE;
    public Token contentToken;
    public TokenGroup typeToken;
    public Pointer typePtr;

    public Operator(Type type, Token start, Token end) {
        super(type);

        int state = 0;
        Token next;
        Token last = start;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key.isAttribute) {
                readModifier(cFile, token, true, true, true, true, true, true, false);
            } else if (state == 0 && token.key == Key.WORD) {
                typeToken = new TokenGroup(token, next = TokenGroup.nextType(next, end));
                state = 1;
            } else if (state == 1 && token.key == Key.OPERATOR) {
                state = 2;
            } else if (state == 2 && token.key.isOperator) {
                this.token = operator = token;
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
            } else if (state == 3 && token.key == Key.PARAM) {
                params = new Parameters(cFile, token);
                state = 4;
            } else if (state == 4 && (token.key == Key.BRACE || token.key == Key.SEMICOLON)) {
                contentToken = token;
                state = 5;
            } else {
                cFile.erro(token, "Unexpected token");
            }

            last = token;
            token = next;
        }

        if (state != 5) {
            cFile.erro(last, "Unexpected end of token");
        }
    }

    @Override
    public boolean load() {
        if (typeToken != null) {
            typePtr = cFile.getPointer(typeToken.start, typeToken.end, null, type);

            if (params != null) {
                params.load(type);

                if (params.args.size() == 1) {
                    if (!params.args.get(0).typePtr.equals(type.self)) {
                        cFile.erro(operator, "The first parameter must be the current Type");

                        return false;
                    } else if (op != Key.INC && op != Key.DEC &&
                                op != Key.ADD && op != Key.SUB &&
                                op != Key.CAST && op != Key.AUTO &&
                                op != Key.NOT && op != Key.BITNOT && op != Key.SETVAL) {

                        cFile.erro(operator, "The operator must have two parameters");
                        return false;
                    }
                } else if (params.args.size() == 2) {
                    if (!params.args.get(0).typePtr.equals(type.self) && !params.args.get(1).typePtr.equals(type.self)) {
                        cFile.erro(operator, "The first or second parameter must be the current Type");
                        return false;
                    } else if (op == Key.INC || op == Key.DEC ||
                            op == Key.CAST || op == Key.AUTO ||
                            op == Key.NOT || op == Key.BITNOT || op == Key.SETVAL) {
                        cFile.erro(operator, "The operator must have a single parameter");
                        return false;
                    }
                }

                if (typePtr.equals(type.self) && (op == Key.CAST || op == Key.AUTO)) {
                    cFile.erro(params.token, "The casting operators cannot return the current type");
                    return false;
                }

                if (typePtr.equals(type.parent) && (op == Key.CAST || op == Key.AUTO)) {
                    cFile.erro(params.token, "The casting operators cannot return the Wrapper Parent");
                    return false;
                }

                return true;
            }
        }
        return false;
    }

    public void build(CppBuilder cBuilder) {

        cBuilder.toHeader();
        cBuilder.idt(1).add(type.template, 1);

        cBuilder.add("static ")
                .add(typePtr)
                .add(" ").add(op, typePtr).add("(").add(params).add(");").ln();

        if (!isAbstract()) {
            cBuilder.toSource();
            cBuilder.add(type.template)
                    .add(typePtr)
                    .add(" ").path(type.self, false).add("::").add(op, typePtr)
                    .add("(").add(params).add(") {").ln()
                    .add("}").ln()
                    .ln();
        }

        cBuilder.toHeader();
    }

    @Override
    public String toString() {
        return "operator " + op + " (" + params + ") : " + typePtr;
    }
}
