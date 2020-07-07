package builder;

public class TempBlock {
    public TempBlock capture;
    public int pos;
    public int idt;
    public boolean yield;

    public TempBlock(TempBlock capture, int pos, int idt, boolean yield) {
        this.capture = capture;
        this.pos = pos;
        this.idt = idt;
        this.yield = yield;
    }
}
