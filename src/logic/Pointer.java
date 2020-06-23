package logic;

import logic.templates.Generic;
import logic.typdef.Type;

public class Pointer {

    public static final Pointer nullPointer = new Pointer(null); // Value
    public static final Pointer voidPointer = new Pointer(null); // No type

    public static final Pointer openPointer = new Pointer(null); // Empty generic [do not use as mark]

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
        this.pointers = pointers != null && pointers.length == 0 ? null : pointers;
        this.typeSource = typeSource;
        this.let = (type == null || !type.isValue()) && let;
    }

    public boolean isOpen() {
        return type == null;
    }

    public boolean isDefault() {
        return this == nullPointer || this == voidPointer || this == openPointer ||
                (typeSource != null && typeSource.basePtr == openPointer);
    }

    public Pointer toLet() {
        if (let || (type != null && type.isValue())) return this;
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

    public boolean isEquivalent(Pointer other) {
        if (this == other) return true;
        if (typeSource != null && other.typeSource != null) return true;
        if (other.typeSource != null && pointers != null) return !this.contains(other);
        if (typeSource != null && other.pointers != null) return !other.contains(this);

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

    public int isDerivedFrom(Pointer other) {
        // null|void|struct|enums [nunca podem entrar neste metodo]
        if (other == nullPointer || other == voidPointer || (other.type != null && other.type.isValue()))
            throw new RuntimeException("Invalid Argument");

        if (other.isOpen()) return 1;
        if (this.equals(other)) return 1;
        for (Pointer p : type.parents) {
            int d = byGeneric(this, p).isDerivedFrom(other);
            if (d > 0) {
                return d + 1;
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
        if (other.isOpen() && other.typeSource == null) return 0;

        int checkLet = !let && other.let ? 0 : 1;

        // Generic Reciver (Includes Open Pointer) - Only allow the same Generic
        if (typeSource != null) return typeSource == other.typeSource ? checkLet : 0;

        // If the source is a generic
        if (other.typeSource != null) {
            // Open Generic
            if (other.isOpen()) {
                return -1; // [Negative] - I can become you !
            } else {
                // Any Generic
                int sourceToDestin = other.typeSource.basePtr.isDerivedFrom(this);
                if (sourceToDestin > 0) {
                    return checkLet;
                } else if (type.isClass()) {
                    int derived = this.isDerivedFrom(other.typeSource.basePtr);
                    if (derived > 0) {
                        return - derived * checkLet; // [Negative] - I can become you !
                    }
                } else if (type.isInterface()) {
                    return -1 * checkLet; // [Negative] - I can become you !
                }
                return 0;
            }
        }

        if (other.equals(this)) return checkLet;

        int typeDistance = 0;

        if (other.type.isStruct()) {
            for (Pointer p : other.type.autoCast) {
                int d = this.canReceive(byGeneric(p, other));
                if (d > 0) {
                    typeDistance = d + 1;
                    break;
                }
            }
        }

        if (typeDistance == 0) {
            for (Pointer p : other.type.parents) {
                p = byGeneric(p, other);
                int d = this.canReceive(p);
                if (d > 0) {
                    typeDistance = d + 1;
                    break;
                }
            }
            if (type.isValue() && contains(type.parent)) {
                return 1;
            }
        }

        return typeDistance * checkLet;
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
        if (this.isOpen()) return "open";
        String s = (let ? "Let" : "Ptr") + "<" + type;
        if (pointers != null) {
            s += "<";
            for (int i = 0; i < pointers.length; i++) {
                if (i > 0) s += ", ";
                s += pointers[i];
            }
            s += ">";
        }
        s += ">";
        return s;
    }
}
