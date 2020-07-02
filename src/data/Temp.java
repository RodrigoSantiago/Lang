package data;

import logic.Pointer;

public class Temp {
    int block;
    Pointer typePtr;
    String name;
    boolean pure;

    public Temp(int block, Pointer typePtr, String name, boolean pure) {
        this.block = block;
        this.typePtr = typePtr;
        this.name = name;
        this.pure = pure;
    }
}
