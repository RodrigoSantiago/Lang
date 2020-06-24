package logic.stack;

import content.Token;
import content.TokenGroup;
import data.ContentFile;
import logic.Pointer;
import logic.stack.block.*;
import java.util.HashMap;

public class Stack {

    public final ContentFile cFile;
    private final Pointer sourcePtr;
    private Pointer returnPtr;

    public Line line;
    public Block block;

    private boolean isExpression;
    private boolean isStatic;
    private boolean isConstructor;

    HashMap<Token, Field> fields = new HashMap<>();

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

    public boolean addField(Token nameToken, Pointer typePtr, boolean isFinal, Block block) {
        if (fields.containsKey(nameToken)) {
            return false;
        }
        fields.put(nameToken, new Field(this, nameToken, typePtr, isFinal, block));
        return true;
    }

    public Field findField(Token nameToken) {
        return fields.get(nameToken);
    }

    public Pointer getPointer(TokenGroup tokenGroup, boolean isLet) {
        return cFile.getPointer(tokenGroup.start, tokenGroup.end, null, null, isLet);
    }
}