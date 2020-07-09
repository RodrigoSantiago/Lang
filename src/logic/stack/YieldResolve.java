package logic.stack;

import builder.CppBuilder;
import content.Key;
import content.Token;
import data.ContentFile;
import logic.Pointer;
import logic.params.Parameters;
import logic.stack.line.LineYield;

import java.util.ArrayList;
import java.util.HashMap;

public class YieldResolve {

    public static void build(CppBuilder cBuilder, int idt, Stack stack, Parameters param, Pointer valuePtr) {
        HashMap<Token, Field> fields = stack.getFields();
        HashMap<Token, Field> shadowFields = stack.getShadowFields();
        ArrayList<LineYield> yields = stack.getYields();

        ContentFile cFile = stack.cFile;
        Pointer typePtr = stack.getReturnPtr();
        Pointer yieldPtr = stack.getYiledPtr().toLet();
        Field yThis = shadowFields.get(new Token("this", 0, 4, Key.THIS, false));
        Field sThis = yThis == null ? fields.get(new Token("this", 0, 4, Key.THIS, false)) : null;

        cBuilder.idt(idt).add("class yield : public ").path(cFile.langObjectPtr())
                .add(", public ").path(typePtr).add(" {").ln()
                .idt(idt).add("public :").ln();

        cBuilder.idt(idt + 1).add("// Type").ln()
                .idt(idt + 1).add("using P = Ptr<yield>;").ln()
                .idt(idt + 1).add("using L = Let<yield>;").ln();

        cBuilder.idt(idt + 1).add("static lang::type* typeOf() { return getType<yield, ").path(typePtr).add(", ")
                .path(cFile.langObjectPtr()).add(">(); }").ln()
                .idt(idt + 1).add("virtual lang::type* getTypeOf() { return typeOf(); }").ln()
                .idt(idt + 1).add("virtual lang_Object* self() { return this; }").ln()
                .ln();

        cBuilder.idt(idt + 1).add("virtual void destroy() { ").path(cFile.langObjectPtr()).add("::destroy(); }").ln()
                .ln();

        cBuilder.idt(idt + 1).add("virtual ").add(cFile.langStringPtr()).add(" ").nameMethod("toString() {").ln()
                .idt(idt + 2).add("return \"YieldIterator\";").ln()
                .idt(idt + 1).add("}").ln()
                .ln();

        cBuilder.idt(idt + 1).add("virtual ").add(cFile.langIntPtr()).add(" ").nameMethod("hash() {").ln()
                .idt(idt + 2).add("return 0;").ln()
                .idt(idt + 1).add("}").ln()
                .ln();

        cBuilder.idt(idt + 1).add("virtual ").add(cFile.langBoolPtr()).add(" ").nameMethod("equals(")
                .add(cFile.langObjectPtr(true)).add(" other) {").ln()
                .idt(idt + 2).add("return ").path(cFile.langObjectPtr()).add("::").nameMethod("equals").add("(other);").ln()
                .idt(idt + 1).add("}").ln()
                .ln();

        cBuilder.idt(idt + 1).add(yieldPtr).add(" yieldValue;").ln();
        cBuilder.idt(idt + 1).add(typePtr).add(" yieldInner;").ln();
        cBuilder.idt(idt + 1).add(cFile.langBoolPtr()).add(" yieldBreak = false;").ln();
        cBuilder.idt(idt + 1).add(cFile.langIntPtr()).add(" yieldID = -1;").ln();
        for (Field shadow : shadowFields.values()) {
            cBuilder.idt(idt + 1).add(shadow.getTypePtr().toLet()).add(" ").nameParam(shadow.nameToken).add(";").ln();
        }
        for (Field field : fields.values()) {
            cBuilder.idt(idt + 1).add(field.typePtr).add(" ").nameParam(field.nameToken).add(";").ln();
        }
        cBuilder.ln();

        boolean first = false;
        cBuilder.idt(idt + 1).add("yield* create(");
        if (param != null) {
            cBuilder.add(param);
            first = param.getCount() > 0;
        }
        if (valuePtr != null) {
            cBuilder.add(first, ", ").add(valuePtr).add(" ").nameParam("value");
            first = true;
        }
        for (Field shadow : shadowFields.values()) {
            cBuilder.add(first, ", ").add(shadow.getTypePtr().toLet()).add(" ").nameParam(shadow.nameToken);
            first = true;
        }
        if (sThis != null) {
            cBuilder.add(first, ", ").add(sThis.getTypePtr().toLet()).add(" ").nameParam(sThis.nameToken);
        }
        cBuilder.add(") {").ln();
        cBuilder.idt(idt + 2).path(cFile.langObjectPtr()).add("::create();").ln();
        cBuilder.idt(idt + 2).add("yieldValue = lang::value<GPtr<").add(yieldPtr).add(">>::def();").ln();
        if (param != null) {
            for (int i = 0; i < param.getCount(); i++) {
                cBuilder.idt(idt + 2).add("this->").nameParam(param.getName(i))
                        .add(" = ").nameParam(param.getName(i)).add(";").ln();
            }
        }
        if (valuePtr != null) {
            cBuilder.idt(idt + 2).add("this->").nameParam("value").add(" = ").nameParam("value").add(";").ln();
        }
        for (Field shadow : shadowFields.values()) {
            cBuilder.idt(idt + 2).add("this->").nameParam(shadow.nameToken)
                    .add(" = ").nameParam(shadow.nameToken).add(";").ln();
        }
        if (sThis != null) {
            cBuilder.idt(idt + 2).add("this->").nameParam(sThis.nameToken).add(" = ").add(sThis.nameToken).add(";").ln();
        }
        cBuilder.idt(idt + 2).add("return this;").ln()
                .idt(idt + 1).add("}").ln()
                .ln();

        cBuilder.idt(idt + 1).add("virtual ").add(cFile.langBoolPtr()).add(" ").nameMethod("move() ").in(true, idt + 2);
        cBuilder.idt(idt + 2).add("if (yieldBreak) return false;").ln()
                .idt(idt + 2).add("if (yieldInner != nullptr && yieldInner->m_move()) { ").ln()
                .idt(idt + 3).add("yieldValue = yieldInner->get_current();").ln()
                .idt(idt + 3).add("return true;").ln()
                .idt(idt + 2).add("} else if (yieldInner != nullptr) yieldInner = nullptr;").ln();

        for (int i = 0; i < yields.size(); i++) {
            int temp = cBuilder.temp();
            yields.get(i).setYieldID(temp);
            cBuilder.idt(idt + 2).add(i > 0, "else ").add("if (yieldID == ").add(i)
                    .add(") goto yield_").add(temp).add(";").ln();
        }
        for (Line line : stack.block.lines) {
            line.build(cBuilder, idt + 2, idt + 2);
        }
        stack.block.buildDestroyer(cBuilder, idt + 2);

        cBuilder.ln();
        cBuilder.idt(idt + 2).add("yieldBreak = true;").ln()
                .idt(idt + 2).add("return false;").ln()
                .out().ln()
                .ln();

        cBuilder.idt(idt + 1).add("virtual ").add(yieldPtr).add(" ").nameGet("current() {").ln()
                .idt(idt + 2).add("return yieldValue;").ln()
                .idt(idt + 1).add("}").ln()
                .ln();

        cBuilder.idt(idt + 1).add("virtual ").add(cFile.langBoolPtr()).add(" ").nameMethod("remove() {").ln()
                .idt(idt + 2).add("return false;").ln()
                .idt(idt + 1).add("}").ln()
                .ln();

        // Transfer OUT [Strong[+Open], Non-Sync Struct]
        cBuilder.idt(idt + 1).add("virtual void transferOut() {").ln();
        for (Field field : shadowFields.values()) {
            Pointer ptr = field.getTypePtr();
            if (!ptr.let && !ptr.isSync()) {
                if (ptr.isOpen()) {
                    cBuilder.idt(1).add("transfer<").add(ptr).add(">::out(").nameField(field.getName()).add(");").ln();
                } else {
                    cBuilder.idt(1).add("this->").nameField(field.getName()).add(".transferOut();").ln();
                }
            }
        }
        cBuilder.idt(idt + 1).add("}").ln();

        // Transfer IN [Strong[+Open], Non-Sync Struct]
        cBuilder.idt(idt + 1).add("virtual void transferIn() {").ln();
        for (Field field : shadowFields.values()) {
            Pointer ptr = field.getTypePtr();
            if (!ptr.let && !ptr.isSync()) {
                if (ptr.isOpen()) {
                    cBuilder.idt(1).add("transfer<").add(ptr).add(">::in(").nameField(field.getName()).add(");").ln();
                } else {
                    cBuilder.idt(1).add("this->").nameField(field.getName()).add(".transferIn();").ln();
                }
            }
        }
        cBuilder.idt(idt + 1).add("}").ln();

        // Transfer Clear [Strong/Weak[+Open], Non-Sync Struct]
        cBuilder.idt(idt + 1).add("virtual void clear() {").ln();
        for (Field field : shadowFields.values()) {
            Pointer ptr = field.getTypePtr();
            if (!ptr.isSync()) {
                if (ptr.isOpen()) {
                    cBuilder.idt(1).add("transfer<").add(ptr).add(">::clear(").nameField(field.getName()).add(");").ln();
                } else {
                    cBuilder.idt(1).add("this->").nameField(field.getName()).add(".clear();").ln();
                }
            }
        }
        if (sThis != null) {
            Pointer ptr = sThis.getTypePtr();
            if (!ptr.isSync()) {
                if (ptr.isOpen()) {
                    cBuilder.idt(1).add("transfer<").add(ptr).add(">::clear(").nameField("this").add(");").ln();
                } else {
                    cBuilder.idt(1).add("this->").nameField("this").add(".clear();").ln();
                }
            }
            cBuilder.idt(1).add("this->").nameParam("this").add(".clear();").ln();
        }
        cBuilder.idt(idt + 1).add("}").ln();

        cBuilder.idt(idt).add("};").ln()
                .ln();

        cBuilder.idt(idt).add("return (new yield())->create(");

        first = false;
        if (param != null) {
            cBuilder.args(param);
            first = param.getCount() > 0;
        }
        if (valuePtr != null) {
            cBuilder.add(first, ", ").nameParam("value");
            first = true;
        }
        if (shadowFields.size() > 0) {
            for (Field shadow : shadowFields.values()) {
                cBuilder.add(first, ", ").nameParam(shadow.nameToken);
                first = true;
            }
        }
        if (sThis != null) {
            cBuilder.add(first, ", ").add("this");
        }
        cBuilder.add(");").ln();
    }
}
