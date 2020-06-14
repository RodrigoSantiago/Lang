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
    private boolean openPointer;

    public Token token;
    public ArrayList<Generic> generics = new ArrayList<>();

    public Template(ContentFile cFile, Token template, boolean openPointer) {
        this.cFile = cFile;
        this.token = template;
        this.openPointer = openPointer;
        Token start = template.getChild();
        Token end = template.getLastChild();

        Token nameToken = null;

        int state = 0;
        Token next;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.WORD) {
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
                cFile.erro(token, "Unexpected token");
            }
            if ((state == 1 || state == 2) && next == end) {
                generics.add(new Generic(this, generics.size(), token, null));
                state = 0;
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
        }

        if (state != 0 || generics.size() == 0) {
            cFile.erro(end != null ? end : template, "Unexpected end of tokens");
        }
    }

    public void preload(Type type) {
        for (Generic generic : generics) {
            if (generic.typeToken != null) {
                type.inheritanceType(generic.typeToken.start, generic.typeToken.end);
            }
        }
    }

    public void load(Type cycleOwner, GenericOwner genericOwner) {
        for (Generic generic : generics) {
            if (generic.typeToken != null) {
                generic.basePtr = cFile.getPointer(generic.typeToken.start, generic.typeToken.end,
                        cycleOwner, genericOwner);
                if (generic.basePtr.type != null && generic.basePtr.type.isFinal()) {
                    generic.basePtr = cFile.langObject();
                    cFile.erro(generic.typeToken.start, "A Generic cannot be a final Type");
                }
            } else {
                generic.basePtr = openPointer ? Pointer.openPointer : cFile.langObject();
            }
            generic.typePtr = new Pointer(generic.basePtr.type, generic.basePtr.pointers, generic);
            generic.defaultPtr = new Pointer(generic.basePtr.type, generic.basePtr.pointers);
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
}
