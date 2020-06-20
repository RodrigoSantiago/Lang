package logic.stack;

import content.Key;
import content.Parser;
import content.Token;
import data.ContentFile;
import logic.Pointer;
import logic.stack.block.*;
import logic.stack.line.*;

public class Stack {

    public ContentFile cFile;
    Pointer returnType;
    boolean isStatic;

    public Line line;
    public Block block;

    private boolean isExpression;

    public Stack(ContentFile cFile, boolean isExpression) {
        this.cFile = cFile;
        this.isExpression = isExpression;
    }

    public void read(Token start, Token end) {
        block = new BlockEmpty(this, start, end);
    }

    public void make() {
        block.make();
    }
}