package logic.stack.block;

import content.Key;
import content.Parser;
import content.Token;
import content.TokenGroup;
import builder.CppBuilder;
import logic.stack.Block;
import logic.stack.Line;
import logic.stack.Stack;

public class BlockEmpty extends Block {

    public final Block parent;
    TokenGroup contentToken;

    public BlockEmpty(Stack stack, Token start, Token end, boolean read) {
        super(stack, start, end);
        parent = null;

        if (read) {
            contentToken = new TokenGroup(start, end);
            Parser.parseLines(this, contentToken.start, contentToken.end);
        }
    }

    public BlockEmpty(Block block, Token start, Token end) {
        super(block, start, end);
        parent = block;

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.BRACE) {
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
                state = 1;
            } else {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            if (next == end && state != 1) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }

        if (contentToken != null) {
            Parser.parseLines(this, contentToken.start, contentToken.end);
        }
    }

    @Override
    public void build(CppBuilder cBuilder, int idt, int off) {
        cBuilder.idt(idt).in(idt + 1);
        for (Line line : lines) {
            line.build(cBuilder, idt + 1, idt + 1);
        }
        cBuilder.out().ln();
    }
}
