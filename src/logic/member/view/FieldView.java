package logic.member.view;

import content.Token;
import logic.Pointer;
import logic.member.Property;
import logic.member.Variable;
import logic.member.Num;

public class FieldView {

    public Token nameToken;
    public Pointer type;

    public Variable srcVar;
    public Num srcNum;
    public Property srcPro;
    public int srcID;

    public FieldView(Token nameToken, Pointer type, Variable variable, int index) {
        this.nameToken = nameToken;
        this.type = type;
        this.srcVar = variable;
        this.srcID = index;
    }

    public FieldView(Token nameToken, Pointer type, Num num, int index) {
        this.nameToken = nameToken;
        this.type = type;
        this.srcNum = num;
        this.srcID = index;
    }

    public FieldView(Token nameToken, Pointer type, Property property) {
        this.nameToken = nameToken;
        this.type = type;
        this.srcPro = property;
    }

    public FieldView(FieldView fieldView, Pointer caller) {
        this.srcVar = fieldView.srcVar;
        this.srcNum = fieldView.srcNum;
        this.srcPro = fieldView.srcPro;
        this.srcID = fieldView.srcID;
    }

    public Token getName() {
        return nameToken;
    }

    public Pointer getType() {
        return type;
    }

    public boolean hasGet() {
        return srcNum != null || srcVar != null || srcPro.hasGet();
    }

    public boolean hasSet() {
        return srcNum == null && (srcVar != null || srcPro.hasSet());
    }

    public boolean hasOwn() {
        return srcNum != null || srcVar != null || srcPro.hasOwn();
    }

    public boolean isGetPrivate() {
        return srcNum == null && (srcPro != null ? srcPro.isGetPrivate() : srcVar.isPrivate());
    }

    public boolean isGetPublic() {
        return srcNum != null || (srcPro != null ? srcPro.isGetPublic() : srcVar.isPublic());
    }

    public boolean isSetPrivate() {
        return srcNum == null && (srcPro != null ? srcPro.isSetPrivate() : srcVar.isPrivate());
    }

    public boolean isSetPublic() {
        return srcNum == null && (srcPro != null ? srcPro.isSetPublic() : srcVar.isPublic());
    }

    public boolean isOwnPrivate() {
        return srcNum == null && (srcPro != null ? srcPro.isOwnPrivate() : srcVar.isPrivate());
    }

    public boolean isOwnPublic() {
        return srcNum != null || (srcPro != null ? srcPro.isOwnPublic() : srcVar.isPublic());
    }

    public boolean isGetFinal() {
        return srcNum != null || (srcPro != null ? srcPro.isFinal() : srcVar.isFinal());
    }

    public boolean isSetFinal() {
        return srcNum != null || (srcPro != null ? srcPro.isFinal() : srcVar.isFinal());
    }

    public boolean isOwnFinal() {
        return srcNum != null || (srcPro != null ? srcPro.isFinal() : srcVar.isFinal());
    }

    public boolean isFinal() {
        return srcNum != null || srcPro == null || srcVar.isFinal();
    }

    public boolean isGetAbstract() {
        return srcNum != null || (srcPro != null ? srcPro.isGetFinal() : srcVar.isFinal());
    }

    public boolean isLetAbstract() {
        return false;
    }

    public boolean isSetAbstract() {
        return false;
    }

    public boolean isStatic() {
        return srcNum != null || (srcVar != null && srcVar.isStatic()) || srcPro.isStatic();
    }
}
