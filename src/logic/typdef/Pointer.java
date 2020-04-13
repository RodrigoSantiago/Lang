package logic.typdef;

import content.Token;

/**
 * Struct -
 */
public class Pointer {

    public static final Pointer nullPointer = null; // Value
    public static final Pointer voidPointer = null; // No type
    public static final Pointer openPointer = null; // Empty generic

    public Typedef type;
    public Pointer[] pointers;
    public Token source;
    public Generic typeSource;

    public Pointer(Typedef type, Pointer[] pointers) {
        this.type = type;
        this.pointers = pointers;
    }
}
