package logic.stack;

import content.Key;
import content.Token;
import logic.Pointer;

import java.util.HashMap;

public class StackExpansion extends Stack {

    public HashMap<Token, Field> shadowFields = new HashMap<>();
    Stack source;

    public StackExpansion(Stack source, Token referenceToken, Pointer returnPtr) {
        super(source, referenceToken, returnPtr);
        this.source = source;
    }

    public Stack getSource() {
        return source;
    }

    @Override
    void thisBase() {

    }

    @Override
    public Field findField(Token nameToken) {
        if (nameToken.key == Key.BASE) return null;

        Field local = super.findField(nameToken);
        if (local != null) {
            return local;
        } else {
            Field shadow = shadowFields.get(nameToken);
            if (shadow == null) {
                Field outside = source.findField(nameToken);
                if (outside != null) {
                    if ((outside.getTypePtr().isPointer() && !outside.getTypePtr().let) ||
                            outside.getName().key == Key.THIS) {
                        shadow = new Field(this, outside);
                        shadowFields.put(outside.getName(), shadow);
                    } else {
                        return outside;
                    }
                }
            }
            return shadow;
        }
    }
}
