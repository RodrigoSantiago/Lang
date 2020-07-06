package logic.stack;

import content.Key;
import content.Token;
import logic.Pointer;
import logic.params.Parameters;

import java.util.HashMap;

public class StackExpansion extends Stack {

    Stack source;
    Line parent;

    public StackExpansion(Line parent, Stack source, Token referenceToken, Pointer returnPtr, Parameters param) {
        super(source, referenceToken, returnPtr);
        this.parent = parent;
        this.param = param;
        this.source = source;
    }

    public Stack getSource() {
        return source;
    }

    @Override
    public void thisBase() {

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
                            outside.getName().key == Key.THIS || isYieldMode()) {
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

    @Override
    public boolean isChildOf(Block block) {
        return parent != null && parent.isChildOf(block);
    }
}
