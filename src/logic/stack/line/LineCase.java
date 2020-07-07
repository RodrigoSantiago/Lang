package logic.stack.line;

import content.Key;
import content.Token;
import content.TokenGroup;
import builder.CppBuilder;
import logic.stack.Block;
import logic.stack.Context;
import logic.stack.Line;
import logic.stack.block.BlockSwitch;
import logic.stack.expression.Expression;

public class LineCase extends Line {

    private boolean isDefault;
    TokenGroup caseToken;
    Expression caseExp;
    private int labelID;

    private BlockSwitch switchOwner;

    public LineCase(Block block, Token start, Token end) {
        super(block, start, end);

        Token init = start;
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && (token.key == Key.CASE || token.key == Key.DEFAULT)) {
                init = next;
                isDefault = token.key == Key.DEFAULT;
                state = 1;
            } else if (state == 1 && token.key == Key.COLON) {
                if (init != token) {
                    if (isDefault) {
                        cFile.erro(token, "Default Statment should not have a value", this);
                    } else {
                        caseToken = new TokenGroup(init, token);
                        caseExp = new Expression(this, init, token);
                    }
                }
                state = 2;
            } else if (state != 1) {
                cFile.erro(token, "Unexpected token", this);
            }
            if (state != 2 && next == end) {
                cFile.erro(token, "Colon expected", this);
            }
            token = next;
        }
    }

    @Override
    public void load() {
        if (caseExp != null) {
            caseExp.load(new Context(stack));
            if (switchOwner != null && switchOwner.getTypePtr() != null) {
                caseExp.requestGet(switchOwner.getTypePtr());
            } else {
                caseExp.requestGet(null);
            }
            if (!caseExp.isLiteral()) {
                cFile.erro(caseToken, "The case value should be Literal", this);
            } else if (switchOwner != null) {
                if (switchOwner.compareCase(this)) {
                    cFile.erro(caseToken, "Repeated Case Expression", this);
                }
            }
        }
    }

    @Override
    public void build(CppBuilder cBuilder, int idt, int off) {
        if ((switchOwner.caseLines.size() == 0 && switchOwner.defaultCase != this) ||
                (switchOwner.caseLines.size() > 0 && switchOwner.caseLines.get(0) != this)) {
            cBuilder.idt(1).out().ln();
        }
        if (switchOwner.isSimple()) {
            if (isDefault) {
                cBuilder.idt(off).add("default : ");
            } else {
                cBuilder.idt(off).add("case ").add(caseExp, idt).add(" : ");
            }
        } else {
            cBuilder.idt(off).add(isDefault ? "default_" : "case_").add(labelID).add(" :; ");
        }
        cBuilder.in(idt);
    }

    public void setLabelID(int id) {
        this.labelID = id;
    }

    public int getLabelID() {
        return labelID;
    }

    public Expression getExpression() {
        return caseExp;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setSwitch(BlockSwitch switchOwner) {
        this.switchOwner = switchOwner;
    }
}
