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
}
