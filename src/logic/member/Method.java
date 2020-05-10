package logic.member;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.GenericOwner;
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
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key.isAttribute) {
                readModifier(cFile, token, true, true, true, true, true, true, false);
            } else if (state == 0 && token.key == Key.GENERIC) {
                template = new Template(cFile, token);
                state = 1;
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
            if (next == end && state != 5) {
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
        }
    }

    @Override
    public boolean load() {
        if (typeToken != null) {
            typePtr = cFile.getPointer(typeToken.start, typeToken.end, null, this);

            if (params != null) {
                params.load();

                return true;
            }
        }
        return false;
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
}
