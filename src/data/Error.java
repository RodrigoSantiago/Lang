package data;

public class Error {
    public static final int WARNING = 0;
    public static final int ERROR = 1;

    public final int level, start, end;
    public final String message;

    public Error(int level, int start, int end, String message) {
        this.level = level;
        this.start = start;
        this.end = end;
        this.message = message;
    }

    @Override
    public String toString() {
        return (level == WARNING ? "warning" : "error") + " : " + message;
    }
}
