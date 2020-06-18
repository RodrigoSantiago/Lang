package logic;

import logic.templates.Generic;
import logic.typdef.Type;

public class Pointer {

    public static final Pointer nullPointer = new Pointer(null); // Value
    public static final Pointer voidPointer = new Pointer(null); // No type
    public static final Pointer openPointer = new Pointer(null); // Empty generic

    public Type type;
    public boolean let;
    public Generic typeSource;
    public Pointer[] pointers;

    public Pointer(Type type) {
        this(type, false);
    }

    public Pointer(Type type, boolean let) {
        this(type, null, let);
    }

    public Pointer(Type type, Pointer[] pointers, boolean let) {
        this(type, pointers, null, let);
    }

    public Pointer(Type type, Pointer[] pointers, Generic typeSource, boolean let) {
        this.type = type;
        this.pointers = pointers;
        this.typeSource = typeSource;
        this.let = let;
    }

    public boolean isDefault() {
        return this == nullPointer || this == voidPointer || this == openPointer ||
                (typeSource != null && typeSource.basePtr == openPointer);
    }

    public Pointer toLet() {
        if (let) return this;
        return new Pointer(type, pointers == null ? null : pointers.clone(), typeSource, true);
    }

    public boolean hasGeneric() {
        if (typeSource != null) return true;
        if (pointers != null) {
            for (Pointer pointer : pointers) {
                if (pointer.hasGeneric()) return true;
            }
        }
        return false;
    }

    public boolean contains(Type type) {
        if (this.type == type) return true;
        if (pointers != null) {
            for (Pointer pointer : pointers) {
                if (pointer.contains(type)) return true;
            }
        }
        return false;
    }

    public int isDerivedFrom(Pointer other) {
        if (other == openPointer) return 0;
        if (other == nullPointer) throw new RuntimeException("null é valor");
        if (other.type == null) throw new RuntimeException("other nao pode ter type null");
        if (this.type == other.type) return 1;
        if (other.type.isValue()) return -1;
        for (Pointer p : type.parents) {
            int d = p.isDerivedFrom(other);
            if (d >= 0) {
                return d;
            }
        }

        return -1;
    }

    public static Pointer byGeneric(Pointer source, Pointer caller) {
        if (!hasGeneric(source, caller)) {
            return source;
        }

        if (source.typeSource != null && caller.type != null && source.typeSource.owner == caller.type.template) {
            return caller.pointers[source.typeSource.index];
        } else if (source.pointers != null) {
            Pointer[] inner = new Pointer[source.pointers.length];
            for (int i = 0; i < source.pointers.length; i++) {
                inner[i] = byGeneric(source.pointers[i], caller);
            }
            return new Pointer(source.type, inner, source.typeSource, source.let);
        }

        return source;
    }

    public static boolean hasGeneric(Pointer source, Pointer caller)  {
        if (source.typeSource != null && caller.type != null && source.typeSource.owner == caller.type.template) {
            return true;
        } else if (source.pointers != null) {
            for (int i = 0; i < source.pointers.length; i++) {
                if (hasGeneric(source.pointers[i], caller)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean overloadEquals(Pointer other) {
        if (other == null) return false;
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
                return true;
            }
        } else if (other.pointers != pointers) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (obj instanceof Pointer) {
            Pointer other = (Pointer) obj;
            if (other.type != type || other.typeSource != typeSource || other.let != let) {
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
                    return true;
                }
            } else if (other.pointers != pointers) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        if (this == voidPointer) return "void";
        if (this == nullPointer) return "null";
        if (this == openPointer) return "any";
        return "Ptr[" + type + "]";
    }
}
