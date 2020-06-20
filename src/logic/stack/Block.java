package logic.stack;

import content.Key;
import content.Parser;
import content.Token;
import logic.stack.Line;
import logic.stack.block.*;
import logic.stack.line.*;

import java.util.ArrayList;

public abstract class Block extends Line {

    ArrayList<Line> lines = new ArrayList<>();

    public Block(Stack stack, Token start, Token end) {
        super(stack, start, end);
    }

    public Block(Block block, Token start, Token end) {
        super(block, start, end);
    }

    public void add(Line line) {
        lines.add(line);
        line.make();
    }

    public void add(Token start, Token end) {
        if (start.key == Key.BRACE) {
            add(new BlockEmpty(this, start, end));
        } else if (start.key == Key.IF) {
            add(new BlockIf(this, start, end));
        } else if (start.key == Key.ELSE) {
            add(new BlockElse(this, start, end));
        } else if (start.key == Key.SWITCH) {
            add(new BlockSwitch(this, start, end));
        } else if (start.key == Key.FOR) {
            add(new BlockFor(this, start, end));
        } else if (start.key == Key.WHILE) {
            add(new BlockWhile(this, start, end));
        } else if (start.key == Key.DO) {
            add(new BlockDo(this, start, end));
        } else if (start.key == Key.LOCK) {
            add(new BlockLock(this, start, end));
        } else if (start.key == Key.NATIVE) {
            add(new BlockNative(this, start, end));
        } else {
            if (start.key == Key.RETURN) {
                add(new LineReturn(this, start, end));
            } else if (start.key == Key.CONTINUE) {
                add(new LineContinue(this, start, end));
            } else if (start.key == Key.BREAK) {
                add(new LineBreak(this, start, end));
            } else if (start.key == Key.CASE) {
                add(new LineCase(this, start, end));
            } else if (start.key == Key.LET || start.key == Key.VAR || start.key == Key.FINAL) {
                add(new LineVar(this, start, end));
            } else {
                // Var/Expression
                Token token = start;
                int state = 0;
                while (token != end) {
                    if (state == 0 && token.key == Key.WORD) {
                        state = 1;
                    } else if (state == 1 && (token.key == Key.WORD || token.key == Key.GENERIC)) {
                        add(new LineVar(this, start, end));
                        break;
                    } else if (state == 1 && token.key == Key.INDEX) {
                        state = 2;
                    } else if (state == 2 && token.key == Key.WORD) {
                        add(new LineVar(this, start, end));
                        break;
                    } else {
                        add(new LineExpression(this, start, end));
                        break;
                    }
                    token = token.getNext();
                }
            }
        }
    }
}
