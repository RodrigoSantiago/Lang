package logic.stack;

import content.Token;
import data.ContentFile;
import logic.stack.block.BlockEmpty;

public class StackExpansion extends Stack {

    Stack source;

    public StackExpansion(Stack source) {
        super(source.cFile, false);
    }

    public void read(Token start, Token end, boolean read) {
        block = new BlockEmpty(this, start, end, read);
    }

    public Stack getSource() {
        return source;
    }
}
