package logic;

import content.Key;
import content.Token;
import data.ContentFile;
import logic.typdef.Type;
import data.Compiler;

public class Using {

    public final ContentFile cFile;

    public Token nameToken, memberToken;
    private boolean isStatic, isDirect, isUsable = true;

    private Namespace namespace;
    private Type directType;
    private Type staticType;

    public Using(ContentFile cFile, Token start, Token end) {
        this.cFile = cFile;

        int state = 0;
        Token next;
        Token token = start;
        Token last = token;
        while (token != end) {
            last = token;
            next = token.getNext();
            if (state == 0 && token.key == Key.USING) {
                state = 1;
            } else if (state == 1 && token.key == Key.WORD) {
                nameToken = token;
                state = 2;
            } else if (state == 2 && token.key == Key.DOT) {
                isStatic = true;
                state = 3;
            } else if (state == 2 && token.key == Key.MUL) {
                isDirect = false;
                isStatic = false;
                state = 4;
            } else if (state == 2 && (token.key == Key.SEMICOLON || next == end)) {
                isDirect = true;
                isStatic = false;
                state = (next == end ? 5 : 6);
            } else if (state == 3 && token.key == Key.MUL) {
                isDirect = false;
                state = 4;
            } else if (state == 3 && token.key == Key.WORD) {
                isDirect = true;
                memberToken = token;
                state = 4;
            }else if (state == 4 && (token.key == Key.SEMICOLON || next == end)) {
                state = (next == end ? 5 : 6);
            } else {
                cFile.erro(token, "Unexpected token");
            }

            token = next;
        }

        if (nameToken == null) {
            isUsable = false;
        }

        if (isDirect && !isStatic && nameToken.endsWith("::")) {
            isDirect = false;
            cFile.erro(nameToken, "Unexpected end of tokens");
        }

        if (state != 6) {
            cFile.erro(last, "Unexpected end of tokens");
        }
    }

    public void preload() {
        if (isStatic) {
            Type type = Compiler.findType(nameToken);
            if (type == null) {
                isUsable = false;
                cFile.erro(nameToken, "Type not found");
            } else {
                staticType = type;
            }
        } else if (isDirect) {
            Type type = Compiler.findType(nameToken);
            if (type == null) {
                isUsable = false;
                cFile.erro(nameToken, "Type not found");
            } else {
                directType = type;
            }
        } else {
            namespace = Compiler.findNamespace(nameToken.toString(0, nameToken.length - 2));
            if (namespace == null) {
                isUsable = false;
                cFile.erro(nameToken, "Namespace not found");
            }
        }
    }

    public boolean isUsable() {
        return isUsable;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isDirect() {
        return isDirect;
    }

    public Type getStaticType() {
        return staticType;
    }

    public Type findType(Token tokenType) {
        if (isStatic || !isUsable) {
            return null;
        }

        if (isDirect()) {
            if (directType.nameToken.equals(tokenType)) {
                return directType;
            }
        } else {
            return namespace.findType(tokenType);
        }
        return null;
    }

    @Override
    public String toString() {
        return nameToken.toString() + (isStatic ? isDirect ? ("." + memberToken) : ".*" : "*");
    }
}
