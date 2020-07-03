package logic.member;

import content.Key;
import content.Token;
import content.TokenGroup;
import builder.CppBuilder;
import logic.Pointer;
import logic.stack.Stack;
import logic.typdef.Type;

public class Destructor extends Member {

    private TokenGroup contentToken;
    private boolean hasImplementation;
    public Stack stack;

    public Destructor(Type type, Token start, Token end) {
        super(type, type.cFile);

        int state = 0;
        Token next;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key.isAttribute) {
                readModifier(cFile, token, false, true, false, false, false, false, false);
            } else if (state == 0 && token.key == Key.BITNOT) {
                state = 1;
            } else if (state == 1 && token.key == Key.THIS) {
                this.token = token;
                state = 2;
            } else if (state == 2 && token.key == Key.PARAM && token.getChild() != null) {
                if (token.getChild() != token.getLastChild()) {
                    cFile.erro(token, "A destruct cannot have parameters", this);
                }
                state = 3;
            } else if (state == 3 && token.key == Key.BRACE) {
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
                state = 4;
            } else if (state == 3 && token.key == Key.SEMICOLON) {
                contentToken = new TokenGroup(token, next);
                state = 4;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state != 4) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }

        isPublic = true;
    }

    @Override
    public boolean load() {
        if (!hasImplementation) {
            cFile.erro(contentToken.start, "A Destructor should implement", this);
        }

        return contentToken != null;
    }

    public void make() {
        if (hasImplementation) {
            stack = new Stack(cFile, token, type.self, Pointer.voidPointer, type, false, false, false);
            stack.read(contentToken.start, contentToken.end, true);
            stack.load();
        }
    }

    public void build(CppBuilder cBuilder) {
        cBuilder.toHeader();
        cBuilder.idt(1).add("virtual void destroy();").ln();

        cBuilder.toSource(type.template != null);
        cBuilder.add(type.template)
                .add("void ").path(type.self, false).add("::destroy() ").in(1)
                .add(stack, 1);

        if (type.parent != null) {
            cBuilder.idt(1).path(type.parent, false).add("::destroy();").ln();
        }

        cBuilder.out().ln()
                .ln();
    }

    @Override
    public String toString() {
        return "~this ()";
    }
}
