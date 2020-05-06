package logic.typdef;

import content.Key;
import content.Token;
import content.TypeToken;
import data.ContentFile;
import data.CppBuilder;
import logic.Generic;
import logic.GenericOwner;
import logic.Generics;
import logic.Pointer;
import logic.member.*;

import java.util.ArrayList;

public abstract class Type implements GenericOwner {

    public final ContentFile cFile;

    public Token nameToken;
    public Token contentToken;
    public Token pathToken;
    public String fileName;

    public Generics generics;
    ArrayList<TypeToken> parentTypeTokens = new ArrayList<>();
    ArrayList<Type> inheritanceTypes = new ArrayList<>();
    public Pointer parent;
    public ArrayList<Pointer> parents = new ArrayList<>();

    private boolean isPrivate, isPublic, isAbstract, isFinal, isStatic;

    public Type(ContentFile cFile, Key key, Token start, Token end) {
        this.cFile = cFile;

        int state = 0;
        Token next;
        Token token = start;
        while (token != end) {
            next = token.getNext();
            if (state == 0 && token.key == key) {
                state = 1;
            } else if (state == 0 && token.key.isAttribute) {
                if (token.key == Key.PUBLIC || token.key == Key.PRIVATE) {
                    if (isPublic || isPrivate) {
                        cFile.erro(token, "Repated acess modifier");
                    } else {
                        isPublic = (token.key == Key.PUBLIC);
                        isPrivate = (token.key == Key.PRIVATE);
                    }
                } else if (token.key == Key.ABSTRACT || token.key == Key.FINAL) {
                    if (!isClass()) {
                        cFile.erro(token, "Unexpected modifier");
                    } else if (isAbstract || isFinal) {
                        cFile.erro(token, "Repated inheritance modifier");
                    } else {
                        isAbstract = (token.key == Key.ABSTRACT);
                        isFinal = (token.key == Key.FINAL);
                    }
                } else if (token.key == Key.STATIC) {
                    if (!isStruct()) {
                        cFile.erro(token, "Unexpected modifier");
                    } else {
                        isStatic = true;
                    }
                }
            } else if (state == 1 && token.key == Key.WORD) {
                nameToken = token;
                state = 2;
            } else if (state == 2 && token.key == Key.GENERIC) {
                if (isEnum()) {
                    cFile.erro(token, "A enum cannot have genrics");
                } else {
                    generics = new Generics(this, token, true);
                }
                state = 3;
            } else if ((state == 2 || state == 3) && token.key == Key.COLON) {
                state = 4;
            } else if (state == 4 && token.key == Key.WORD) {
                if (next != null && (next.key == Key.GENERIC)) {
                    next = next.getNext();
                }
                while (next != null && next.key == Key.INDEX) {
                    next = next.getNext();
                }
                parentTypeTokens.add(new TypeToken(token, next));
                state = 5;
            } else if (state == 5 && token.key == Key.COMMA) {
                state = 4;
            } else if (token.key == Key.BRACE) {
                contentToken = token;
                state = 6;
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end && state != 6) {
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
        }

        if (isEnum()) {
            isStatic = true;
        }

        if (isEnum() || isStruct()) {
            isFinal = true;
        }
    }

    public void preload() {
        String pathName = cFile.namespace.name + "::" + nameToken;
        fileName = (isClass() ? "c_" : isStruct() ? "s_" : isEnum() ? "e_" : "i_")
                + pathName.replace("_", "__").replace("::", "_");
        pathToken = new Token(pathName);

        for (TypeToken parentTypeToken : parentTypeTokens) {
            inheritanceType(parentTypeToken.start, parentTypeToken.end);
        }

        if (generics != null) {
            generics.preload(this);
        }
    }

    public void load() {
        if (generics != null) {
            generics.load(this, null);
        }
    }

    public void cross() {

    }

    public void build(CppBuilder cBuilder) {
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public boolean isInternal() {
        return !isPrivate && !isPublic;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isClass() {
        return false;
    }

    public boolean isInterface() {
        return false;
    }

    public boolean isStruct() {
        return false;
    }

    public boolean isEnum() {
        return false;
    }

    public boolean isLangBase() {
        return false;
    }

    @Override
    public Pointer findGeneric(Token genericName) {
        if (generics != null) {
            return generics.findGeneric(genericName);
        }
        return null;
    }

    public boolean cyclicVerify(Type type) {
        if (type == this) return true;

        for (Type parent : inheritanceTypes) {
            if (parent.cyclicVerify(type)) {
                return true;
            }
        }
        return false;
    }

    public void inheritanceType(Token typeToken, Token end) {
        Type type = cFile.findType(typeToken);
        if (type != null) {
            inheritanceTypes.add(type);

            int state = 0;
            Token token = typeToken.getNext();
            while (token != end) {
                Token next = token.getNext();

                if (state == 0 && token.key == Key.GENERIC) {
                    int iState = 0;
                    Token iToken = token.getChild();
                    Token iEnd = token.getLastChild();
                    while (iToken != null && iToken != iEnd) {
                        Token iNext = iToken.getNext();
                        if (iState == 0 && iToken.key == Key.WORD) {
                            if (iNext != null && (iNext.key == Key.GENERIC)) {
                                iNext = iNext.getNext();
                            }
                            while (iNext != null && iNext.key == Key.INDEX) {
                                iNext = iNext.getNext();
                            }
                            inheritanceType(iToken, iNext);
                            iState = 1;
                        } else if (iState == 1 && iToken.key == Key.COMMA) {
                            iState = 0;
                        } else {
                            // no erros
                        }
                        iToken = iNext;
                    }
                    state = 1;

                } else if (token.key == Key.INDEX
                        && token.getChild() != null
                        && token.getLastChild() == token.getChild()) {
                    state = 1;

                } else {
                    // no erros
                }
                token = next;
            }
        }
    }

    public void add(Field field) {

    }

    public void add(Property prtoperty) {

    }

    public void add(Num num) {

    }

    public void add(Method method) {

    }

    public void add(Indexer indexer) {

    }

    public void add(Operator operator) {

    }

    public void add(Constructor constructor) {

    }

    public void add(Destructor destructor) {

    }

    public void add(TNative tnative) {

    }

    @Override
    public String toString() {
        return nameToken+"["+hashCode()+"]";
    }
}
