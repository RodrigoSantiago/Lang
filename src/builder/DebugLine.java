package builder;

public class DebugLine {
    public int line;
    public int pos;

    public DebugLine(int line, int pos) {
        this.line = line;
        this.pos = pos;
    }

    public void update(int lineExtra, int posExtra) {
        line += lineExtra;
        pos += posExtra;
    }
}
