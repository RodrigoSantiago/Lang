package logic.member;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.ContentFile;
import logic.GenericOwner;
import logic.Template;
import logic.params.Parameters;
import logic.Pointer;
import logic.typdef.Type;

public class Method extends Member implements GenericOwner {

    Token nameToken;
    Token contentToken;
    Pointer returnType;
    Parameters params;
    Template template;
    TokenGroup returnTypeToken;

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
                returnTypeToken = new TokenGroup(token, next = TokenGroup.nextType(next, end));
                state = 2;
            } else if (state == 2 && token.key == Key.WORD) {
                this.token = token;
                nameToken = token;
                state = 3;
            } else if (token.key == Key.PARAM) {
                if (state != 3 || params != null) {
                    cFile.erro(token, "Unexpected parameters");
                } else {
                    state = 4;
                }
                if (params == null) {
                    params = new Parameters(cFile, token);
                }
            } else if (token.key == Key.BRACE || token.key == Key.SEMICOLON) {
                if (state != 4 || contentToken != null) {
                    cFile.erro(token, "Unexpected token");
                } else {
                    state = 5;
                }
                if (contentToken == null) {
                    contentToken = token;
                }
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end && state != 5) {
                cFile.erro(token, "Unexpected end of token");
            }
            token = next;
        }

        returnType = cFile.getPointer(returnTypeToken.start, returnTypeToken.end, null, this);
    }

    @Override
    public boolean load() {
        return true;
    }

    @Override
    public String toString() {
        return nameToken+" (" + params+") : "+ returnType;
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
