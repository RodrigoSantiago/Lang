package logic;

import content.Key;
import content.Token;
import data.ContentFile;
import logic.typdef.Type;

import java.util.ArrayList;

public class Template {

    ContentFile cFile;
    public ArrayList<Generic> generics = new ArrayList<>();

    public Template(ContentFile cFile, Token genericToken) {
        this.cFile = cFile;

        int state = 0;

        Token nameToken = null;
        Token end = genericToken.getLastChild();
        Token token = genericToken.getChild();
        Token next;

        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.WORD) {
                nameToken = token;
                state = 1;
            } else if (state == 1 && token.key == Key.COLON) {
                state = 2;
            } else if (state == 1 && token.key == Key.COMMA) {
                generics.add(new Generic(this, generics.size(), nameToken, null, null));
                state = 0;
            } else if (state == 2 && token.key == Key.WORD) {
                if (next != null && (next.key == Key.GENERIC)) {
                    next = next.getNext();
                }
                while (next != null && next.key == Key.INDEX) {
                    next = next.getNext();
                }
                generics.add(new Generic(this, generics.size(), nameToken, token, next));
                state = 3;
            } else if (state == 3 && token.key == Key.COMMA) {
                state = 0;
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (state == 1 && next == end) {
                generics.add(new Generic(this, generics.size(), nameToken, null, null));
            }
            if (state == 2 && next == end) {
                generics.add(new Generic(this, generics.size(), nameToken, null, null));
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
        }

        if (state == 0 && generics.size() == 0) {
            cFile.erro(genericToken, "Unexpected end of tokens");
        }
    }

    public void preload(Type type) {
        for (Generic generic : generics) {
            if (generic.typeToken != null) {
                type.inheritanceType(generic.typeToken, generic.end);
            }
        }
    }

    public void load(Type cycleOwner, GenericOwner genericOwner) {
        for (Generic generic : generics) {
            Pointer pointer;
            if (generic.typeToken == null) {
                pointer = Pointer.openPointer;
            } else {
                pointer = cFile.getPointer(generic.typeToken, generic.end, cycleOwner, genericOwner);
            }
            generic.pointer = new Pointer(pointer.type, pointer.pointers, generic);
        }
    }

    public Pointer findGeneric(Token genericName) {
        for (Generic generic : generics) {
            if (genericName.equals(generic.nameToken)) {
                return generic.pointer;
            }
        }
        return null;
    }
}
