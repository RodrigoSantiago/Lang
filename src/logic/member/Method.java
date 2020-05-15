package logic.member;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.CppBuilder;
import logic.GenericOwner;
import logic.templates.Generic;
import logic.templates.Template;
import logic.params.Parameters;
import logic.Pointer;
import logic.typdef.Type;

public class Method extends Member implements GenericOwner {

    public Token nameToken;
    public Token contentToken;
    public Parameters params;
    public Template template;

    public Pointer typePtr;
    public TokenGroup typeToken;

    public Method(Type type, Token start, Token end) {
        super(type);

        int state = 0;
        Token next;
        Token last = start;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key.isAttribute) {
                readModifier(cFile, token, true, true, type.isAbsAllowed(), type.isFinalAllowed(), true, true, false);
            } else if (state == 0 && token.key == Key.GENERIC) {
                template = new Template(cFile, token);
                state = 1;
            } else if ((state == 0 || state == 1) && token.key == Key.VOID) {
                typeToken = new TokenGroup(token, next);
                state = 2;
            } else if ((state == 0 || state == 1) && token.key == Key.WORD) {
                typeToken = new TokenGroup(token, next = TokenGroup.nextType(next, end));
                state = 2;
            } else if (state == 2 && token.key == Key.WORD) {
                this.token = token;
                nameToken = token;
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
            cFile.erro(last, "Unexpected end of tokens");
        }

        if (isAbstract() && template != null) {
            cFile.erro(template.token, "A abstract method cannot have templates");
            isAbstract = false;
            isFinal = true;
        }
    }

    public boolean toAbstract() {
        if (template != null) {
            if (!isAbstract) {
                cFile.erro(template.token, "A abstract method cannot have templates");
            }
            return false;
        } else {
            isAbstract = true;
            return true;
        }
    }

    public void toPublic() {
        isPrivate = false;
        isPublic = true;
    }

    @Override
    public boolean load() {
        if (typeToken != null) {
            if (typeToken.start.key == Key.VOID) {
                typePtr = Pointer.voidPointer;
            } else {
                typePtr = cFile.getPointer(typeToken.start, typeToken.end, null, this);
            }

            if (template != null) {
                template.load(null, isStatic() ? null : type);
                if (!isStatic()) {
                    boolean templateMiss = false;
                    if (type.template != null) {
                        for (Generic gen : template.generics) {
                            if (type.template.findGeneric(gen.nameToken) != null) {
                                cFile.erro(gen.nameToken, "Generic name conflict");
                                templateMiss = true;
                            }
                        }
                        if (templateMiss) {
                            return false;
                        }
                    }
                }
            }

            if (params != null) {
                params.load(this);

                return true;
            }

        }
        return false;
    }

    public void build(CppBuilder cBuilder) {

        cBuilder.toHeader();
        cBuilder.idt(1).add(template, 1);

        if (!isFinal() && !isStatic()) {
            cBuilder.add("virtual ");
        } else if (isStatic()) {
            cBuilder.add("static ");
        }
        cBuilder.add(typePtr)
                .add(" m_").add(nameToken).add("(").add(params).add(")").add(isAbstract() ? " = 0;" : ";").ln();

        if (!isAbstract()) {
            cBuilder.toSource();
            if (!isStatic()) {
                cBuilder.add(template);
            }
            cBuilder.add(typePtr)
                    .add(" ").path(type.self, isStatic()).add("::m_").add(nameToken)
                    .add("(").add(params).add(") {").ln()
                    .add(isStatic(), "    init();").ln()
                    .add("}").ln()
                    .ln();
        }

        cBuilder.toHeader();
    }

    @Override
    public String toString() {
        return nameToken+" (" + params+") : "+ typePtr;
    }

    @Override
    public Pointer findGeneric(Token genericToken) {
        Pointer pointer = null;
        if (template != null) {
            pointer = template.findGeneric(genericToken);
        }
        if (pointer == null && !isStatic) {
            pointer = type.findGeneric(genericToken);
        }
        return pointer;
    }

    public void setOverriden() {

    }
}
