package logic.stack.block;

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

    Token paramToken;
    TokenGroup contentToken;
    Expression expression;

    public ArrayList<LineCase> caseLines = new ArrayList<>();
    LineCase defaultCase;

    public BlockSwitch(Block block, Token start, Token end) {
        super(block, start, end);

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.SWITCH) {
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
        }
        super.load();
    }

    @Override
    public Line isBreakble(Token label) {
        if (label == null || label.equals(this.label)) {
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
            defaultCase = line;
        } else {
            line.setSwitch(this);
            caseLines.add(line);
        }
    }

    @Override
    public void add(BlockElse blockElse) {
        if (caseLines.size() == 0) {
            cFile.erro(blockElse.start, "The first Switch Statment must be a case or default", this);
        }
        super.add(blockElse);
    }

    @Override
    public void add(BlockWhile blockWhile) {
        if (caseLines.size() == 0) {
            cFile.erro(blockWhile.start, "The first Switch Statment must be a case or default", this);
        }
        super.add(blockWhile);
    }

    @Override
    public void add(BlockDo blockDo) {
        if (caseLines.size() == 0) {
            cFile.erro(blockDo.start, "The first Switch Statment must be a case or default", this);
        }
        super.add(blockDo);
    }

    @Override
    public void add(Line line) {
        if (caseLines.size() == 0) {
            cFile.erro(line.start, "The first Switch Statment must be a case or default", this);
        }
        super.add(line);
    }

    public void end() {
        if (caseLines.size() == 0) {
            cFile.erro(start, "A Switch Statment must have a case or default", this);
        }
    }

    public Pointer getTypePtr() {
        return expression == null ? null : expression.getReturnType();
    }
}
