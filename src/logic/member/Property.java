package logic.member;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.Pointer;
import logic.member.view.FieldView;
import logic.typdef.Type;

public class Property extends Member {

    Token nameToken;
    Token contentToken;
    TokenGroup initToken;
    TokenGroup typeToken;
    Pointer typePtr;
    private Token getContentToken, setContentToken, ownContentToken;

    private boolean hasGet, isGetFinal, isGetAbstract, isGetPublic, isGetPrivate;
    private boolean hasOwn, isOwnFinal, isOwnAbstract, isOwnPublic, isOwnPrivate;
    private boolean hasSet, isSetFinal, isSetAbstract, isSetPublic, isSetPrivate;
    private boolean isGetOwn;

    public Property(Type type, Token start, Token end) {
        super(type);

        int state = 0;
        Token next;
        Token last = start;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key.isAttribute) {
                readModifier(cFile, token, true, true, type.isAbsAllowed(), type.isFinalAllowed(), true, true, false);
            } else if (state == 0 && token.key == Key.WORD) {
                typeToken = new TokenGroup(token, next = TokenGroup.nextType(next, end));
                state = 1;
            } else if (state == 1 && token.key == Key.WORD) {
                this.token = token;
                nameToken = token;
                state = 2;
            } else if (state == 2 && token.key == Key.BRACE) {
                contentToken = token;
                readBlocks(contentToken);
                state = 3;
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

            last = token;
            token = next;
        }

        if (state < 3) {
            cFile.erro(last, "Unexpected end of tokens");
        }
    }

    private void readBlocks(Token group) {
        boolean isPrivate = false, isPublic = false, isAbstract = false, isFinal = false;
        boolean getOwn = false;
        int t = 0;

        int state = 0;
        Token next;
        Token end =  group.getLastChild();
        Token token = group.getChild();
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
                } else if ((type.isFinalAllowed() && token.key == Key.FINAL) ||
                        (type.isAbsAllowed() && token.key == Key.ABSTRACT)) {
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
                t = 0;
                state = 1;
            } else if (state == 0 && token.equals("set")) {
                t = 1;
                state = 1;
            } else if (state == 0 && token.equals("own")) {
                t = 2;
                state = 1;
            } else if (state == 1 && token.key == Key.COMMA) {
                state = 2;
            } else if (state == 2 && token.equals("get")) {
                if (t == 0) {
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
                if (t == 2) {
                    if (hasOwn) {
                        cFile.erro(token, "Repeated own");
                    } else {
                        getOwn = true;
                    }
                } else {
                    cFile.erro(token, "Unexpected token");
                }
                state = 3;
            } else if ((state == 1 || state == 3) && (token.key == Key.BRACE || token.key == Key.SEMICOLON)) {
                if (t == 0) {
                    if (hasGet || isGetOwn) cFile.erro(token, "Repeated get");
                    hasGet = true;
                    getContentToken = token;
                    isGetPublic = isPublic;
                    isGetPrivate = isPrivate;
                    isGetFinal = isFinal;
                    isGetAbstract = isAbstract;
                } else if (t == 1) {
                    if (hasSet) cFile.erro(token, "Repeated set");
                    hasSet = true;
                    setContentToken = token;
                    isSetPublic = isPublic;
                    isSetPrivate = isPrivate;
                    isSetFinal = isFinal;
                    isSetAbstract = isAbstract;
                } else {
                    if (hasOwn || isGetOwn) cFile.erro(token, "Repeated own");
                    hasOwn = true;
                    ownContentToken = token;
                    isOwnPublic = isPublic;
                    isOwnPrivate = isPrivate;
                    isOwnFinal = isFinal;
                    isOwnAbstract = isAbstract;
                }
                isGetOwn = getOwn;
                t = 0;
                isPublic = isPrivate = isAbstract = isFinal = getOwn = false;
                state = 0;
            } else {
                cFile.erro(token, "Unexpected token");
            }
            token = next;
        }

        if (state != 0 || (!hasGet && !hasSet && !hasOwn)) {
            cFile.erro(end != null ? end : group, "Unexpected end of tokens");
        }
    }

    public void toAbstract() {
        isAbstract = true;
        isGetAbstract = true;
        isSetAbstract = true;
        isOwnAbstract = true;
    }

    @Override
    public boolean load() {
        if (typeToken != null) {
            typePtr = cFile.getPointer(typeToken.start, typeToken.end, null, isStatic() ? null : type);

            return hasGet || hasSet || hasOwn;
        }
        return false;
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
