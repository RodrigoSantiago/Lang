package logic.stack.line;

import builder.CppBuilder;
import content.Key;
import content.Token;
import content.TokenGroup;
import logic.Pointer;
import logic.stack.Block;
import logic.stack.Context;
import logic.stack.Line;
import logic.stack.expression.Expression;

public class LineYield extends Line {

    public Token token;
    TokenGroup returnToken;
    Expression returnExp;
    private int yieldID;
    private boolean isReturn, isReturnInner, isBreak;

    public LineYield(Block block, Token start, Token end) {
        super(block, start, end);

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.YIELD) {
                this.token = token;
                state = 1;
            } else if (state == 1 && token.key == Key.RETURN) {
                isReturn = true;
                Token init = next;
                while (next != null && next != end && next.key != Key.SEMICOLON) {
                    next = next.getNext();
                }
                if (init != null && init != end && init.key != Key.SEMICOLON) {
                    returnToken = new TokenGroup(init, next);
                    returnExp = new Expression(this, init, next);
                } else {
                    cFile.erro(token, "Expression expected", this);
                }
                state = 2;
            } else if (state == 1 && token.key == Key.BREAK) {
                isBreak = true;
                state = 2;
            } else if (state == 2 && token.key == Key.SEMICOLON) {
                state = 3;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end) {
                if (state == 2) {
                    cFile.erro(token, "Semicolon expected", this);
                } else if (state != 3) {
                    cFile.erro(token, "Unexpected end of tokens", this);
                }
            }
            token = next;
        }

        stack.setYieldMode(this);
    }

    @Override
    public void load() {
        if (returnExp != null) {
            returnExp.load(new Context(stack));
            if (stack.getYiledPtr() == null) {
                returnExp.requestOwn(null);
                cFile.erro(token, "Unexpected expression (yield)", this);
            } else {
                Pointer naturalPtr = returnExp.getNaturalPtr(stack.getYiledPtr());
                if (naturalPtr != null && naturalPtr.type == cFile.langIterator()) {
                    isReturnInner = true;
                    returnExp.requestOwn(stack.getReturnPtr());
                } else {
                    returnExp.requestOwn(stack.getYiledPtr());
                }
            }
        }
        super.load();
    }

    @Override
    public void build(CppBuilder cBuilder, int idt, int off) {
        if (isReturnInner) {
            cBuilder.idt(off).add("yieldInner = ").add(returnExp, idt).add(";").ln();
            cBuilder.idt(off).add("if (yieldInner == nullptr || !yieldInner->m_move()) yieldValue = lang::value<GPtr<").add(stack.getYiledPtr()).add(">>::def();").ln();
            cBuilder.idt(off).add("else yieldValue = yieldInner->get_current();").ln();
            cBuilder.idt(off).add("yieldID = ").add(yieldID).add(";").ln();
            cBuilder.idt(off).add("return true;").ln();
            cBuilder.idt(off).add("yield_").add(yieldID).add(":;").ln();
        } else if (isReturn) {
            cBuilder.idt(off).add("yieldValue = ").add(returnExp, idt).add(";").ln();
            cBuilder.idt(off).add("yieldID = ").add(yieldID).add(";").ln();
            cBuilder.idt(off).add("return true;").ln();
            cBuilder.idt(off).add("yield_").add(yieldID).add(":;").ln();
        } else {
            cBuilder.idt(off).add("yieldBreak = true;").ln();
            cBuilder.idt(off).add("return false;").ln();
        }
    }

    public boolean isReturn() {
        return isReturn;
    }

    public boolean isBreak() {
        return isBreak;
    }

    public void setYieldID(int id) {
        yieldID = id;
    }

    public int getYieldID() {
        return yieldID;
    }
}
