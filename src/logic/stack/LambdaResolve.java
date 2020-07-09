package logic.stack;

import builder.CppBuilder;
import content.Token;
import logic.Pointer;
import logic.member.view.FieldView;
import logic.stack.expression.LambdaCall;

import java.util.HashMap;

public class LambdaResolve {

    public static int build(CppBuilder cBuilder, int idt, LambdaCall call) {
        int t = cBuilder.temp();
        String name = "lambda" + t;
        Pointer lambda = call.getNaturalPtr();
        Stack inner = call.innerStack;
        HashMap<Token, Field> shadowFields = inner.getShadowFields();

        cBuilder.idt(idt).add("struct ").add(name).add(" {").ln();
        for (Field field : shadowFields.values()) {
            cBuilder.idt(idt + 1).add(field.typePtr).add(" ").nameParam(field.nameToken).add(";").ln();
        }
        cBuilder.idt(idt + 1).add(name).add("(");
        boolean first = false;
        for (Field field : shadowFields.values()) {
            cBuilder.add(first, ", ").add(field.typePtr).add(" i_").add(field.nameToken);
            first = true;
        }
        cBuilder.add(") ");
        first = false;
        for (Field field : shadowFields.values()) {
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
        cBuilder.idt(idt + 1).add("}").ln();

        cBuilder.idt(idt).add("};").ln();

        return t;
    }
}
