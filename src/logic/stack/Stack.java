package logic.stack;

import content.Key;
import content.Parser;
import content.Token;
import data.ContentFile;
import logic.Pointer;
import logic.stack.block.*;
import logic.stack.line.*;

public class Stack {

    public final ContentFile cFile;
    private final Pointer sourcePtr;
    private Pointer returnPtr;

    public Line line;
    public Block block;

    private boolean isExpression;
    private boolean isStatic;
    private boolean isConstructor;

    Stack(Stack source, Pointer returnPtr) {
        this.cFile = source.cFile;
        this.sourcePtr = source.sourcePtr;
        this.returnPtr = returnPtr;
        this.isExpression = false;
        this.isStatic = source.isStatic;
        this.isConstructor = false;
    }

    public Stack(ContentFile cFile, Pointer sourcePtr, Pointer returnPtr, boolean isExpression, boolean isStatic, boolean isConstructor) {
        this.cFile = cFile;
        this.sourcePtr = sourcePtr;
        this.returnPtr = returnPtr;
        this.isExpression = isExpression;
        this.isStatic = isStatic;
        this.isConstructor = isConstructor;
    }

    public void read(Token start, Token end, boolean read) {
        block = new BlockEmpty(this, start, end, read);
    }

    public void load() {
        block.load();
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isConstructor() {
        return isConstructor;
    }

    public Pointer getSourcePtr() {
        return sourcePtr;
    }

    public Pointer getReturnPtr() {
        return returnPtr;
    }
}