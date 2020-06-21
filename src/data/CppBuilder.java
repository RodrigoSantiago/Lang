package data;

import content.Key;
import content.Token;
import logic.member.view.ParamView;
import logic.params.Parameters;
import logic.templates.Generic;
import logic.templates.Template;
import logic.Pointer;
import logic.typdef.Type;

import java.util.ArrayList;

public class CppBuilder {

    private String[] indents;

    private int indent = 0, sourcePos, headerPos, genericPos;
    private StringBuilder sBuilder, hBuilder, gBuilder, tBuilder, dBuilder;
    private ArrayList<Type> tDependences, sDependences, hDependences, gDependences, dDependences;
    private Type type;
    private boolean useTemplates;

    public CppBuilder() {
        sBuilder = new StringBuilder();
        hBuilder = new StringBuilder();
        gBuilder = new StringBuilder();
        dBuilder = new StringBuilder();
        tBuilder = sBuilder;

        sDependences = new ArrayList<>();
        hDependences = new ArrayList<>();
        gDependences = new ArrayList<>();
        dDependences = new ArrayList<>();
        tDependences = sDependences;

        indents = new String[10];
        for (int i = 0; i < indents.length; i++) {
            indents[i] = "";
            for (int j = 0; j < i; j++) {
                indents[i] += "    ";
            }
        }
    }

    public CppBuilder toSource(boolean hasGeneric) {
        return hasGeneric ? toGeneric() : toSource();
    }

    public CppBuilder toSource() {
        tBuilder = sBuilder;
        tDependences = sDependences;
        return this;
    }

    public CppBuilder toHeader() {
        tBuilder = hBuilder;
        tDependences = hDependences;
        return this;
    }

    public CppBuilder toGeneric() {
        tBuilder = gBuilder;
        tDependences = gDependences;
        return this;
    }

    public void reset(Type type) {
        sourcePos = headerPos = genericPos = 0;

        sBuilder.setLength(0);
        hBuilder.setLength(0);
        gBuilder.setLength(0);
        dBuilder.setLength(0);
        sDependences.clear();
        hDependences.clear();
        gDependences.clear();
        dDependences.clear();
        toHeader();

        this.type = type;
        this.useTemplates = false;
    }

    public void markHeader() {
        headerPos = hBuilder.length();
    }

    public void markSource() {
        sourcePos = sBuilder.length();
    }

    public void markGeneric() {
        genericPos = gBuilder.length();
    }

    public void dependence(Pointer pointer) {
        if (pointer.typeSource == null && pointer.type != null) {
            if (!pointer.type.isLangBase() && !dDependences.contains(pointer.type)) {
                dDependences.add(pointer.type);
                if (pointer.type.hasGeneric()) {
                    sDependences.add(pointer.type);
                }
                hDependences.remove(pointer.type);
            }
            if (pointer.pointers != null) {
                for (Pointer p : pointer.pointers) {
                    dependence(p);
                }
            }
        }
    }

    // Header indirect Dependencies (Forward Declaration)
    public void headerDependence() {
        for (Type type : hDependences) {
            if (type.template != null) {
                dBuilder.append("template<");
                for (int i = 0; i < type.template.getCount(); i++) {
                    dBuilder.append(i > 0 ? ", typename g_" : "typename g_").append(type.template.getNameToken(i));
                }
                dBuilder.append(">\n");
            }
            dBuilder.append("class ").append(type.pathToken).append(";\n");
        }
        hBuilder.insert(headerPos, dBuilder);
        dBuilder.setLength(0);
    }

    // Source Dependencies (Include Generic Sources)
    public void sourceDependence() {
        dBuilder.append("#include \"").append(type.fileName).append(type.hasGeneric() ? ".hpp\"\n" : ".h\"\n");

        for (Type type : sDependences) {
            if (type != this.type) {
                dBuilder.append("#include \"").append(type.fileName).append(type.hasGeneric() ? ".hpp\"\n" : ".h\"\n");
            }
        }
        sBuilder.insert(sourcePos, dBuilder);
        dBuilder.setLength(0);
    }

    // Generic Source Dependencies (Include Target Header + Any other include, inclusive Generics Sources)
    public void genericDependence() {
        dBuilder.append("#include \"").append(type.fileName).append(".h\"\n");

        for (Type type : gDependences) {
            if (type != this.type) {
                dBuilder.append("#include \"").append(type.fileName).append(type.hasGeneric() ? ".hpp\"\n" : ".h\"\n");
            }
        }
        gBuilder.insert(genericPos, dBuilder);
        dBuilder.setLength(0);
    }

    // Header direct Dependencies (Inheritance and Struct and Enum Variables)
    public void directDependence() {
        for (Type type : dDependences) {
            dBuilder.append("#include \"").append(type.fileName).append(".h\"\n");
        }
        hBuilder.insert(headerPos, dBuilder);
        dBuilder.setLength(0);
    }

    public CppBuilder add(boolean addIf, String str) {
        if (addIf) {
            tBuilder.append(str);
        }
        return this;
    }

    public CppBuilder add(String str) {
        tBuilder.append(str);
        return this;
    }

    public CppBuilder add(boolean addIf, int number) {
        if (addIf) {
            tBuilder.append(number);
        }
        return this;
    }

    public CppBuilder add(int number) {
        tBuilder.append(number);
        return this;
    }

    public CppBuilder add(boolean addIf, Token token) {
        if (addIf) {
            token.addToBuilder(tBuilder);
        }
        return this;
    }

