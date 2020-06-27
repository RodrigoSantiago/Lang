package logic.templates;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.ContentFile;
import logic.GenericOwner;
import logic.Pointer;
import logic.typdef.Type;

import java.util.ArrayList;

public class Template {

    private final ContentFile cFile;
    private final boolean isOpenAllowed;
    private final ArrayList<Generic> generics = new ArrayList<>();

    public final Token token;

    public Template(ContentFile cFile, Token template, boolean isOpenAllowed) {
        this.cFile = cFile;
        this.token = template;
        this.isOpenAllowed = isOpenAllowed;
        Token start = template.getChild();
        Token end = template.getLastChild();

        Token nameToken = null;

        int state = 0;
        Token next;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.WORD) {
                if (token.isComplex()) cFile.erro(token, "Complex names are not allowed", this);
                nameToken = token;
                if (next == end) {
                    generics.add(new Generic(this, generics.size(), nameToken, null));
                    nameToken = null;
                    state = 0;
                } else {
                    state = 1;
                }
            } else if (state == 1 && token.key == Key.COMMA) {
                generics.add(new Generic(this, generics.size(), nameToken, null));
                nameToken = null;
                state = 0;
            } else if (state == 1 && token.key == Key.COLON) {
                state = 2;
            } else if (state == 2 && token.key == Key.WORD) {
                TokenGroup typeToken = new TokenGroup(token, next = TokenGroup.nextType(next, end));
                generics.add(new Generic(this, generics.size(), nameToken, typeToken));
                nameToken = null;

                if (next == end) {
                    state = 0;
                } else {
                    state = 3;
                }
            } else if (state == 3 && token.key == Key.COMMA) {
                state = 0;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if ((state == 1 || state == 2) && next == end) {
                generics.add(new Generic(this, generics.size(), token, null));
                state = 0;
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }

        if (state != 0 || generics.size() == 0) {
            cFile.erro(end != null ? end : template, "Unexpected end of tokens", this);
        }
    }

    public void preload(Type type) {
        for (Generic generic : generics) {
            if (generic.typeToken != null) {
                type.inheritanceType(generic.typeToken.start, generic.typeToken.end);
            }
        }
    }

    public void load(Type owner) {
        for (Generic generic : generics) {
            if (generic.typeToken != null) {
                generic.basePtr = cFile.getPointer(generic.typeToken.start, generic.typeToken.end, owner, null, false);
                if (generic.basePtr == null) {
                    generic.basePtr = cFile.langObjectPtr();
                }
                if (generic.basePtr.type != null && generic.basePtr.type.isFinal()) {
                    generic.basePtr = cFile.langObjectPtr();
                    cFile.erro(generic.typeToken.start, "A Generic cannot be a final Type", this);
                }
                generic.defaultPtr = new Pointer(generic.basePtr.type, generic.basePtr.pointers, false);
            } else {
                generic.basePtr = isOpenAllowed ? Pointer.openPointer : cFile.langObjectPtr();
                generic.defaultPtr = cFile.langObjectPtr();
            }
            generic.typePtr = new Pointer(generic.basePtr.type, generic.basePtr.pointers, generic, false);
        }
    }

    public Pointer findGeneric(Token genericName) {
        for (Generic generic : generics) {
            if (genericName.equals(generic.nameToken)) {
                return generic.typePtr;
            }
        }
        return null;
    }

    public int getCount() {
        return generics.size();
    }

    public Token getNameToken(int pos) {
        return generics.get(pos).nameToken;
    }

    public Pointer getTypePtr(int pos) {
        return generics.get(pos).typePtr;
    }

    public Pointer getBasePtr(int pos) {
        return generics.get(pos).basePtr;
    }

    public Pointer getDefaultPtr(int pos) {
        return generics.get(pos).defaultPtr;
    }
}
