package logic.stack.block;

import content.Key;
import content.Parser;
import content.Token;
import content.TokenGroup;
import logic.stack.Block;
import logic.stack.Line;

public class BlockDo extends Block {

    Token label;
    TokenGroup contentTokenGroup;
    BlockWhile blockWhile;

    public BlockDo(Block block, Token start, Token end) {
        super(block, start, end);
        System.out.println("DO");

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.DO) {
                state = 1;
            } else if (state == 1 && token.key == Key.COLON) {
                state = 2;
            } else if (state == 2 && token.key == Key.WORD) {
                label = token;
                state = 3;
            } else if ((state == 1 || state == 2 || state == 3) && token.key == Key.BRACE) {
                if (state == 2) {
                    cFile.erro(token.start, token.start + 1, "Label Statment expected", this);
                }
                contentTokenGroup = new TokenGroup(token.getChild(), token.getLastChild());
                state = 4;
            } else if ((state == 1 || state == 2 || state == 3) && token.key == Key.SEMICOLON) {
                if (state == 2) {
                    cFile.erro(token.start, token.start + 1, "Label Statment expected", this);
                }
                state = 4;
            } else if (state == 1 || state == 3) {
                contentTokenGroup = new TokenGroup(token, end);
                next = end;
                state = 4;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state != 4) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }

        if (contentTokenGroup != null) {
            Parser.parseLines(this, contentTokenGroup.start, contentTokenGroup.end);
        }
    }

    @Override
    public boolean isLoopStatment() {
        return true;
    }

    @Override
    public Line isBreakble(Token label) {
        if (label == this.label || (this.label != null && this.label.equals(label))) {
            return this;
        } else {
            return super.isBreakble(label);
        }
    }

    @Override
    public Line isContinuable(Token label) {
        if (label == this.label || (this.label != null && this.label.equals(label))) {
            return this;
        } else {
            return super.isContinuable(label);
        }
    }

    public void setWhile(BlockWhile blockWhile) {
        if (blockWhile == null) {
            cFile.erro(start, "While Statment expected", this);
        } else {
            this.blockWhile = blockWhile;
            if (blockWhile.contentTokenGroup != null) {
                cFile.erro(blockWhile.contentTokenGroup.start, "Unexpected do-while inner block", this);
            }
        }
    }

    public BlockWhile getWhile() {
        return blockWhile;
    }
}
