package logic;

import logic.templates.Generic;
import logic.typdef.Type;

public class Pointer {

    public static final Pointer nullPointer = new Pointer(null); // Value
    public static final Pointer voidPointer = new Pointer(null); // No type

    public static final Pointer openPointer = new Pointer(null); // Empty generic [do not use as mark]

    public final Type type;
    public final boolean let;
    public final Generic typeSource;
    public final Pointer[] pointers;

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
        this.pointers = pointers != null && pointers.length == 0 ? null : pointers;
        this.typeSource = typeSource;
        this.let = (type == null || !type.isValue()) && let;
    }

    public boolean isOpen() {
        return type == null;
    }

    public boolean isDefault() {
        return this == nullPointer || this == voidPointer || this == openPointer;
    }

    public boolean isSync() {
        return type != null && type.isSync();
    }

    public Pointer toLet() {
        return toLet(true);
    }

    public Pointer toLet(boolean toLet) {
        if (toLet == let || (type != null && type.isValue()) || isDefault()) return this;
        return new Pointer(type, pointers == null ? null : pointers.clone(), typeSource, toLet);
    }

    public boolean isPointer() {
        return this == nullPointer || (this.type != null && this.type.isPointer());
    }

    public boolean isValue() {
        return this == openPointer || (this.type != null && this.type.isValue());
    }

    public boolean isLangBase() {
        return this.type != null && this.type.isLangBase();
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

    public boolean isEquivalent(Pointer other) {
        if (this == other) return true;
        if (typeSource != null && other.typeSource != null) return true;
        if (other.typeSource != null && pointers != null) return !this.contains(other);
        if (typeSource != null && other.pointers != null) return !other.contains(this);
        if (typeSource != null || other.typeSource != null) return true;

        if (type == other.type && pointers != null && other.pointers != null &&
                pointers.length == other.pointers.length) {
            for (int i = 0; i < pointers.length; i++) {
                if (!pointers[i].isEquivalent(other.pointers[i])) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public boolean contains(Pointer other) {
        if (this.equals(other)) return true;
        if (pointers != null) {
            for (Pointer pointer : pointers) {
                if (pointer.contains(other)) return true;
            }
        }
        return false;
    }

    public boolean contains(Generic typeSource) {
        if (this.typeSource == typeSource) return true;
        if (pointers != null) {
            for (Pointer pointer : pointers) {
                if (pointer.contains(typeSource)) return true;
            }
        }
        return false;
    }

    public int isDerivedFrom(Pointer other) {
        // null|void
        if (other == nullPointer || other == voidPointer) throw new RuntimeException("Invalid Argument");

        if (other.isOpen()) return 1;
        if (this.equalsIgnoreLet(other)) return 1;
        for (Pointer p : type.parents) {
            int d = byGeneric(p, this).isDerivedFrom(other);
            if (d > 0) {
                return d + 1;
            }
        }

        return 0;
    }

    public int canSpecify(Pointer other) {
        if (other == voidPointer || other == nullPointer) return 0;
        if (other.isOpen()) return 1;

        if (equalsIgnoreLet(other)) return 1;

        if (type.isPointer()) {
            for (Pointer p : type.parents) {
                int d = byGeneric(p, this).canSpecify(other);
                if (d > 0) {
                    return d + 1;
                }
            }
        }

        return 0;
    }

    public int canReceive(Pointer other) {
        // Void Never
        if (other == voidPointer) return 0;

        // Null Aways (Pointers)
        if (other == nullPointer) return type != null && type.isPointer() ? 1 : 0;

        // Default Aways (Non-generic Open)
        if (other.isOpen() && other.typeSource == null) return 1;

        // If the Reciver is a generic (Includes Open Pointer) - Only allow the same Generic
        if (typeSource != null) return typeSource == other.typeSource ? 1 : 0;

        // If the Source is a generic
        if (other.typeSource != null) {
            // Open Generic
            if (other.isOpen()) {
                return -1; // [Negative] - I can become you !
            } else {
                // Any Generic
                int sourceToDestin = other.typeSource.basePtr.isDerivedFrom(this);
                if (sourceToDestin > 0) {
                    return 1;
                } else if (type.isClass()) {
                    int derived = this.isDerivedFrom(other.typeSource.basePtr);
                    if (derived > 0) {
                        return -derived; // [Negative] - I can become you !
                    }
                } else if (type.isInterface()) {
                    return -1; // [Negative] - I can become you !
                }
                return 0;
            }
        }

        if (other.equalsIgnoreLet(this)) return 1;
        if (other.type.isFunction() && type.isFunction()) return 0;

        // Implicit Cast
        for (Pointer p : other.type.autoCast) {
            if (equalsIgnoreLet(byGeneric(p, other))) {
                return 2;
            }
        }

        // Type Inheritence
        if (other.type.isPointer()) {
            for (Pointer p : other.type.parents) {
                int d = this.canReceive(byGeneric(p, other));
                if (d > 0) {
                    return d + 1;
                }
            }
        }

        // Auto Boxing
        if (other.type.isValue()) {
            int dif;
            if (other.type.isFunction()) {
                dif = canReceive(other.type.parent);
            } else {
                dif = canReceive(byGeneric(other.type.parent, other));
            }
            if (dif > 0) {
                return dif + 1;
            }
        }
        // Auto Unboxing
        if (other.type.isClass() && this.type.isValue()) {
            if (this.type.isFunction()) {
                return other.equalsIgnoreLet(this.type.parent) ? 2 : 0;
            } else {
                return other.equalsIgnoreLet(byGeneric(this.type.parent, this)) ? 2 : 0;
            }
        }
        return 0;
    }

    public static Pointer byGeneric(Pointer source, Pointer caller) {
        if (!hasGeneric(source, caller)) {
            return source;
        }

        if (source.typeSource != null && caller.type != null && source.typeSource.owner == caller.type.template) {
            return caller.pointers[source.typeSource.index].toLet(source.let);
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

    public static Pointer capture(Generic source, Pointer original, Pointer input) {
        if (original.typeSource == source) {
            return !input.isDefault() && input.isDerivedFrom(source.basePtr) > 0 ? input.toLet(false) : null;
        } else if (original.type == input.type &&
                original.pointers != null && input.pointers != null &&
                original.pointers.length == input.pointers.length) {

            for (int i = 0; i < original.pointers.length; i++) {
                Pointer cap = capture(source, original.pointers[i], input.pointers[i]);
                if (cap != null) {
                    return cap.toLet(false);
                }
            }
        }
        return null;
    }

    public static Pointer apply(Generic generic, Pointer original, Pointer source) {
        if (source.typeSource == generic) {
            return original.toLet(source.let);
        } else if (source.pointers != null) {
            Pointer[] inner = new Pointer[source.pointers.length];
            for (int i = 0; i < source.pointers.length; i++) {
                inner[i] = apply(generic, original, source.pointers[i]);
            }
            return new Pointer(source.type, inner, source.typeSource, source.let);
        }

        return source;
    }

    public static Pointer force(Generic generic, Pointer source) {
        if (source.typeSource == generic) {
            return generic.defaultPtr.toLet(source.let);
        } else if (source.pointers != null) {
            Pointer[] inner = new Pointer[source.pointers.length];
            for (int i = 0; i < source.pointers.length; i++) {
                inner[i] = force(generic, source.pointers[i]);
            }
            return new Pointer(source.type, inner, source.typeSource, source.let);
        }

        return source;
    }

    public static int OwnTable(Pointer reciver, Pointer src) {
        if (reciver.let) {
            if (src.let) return 2;
            else if (src.type != null && src.type.isValue()) return 2;
            else return 1;
        } else if (reciver.type != null && reciver.type.isValue()) {
            if (src.let) return 2;
            else if (src.type != null && src.type.isValue()) return 2;
            else return 1;
        } else {
            if (src.let) return -1;
            else if (src.type != null && src.type.isValue()) return 2;
            else return 0;
        }
    }

    public boolean equalsIgnoreLet(Pointer other) {
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
        if (this == nullPointer) return "nullptr";
        if (this == openPointer) return "open";
        if (this.isOpen()) return typeSource.nameToken + ":open";
        String s = (let ? "let " : "") + (typeSource == null ? type.toString() :
                typeSource.nameToken.toString() + ":" + typeSource.basePtr);
        if (pointers != null) {
            s += "<";
            for (int i = 0; i < pointers.length; i++) {
                if (i > 0) s += ", ";
                s += pointers[i];
            }
            s += ">";
        }
        return s;
    }
}
