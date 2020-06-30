package logic.member;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.CppBuilder;
import logic.Pointer;
import logic.member.view.FieldView;
import logic.stack.Stack;
import logic.typdef.Type;

public class Property extends Member {

    private Token nameToken;
    private TokenGroup typeToken;
    private Pointer typePtr;

    private TokenGroup contentToken;
    private TokenGroup initToken;
    private Token getContentToken, setContentToken, ownContentToken;

    private boolean hasGet, isGetFinal, isGetAbstract, isGetPublic, isGetPrivate;
    private boolean hasOwn, isOwnFinal, isOwnAbstract, isOwnPublic, isOwnPrivate;
    private boolean hasSet, isSetFinal, isSetAbstract, isSetPublic, isSetPrivate;
    private boolean isGetOwn;

    public Property(Type type, Token start, Token end) {
        super(type);

        int state = 0;
        Token next;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key.isAttribute) {
                readModifier(cFile, token, true, true, type.isAbsAllowed(), type.isFinalAllowed(), true, true, false);
            } else if (state == 0 && token.key == Key.WORD) {
                typeToken = new TokenGroup(token, next = TokenGroup.nextType(next, end));
                state = 1;
            } else if (state == 1 && token.key == Key.WORD) {
                if (token.isComplex()) cFile.erro(token, "Complex names are not allowed", this);
                this.token = token;
                nameToken = token;
                state = 2;
            } else if (state == 2 && token.key == Key.BRACE) {
                if (token.getChild() == null) {
                    if (next != end) {
                        contentToken = new TokenGroup(next, end);
                        next = end;
                    }
                    cFile.erro(token, "Brace closure expected", this);
                } else {
                    if (token.isOpen()) cFile.erro(token, "Brace closure expected", this);
                    contentToken = new TokenGroup(token.getChild(), token.getLastChild());
                }
                if (contentToken != null) readBlocks(contentToken);
                state = 3;
            } else if (state == 3 && token.key == Key.SETVAL) {
                Token initStart = next;
                while (next != end && next.key != Key.SEMICOLON) {
                    next = next.getNext();
                }

                if (initStart == next) {
                    cFile.erro(token, "Expression expected", this);
                } else {
                    initToken = new TokenGroup(initStart, next);
                }
                state = 4;
            } else if (state == 4 && token.key == Key.SEMICOLON) {
                state = 5;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state < 3) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            if (next == end && state == 4) {
                cFile.erro(token, "Semicolon expected", this);
            }
            token = next;
        }

    }

    private void readBlocks(TokenGroup group) {
        boolean isPrivate = false, isPublic = false, isAbstract = false, isFinal = false;
        boolean getOwn = false;
        int t = 0;

        int state = 0;
        Token next;
        Token end =  group.end;
        Token token = group.start;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key.isAttribute) {
                if (token.key == Key.PUBLIC || token.key == Key.PRIVATE) {
                    if (isPublic || isPrivate) {
                        cFile.erro(token, "Repeated acess modifier", this);
                    } else {
                        isPublic = (token.key == Key.PUBLIC);
                        isPrivate = (token.key == Key.PRIVATE);
                    }
                } else if ((type.isFinalAllowed() && token.key == Key.FINAL) ||
                        (type.isAbsAllowed() && token.key == Key.ABSTRACT)) {
                    if (isFinal || isAbstract) {
                        cFile.erro(token, "Repeated modifier", this);
                    } else {
                        isFinal = (token.key == Key.FINAL);
                        isAbstract = (token.key == Key.ABSTRACT);
                    }
                } else {
                    cFile.erro(token, "Unexpected modifier", this);
                }
            } else if (state == 0 && token.equals("get")) {
                t = 0;
                state = 1;
            } else if (state == 0 && token.equals("set")) {
                t = 1;
                state = 1;
            } else if (state == 0 && token.equals("own")) {
                t = 2;
                state = 1;
            } else if (state == 1 && token.key == Key.COMMA) {
                state = 2;
            } else if (state == 2 && token.equals("get")) {
                if (t == 2) {
                    if (hasGet) {
                        cFile.erro(token, "Repeated get", this);
                    } else {
                        getOwn = true;
                    }
                } else {
                    cFile.erro(token, "Unexpected token", this);
                }
                state = 3;
            } else if (state == 2 && token.equals("own")) {
                if (t == 0) {
                    if (hasOwn) {
                        cFile.erro(token, "Repeated own", this);
                    } else {
                        getOwn = true;
                    }
                } else {
                    cFile.erro(token, "Unexpected token", this);
                }
                state = 3;
            } else if ((state == 1 || state == 3) && (token.key == Key.BRACE || token.key == Key.SEMICOLON)) {
                if (t == 0) {
                    if (hasGet || isGetOwn) cFile.erro(token, "Repeated get", this);
                    hasGet = true;
                    getContentToken = token;
                    isGetPublic = (this.isPublic && !isPrivate) || isPublic;
                    isGetPrivate = (this.isPrivate && !isPublic) || isPrivate;
                    isGetFinal = (this.isFinal && !isAbstract) || isFinal;
                    isGetAbstract = (this.isAbstract && !isFinal) || isAbstract;
                } else if (t == 1) {
                    if (hasSet) cFile.erro(token, "Repeated set", this);
                    hasSet = true;
                    setContentToken = token;
                    isSetPublic = (this.isPublic && !isPrivate) || isPublic;
                    isSetPrivate = (this.isPrivate && !isPublic) || isPrivate;
                    isSetFinal = (this.isFinal && !isAbstract) || isFinal;
                    isSetAbstract = (this.isAbstract && !isFinal) || isAbstract;
                } else {
                    if (hasOwn || isGetOwn) cFile.erro(token, "Repeated own", this);
                    hasOwn = true;
                    ownContentToken = token;
                    isOwnPublic = (this.isPublic && !isPrivate) || isPublic;
                    isOwnPrivate = (this.isPrivate && !isPublic) || isPrivate;
                    isOwnFinal = (this.isFinal && !isAbstract) || isFinal;
                    isOwnAbstract = (this.isAbstract && !isFinal) || isAbstract;
                }
                isGetOwn = isGetOwn || getOwn;
                t = 0;
                isPublic = isPrivate = isAbstract = isFinal = getOwn = false;
                state = 0;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && (state != 0 || (!hasGet && !hasSet && !hasOwn))) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }
    }

    public void toAbstract() {
        isAbstract = true;
        isGetAbstract = true;
        isSetAbstract = true;
        isOwnAbstract = true;
    }

    @Override
    public boolean load() {
        if ((!hasSet || isSetAbstract) && initToken != null) {
            cFile.erro(initToken, "Init token not allowed without a implemented SET", this);
        }

        if (hasGet) {
            if ((isGetPrivate && !isOwnPrivate) || (!isGetPublic && isOwnPublic)) {
                cFile.erro(token, "The OWN acess should be equal or more restrict than GET", this);
            }
        } else {
            cFile.erro(token, "Missing GET Statement", this);
        }
        if (hasGet && hasOwn) {
            if ((isGetPrivate && !isSetPrivate) || (!isGetPublic && isSetPublic)) {
                cFile.erro(token, "The SET acess should be equal or more restrict than GET", this);
            }
        }
        if (hasGet && hasOwn && !isGetOwn && hasSet) {
            if ((isOwnPrivate && !isSetPrivate) || (!isOwnPublic && isSetPublic)) {
                cFile.erro(token, "The SET acess should be equal or more restrict than OWN", this);
            }
        }

        if ((contentToken != null && contentToken.start.key == Key.SEMICOLON) || !hasGet && !hasSet && !hasOwn) {
            cFile.erro(contentToken == null ? token : contentToken.start,
                    "A Property should have at least one member", this);
        }
        if (hasGet && !isGetAbstract && (getContentToken == null || getContentToken.key == Key.SEMICOLON) &&
                (!isGetOwn || (ownContentToken == null || ownContentToken.key == Key.SEMICOLON))) {
            cFile.erro(getContentToken == null ? token : getContentToken,
                    "A Non-Abstract Get Member should implement", this);
        }
        if (hasOwn && !isOwnAbstract && (ownContentToken == null || ownContentToken.key == Key.SEMICOLON) &&
                (!isGetOwn || (getContentToken == null || getContentToken.key == Key.SEMICOLON))) {
            cFile.erro(ownContentToken == null ? token : ownContentToken,
                    "A Non-Abstract Own Member should implement", this);
        }
        if (hasSet && !isSetAbstract && (setContentToken == null || setContentToken.key == Key.SEMICOLON)) {
            cFile.erro(setContentToken == null ? token : setContentToken,
                    "A Non-Abstract Set Member should implement", this);
        }
        if (typeToken != null) {
            typePtr = cFile.getPointer(typeToken.start, typeToken.end, null, isStatic() ? null : type, isLet());
            if (typePtr == null) {
                typePtr = cFile.langObjectPtr(isLet());
            }

            return hasGet || hasSet || hasOwn;
        }
        return false;
    }

    public void make() {
        if (hasGet && getContentToken != null && getContentToken.key == Key.BRACE && getContentToken.getChild() != null) {
            Stack stack = new Stack(cFile, token, type.self, isGetOwn ? typePtr : typePtr.toLet(),
                    isStatic() ? null : type, false, isStatic(), true);

            stack.read(getContentToken.getChild(), getContentToken.getLastChild(), true);
            stack.load();
        }
        if (hasOwn && ownContentToken != null && ownContentToken.key == Key.BRACE && ownContentToken.getChild() != null) {
            Stack stack = new Stack(cFile, token, type.self, typePtr,
                    isStatic() ? null : type, false, isStatic(), true);

            stack.read(ownContentToken.getChild(), ownContentToken.getLastChild(), true);
            stack.load();
        }
        if (hasSet && setContentToken != null && setContentToken.key == Key.BRACE && setContentToken.getChild() != null) {
            Stack stack = new Stack(cFile, token, type.self, Pointer.voidPointer,
                    isStatic() ? null : type, false, isStatic(), true);

            stack.read(setContentToken.getChild(), setContentToken.getLastChild(), true);
            stack.value(typePtr);
            stack.load();
        }
        if (initToken != null && initToken.start != null && initToken.start != initToken.end) {
            Stack stack = new Stack(cFile, token, type.self, typePtr, isStatic() ? null : type, true, isStatic(), true);
            stack.read(initToken.start, initToken.end, true);
            stack.load();
        }
    }

    public void build(CppBuilder cBuilder) {

        if (hasGet()) {
            cBuilder.toHeader();
            cBuilder.idt(1);
            if (!isGetFinal() && !isStatic() && type.isPointer()) {
                cBuilder.add("virtual ");
            } else if (isStatic()) {
                cBuilder.add("static ");
            }
            Pointer getPtr = typePtr.toLet();
            cBuilder.add(getPtr)
                    .add(" get_").add(nameToken).add("()")
                    .add(isGetAbstract() ? " = 0;" : ";").ln();

            if (!isGetAbstract()) {
                cBuilder.toSource(type.template != null);
                if (!isStatic()) {
                    cBuilder.add(type.template);
                }
                cBuilder.add(getPtr)
                        .add(" ").path(type.self, isStatic()).add("::get_").add(nameToken).add("() {").ln()
                        .add("}").ln()
                        .ln();
            }


        }

        if (hasOwn()) {
            cBuilder.toHeader();
            cBuilder.idt(1);
            if (!isOwnFinal() && !isStatic() && type.isPointer()) {
                cBuilder.add("virtual ");
            } else if (isStatic()) {
                cBuilder.add("static ");
            }
            cBuilder.add(typePtr)
                    .add(" own_").add(nameToken).add("()")
                    .add(isOwnAbstract() ? " = 0;" : ";").ln();

            if (!isOwnAbstract()) {
                cBuilder.toSource(type.template != null);
                if (!isStatic()) {
                    cBuilder.add(type.template);
                }
                cBuilder.add(typePtr)
                        .add(" ").path(type.self, isStatic()).add("::own_").add(nameToken).add("() {").ln()
                        .add("}").ln()
                        .ln();
            }
        }

        if (hasSet()) {
            cBuilder.toHeader();
            cBuilder.idt(1);
            if (!isSetFinal() && !isStatic() && type.isPointer()) {
                cBuilder.add("virtual ");
            } else if (isStatic()) {
                cBuilder.add("static ");
            }
            cBuilder.add("void set_").add(nameToken).add("(").add(typePtr).add(" v_value)")
                    .add(isSetAbstract() ? " = 0;" : ";").ln();

            if (!isSetAbstract()) {
                cBuilder.toSource(type.template != null);
                if (!isStatic()) {
                    cBuilder.add(type.template);
                }
                cBuilder.add("void ").path(type.self, isStatic()).add("::set_").add(nameToken)
                        .add("(").add(typePtr).add(" v_value) {").ln()
                        .add("}").ln()
                        .ln();
            }
        }
    }

    public void buildImplGet(CppBuilder cBuilder, Pointer self) {
        Pointer getPtr = typePtr.toLet();

        cBuilder.toHeader();
        cBuilder.idt(1);
        cBuilder.add("virtual ").add(getPtr).add(" get_").add(nameToken).add("();").ln();

        cBuilder.toSource(type.template != null);
        cBuilder.add(type.template)
                .add(getPtr)
                .add(" ").path(self, false).add("::get_").add(nameToken).add("() {").ln()
                .idt(1).add("return ").path(self.type.parent, false).add("::get_").add(nameToken).add("();").ln()
                .add("}").ln()
                .ln();
    }

    public void buildImplOwn(CppBuilder cBuilder, Pointer self) {
        cBuilder.toHeader();
        cBuilder.idt(1);
        cBuilder.add("virtual ");
        cBuilder.add(typePtr)
                .add(" own_").add(nameToken).add("();").ln();

        cBuilder.toSource(type.template != null);
        cBuilder.add(type.template)
                .add(typePtr)
                .add(" ").path(self, false).add("::own_").add(nameToken).add("() {").ln()
                .idt(1).add("return ").path(self.type.parent, false).add("::own_").add(nameToken).add("();").ln()
                .add("}").ln()
                .ln();
    }

    public void buildImplSet(CppBuilder cBuilder, Pointer self) {
        cBuilder.toHeader();
        cBuilder.idt(1);
        cBuilder.add("virtual void set_").add(nameToken).add("(").add(typePtr).add(" v_value);").ln();

        cBuilder.toSource(type.template != null);
        cBuilder.add(type.template)
                .add("void ").path(self, false).add("::set_").add(nameToken)
                .add("(").add(typePtr).add(" v_value) {").ln()
                .idt(1).path(self.type.parent, false).add("::set_").add(nameToken).add("(v_value);").ln()
                .add("}").ln()
                .ln();
    }

    public FieldView getField() {
        return new FieldView(nameToken, typePtr, this);
    }

    public Token getName() {
        return nameToken;
    }

    public Pointer getTypePtr() {
        return typePtr;
    }

    public boolean hasGet() {
        return hasGet || isGetOwn;
    }

    public boolean isGetFinal() {
        return isGetFinal;
    }

    public boolean isGetAbstract() {
        return isGetAbstract;
    }

    public boolean isGetPublic() {
        return isGetPublic;
    }

    public boolean isGetPrivate() {
        return isGetPrivate;
    }

    public boolean hasSet() {
        return hasSet;
    }

    public boolean isSetFinal() {
        return isSetFinal;
    }

    public boolean isSetAbstract() {
        return isSetAbstract;
    }

    public boolean isSetPublic() {
        return isSetPublic;
    }

    public boolean isSetPrivate() {
        return isSetPrivate;
    }

    public boolean hasOwn() {
        return hasOwn || isGetOwn;
    }

    public boolean isOwnFinal() {
        return isOwnFinal;
    }

    public boolean isOwnAbstract() {
        return isOwnAbstract;
    }

    public boolean isOwnPublic() {
        return isOwnPublic;
    }

    public boolean isOwnPrivate() {
        return isOwnPrivate;
    }

    @Override
    public String toString() {
        return nameToken+" : "+ typeToken+" {} ";
    }


}
