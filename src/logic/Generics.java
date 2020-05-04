package logic;

import content.Key;
import content.Token;
import data.ContentFile;
import logic.typdef.Type;

import java.util.ArrayList;

public class Generics {

    Type owner;
    ContentFile cFile;
    private boolean isStatic;
    private ArrayList<Generic> generics = new ArrayList<>();

    public Generics(Type owner, Token genericToken, boolean isStatic) {
        this.owner = owner;
        this.cFile = owner.cFile;

        this.isStatic = isStatic;
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
                generics.add(new Generic(nameToken, null, null));
                state = 0;
            } else if (state == 2 && token.key == Key.WORD) {
                if (next != null && (next.key == Key.GENERIC)) {
                    next = next.getNext();
                }
                while (next != null && next.key == Key.INDEX) {
                    next = next.getNext();
                }
                generics.add(new Generic(nameToken, token, next));
                state = 3;
            } else if (state == 3 && token.key == Key.COMMA) {
                state = 0;
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (state == 1 && next == end) {
                generics.add(new Generic(nameToken, null, null));
            }
            if (state == 2 && next == end) {
                generics.add(new Generic(nameToken, null, null));
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
            if (generic.typeToken == null) {
                generic.pointer = Pointer.openPointer;
            } else {
                generic.pointer = cFile.getPointer(generic.typeToken, generic.end, cycleOwner, genericOwner);
            }
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
