package logic.stack;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.ContentFile;
import builder.CppBuilder;
import logic.GenericOwner;
import logic.Pointer;
import logic.params.Parameters;
import logic.stack.block.*;
import logic.stack.expression.ConstructorCall;
import logic.stack.expression.Expression;
import logic.stack.expression.LambdaCall;
import logic.stack.line.LineYield;

import java.util.ArrayList;
import java.util.HashMap;

public class Stack {

    public final ContentFile cFile;
    private final Pointer sourcePtr;
    private Pointer returnPtr;
    private Pointer yieldPtr;
    public Token referenceToken;

    public Block block;
    public Expression expression;

    private boolean isExpression;
    private boolean isStatic;
    private boolean isConstructor;
    private boolean isYield;
    private boolean hasConstructorCall;

    private Pointer valuePtr;
    private Stack source;
    private Line parent;

    public Parameters param;
    public HashMap<Token, Field> fields = new HashMap<>();
    public HashMap<Token, Field> shadowFields = new HashMap<>();

    public ArrayList<LineYield> yields = new ArrayList<>();
    public ArrayList<LambdaCall> lambdas = new ArrayList<>();

    private GenericOwner generics;
    private ConstructorCall constructorCall;

    public Stack(Line parent, Stack source, Token referenceToken, Pointer returnPtr, Parameters param) {
        this.parent = parent;
        this.source = source;
        this.cFile = source.cFile;
        this.sourcePtr = source.sourcePtr;
        this.returnPtr = returnPtr;
        this.isExpression = false;
        this.isStatic = source.isStatic;
        this.isConstructor = false;
        this.referenceToken = referenceToken;
        this.param = param;
    }

    public Stack(ContentFile cFile, Token referenceToken, Pointer sourcePtr, Pointer returnPtr, GenericOwner generics,
                 boolean isExpression, boolean isStatic, boolean isConstructor, Parameters param, Pointer valuePtr) {
        this.cFile = cFile;
        this.sourcePtr = sourcePtr;
        this.returnPtr = returnPtr;
        this.generics = generics;
        this.isExpression = isExpression;
        this.isStatic = isStatic;
        this.isConstructor = isConstructor;
        this.referenceToken = referenceToken;
        this.param = param;
        this.valuePtr = valuePtr;
    }

    public void read(Token start, Token end, boolean read) {
        if (isExpression) {
            block = new BlockEmpty(this, start, end, false);
            expression = new Expression(block, start, end);
        } else {
            block = new BlockEmpty(this, start, end, read);
        }
    }

    public void load() {
        if (param != null) {
            addParam(param);
        }
        if (valuePtr != null) {
            value(valuePtr);
        }
        if (isExpression) {
            expression.load(new Context(this));
            expression.requestOwn(returnPtr);
        } else {
            if (!hasConstructorCall && !isStatic) {
                thisBase();
            }

            block.load();
        }
    }

    public void build(CppBuilder cBuilder, int idt) {
        if (isExpression) {
            expression.build(cBuilder, idt);
        } else {
            for (LambdaCall lambda : lambdas) {
                lambda.setLambdaID(LambdaResolve.build(cBuilder, idt, lambda));
            }
            if (isYieldMode()) {
                YieldResolve.build(cBuilder, idt, this, param, valuePtr);
            } else {
                for (Line line : block.lines) {
                    line.build(cBuilder, idt, idt);
                }
                block.buildDestroyer(cBuilder, idt);
            }
        }
    }

    public ArrayList<LambdaCall> getLambdas() {
        return lambdas;
    }

    public Stack getSource() {
        return source;
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
        if (params == null) return;

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
        Field field = new Field(this, nameToken, typePtr, isFinal, block);
        fields.put(nameToken, field);
        block.addField(field);
        return true;
    }

    public Field findField(Token nameToken) {
        if (nameToken.key == Key.BASE && (source != null || isYieldMode())) return null;

        Field local = fields.get(nameToken);
        if (local == null && source != null) {
            Field shadow = shadowFields.get(nameToken);
            if (shadow == null) {
                Field outside = source.findField(nameToken);
                if (outside != null) {
                    shadow = new Field(this, outside);
                    shadowFields.put(outside.getName(), shadow);
                }
            }
            return shadow;
        }
        return local;
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

    public void value (Pointer valuePtr) {
        Token nameValue = new Token("value", 0, 5, Key.WORD, false);
        fields.put(nameValue, new Field(this, referenceToken, nameValue, valuePtr, false, block));
    }

    public void thisBase() {
        Token nameThis = new Token("this", 0, 4, Key.THIS, false);
        fields.put(nameThis, new Field(this, referenceToken, nameThis, sourcePtr.toLet(), true, block));

        if (sourcePtr.type.parent != null && !isYieldMode() && !isLambda()) {
            Token nameBase =  new Token("base", 0, 4, Key.BASE, false);
            fields.put(nameBase, new Field(this, referenceToken, nameBase, sourcePtr.type.parent.toLet(), true, block));
        }
    }

    public boolean isStaticConstructor() {
        return isStatic && isConstructor;
    }

    public void setContainsConstructorCall() {
        hasConstructorCall = true;
    }

    public boolean isLiteral() {
        return isExpression && expression.isLiteral();
    }

    public Pointer getYiledPtr() {
        return yieldPtr;
    }

    public void setYieldMode(LineYield lineYield) {
        if (!isYield) {
            if (isConstructor) {
                cFile.erro(lineYield.token, "A constructor cannot have a yield", this);
            } else {
                isYield = true;
                if (returnPtr.type != cFile.langIterator()) {
                    yieldPtr = cFile.langObjectPtr();
                    cFile.erro(lineYield.token, "A yield block should return a Iterator", this);
                } else {
                    if (returnPtr.let) {
                        cFile.erro(lineYield.token, "A yield block should not return a Weak Pointer", this);
                    }
                    yieldPtr = returnPtr.pointers[0];
                }
            }
        }
        yields.add(lineYield);
    }

    public boolean isYieldMode() {
        return isYield;
    }

    public boolean isLambda() {
        return parent != null;
    }

    public boolean isChildOf(Block block) {
        return parent != null && parent.isChildOf(block);
    }

    public void addLambda(LambdaCall lambdaCall) {
        lambdas.add(lambdaCall);
    }
}