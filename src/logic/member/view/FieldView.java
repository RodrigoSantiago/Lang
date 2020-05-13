package logic.member.view;

import content.Token;
import logic.Pointer;
import logic.member.Property;
import logic.member.Variable;
import logic.member.Num;
import logic.typdef.Type;

public class FieldView {

    public Token nameToken;
    public Pointer pointer;
    public Pointer caller;

    public Variable srcVar;
    public Num srcNum;
    public Property srcPro;
    public int srcID;

    private boolean hasGetAbs, hasSetAbs, hasOwnAbs;
    private boolean hasGetImpl, hasSetImpl, hasOwnImpl;
    public int getAcess, setAcess, ownAcess;

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
        getAcess = !property.hasGet() || property.isGetPrivate() ? 0 : property.isGetPublic() ? 2 : 1;
        setAcess = !property.hasSet() || property.isSetPrivate() ? 0 : property.isSetPublic() ? 2 : 1;
        ownAcess = !property.hasOwn() || property.isOwnPrivate() ? 0 : property.isOwnPublic() ? 2 : 1;
    }

    public FieldView(Pointer caller, FieldView fieldView) {
        this.nameToken = fieldView.nameToken;
        this.srcVar = fieldView.srcVar;
        this.srcNum = fieldView.srcNum;
        this.srcPro = fieldView.srcPro;
        this.srcID = fieldView.srcID;
        this.caller = caller;
        if (Pointer.hasGeneric(fieldView.getPointer(), caller)) {
            pointer = Pointer.byGeneric(fieldView.getPointer(), caller);
        } else {
            pointer = fieldView.getPointer();
        }
        hasGetAbs = fieldView.hasGetAbs;
        hasSetAbs = fieldView.hasSetAbs;
        hasOwnAbs = fieldView.hasOwnAbs;
        hasGetImpl = fieldView.hasGetImpl;
        hasSetImpl = fieldView.hasSetImpl;
        hasOwnImpl = fieldView.hasOwnImpl;
        getAcess = fieldView.getAcess;
        setAcess = fieldView.setAcess;
        ownAcess = fieldView.ownAcess;
    }

    public Token getName() {
        return nameToken;
    }

    public Pointer getPointer() {
        return pointer;
    }

    public boolean isFrom(Type type) {
        return  (srcPro != null && srcPro.type == type) ||
                (srcVar != null && srcVar.type == type) || srcNum.type == type;
    }

    public boolean canShadow(FieldView other) {
        return isVariable() && other.isVariable();
    }

    public boolean canOverride(FieldView other) {
        return isProperty() && other.isProperty() && getPointer().equals(other.getPointer());
    }

    public boolean canAcessGet(Type type) {
        return (getAcess == 0 && srcPro.cFile == type.cFile) ||
                (getAcess == 1 && srcPro.cFile.library == type.cFile.library) || (getAcess == 2);
    }

    public boolean canAcessSet(Type type) {
        return (setAcess == 0 && srcPro.cFile == type.cFile) ||
                (setAcess == 1 && srcPro.cFile.library == type.cFile.library) || (setAcess == 2);
    }

    public boolean canAcessOwn(Type type) {
        return (ownAcess == 0 && srcPro.cFile == type.cFile) ||
                (ownAcess == 1 && srcPro.cFile.library == type.cFile.library) || (ownAcess == 2);
    }

    public void addOverriden(FieldView other) {
        if (!hasGet()) getAcess = other.getAcess;
        if (!hasGet() && other.hasGet()) {
            if (other.isGetAbstract()) {
                hasGetAbs = true;
            } else {
                hasGetImpl = true;
            }
        } else if (hasGet() && isGetAbstract() && other.hasGet() && !other.isGetAbstract()) {
            hasGetImpl = true;
        }

        if (!hasSet()) setAcess = other.setAcess;
        if (!hasSet() && other.hasSet()) {
            if (other.isSetAbstract()) {
                hasSetAbs = true;
            } else {
                hasSetImpl = true;
            }
        } else if (hasSet() && isSetAbstract() && other.hasSet() && !other.isSetAbstract()) {
            hasSetImpl = true;
        }

        if (!hasOwn()) ownAcess = other.ownAcess;
        if (!hasOwn() && other.hasOwn()) {
            if (other.isOwnAbstract()) {
                hasOwnAbs = true;
            } else {
                hasOwnImpl = true;
            }
        } else if (hasOwn() && isOwnAbstract() && other.hasOwn() && !other.isOwnAbstract()) {
            hasOwnImpl = true;
        }
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
        return srcPro != null ? srcPro.hasGet() || hasGetAbs || hasGetImpl : true;
    }

    public boolean isGetFinal() {
        return srcPro != null ? srcPro.isGetFinal() : false;
    }

    public boolean isGetAbstract() {
        return srcPro != null ? (!hasGetImpl && (hasGetAbs || srcPro.isGetAbstract())) : false;
    }

    public boolean isGetPublic() {
        return srcPro != null ? srcPro.isGetPublic() || getAcess == 2 : srcVar != null ? srcVar.isPublic() : true;
    }

    public boolean isGetPrivate() {
        return srcPro != null ? getAcess == 0 : srcVar != null ? srcVar.isPrivate() : false;
    }

    public boolean hasSet() {
        return srcPro != null ? (srcPro.hasSet() || hasSetAbs || hasSetImpl) : srcVar != null;
    }

    public boolean isSetFinal() {
        return srcPro != null ? srcPro.isSetFinal() : false;
    }

    public boolean isSetAbstract() {
        return srcPro != null ? (!hasSetImpl && (hasSetAbs || srcPro.isSetAbstract())) : false;
    }

    public boolean isSetPublic() {
        return srcPro != null ? srcPro.isSetPublic() || setAcess == 2 : srcVar != null ? srcVar.isPublic() : false;
    }

    public boolean isSetPrivate() {
        return srcPro != null ? setAcess == 0 : srcVar != null ? srcVar.isPrivate() : false;
    }

    public boolean hasOwn() {
        return srcPro != null ? (srcPro.hasOwn() || hasOwnAbs || hasOwnImpl) : srcVar != null;
    }

    public boolean isOwnFinal() {
        return srcPro != null ? srcPro.isOwnFinal() : false;
    }

    public boolean isOwnAbstract() {
        return srcPro != null ? (!hasOwnImpl && (hasOwnAbs || srcPro.isOwnAbstract())) : false;
    }

    public boolean isOwnPublic() {
        return srcPro != null ? srcPro.isOwnPublic() || ownAcess == 2 : srcVar != null ? srcVar.isPublic() : false;
    }

    public boolean isOwnPrivate() {
        return srcPro != null ? ownAcess == 0 : srcVar != null ? srcVar.isPrivate() : false;
    }
}
