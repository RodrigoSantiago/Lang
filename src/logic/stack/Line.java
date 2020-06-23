package logic.stack;

import content.Token;
import data.ContentFile;
import logic.Pointer;

public abstract class Line {
    public final Stack stack;
    public final ContentFile cFile;
    public final Block parent;

    public Token start;
    public Token end;

    public Line(Block parent, Token start, Token end) {
        this.stack = parent.stack;
        this.cFile = stack.cFile;
        this.start = start;
        this.end = end;
        this.parent = parent;
    }

    public Line(Stack stack, Token start, Token end) {
        this.stack = stack;
        this.cFile = stack.cFile;
        this.start = start;
        this.end = end;
        this.parent = null;
    }

    public void load() {
    }

    public boolean isIfStatment() {
        return false;
    }

    public boolean isLoopStatment() {
        return false;
    }

    public Line isBreakble(Token label) {
        return parent != null ? parent.isBreakble(label) : null;
    }

    public Line isContinuable(Token label) {
        return parent != null ? parent.isContinuable(label) : null;
    }

    public boolean isChildOf(Block source) {
        if (this == source) return true;
        if (parent != null) return parent.isChildOf(source);
        return false;
    }
}
