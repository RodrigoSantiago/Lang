package data;

import logic.Pointer;
import logic.typdef.Type;

public class CppBuilder {

    private static String[] indents;

    private int indent = 0;
    private StringBuilder sBuilder, hBuilder, tBuilder;

    static {
        indents = new String[10];
        for (int i = 0; i < indents.length; i++) {
            indents[i] = "";
            for (int j = 0; j < i; j++) {
                indents[i] += "    ";
            }
        }
    }

    public CppBuilder() {
        sBuilder = new StringBuilder();
        hBuilder = new StringBuilder();
        tBuilder = sBuilder;
    }

    public CppBuilder toSource() {
        tBuilder = sBuilder;
        return this;
    }

    public CppBuilder toHeader() {
        tBuilder = hBuilder;
        return this;
    }

    public void reset() {
        sBuilder.setLength(0);
        hBuilder.setLength(0);
        tBuilder = sBuilder;
    }

    public CppBuilder add(String str) {
        tBuilder.append(str);
        return this;
    }

    public CppBuilder add(int number) {
        tBuilder.append(number);
        return this;
    }

    public CppBuilder add(Pointer pointer) {
        return this;
    }

    public CppBuilder idt() {
        return this;
    }

    public CppBuilder idt(int add) {
        if (add < 10) {
            tBuilder.append(indents[add]);
        } else {
            for (int i = 0; i < add; i++) {
                tBuilder.append(indents[1]);
            }
        }
        return this;
    }

    public CppBuilder ln() {
        return this;
    }

    public CppBuilder file(Type type, String ext) {
        tBuilder.append(type.fileName).append(ext);
        return this;
    }

    public String getHeader() {
        return hBuilder.toString();
    }

    public String getSource() {
        return sBuilder.toString();
    }
}
