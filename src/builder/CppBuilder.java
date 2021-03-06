package builder;

import content.Key;
import content.Token;
import logic.member.view.ParamView;
import logic.params.Parameters;
import logic.stack.Stack;
import logic.stack.expression.CallGroup;
import logic.stack.expression.Expression;
import logic.templates.Template;
import logic.Pointer;
import logic.typdef.Type;

import java.util.ArrayList;

public class CppBuilder {

    private Type type;
    private int sourcePos;
    private int headerPos;
    private int genericPos;
    private StringBuilder tBuilder;
    private final StringBuilder sBuilder;
    private final StringBuilder hBuilder;
    private final StringBuilder gBuilder;
    private final StringBuilder dBuilder;
    private ArrayList<Type> tDependences;
    private final ArrayList<Type> sDependences;
    private final ArrayList<Type> hDependences;
    private final ArrayList<Type> gDependences;
    private final ArrayList<Type> dDependences;

    private int tempID;
    private TempBlock capture;
    private final ArrayList<Temp> tempVars;
    private final ArrayList<TempBlock> tempBlocks;

    private final String[] indents;

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

        tempVars = new ArrayList<>();
        tempBlocks = new ArrayList<>();

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

        tempID = 0;
        capture = null;
        tempBlocks.clear();
        tempVars.clear();

        this.type = type;
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

    public void dependenceAdd(Pointer pointer) {
        if (pointer.typeSource == null && pointer.type != null) {
            if (!dDependences.contains(pointer.type)) {
                if (!pointer.type.isLangBase()) {
                    dDependences.add(pointer.type);
                    if (pointer.type.hasGeneric()) {
                        sDependences.add(pointer.type);
                    }
                    hDependences.remove(pointer.type);
                }
                if (pointer.type.isValue()) {
                    dependenceAdd(pointer.type.parent);
                }
            }
            if (pointer.pointers != null) {
                for (Pointer p : pointer.pointers) {
                    dependenceAdd(p);
                }
            }
        }
    }

