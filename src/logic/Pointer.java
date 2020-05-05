package logic;

import logic.typdef.Type;

/**
 * Struct -
 */
public class Pointer {

    public static final Pointer nullPointer = new Pointer(null); // Value
    public static final Pointer voidPointer = new Pointer(null); // No type
    public static final Pointer openPointer = new Pointer(null); // Empty generic

    public Type type;
    public Generic typeSource;
    public Pointer[] pointers;

    public Pointer(Type type) {
        this(type, null);
    }

    public Pointer(Type type, Pointer[] pointers) {
        this(type, pointers, null);
    }

    public Pointer(Type type, Pointer[] pointers, Generic typeSource) {
        this.type = type;
        this.pointers = pointers;
        this.typeSource = typeSource;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (obj instanceof Pointer) {
            Pointer other = (Pointer) obj;
            if (other.type != type || other.typeSource != typeSource) {
                return false;
            } else if (other.pointers != null && pointers != null) {
                if (other.pointers.length != pointers.length) {
                    return false;
                } else {
                    for (int i = 0; i < pointers.length; i++) {
                        if (!pointers[i].equals(other.pointers[i])) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Ptr[" + type + "]";
    }
}
