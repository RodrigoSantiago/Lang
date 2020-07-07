package logic.member;

import content.Key;
import content.Token;
import content.TokenGroup;
import builder.CppBuilder;
import logic.Pointer;
import logic.member.view.IndexerView;
import logic.params.Parameters;
import logic.stack.Stack;
import logic.typdef.Type;

public class Indexer extends Member {

    private TokenGroup typeToken;
    private Pointer typePtr;

    private Parameters params;
    private TokenGroup contentToken;
    private Token getContentToken, setContentToken, ownContentToken;
    private boolean hasGet, isGetFinal, isGetAbstract, isGetPublic, isGetPrivate;
    private boolean hasOwn, isOwnFinal, isOwnAbstract, isOwnPublic, isOwnPrivate;
    private boolean hasSet, isSetFinal, isSetAbstract, isSetPublic, isSetPrivate;
    private boolean isGetOwn;

    private Stack stackGet, stackSet, stackOwn;

    public Indexer(Type type, Token start, Token end) {
        super(type, type.cFile, start);

        int state = 0;
        Token next;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key.isAttribute) {
                readModifier(cFile, token, true, true, type.isAbsAllowed(), type.isFinalAllowed(), false, true, false);
            } else if (state == 0 && token.key == Key.WORD) {
                typeToken = new TokenGroup(token, next = TokenGroup.nextType(next, end));
                state = 1;
            } else if (state == 1 && token.key == Key.THIS) {
                this.token = token;
                state = 2;
            } else if (state == 2 && token.key == Key.INDEX && token.getChild() != null) {
                params = new Parameters(cFile, token);
                state = 3;
            } else if (state == 3 && token.key == Key.BRACE) {
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
                state = 4;
            } else if (state == 3 && token.key == Key.SEMICOLON) {
                contentToken = new TokenGroup(token, next);
                state = 4;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state != 4) {
                cFile.erro(token, "Unexpected end of tokens", this);
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
        Token end = group.end;
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

    @Override
    public boolean load() {
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
                    "A Indexer should have at least one member", this);
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
            typePtr = cFile.getPointer(typeToken.start, typeToken.end, null, type, isLet());
            if (typePtr == null) {
                typePtr = cFile.langObjectPtr(isLet());
            }

            if (params != null) {
                params.load(type);

                return hasGet || hasSet || hasOwn;
            }
        }
        return false;
    }

    @Override
    public void make() {
        if (hasGet && getContentToken != null && getContentToken.key == Key.BRACE && getContentToken.getChild() != null) {
            stackGet = new Stack(cFile, token, type.self, isGetOwn ? typePtr : typePtr.toLet(),
                    isStatic() ? null : type, false, isStatic(), false, getParams(), null);

            stackGet.read(getContentToken.getChild(), getContentToken.getLastChild(), true);
            stackGet.load();
        }
        if (hasOwn && ownContentToken != null && ownContentToken.key == Key.BRACE && ownContentToken.getChild() != null) {
            stackOwn = new Stack(cFile, token, type.self, typePtr,
                    isStatic() ? null : type, false, isStatic(), false, getParams(), null);

            stackOwn.read(ownContentToken.getChild(), ownContentToken.getLastChild(), true);
            stackOwn.load();
        }
        if (hasSet && setContentToken != null && setContentToken.key == Key.BRACE && setContentToken.getChild() != null) {
            stackSet = new Stack(cFile, token, type.self, Pointer.voidPointer,
                    isStatic() ? null : type, false, isStatic(), false, getParams(), typePtr);

            stackSet.read(setContentToken.getChild(), setContentToken.getLastChild(), true);
            stackSet.load();
        }
    }

    @Override
    public void build(CppBuilder cBuilder) {

        if (hasGet()) {
            cBuilder.toHeader();
            cBuilder.idt(1);
            if (!isGetFinal() && type.isPointer()) {
                cBuilder.add("virtual ");
            }
            Pointer getPtr = typePtr.toLet();
            cBuilder.add(getPtr)
                    .add(" get").add("(").add(params).add(")")
                    .add(isGetAbstract() ? " = 0;" : ";").ln();

            if (!isGetAbstract()) {
                cBuilder.toSource(type.template != null);
                cBuilder.add(type.template)
                        .add(getPtr)
                        .add(" ").path(type.self, false).add("::get").add("(").add(params).add(") ").in(1)
                        .add(stackGet == null ? stackOwn : stackGet, 1)
                        .out().ln()
                        .ln();
            }
        }

        if (hasOwn()) {
            cBuilder.toHeader();
            cBuilder.idt(1);
            if (!isOwnFinal() && type.isPointer()) {
                cBuilder.add("virtual ");
            }
            cBuilder.add(typePtr)
                    .add(" own").add("(").add(params).add(")")
                    .add(isGetAbstract() ? " = 0;" : ";").ln();

            if (!isOwnAbstract()) {
                cBuilder.toSource(type.template != null);
                cBuilder.add(type.template)
                        .add(typePtr)
                        .add(" ").path(type.self, false).add("::own").add("(").add(params).add(") ").in(1)
                        .add(stackOwn == null ? stackGet : stackOwn, 1)
                        .out().ln()
                        .ln();
            }
        }

        if (hasSet()) {
            cBuilder.toHeader();
            cBuilder.idt(1);
            if (!isSetFinal() && type.isPointer()) {
                cBuilder.add("virtual ");
            }
            cBuilder.add("void set").add("(").add(params).add(params.isEmpty() ? "" : ", ").add(typePtr).add(" v_value)")
                    .add(isSetAbstract() ? " = 0;" : ";").ln();

            if (!isSetAbstract()) {
                cBuilder.toSource(type.template != null);
                cBuilder.add(type.template)
                        .add("void ").path(type.self, false).add("::set")
                        .add("(").add(params).add(params.isEmpty() ? "" : ", ").add(typePtr).add(" v_value) ").in(1)
                        .add(stackSet, 1)
                        .out().ln()
                        .ln();
            }
        }
    }

    public void buildImplGet(CppBuilder cBuilder, Pointer self, IndexerView iw) {
        Pointer getPtr = typePtr.toLet();

        cBuilder.toHeader();
        cBuilder.idt(1);
        cBuilder.add("virtual ");
        cBuilder.add(getPtr)
                .add(" get").add("(").add(params).add(");").ln();

        cBuilder.toSource(type.template != null);
        cBuilder.add(type.template)
                .add(getPtr)
                .add(" ").path(self, false).add("::get").add("(").add(params).add(") {").ln()
                .idt(1).add("return ").path(self.type.parent, false).add("::get(").args(iw.getParams()).add(");").ln()
                .add("}").ln()
                .ln();
    }

    public void buildImplOwn(CppBuilder cBuilder, Pointer self, IndexerView iw) {
        cBuilder.toHeader();
        cBuilder.idt(1);
        cBuilder.add("virtual ");
        cBuilder.add(typePtr)
                .add(" own").add("(").add(params).add(");").ln();

        cBuilder.toSource(type.template != null);
        cBuilder.add(type.template)
                .add(typePtr)
                .add(" ").path(self, false).add("::own").add("(").add(params).add(") {").ln()
                .idt(1).add("return ").path(self.type.parent, false).add("::own(").args(iw.getParams()).add(");").ln()
                .add("}").ln()
                .ln();
    }

    public void buildImplSet(CppBuilder cBuilder, Pointer self, IndexerView iw) {
        cBuilder.toHeader();
        cBuilder.idt(1);
        cBuilder.add("virtual void set(").add(params).add(params.isEmpty() ? "" : ", ").add(typePtr).add(" v_value);").ln();

        cBuilder.toSource(type.template != null);
        cBuilder.add(type.template)
                .add("void ").path(self, false).add("::set")
                .add("(").add(params).add(params.isEmpty() ? "" : ", ").add(typePtr).add(" v_value) {").ln()
                .idt(1).path(self.type.parent, false).add("::set(").args(iw.getParams(), true).add(");").ln()
                .add("}").ln()
                .ln();
    }

    public void toAbstract() {
        isAbstract = true;
        isGetAbstract = true;
        isSetAbstract = true;
        isOwnAbstract = true;
    }

    public Parameters getParams() {
        return params;
    }

    public Pointer getTypePtr() {
        return typePtr;
    }

    public Stack getStackGet() {
        return stackGet;
    }

    public Stack getStackSet() {
        return stackSet;
    }

    public Stack getStackOwn() {
        return stackOwn;
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
}
