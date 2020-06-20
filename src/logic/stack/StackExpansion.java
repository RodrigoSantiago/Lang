package logic.stack;

import content.Token;
import data.ContentFile;

public class StackExpansion extends Stack {

    Stack source;

    public StackExpansion(Stack source) {
        super(source.cFile, false);
    }

    public Stack getSource() {
        return source;
    }
}
