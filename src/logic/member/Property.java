package logic.member;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.ContentFile;
import logic.member.view.FieldView;
import logic.typdef.Type;

import java.util.ArrayList;

public class Property extends Member {

    Token nameToken;
    Token contentToken;
    TokenGroup typeToken;
    TokenGroup initToken;
    Token getContentToken, setContentToken, ownContentToken;

    boolean hasGet, isGetFinal, isGetAbstract, isGetPublic, isGetPrivate;
    boolean hasOwn, isOwnFinal, isOwnAbstract, isOwnPublic, isOwnPrivate;
    boolean hasSet, isSetFinal, isSetAbstract, isSetPublic, isSetPrivate;
    boolean isGetOwn;

    public Property(Type type, Token start, Token end) {
        super(type);

        int state = 0;
        Token next;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key.isAttribute) {
                readModifier(cFile, token, true, true, true, true, true, true, false);
            } else if (state == 0 && token.key == Key.WORD) {
                if (next != null && next != end && (next.key == Key.GENERIC)) {
                    next = next.getNext();
                }
                while (next != null && next != end && next.key == Key.INDEX) {
                    next = next.getNext();
                }
                typeToken = new TokenGroup(token, next);
                state = 1;
            } else if (state == 1 && token.key == Key.WORD) {
                this.token = token;
                nameToken = token;
                state = 2;
            } else if (token.key == Key.BRACE) {
                if (state != 2 || contentToken != null) {
                    cFile.erro(token, "Unexpected token");
                } else {
                    state = 3;
                }
                if (contentToken == null) {
                    contentToken = token;
                    if (contentToken.getChild() != null) {
                        readBlocks(contentToken.getChild(), contentToken.getLastChild());
                    }
                }
            } else if (state == 3 && token.key == Key.SETVAL) {
                while (next != null && next != end && next.key != Key.SEMICOLON) {
                    next = next.getNext();
                }
                if (next != end && next != null) {
                    next = next.getNext();
                } else {
                    cFile.erro(token, "Unexpected end of tokens");
                }
                initToken = new TokenGroup(token, next);
                state = 4;
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end && state < 3) {
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
        }
    }

    private void readBlocks(Token init, Token end) {
        boolean isPrivate = false, isPublic = false, isAbstract = false, isFinal = false;
        boolean getOwn = false;
        int type = 0;

        int state = 0;
        Token next;
        Token token = init;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key.isAttribute) {
                if (token.key == Key.PUBLIC || token.key == Key.PRIVATE) {
                    if (isPublic || isPrivate) {
                        cFile.erro(token, "Repeated acess modifier");
                    } else {
                        isPublic = (token.key == Key.PUBLIC);
                        isPrivate = (token.key == Key.PRIVATE);
                    }
                } else if (token.key == Key.FINAL || token.key == Key.ABSTRACT) {
                    if (isFinal || isAbstract) {
                        cFile.erro(token, "Repeated modifier");
                    } else {
                        isFinal = (token.key == Key.FINAL);
                        isAbstract = (token.key == Key.ABSTRACT);
                    }
                } else {
                    cFile.erro(token, "Unexpected modifier");
                }
            } else if (state == 0 && token.equals("get")) {
                type = 0;
                state = 1;
            } else if (state == 0 && token.equals("set")) {
                type = 1;
                state = 1;
            } else if (state == 0 && token.equals("own")) {
                type = 2;
                state = 1;
            } else if (state == 1 && token.key == Key.COMMA) {
                state = 2;
            } else if (state == 2 && token.equals("get")) {
                if (type == 0) {
                    if (hasGet) {
                        cFile.erro(token, "Repeated get");
                    } else {
                        getOwn = true;
                    }
                } else {
                    cFile.erro(token, "Unexpected token");
                }
                state = 3;
            } else if (state == 2 && token.equals("own")) {
                if (type == 2) {
                    if (hasOwn) {
                        cFile.erro(token, "Repeated own");
                    } else {
                        getOwn = true;
                    }
                } else {
                    cFile.erro(token, "Unexpected token");
                }
                state = 3;
            } else if (token.key == Key.BRACE || token.key == Key.SEMICOLON) {
                if (state != 1 && state != 3) {
                    cFile.erro(token, "Unexpected token");
                }
                if (state >= 1) {
                    if (type == 0) {
                        hasGet = true;
                        getContentToken = token;
                        isGetPublic = isPublic;
                        isGetPrivate = isPrivate;
                        isGetFinal = isFinal;
                        isGetAbstract = isAbstract;
                    } else if (type == 1) {
                        hasSet = true;
                        setContentToken = token;
                        isSetPublic = isPublic;
                        isSetPrivate = isPrivate;
                        isSetFinal = isFinal;
                        isSetAbstract = isAbstract;
                    } else {
                        hasOwn = true;
                        ownContentToken = token;
                        isOwnPublic = isPublic;
                        isOwnPrivate = isPrivate;
                        isOwnFinal = isFinal;
                        isOwnAbstract = isAbstract;
                    }
                    isGetOwn = getOwn;
                    type = 0;
                    isPublic = isPrivate = isAbstract = isFinal = getOwn = false;
                }
                state = 0;
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end && state != 0) {
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
        }
    }

    @Override
    public boolean load() {
        return true;
    }

    public FieldView getField() {
        return new FieldView(nameToken, null, this);
    }

    public boolean hasGet() {
        return hasGet || isGetOwn;
    }

    public boolean isGetFinal() {
        return isGetFinal;
    }

    public boolean isGetAbstract() {
        return isGetAbstract;
    }

    public boolean isGetPublic() {
        return isGetPublic;
    }

    public boolean isGetPrivate() {
        return isGetPrivate;
    }

    public boolean hasSet() {
        return hasSet;
    }

    public boolean isSetFinal() {
        return isSetFinal;
    }

    public boolean isSetAbstract() {
        return isSetAbstract;
    }

    public boolean isSetPublic() {
        return isSetPublic;
    }

    public boolean isSetPrivate() {
        return isSetPrivate;
    }

    public boolean hasOwn() {
        return hasOwn || isGetOwn;
    }

    public boolean isOwnFinal() {
        return isOwnFinal;
    }

    public boolean isOwnAbstract() {
        return isOwnAbstract;
    }

    public boolean isOwnPublic() {
        return isOwnPublic;
    }

    public boolean isOwnPrivate() {
        return isOwnPrivate;
    }

    @Override
    public String toString() {
        return nameToken+" : "+ typeToken+" {} ";
    }


}
