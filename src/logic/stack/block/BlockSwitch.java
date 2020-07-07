package logic.stack.block;

import builder.CppBuilder;
import builder.Temp;
import content.Key;
import content.Parser;
import content.Token;
import content.TokenGroup;
import logic.Pointer;
import logic.stack.Block;
import logic.stack.Context;
import logic.stack.Line;
import logic.stack.expression.Expression;
import logic.stack.line.LineCase;
import logic.stack.line.LineVar;

import java.util.ArrayList;

public class BlockSwitch extends Block {

    Token label;
    Token token;

    Token paramToken;
    TokenGroup contentToken;
    Expression expression;

    Pointer typePtr;
    boolean simple;
    private int labelID;
    private boolean bk;

    public ArrayList<LineCase> caseLines = new ArrayList<>();
    public LineCase defaultCase;

    public BlockSwitch(Block block, Token start, Token end) {
        super(block, start, end);
        this.token = start;

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.SWITCH) {
                this.token = token;
                state = 1;
            }else if (state == 1 && token.key == Key.PARAM && token.getChild() != null) {
                paramToken = token;
                if (token.isEmptyParent()) {
                    cFile.erro(token, "Empty expressison", this);
                } else {
                    expression = new Expression(this, token.getChild(), token.getLastChild());
                }
                state = 2;
            } else if (state == 2 && token.key == Key.COLON) {
                state = 3;
            } else if (state == 3 && token.key == Key.WORD) {
                label = token;
                state = 4;
            }  else if ((state == 1 || state == 2 || state == 3 || state == 4) && token.key == Key.BRACE) {
                if (state == 1) {
                    cFile.erro(token.start, token.start + 1, "Missing expression", this);
                } else if (state == 3) {
                    cFile.erro(token.start, token.start + 1, "Label Statment expected", this);
                }
                if (token.getChild() == null) {
                    if (next != end) {
                        contentToken = new TokenGroup(next, end);
                        next = end;
                    }
                    cFile.erro(token, "Brace closure expected", this);
                } else {
                    if (token.isOpen()) cFile.erro(token, "Brace closure expected", this);
                    contentToken = new TokenGroup(token.getChild(), token.getLastChild());
                }
                state = 5;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state != 5) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }

        if (contentToken != null) {
            Parser.parseLines(this, contentToken.start, contentToken.end);
        }
    }

    @Override
    public void load() {
        if (expression != null) {
            expression.load(new Context(stack));
            expression.requestGet(null);

            typePtr = expression.getNaturalPtr();
            if (typePtr != null && typePtr != Pointer.voidPointer) {
                if (typePtr.hasGeneric() || typePtr.isPointer()) {
                    cFile.erro(token, "A Switch statment should have only Structs or Enums", this);
                } else if (typePtr.equals(cFile.langFloatPtr()) || typePtr.equals(cFile.langDoublePtr())) {
                    cFile.erro(token, "A Switch statment cannot have Floating values", this);
                } else if (typePtr.equals(cFile.langBoolPtr())) {
                    cFile.erro(token, "A Switch statment cannot have Bool values", this);
                } else if (typePtr.type == cFile.langFunction()) {
                    cFile.erro(token, "A Switch statment cannot have Functions values", this);
                } else if (typePtr.isLangBase()) {
                    simple = true;
                }
            } else {
                typePtr = cFile.langObjectPtr(true);
            }
        }
        super.load();
    }

    @Override
    public void build(CppBuilder cBuilder, int idt, int off) {
        labelID = cBuilder.temp();

        if (simple) {
            cBuilder.idt(idt).add("switch (").add(expression, idt).add(") {").ln(); // DO NOT IN
            for (Line line : lines) {
                line.build(cBuilder, idt + 1, idt + 1);
            }
            cBuilder.idt(1).out().ln(); // [last case]
            cBuilder.idt(idt).add("}").ln();
            if (bk && label != null) {
                cBuilder.idt(idt).add("break_").add(labelID).add(":;").ln();
            }
        } else {
            cBuilder.idt(idt).add("/* switch */ ").in(idt + 1);
            int tempID = cBuilder.temp();
            cBuilder.idt(idt + 1).add(typePtr).add(" t").add(tempID).add(" = ").add(expression, idt).add(";").ln();
            for (int i = 0; i < caseLines.size(); i++) {
                LineCase lineCase = caseLines.get(i);
                lineCase.setLabelID(cBuilder.temp());
                cBuilder.idt(idt + 1).add(i > 0, "else ")
                        .add("if (t").add(tempID).add(" == ").add(lineCase.getExpression(), idt).add(") goto case_")
                        .add(lineCase.getLabelID()).add(";").ln();
            }
            if (defaultCase != null) {
                defaultCase.setLabelID(cBuilder.temp());
                cBuilder.idt(idt + 1).add(caseLines.size() > 0, "else ").add("goto default_")
                        .add(defaultCase.getLabelID()).add(";").ln();
            }

            for (Line line : lines) {
                line.build(cBuilder, idt + 1, idt + 1);
            }
            cBuilder.idt(1).out().ln(); // [last case]
            cBuilder.out().ln();
            if (bk) {
                cBuilder.idt(idt).add("break_").add(labelID).add(":;").ln();
            }
        }
    }

    @Override
    public Line isBreakble(Token label) {
        if (label == null || label.equals(this.label)) {
            bk = true;
            return this;
        } else {
            return super.isBreakble(label);
        }
    }

    @Override
    public void add(LineVar line) {
        super.add((Line) line);
        cFile.erro(line.start, "A variable cannot be declared directly inside a Switch Statment", this);
    }

    @Override
    public void add(LineCase line) {
        super.add((Line) line);
        if (line.isDefault()) {
            if (defaultCase != null) {
                cFile.erro(line.start, "Repeated Default Statment", this);
            }
            line.setSwitch(this);
            defaultCase = line;
        } else {
            line.setSwitch(this);
            caseLines.add(line);
        }
    }

    @Override
    public void add(BlockElse blockElse) {
        if (defaultCase == null && caseLines.size() == 0) {
            cFile.erro(blockElse.start, "The first Switch Statment must be a case or default", this);
        }
        super.add(blockElse);
    }

    @Override
    public void add(BlockWhile blockWhile) {
        if (defaultCase == null && caseLines.size() == 0) {
            cFile.erro(blockWhile.start, "The first Switch Statment must be a case or default", this);
        }
        super.add(blockWhile);
    }

    @Override
    public void add(BlockDo blockDo) {
        if (defaultCase == null && caseLines.size() == 0) {
            cFile.erro(blockDo.start, "The first Switch Statment must be a case or default", this);
        }
        super.add(blockDo);
    }

    @Override
    public void add(Line line) {
        if (defaultCase == null && caseLines.size() == 0) {
            cFile.erro(line.start, "The first Switch Statment must be a case or default", this);
        }
        super.add(line);
    }

    @Override
    public void end() {
        if (defaultCase == null && caseLines.size() == 0) {
            cFile.erro(start, "A Switch Statment must have a case or default", this);
        }
    }

    @Override
    public int getLabelID() {
        return labelID;
    }

    public Pointer getTypePtr() {
        return typePtr;
    }

    public boolean isSimple() {
        return simple;
    }

    public boolean compareCase(LineCase caseTest) {
        for (LineCase lineCase : caseLines) {
            if (lineCase == caseTest) continue;

            Expression exp = lineCase.getExpression();
            if (exp != null && exp.isLiteral()) {
                if (exp.getLiteral().compareTo(caseTest.getExpression().getLiteral())) {
                    return true;
                }
            }
        }
        return false;
    }
}
