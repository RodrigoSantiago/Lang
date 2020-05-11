package data;

import content.Token;
import logic.templates.Template;
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

    public CppBuilder add(Token token) {
        token.addToBuilder(tBuilder);
        return this;
    }

    public CppBuilder name(String name) {
        tBuilder.append(name);
        return this;
    }

    public CppBuilder nameClass(Token token) {
        token.addToBuilder(tBuilder);
        return this;
    }

    public CppBuilder nameGeneric(Token token) {
        tBuilder.append("g_");
        token.addToBuilder(tBuilder);
        return this;
    }

    public CppBuilder add(Pointer pointer) {
        if (pointer == Pointer.nullPointer) return add("nullptr");
        if (pointer == Pointer.voidPointer) return add("void");
        if (pointer.typeSource != null) {
            nameGeneric(pointer.typeSource.nameToken);
        } else {
            try {
                if (pointer.type.isClass() || pointer.type.isInterface()) {
                    // Ptr<
                    add(pointer.type.pathToken);
                } else {
                    add(pointer.type.pathToken);
                }
            }catch ( Exception e) {
                System.out.println();
            }

            if (pointer.pointers != null) {
                tBuilder.append("<");
                for (int i = 0; i < pointer.pointers.length; i++) {
                    if (i > 0) tBuilder.append(", ");
                    add(pointer.pointers[i]);
                }
                tBuilder.append(">");
            }
            if (pointer.type.isClass() || pointer.type.isInterface()) {
                // >
                add("*");
            }
        }
        return this;
    }

    public CppBuilder parent(Pointer pointer) {
        if (pointer == null) return add("IObject");
        if (pointer == Pointer.nullPointer) return add("nullptr");
        if (pointer == Pointer.voidPointer) return add("void");
        if (pointer.typeSource != null) {
            nameGeneric(pointer.typeSource.nameToken);
        } else {
            if (pointer.type.isClass() || pointer.type.isInterface()) {
                // Ptr<
                add(pointer.type.pathToken);
            } else {
                add(pointer.type.pathToken);
            }

            if (pointer.pointers != null) {
                tBuilder.append("<");
                for (int i = 0; i < pointer.pointers.length; i++) {
                    if (i > 0) tBuilder.append(", ");
                    add(pointer.pointers[i]);
                }
                tBuilder.append(">");
            }
        }
        return this;
    }

    public CppBuilder add(Template template) {
        if (template != null) {
            add("template<");
            for (int i = 0; i < template.generics.size(); i++) {
                if (i != 0) add(", ");
                add("typename ").nameGeneric(template.generics.get(i).nameToken);
            }
            add(">").ln();
        }
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
        tBuilder.append("\n");
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
