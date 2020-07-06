package logic.stack;

import builder.CppBuilder;
import data.ContentFile;
import logic.Pointer;
import logic.member.*;
import logic.member.view.IndexerView;
import logic.member.view.MethodView;
import logic.params.Parameters;

import java.util.ArrayList;

public class YieldResolve {

    public static void build(CppBuilder cBuilder, int idt, Stack stack, int yieldID,
                             Parameters param, Pointer valuePtr) {

        ContentFile cFile = stack.cFile;
        Pointer typePtr = stack.getReturnPtr();
        Pointer yieldPtr = stack.getYiledPtr().toLet();

        cBuilder.idt(idt).add("class yield : public ").path(cFile.langObjectPtr(), false)
                .add(", public ").path(typePtr, false).add(" {").ln()
                .idt(idt).add("public :").ln();

        cBuilder.idt(idt + 1).add("// Type").ln()
                .idt(idt + 1).add("using P = Ptr<yield>;").ln()
                .idt(idt + 1).add("using L = Let<yield>;").ln();

        cBuilder.idt(idt + 1).add("static lang::type* typeOf() { return getType<yield, ").path(typePtr, false).add(", ")
                .path(cFile.langObjectPtr(), false).add(">(); }").ln()
                .idt(idt + 1).add("virtual lang::type* getTypeOf() { return typeOf(); }").ln()
                .idt(idt + 1).add("virtual lang_Object* self() { return this; }").ln()
                .ln();

        cBuilder.idt(idt + 1).add("virtual void destroy() { ").path(cFile.langObjectPtr(), false).add("::destroy(); }").ln()
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
                .idt(idt + 2).add("return ").path(cFile.langObjectPtr(), false).add("::").nameMethod("equals").add("(other);").ln()
                .idt(idt + 1).add("}").ln()
                .ln();

        cBuilder.idt(idt + 1).add(yieldPtr).add(" yieldValue;").ln();
        cBuilder.idt(idt + 1).add(cFile.langBoolPtr()).add(" yieldBreak;").ln();
        cBuilder.idt(idt + 1).add(cFile.langIntPtr()).add(" yieldID = -1;").ln();
        for (Field shadow : stack.shadowFields.values()) {
            cBuilder.idt(idt + 1).add(shadow.getTypePtr().toLet()).add(" ").nameParam(shadow.nameToken).add(";").ln();
        }
        for (Field field : stack.fields.values()) {
            cBuilder.idt(idt + 1).add(field.typePtr).add(" ").nameParam(field.nameToken).add(";").ln();
        }
        cBuilder.ln();

        if (valuePtr == null) {
            cBuilder.idt(idt + 1).add("yield* create(").add(param);
        } else {
            cBuilder.idt(idt + 1).add("yield* create(").add(param).add(param.getCount() > 0, ", ")
                    .add(valuePtr).add(" ").nameParam("value");
        }
        if (stack.shadowFields.size() > 0) {
            boolean first = (param.getCount() > 0 || valuePtr != null);
            for (Field shadow : stack.shadowFields.values()) {
                if (first) cBuilder.add(", ");
                first = true;
                cBuilder.add(shadow.getTypePtr().toLet()).add(" ").nameParam(shadow.nameToken);
            }
        }
        cBuilder.add(" ) {").ln();
        cBuilder.idt(idt + 2).path(cFile.langObjectPtr(), false).add("::create();").ln();
        cBuilder.idt(idt + 2).add("yieldValue = lang::value<GPtr<").add(yieldPtr).add(">>::def();").ln();
        for (Field shadow : stack.shadowFields.values()) {
            cBuilder.idt(idt + 2).add("this->").nameParam(shadow.nameToken)
                    .add(" = ").nameParam(shadow.nameToken).add(";").ln();
        }
        for (int i = 0; i < param.getCount(); i++) {
            cBuilder.idt(idt + 2).add("this->").nameParam(param.getNameToken(i))
                    .add(" = ").nameParam(param.getNameToken(i)).add(";").ln();
        }

        cBuilder.idt(idt + 2).add("return this;").ln()
                .idt(idt + 1).add("}").ln()
                .ln();

        cBuilder.idt(idt + 1).add("virtual ").add(cFile.langBoolPtr()).add(" ").nameMethod("move() {").ln();
        cBuilder.idt(idt + 2).add("if (yieldBreak) return false;").ln();
        for (int i = 0; i < yieldID; i++) {
            cBuilder.idt(idt + 2).add(i > 0, "else ").add("if (yieldID == ").add(i).add(") goto yield_").add(i).add(";").ln();
        }
        for (Line line : stack.block.lines) {
            line.build(cBuilder, idt + 2, idt + 2);
        }
        stack.block.buildDestroyer(cBuilder, idt + 2);

        cBuilder.ln();
        cBuilder.idt(idt + 2).add("yieldBreak = true;").ln()
                .idt(idt + 2).add("return false;").ln()
                .idt(idt + 1).add("}").ln()
                .ln();

        cBuilder.idt(idt + 1).add("virtual ").add(yieldPtr).add(" ").nameGet("current() {").ln()
                .idt(idt + 2).add("return yieldValue;").ln()
                .idt(idt + 1).add("}").ln()
                .ln();

        cBuilder.idt(idt + 1).add("virtual ").add(cFile.langBoolPtr()).add(" ").nameMethod("remove() {").ln()
                .idt(idt + 2).add("return false;").ln()
                .idt(idt + 1).add("}").ln()
                .ln();

        cBuilder.idt(idt).add("};").ln()
                .ln();

        cBuilder.idt(idt).add("return (new yield())->create(").args(param, valuePtr != null);
        if (stack.shadowFields.size() > 0) {
            boolean first = (param.getCount() > 0 || valuePtr != null);
            for (Field shadow : stack.shadowFields.values()) {
                if (first) cBuilder.add(", ");
                cBuilder.nameParam(shadow.nameToken);
            }
        }
        cBuilder.add(");").ln();
    }
}
