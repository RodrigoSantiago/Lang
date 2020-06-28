package logic.stack;

import content.Token;
import content.TokenGroup;
import data.ContentFile;
import logic.GenericOwner;
import logic.Pointer;
import logic.params.Parameters;
import logic.stack.block.*;
import logic.stack.expression.ConstructorCall;

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
    private boolean hasConstructorCall;

    private ConstructorCall constructorCall;

    HashMap<Token, Field> fields = new HashMap<>();

    private GenericOwner generics;

    Stack(Stack source, Pointer returnPtr) {
        this.cFile = source.cFile;
        this.sourcePtr = source.sourcePtr;
        this.returnPtr = returnPtr;
        this.isExpression = false;
        this.isStatic = source.isStatic;
        this.isConstructor = false;
    }

    public Stack(ContentFile cFile, Pointer sourcePtr, Pointer returnPtr, GenericOwner generics,
                 boolean isExpression, boolean isStatic, boolean isConstructor) {
        this.cFile = cFile;
        this.sourcePtr = sourcePtr;
        this.returnPtr = returnPtr;
        this.generics = generics;
        this.isExpression = isExpression;
        this.isStatic = isStatic;
        this.isConstructor = isConstructor;
    }

    public void read(Token start, Token end, boolean read) {
        block = new BlockEmpty(this, start, end, read);
    }

    public void load() {
        if (!hasConstructorCall && !isStatic) {
            thisBase();
        }

        block.load();
    }

    public boolean isStatic() {
        return isStatic || (isConstructor && constructorCall == null && hasConstructorCall);
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

    public void addParam(Parameters params) {
        for (int i = 0; i < params.getCount(); i++) {
            addParam(params.getNameToken(i), params.getTypePtr(i), false);
        }
    }

    public void addParam(Token nameToken, Pointer typePtr, boolean isFinal) {
        fields.put(nameToken, new Field(this, nameToken, typePtr, isFinal, block));
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
        return cFile.getPointer(tokenGroup.start, tokenGroup.end, null, generics, isLet);
    }

    public boolean isConstructorAllowed() {
        return isConstructor && !isStatic;
    }

    public ConstructorCall getConstructorCall() {
        return constructorCall;
    }

    public boolean addConstructorCall(ConstructorCall constructorCall) {
        if (this.constructorCall == null) {
            this.constructorCall = constructorCall;
            thisBase();

            return true;
        } else {
            return false;
        }
    }

    private void thisBase() {
        Token nameThis = new Token("this");
        fields.put(nameThis, new Field(this, nameThis, sourcePtr.toLet(), true, block));

        if (sourcePtr.type.parent != null) {
            Token nameBase = new Token("base");
            fields.put(nameBase, new Field(this, nameBase, sourcePtr.type.parent.toLet(), true, block));
        }
    }

    public boolean isStaticConstructor() {
        return isStatic && isConstructor;
    }

    public void setContainsConstructorCall() {
        hasConstructorCall = true;
    }
}