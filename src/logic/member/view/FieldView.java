package logic.member.view;

import content.Token;
import data.ContentFile;
import logic.Pointer;
import logic.member.Property;
import logic.member.Variable;
import logic.member.Num;
import logic.stack.Stack;
import logic.typdef.Type;

public class FieldView {

    Pointer caller;
    public Token nameToken;
    public Pointer typePtr;

    private Variable srcVar;
    private int srcID;

    private Num srcNum;
    public Property srcPro;

    public Property srcGet;
    public Property srcSet;
    public Property srcOwn;

    public FieldView(Token nameToken, Pointer typePtr, Variable variable, int index) {
        this.nameToken = nameToken;
        this.typePtr = typePtr;
        this.srcVar = variable;
        this.srcID = index;
    }

    public FieldView(Token nameToken, Pointer typePtr, Num num, int index) {
        this.nameToken = nameToken;
        this.typePtr = typePtr;
        this.srcNum = num;
        this.srcID = index;
    }

    public FieldView(Token nameToken, Pointer typePtr, Property property) {
        this.nameToken = nameToken;
        this.typePtr = typePtr;
        this.srcPro = property;
        srcGet = property.hasGet() ? property : null;
        srcSet = property.hasSet() ? property : null;
        srcOwn = property.hasOwn() ? property : null;
    }

    public FieldView(Pointer caller, FieldView fieldView) {
        this.caller = caller;
        this.nameToken = fieldView.nameToken;
        this.srcVar = fieldView.srcVar;
        this.srcNum = fieldView.srcNum;
        this.srcPro = fieldView.srcPro;
        this.srcID = fieldView.srcID;
        if (Pointer.hasGeneric(fieldView.getTypePtr(), caller)) {
            typePtr = Pointer.byGeneric(fieldView.getTypePtr(), caller);
        } else {
            typePtr = fieldView.getTypePtr();
        }
        srcGet = fieldView.srcGet;
        srcSet = fieldView.srcSet;
        srcOwn = fieldView.srcOwn;
    }

    public ContentFile getGetFile() {
        return srcGet != null ? srcGet.cFile : srcPro != null ? srcPro.cFile : srcVar != null ? srcVar.cFile : srcNum.cFile;
    }

    public ContentFile getSetFile() {
        return srcSet != null ? srcSet.cFile : srcPro != null ? srcPro.cFile : srcVar != null ? srcVar.cFile : srcNum.cFile;
    }

    public ContentFile getOwnFile() {
        return srcOwn != null ? srcOwn.cFile : srcPro != null ? srcPro.cFile : srcVar != null ? srcVar.cFile : srcNum.cFile;
    }

    public Token getName() {
        return nameToken;
    }

    public Pointer getTypePtr() {
        return typePtr;
    }

    public boolean isFrom(Type type) {
        return  (srcPro != null && srcPro.type == type) ||
                (srcVar != null && srcVar.type == type) || (srcNum != null && srcNum.type == type);
    }

    public boolean canShadow(FieldView other) {
        return isVariable() && other.isVariable();
    }

    public boolean canOverride(FieldView other) {
        return isProperty() && other.isProperty() && getTypePtr().equals(other.getTypePtr());
    }

    public boolean canAcessGet(Type type) {
        return srcGet != null && (
                (srcGet.isPrivate() && srcPro.cFile == type.cFile) ||
                (srcGet.isPublic()) || (srcPro.cFile.library == type.cFile.library));
    }

    public boolean canAcessSet(Type type) {
        return srcSet != null && (
                (srcSet.isPrivate() && srcPro.cFile == type.cFile) ||
                        (srcSet.isPublic()) || (srcPro.cFile.library == type.cFile.library));
    }

    public boolean canAcessOwn(Type type) {
        return srcOwn != null && (
                (srcOwn.isPrivate() && srcPro.cFile == type.cFile) ||
                        (srcOwn.isPublic()) || (srcPro.cFile.library == type.cFile.library));
    }

    public void setGetSource(FieldView other) {
        srcGet = other.srcGet;
    }

    public void setSetSource(FieldView other) {
        srcSet = other.srcSet;
    }

    public void setOwnSource(FieldView other) {
        srcOwn = other.srcOwn;
    }

    public boolean isProperty() {
        return srcPro != null;
    }

    public boolean isVariable() {
        return srcVar != null;
    }

    public boolean isNum() {
        return srcNum != null;
    }

    public boolean isStatic() {
        return srcPro != null ? srcPro.isStatic() : srcVar != null ? srcVar.isStatic() : true;
    }

    public boolean isLet() {
        return srcPro != null ? srcPro.isLet() : srcVar != null ? srcVar.isLet() : false;
    }

    public boolean hasGet() {
        return srcPro == null || srcGet != null;
    }

    public boolean isGetFinal() {
        return srcPro == null || srcGet.isFinal();
    }

    public boolean isGetAbstract() {
        return srcPro != null && srcGet.isGetAbstract();
    }

    public boolean isGetPublic() {
        return srcPro != null ? srcGet.isGetPublic() : srcVar != null ? srcVar.isPublic() : true;
    }

    public boolean isGetPrivate() {
        return srcPro != null ? srcGet.isGetPrivate() : srcVar != null ? srcVar.isPrivate() : true;
    }

    public boolean hasSet() {
        return srcPro != null ? srcSet != null : srcVar != null;
    }

    public boolean isSetFinal() {
        return srcPro != null ? srcSet.isSetFinal() : false;
    }

    public boolean isSetAbstract() {
        return srcPro != null ? srcSet.isSetAbstract() : false;
    }

    public boolean isSetPublic() {
        return srcPro != null ? srcSet.isSetPublic() : srcVar != null ? srcVar.isPublic() : false;
    }

    public boolean isSetPrivate() {
        return srcPro != null ? srcSet.isSetPrivate() : srcVar != null ? srcVar.isPrivate() : false;
    }

    public boolean hasOwn() {
        return srcPro != null ? srcOwn != null : srcVar != null;
    }

    public boolean isOwnFinal() {
        return srcPro != null ? srcOwn.isOwnFinal() : false;
    }

    public boolean isOwnAbstract() {
        return srcPro != null ? srcOwn.isOwnAbstract() : false;
    }

    public boolean isOwnPublic() {
        return srcPro != null ? srcOwn.isOwnPublic() : srcVar != null ? srcVar.isPublic() : false;
    }

    public boolean isOwnPrivate() {
        return srcPro != null ? srcOwn.isOwnPrivate() : srcVar != null ? srcVar.isPrivate() : false;
    }

    public boolean isReadOnly(Stack stack) {
        return srcVar != null ? srcVar.isFinal() && !stack.isConstructor() : srcNum != null;
    }
}
