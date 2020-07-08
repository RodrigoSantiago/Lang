package logic.stack;

import builder.CppBuilder;
import logic.Pointer;
import logic.stack.expression.LambdaCall;

public class LambdaResolve {

    public static int build(CppBuilder cBuilder, int idt, LambdaCall call) {
        int t = cBuilder.temp();
        String name = "lambda" + t;
        Pointer lambda = call.getNaturalPtr();
        Stack inner = call.innerStack;

        cBuilder.idt(idt).add("struct ").add(name).add(" {").ln();
        for (Field field : inner.shadowFields.values()) {
            cBuilder.idt(idt + 1).add(field.typePtr).add(" ").nameParam(field.nameToken).add(";").ln();
        }
        cBuilder.idt(idt + 1).add(name).add("(");
        boolean first = false;
        for (Field field : inner.shadowFields.values()) {
            cBuilder.add(first, ", ").add(field.typePtr).add(" i_").add(field.nameToken);
            first = true;
        }
        cBuilder.add(") ");
        first = false;
        for (Field field : inner.shadowFields.values()) {
            if (!first) cBuilder.add(" : ").ln();
            else cBuilder.add(", ").ln();
            first = true;
            cBuilder.idt(idt + 2).nameParam(field.nameToken).add("(i_").add(field.nameToken).add(")");
        }
        cBuilder.add(" {").ln();
        cBuilder.idt(idt + 1).add("}").ln();
        cBuilder.idt(idt + 1).add(lambda.pointers[0]).add(" ").add("run").add("(");
        for (int i = 0; i < lambda.pointers.length - 1; i++) {
            cBuilder.add(i > 1, ", ").add(lambda.pointers[i + 1]).add(" ").nameParam(call.nameTokens.get(i));
        }
        cBuilder.add(") ").in(idt + 2)
                .add(inner, idt + 2)
                .out().ln();

        cBuilder.idt(idt + 1).add("void transferOut() {").ln();
        for (Field field : inner.shadowFields.values()) {
            if (field.getTypePtr().isOpen()) {
                cBuilder.idt(1).add("transfer<").add(field.getTypePtr()).add(">::transferOut(").nameParam(field.getName()).add(");").ln();
            } else if (!field.getTypePtr().type.isSync()) {
                cBuilder.idt(1).add("this->").nameParam(field.getName()).add(".transferOut();").ln();
            }
        }
        cBuilder.idt(idt + 1).add("}").ln();
        cBuilder.idt(idt + 1).add("void transferIn() {").ln();
        for (Field field : inner.shadowFields.values()) {
            if (field.getTypePtr().isOpen()) {
                cBuilder.idt(1).add("transfer<").add(field.getTypePtr()).add(">::transferIn(").nameParam(field.getName()).add(");").ln();
            } else if (!field.getTypePtr().type.isSync()) {
                cBuilder.idt(1).add("this->").nameParam(field.getName()).add(".transferIn();").ln();
            }
        }
        cBuilder.idt(idt + 1).add("}").ln();
        cBuilder.idt(idt + 1).add("void clear() {").ln();
        for (Field field : inner.shadowFields.values()) {
            if (field.getTypePtr().isOpen()) {
                cBuilder.idt(1).add("transfer<").add(field.getTypePtr()).add(">::clear(").nameParam(field.getName()).add(");").ln();
            } else if (!field.getTypePtr().type.isSync()) {
                cBuilder.idt(1).add("this->").nameParam(field.getName()).add(".clear();").ln();
            }
        }
        cBuilder.idt(idt + 1).add("}").ln();
        cBuilder.idt(idt).add("};").ln();

        return t;
    }
}
