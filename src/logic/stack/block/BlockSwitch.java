package logic.stack.block;

import content.Key;
import content.Parser;
import content.Token;
import content.TokenGroup;
import logic.stack.Block;
import logic.stack.Line;
import logic.stack.Stack;
import logic.stack.expression.Expression;
import logic.stack.line.LineCase;
import logic.stack.line.LineVar;

import java.util.ArrayList;

public class BlockSwitch extends Block {

    Token paramToken;
    TokenGroup contentTokenGroup;
    Expression expression;

    public ArrayList<LineCase> caseLines = new ArrayList<>();
    LineCase defaultCase;

    public BlockSwitch(Block block, Token start, Token end) {
        super(block, start, end);
        System.out.println("SWITCH");

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.SWITCH) {
                state = 1;
            } else if (state == 1 && token.key == Key.PARAM) {
                paramToken = token;
                if (paramToken.getChild() != null) {
                    expression = new Expression(this, paramToken.getChild(), paramToken.getLastChild());
                } else {
                    cFile.erro(token, "Unexpected end of tokens");
                }

                state = 2;
            } else if ((state == 1 || state == 2) && token.key == Key.BRACE) {
                if (state == 1) {
                    cFile.erro(token.start, token.start + 1, "Missing expression");
                }
                contentTokenGroup = new TokenGroup(token, next);
                state = 3; // completed
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end && state != 3) {
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
        }

        if (contentTokenGroup != null) {
            if (contentTokenGroup.start.key == Key.BRACE) {
                Parser.parseLines(this, contentTokenGroup.start.getChild(), contentTokenGroup.start.getLastChild());
            }
        }
    }

    @Override
    public void add(LineVar line) {
        super.add((Line) line);
        cFile.erro(line.start, "A variable cannot be declared directly inside a Switch Statment");
    }

    @Override
    public void add(LineCase line) {
        super.add((Line) line);
        if (line.isDefault()) {
            if (defaultCase != null) {
                cFile.erro(line.start, "Repeated Default Statment");
            }
            defaultCase = line;
        } else {
            caseLines.add(line);
        }
    }

    @Override
    public void add(BlockElse blockElse) {
        if (caseLines.size() == 0) {
            cFile.erro(blockElse.start, "The first Switch Statment must be a case or default");
        }
        super.add(blockElse);
    }

    @Override
    public void add(BlockWhile blockWhile) {
        if (caseLines.size() == 0) {
            cFile.erro(blockWhile.start, "The first Switch Statment must be a case or default");
        }
        super.add(blockWhile);
    }

    @Override
    public void add(BlockDo blockDo) {
        if (caseLines.size() == 0) {
            cFile.erro(blockDo.start, "The first Switch Statment must be a case or default");
        }
        super.add(blockDo);
    }

    @Override
    public void add(Line line) {
        if (caseLines.size() == 0) {
            cFile.erro(line.start, "The first Switch Statment must be a case or default");
        }
        super.add(line);
    }

    public void end() {
        if (caseLines.size() == 0) {
            cFile.erro(start, "A Switch Statment must have a case or default");
        }
    }
}
