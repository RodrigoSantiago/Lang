package logic.stack.block;

import content.Parser;
import content.Token;
import logic.stack.Block;
import logic.stack.Stack;

public class BlockEmpty extends Block {

    public final Block parent;

    public BlockEmpty(Stack stack, Token start, Token end, boolean read) {
        super(stack, start, end);
        parent = null;

        if (read) {
            if (start.getChild() != null) {
                Parser.parseLines(this, start.getChild(), start.getLastChild());
            } else {
                cFile.erro(start, "Unexpected end of tokens", this);
            }
        }
    }

    public BlockEmpty(Block block, Token start, Token end) {
        super(block, start, end);
        parent = block;

        if (start.getChild() != null) {
            Parser.parseLines(this, start.getChild(), start.getLastChild());
        } else {
            cFile.erro(start, "Unexpected end of tokens", this);
        }
    }
}
