package logic.stack;

import content.Key;
import content.Parser;
import content.Token;
import data.ContentFile;
import logic.Pointer;
import logic.stack.block.*;
import logic.stack.line.*;

public class Stack {

    ContentFile cFile;
    Pointer returnType;
    boolean isStatic;

    public Line line;
    public Block block;

    private boolean isExpression;
    Token start;
    Token end;

    public Stack(ContentFile cFile, Token start, Token end, boolean isExpression) {
        this.cFile = cFile;
        this.start = start;
        this.end = end;
        this.isExpression = isExpression;
    }

    public void make() {
        block = new BlockEmpty(this, start, end);
        block.make();
    }
}