package logic.stack;

import content.Token;
import logic.Pointer;

public class StackExpansion extends Stack {

    Stack source;

    public StackExpansion(Stack source, Token referenceToken, Pointer returnPtr) {
        super(source, referenceToken, returnPtr);
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
