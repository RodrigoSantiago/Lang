package logic.stack;

import content.Token;
import data.ContentFile;
import logic.Pointer;
import logic.stack.block.BlockEmpty;

public class StackExpansion extends Stack {

    Stack source;

    public StackExpansion(Stack source, Pointer returnPtr) {
        super(source, returnPtr);
        this.source = source;
    }

    public Stack getSource() {
        return source;
    }

    @Override
    public Field findField(Token nameToken) {
        Field local = super.findField(nameToken);
        if (local != null) {
            return local;
        } else {
            return source.findField(nameToken);
        }
    }
}
