package builder;

import logic.Pointer;

public class Temp {

    public final int blockID;
    public final Pointer typePtr;
    public final String name;
    public final boolean ptr;
    public final int array;

    public Temp(int blockID, Pointer typePtr, String name, boolean ptr, int array) {
        this.blockID = blockID;
        this.typePtr = typePtr;
        this.name = name;
        this.ptr = ptr;
        this.array = array;
    }
}
