package logic.member.view;

import content.Token;
import logic.Pointer;
import logic.member.Property;
import logic.member.Variable;
import logic.member.Num;

public class FieldView {

    public Token nameToken;
    public Pointer pointer;

    public Variable srcVar;
    public Num srcNum;
    public Property srcPro;
    public int srcID;

    public FieldView(Token nameToken, Pointer pointer, Variable variable, int index) {
        this.nameToken = nameToken;
        this.pointer = pointer;
        this.srcVar = variable;
        this.srcID = index;
    }

    public FieldView(Token nameToken, Pointer pointer, Num num, int index) {
        this.nameToken = nameToken;
        this.pointer = pointer;
        this.srcNum = num;
        this.srcID = index;
    }

    public FieldView(Token nameToken, Pointer pointer, Property property) {
        this.nameToken = nameToken;
        this.pointer = pointer;
        this.srcPro = property;
    }
}
