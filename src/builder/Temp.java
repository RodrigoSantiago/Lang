package builder;

import logic.Pointer;

public class Temp {

    public final TempBlock block;
    public final Pointer typePtr;
    public final String name;
    public final boolean ptr;
    public final int array;

    public Temp(TempBlock block, Pointer typePtr, String name, boolean ptr, int array) {
        this.block = block;
        this.typePtr = typePtr;
        this.name = name;
        this.ptr = ptr;
        this.array = array;
    }
}