    public void dependence(Pointer pointer) {
        if (pointer.type != null && !tDependences.contains(pointer.type)) {
            if (!pointer.type.isLangBase()) {
                if (tDependences != hDependences || !dDependences.contains(pointer.type)) {
                    tDependences.add(pointer.type);
                }
            }
            if (pointer.type.isValue()) {
                dependence(pointer.type.parent);
            }
        }
    }
    public void dependence(ParamView params) {
        if (params != null) {
            for (int i = 0; i < params.getArgsCount(); i++) {
                dependence(params.getArgTypePtr(i));
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
        dBuilder.append("#include \"").append(type.fileName).append(type.hasGenericFile() ? ".hpp\"\n" : ".h\"\n");

        for (Type type : sDependences) {
            if (type != this.type) {
                dBuilder.append("#include \"")
                        .append(type.fileName).append(type.hasGenericFile() ? ".hpp\"\n" : ".h\"\n");
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
                dBuilder.append("#include \"")
                        .append(type.fileName).append(type.hasGenericFile() ? ".hpp\"\n" : ".h\"\n");
            }
        }
        gBuilder.insert(genericPos, dBuilder);
        dBuilder.setLength(0);
    }

    // Header direct Dependencies (Inheritance and Struct and Enum Variables)
    public void directDependence() {
        for (Type type : dDependences) {
            dBuilder.append("#include \"")
                    .append(type.fileName).append(".h\"\n");
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

    public CppBuilder up(String str) {
        tBuilder.append(str.toUpperCase());
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

    public CppBuilder add(long number) {
        tBuilder.append(number);
        return this;
    }

    public CppBuilder add(double number) {
        tBuilder.append(number);
        return this;
    }

    public CppBuilder add(float number) {
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

    public CppBuilder add(Key key) {
        tBuilder.append(key.string);
        return this;
    }

    public CppBuilder add(Expression expression, int idt) {
        expression.build(this, idt);
        return this;
    }

    public CppBuilder add(ArrayList<Expression> parameters, int idt) {
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) add(", ");
            parameters.get(i).build(this, idt);
        }
        return this;
    }

    public CppBuilder add(CallGroup group, int idt) {
        group.build(this, idt);
        return this;
    }

    public CppBuilder add(Stack stack, int idt) {
        stack.build(this, idt);
        return this;
    }

    public CppBuilder add(Temp temp) {
        return add(temp.name);
    }

    public CppBuilder add(Template template) {
        return add(template, 0);
    }

    public CppBuilder add(Template template, int indent) {
        if (template != null) {
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
            add(params.getTypePtr(i)).add(" ").nameParam(params.getName(i));
        }
        return this;
    }

    public CppBuilder add(Parameters params, Pointer valuePtr) {
        for (int i = 0; i < params.getCount(); i++) {
            if (i > 0) add(", ");
            add(params.getTypePtr(i)).add(" ").nameParam(params.getName(i));
        }
        if (valuePtr != null) {
            if (params.getCount() > 0) add(", ");
            add(valuePtr).add(" ").nameParam("value");
        }
        return this;
    }

    public CppBuilder add(ParamView params) {
        for (int i = 0; i < params.getArgsCount(); i++) {
            if (i > 0) add(", ");
            add(params.getArgTypePtr(i)).add(" ").nameParam(params.getArgName(i));
        }
        return this;
    }

    public CppBuilder args(Parameters params) {
        return args(params, false);
    }

    public CppBuilder args(Parameters params, boolean extraValue) {
        for (int i = 0; i < params.getCount(); i++) {
            if (i > 0) add(", ");
            nameParam(params.getName(i));
        }
        if (extraValue) {
            if (params.getCount() > 0) add(", ");
            nameParam("value");
        }
        return this;
    }

    public CppBuilder args(ParamView params) {
        return args(params, false);
    }

    public CppBuilder args(ParamView params, boolean extraValue) {
        for (int i = 0; i < params.getArgsCount(); i++) {
            if (i > 0) add(", ");
            nameParam(params.getArgName(i));
        }
        if (extraValue) {
            if (params.getArgsCount() > 0) add(", ");
            nameParam("value");
        }
        return this;
    }

    public CppBuilder add(Pointer pointer) {
        if (pointer == Pointer.nullPointer) return add("nullptr");
        if (pointer == Pointer.voidPointer) return add("void");
        if (pointer.typeSource != null) {
            if (pointer.typeSource.basePtr.isOpen()) {
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
            dependence(pointer);

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

    public CppBuilder path(Pointer pointer) {
        return path(pointer, false);
    }

    public CppBuilder path(Pointer pointer, boolean _static) {
        if (pointer == Pointer.nullPointer) return add("nullptr");
        if (pointer == Pointer.voidPointer) return add("void");
        if (pointer.typeSource != null) {
            nameGeneric(pointer.typeSource.nameToken);
        } else {
            dependence(pointer);

            if (_static) {
                add(pointer.type.staticPathToken);
            } else {
                add(pointer.type.pathToken);
                if (pointer.pointers != null) {
                    tBuilder.append("<");
                    for (int i = 0; i < pointer.pointers.length; i++) {
                        if (i > 0) tBuilder.append(", ");
                        path(pointer.pointers[i]);
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
            dependence(pointer);

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
        dependenceAdd(pointer);
        return path(pointer);
    }

    public CppBuilder nameGeneric(Token token) {
        return add("g_").add(token);
    }

    public CppBuilder nameField(Token nameToken) {
        return add("f_").add(nameToken);
    }

    public CppBuilder nameField(String name) {
        return add("f_").add(name);
    }

    public CppBuilder nameStaticField(Token nameToken) {
        return add("s_").add(nameToken);
    }

    public CppBuilder nameStaticField(String name) {
        return add("s_").add(name);
    }

    public CppBuilder nameParam(Token nameToken) {
        return add("v_").add(nameToken);
    }

    public CppBuilder nameParam(String name) {
        return add("v_").add(name);
    }

    public CppBuilder nameMethod(Token nameToken) {
        return add("m_").add(nameToken);
    }

    public CppBuilder nameMethod(String name) {
        return add("m_").add(name);
    }

    public CppBuilder nameGet(Token nameToken) {
        return add("get_").add(nameToken);
    }

    public CppBuilder nameGet(String name) {
        return add("get_").add(name);
    }

    public CppBuilder nameSet(Token nameToken) {
        return add("set_").add(nameToken);
    }

    public CppBuilder nameSet(String name) {
        return add("set_").add(name);
    }

    public CppBuilder nameOwn(Token nameToken) {
        return add("own_").add(nameToken);
    }

    public CppBuilder nameOwn(String name) {
        return add("own_").add(name);
    }

    public CppBuilder nameGet() {
        return add("get");
    }

    public CppBuilder nameSet() {
        return add("set");
    }

    public CppBuilder nameOwn() {
        return add("own");
    }

    public CppBuilder nameOp(Key operator, Pointer castPtr) {
        if (operator == Key.CAST) {
            return add("cast_").nameTypeOp(castPtr);
        } else if (operator == Key.AUTO) {
            return add("auto_").nameTypeOp(castPtr);
        } else {
            return add(operator.name().toLowerCase());
        }
    }

    private CppBuilder nameTypeOp(Pointer pointer) {
        if (pointer == Pointer.nullPointer) return add("nullptr");
        if (pointer == Pointer.voidPointer) return add("void");
        if (pointer.typeSource != null) {
            nameGeneric(pointer.typeSource.nameToken);
        } else {
            if (pointer.let) add("let_"); // never happens
            add(pointer.type.pathToken);

            if (pointer.pointers != null) {
                tBuilder.append("_s_");
                for (int i = 0; i < pointer.pointers.length; i++) {
                    if (i > 0) add("_c_");
                    nameTypeOp(pointer.pointers[i]);
                }
                tBuilder.append("_e_");
            }
        }
        return this;
    }

    public void cast(Pointer src, Pointer dst, CallGroup group, int idt) {
        add("cast<").add(src).add(", ").add(dst).add(">::as(");
        group.build(this, idt);
        add(")");
    }

    public CppBuilder in(int idt) {
        return in(false, idt);
    }

    public CppBuilder in(boolean yield, int idt) {
        add("{").ln();
        TempBlock tBlock = new TempBlock(capture, tBuilder.length(), idt, yield);
        if (yield || capture == null || !capture.yield) {
            capture = tBlock;
        }
        tempBlocks.add(tBlock);
        return this;
    }

    public int temp() {
        return (tempID++);
    }

    public Temp temp(Pointer typePtr) {
        return temp(typePtr, false);
    }

    public Temp temp(Pointer typePtr, int array) {
        Temp temp = new Temp(capture, typePtr, "t" + (tempID++), true, array);
        tempVars.add(temp);
        return temp;
    }

    public Temp temp(Pointer typePtr, boolean pure) {
        Temp temp = new Temp(capture, typePtr, "t" + (tempID++), pure, 0);
        tempVars.add(temp);
        return temp;
    }

    public CppBuilder out() {
        StringBuilder prev = tBuilder;
        TempBlock tblock = tempBlocks.remove(tempBlocks.size() - 1);
        boolean has = false;
        tBuilder = dBuilder;
        for (int i = 0; i < tempVars.size(); i++) {
            Temp tempVar = tempVars.get(i);
            if (tempVar.block == tblock) {
                has = true;
                idt(tblock.idt);
                if (tempVar.array > 0) {
                    path(tempVar.typePtr).add(" ").add(tempVar.name).add(" [").add(tempVar.array).add("]").add(";").ln();
                } else if (tempVar.ptr) {
                    path(tempVar.typePtr).add("* ").add(tempVar.name).add(";").ln();
                } else {
                    add(tempVar.typePtr).add(" ").add(tempVar.name).add(";").ln();
                }
                tempVars.remove(i);
                i--;
            }
        }
        tBuilder = prev;

        if (has) {
            tBuilder.insert(tblock.pos, dBuilder);
            dBuilder.setLength(0);
        }
        if (tempBlocks.size() == 0) {
            tempID = 0;
        }
        capture = tblock.capture;
        return idt(tblock.idt - 1).add("}");
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