    public CppBuilder add(Token token) {
        token.addToBuilder(tBuilder);
        return this;
    }

    public CppBuilder add(Key operator, Pointer typePtr) {
        if (operator == Key.CAST) {
            add("cast_")._namePtr(typePtr);
        } else if (operator == Key.AUTO) {
            add("auto_")._namePtr(typePtr);
        } else {
            add(operator.name().toLowerCase());
        }
        return this;
    }

    private CppBuilder _namePtr(Pointer typePtr) {
        if (typePtr == Pointer.voidPointer) return add("void");
        add(typePtr.type.pathToken);
        if (typePtr.pointers != null) {
            for (Pointer pointer : typePtr.pointers) {
                _namePtr(pointer);
            }
            add("_cast");
        }
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
            if (pointer.typeSource.basePtr == Pointer.openPointer) {
                if (pointer.let) {
                    add("GLet<").nameGeneric(pointer.typeSource.nameToken).add(">");
                } else {
                    add("GPtr<").nameGeneric(pointer.typeSource.nameToken).add(">");
                }
            } else if (pointer.let) {
                add("Let<").nameGeneric(pointer.typeSource.nameToken).add(">");
            } else {
                add("Ptr<").nameGeneric(pointer.typeSource.nameToken).add(">");
            }
        } else {
            if (!pointer.type.isLangBase() && !tDependences.contains(pointer.type)) {
                if (tDependences != hDependences || !dDependences.contains(pointer.type)) {
                    tDependences.add(pointer.type);
                }
            }

            if (pointer.type.isValue()) {
                add(pointer.type.pathToken);
            } else if (pointer.let) {
                add("Let<").add(pointer.type.pathToken);
            } else {
                add("Ptr<").add(pointer.type.pathToken);
            }

            if (pointer.pointers != null) {
                tBuilder.append("<");
                for (int i = 0; i < pointer.pointers.length; i++) {
                    if (i > 0) tBuilder.append(", ");
                    if (pointer.type.isFunction()) {
                        add(pointer.pointers[i]);
                    } else {
                        gen(pointer.pointers[i]);
                    }
                }
                tBuilder.append(">");
            }
            if (pointer.type.isPointer()) {
                add(">");
            }
        }
        return this;
    }

    public CppBuilder path(Pointer pointer, boolean _static) {
        if (pointer == Pointer.nullPointer) return add("nullptr");
        if (pointer == Pointer.voidPointer) return add("void");
        if (pointer.typeSource != null) {
            nameGeneric(pointer.typeSource.nameToken);
        } else {
            if (_static) {
                add(pointer.type.staticPathToken);
            } else {
                add(pointer.type.pathToken);
                if (pointer.pointers != null) {
                    tBuilder.append("<");
                    for (int i = 0; i < pointer.pointers.length; i++) {
                        if (i > 0) tBuilder.append(", ");
                        path(pointer.pointers[i], false);
                    }
                    tBuilder.append(">");
                }
            }
        }
        return this;
    }

    public CppBuilder gen(Pointer pointer) {
        if (pointer == Pointer.nullPointer) return add("nullptr");
        if (pointer == Pointer.voidPointer) return add("void");
        if (pointer.typeSource != null) {
            nameGeneric(pointer.typeSource.nameToken);
        } else {
            if (!pointer.type.isLangBase() && !tDependences.contains(pointer.type)) {
                if (tDependences != hDependences || !dDependences.contains(pointer.type)) {
                    tDependences.add(pointer.type);
                }
            }

            add(pointer.type.pathToken);

            if (pointer.pointers != null) {
                tBuilder.append("<");
                for (int i = 0; i < pointer.pointers.length; i++) {
                    if (i > 0) tBuilder.append(", ");
                    if (pointer.type.isFunction()) {
                        add(pointer.pointers[i]);
                    } else {
                        gen(pointer.pointers[i]);
                    }
                }
                tBuilder.append(">");
            }
        }
        return this;
    }

    public CppBuilder parent(Pointer pointer) {
        dependence(pointer);
        return path(pointer, false);
    }

    public CppBuilder add(Template template) {
        return add(template, 0);
    }

    public CppBuilder add(Template template, int indent) {
        if (template != null) {
            useTemplates = true;
            add("template<");
            for (int i = 0; i < template.getCount(); i++) {
                if (i != 0) add(", ");
                add("typename ").nameGeneric(template.getNameToken(i));
            }
            add(">").ln().idt(indent);
        }
        return this;
    }

    public CppBuilder add(Parameters params) {
        for (int i = 0; i < params.getCount(); i++) {
            if (i > 0) add(", ");
            add(params.getTypePtr(i)).add(" v_").add(params.getNameToken(i));
        }
        return this;
    }

    public CppBuilder add(ParamView params) {
        for (int i = 0; i < params.getArgsCount(); i++) {
            if (i > 0) add(", ");
            add(params.getArgTypePtr(i)).add(" v_").add(params.getArgName(i));
        }
        return this;
    }

    public CppBuilder args(ParamView params) {
        return args(params, false);
    }

    public CppBuilder args(ParamView params, boolean extraValue) {
        for (int i = 0; i < params.getArgsCount(); i++) {
            if (i > 0) add(", ");
            add("v_").add(params.getArgName(i));
        }
        if (extraValue) {
            if (params.getArgsCount() > 0) add(", ");
            add("v_value");
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

    public String getHeader() {
        return hBuilder.toString();
    }

    public String getSource() {
        return sBuilder.toString();
    }

    public String getGeneric() {
        return gBuilder.toString();
    }
}
