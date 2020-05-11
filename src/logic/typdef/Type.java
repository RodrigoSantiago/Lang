package logic.typdef;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.ContentFile;
import data.CppBuilder;
import logic.GenericOwner;
import logic.templates.Template;
import logic.Pointer;
import logic.member.*;
import logic.member.view.FieldView;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class Type implements GenericOwner {

    public final ContentFile cFile;

    public Token nameToken;
    public Token pathToken;
    public String fileName;

    public Template template;
    public Token contentToken;

    public Pointer self;
    public Pointer parent;
    public ArrayList<Pointer> parents = new ArrayList<>();
    public ArrayList<TokenGroup> parentTokens = new ArrayList<>();

    public HashMap<Token, FieldView> fields = new HashMap<>();

    public ArrayList<Property> properties = new ArrayList<>();
    public ArrayList<Variable> variables = new ArrayList<>();
    public ArrayList<Num> nums = new ArrayList<>();

    public ArrayList<Method> methods = new ArrayList<>();
    public ArrayList<Indexer> indexers = new ArrayList<>();
    public ArrayList<Operator> operators = new ArrayList<>();
    public ArrayList<Constructor> constructors = new ArrayList<>();
    public ArrayList<Destructor> destructors = new ArrayList<>(1);
    public ArrayList<MemberNative> memberNatives = new ArrayList<>();

    private ArrayList<Type> inheritanceTypes = new ArrayList<>();
    private boolean isPrivate, isPublic, isAbstract, isFinal, isStatic;

    public Type(ContentFile cFile, Key key, Token start, Token end) {
        this.cFile = cFile;

        int state = 0;
        Token next;
        Token last = start;
        Token token = start;
        while (token != end) {
            next = token.getNext();

            if (state == 0 && token.key == key) {
                state = 1;
            } else if (state == 0 && token.key.isAttribute) {
                if (token.key == Key.PUBLIC || token.key == Key.PRIVATE) {
                    if (isPublic || isPrivate) {
                        cFile.erro(token, "Repeated acess modifier");
                    } else {
                        isPublic = (token.key == Key.PUBLIC);
                        isPrivate = (token.key == Key.PRIVATE);
                    }
                } else if (token.key == Key.ABSTRACT || token.key == Key.FINAL) {
                    if (!isClass()) {
                        cFile.erro(token, "Unexpected modifier");
                    } else if (isAbstract || isFinal) {
                        cFile.erro(token, "Repeated inheritance modifier");
                    } else {
                        isAbstract = (token.key == Key.ABSTRACT);
                        isFinal = (token.key == Key.FINAL);
                    }
                } else if (token.key == Key.STATIC) {
                    if (!isStruct()) {
                        cFile.erro(token, "Unexpected modifier");
                    } else if (isStatic) {
                        cFile.erro(token, "Repeated modifier");
                    } else {
                        isStatic = true;
                    }
                } else {
                    cFile.erro(token, "Unexpected modifier");
                }
            } else if (state == 1 && token.key == Key.WORD) {
                nameToken = token;
                state = 2;
            } else if (state == 2 && token.key == Key.GENERIC) {
                if (isEnum()) {
                    cFile.erro(token, "A enum cannot have genrics");
                } else {
                    template = new Template(cFile, token);
                }
                state = 3;
            } else if ((state == 2 || state == 3) && token.key == Key.COLON) {
                state = 4;
            } else if (state == 4 && token.key == Key.WORD) {
                parentTokens.add(new TokenGroup(token, next = TokenGroup.nextType(next, end)));
                state = 5;
            } else if (state == 5 && token.key == Key.COMMA) {
                state = 4;
            } else if (token.key == Key.BRACE) {
                contentToken = token;
                state = 6;
            } else {
                cFile.erro(token, "Unexpected token");
            }

            last = token;
            token = next;
        }

        if (state != 6) {
            cFile.erro(last, "Unexpected end of tokens");
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

        for (TokenGroup parentTypeToken : parentTokens) {
            inheritanceType(parentTypeToken.start, parentTypeToken.end);
        }

        if (template != null) {
            template.preload(this);
        }
    }

    public void load() {
        if (template != null) {
            template.load(this, null);
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
    public Pointer findGeneric(Token genericToken) {
        if (template != null) {
            return template.findGeneric(genericToken);
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

    public void add(Variable variable) {
        if (variable.load()) {
            if (isInterface() && !variable.isStatic()) {
                cFile.erro(variable.token, "Instance variables not allowed");
            } else {
                for (FieldView field : variable.getFields()) {
                    if (fields.containsKey(field.nameToken)) {
                        cFile.erro(field.nameToken, "Repeated field name");
                    } else {
                        fields.put(field.nameToken, field);
                    }
                }
                variables.add(variable);
            }
        }
    }

    public void add(Property property) {
        if (property.load()) {
            FieldView field = property.getField();
            if (fields.containsKey(field.nameToken)) {
                cFile.erro(field.nameToken, "Repeated field name");
            } else {
                fields.put(field.nameToken, field);
            }
            properties.add(property);
        }
    }

    public void add(Num num) {
        if (num.load()) {
            if (!isEnum()) {
                cFile.erro(num.token, "Unexpected enumeration");
            } else {
                for (FieldView field : num.getFields()) {
                    if (fields.containsKey(field.nameToken)) {
                        cFile.erro(field.nameToken, "Repeated field name");
                    } else {
                        fields.put(field.nameToken, field);
                    }
                }
                nums.add(num);
            }
        }
    }

    public void add(Method method) {
        if (method.load()) {
            methods.add(method);
        }
    }

    public void add(Indexer indexer) {
        if (indexer.load()) {
            indexers.add(indexer);
        }
    }

    public void add(Operator operator) {
        if (operator.load()) {
            if (!isStruct()) {
                cFile.erro(operator.token, "Operators not allowed");
            } else {
                operators.add(operator);
            }
        }
    }

    public void add(Constructor constructor) {
        if (constructor.load()) {
            if (isInterface() && !constructor.isStatic()) {
                cFile.erro(constructor.token, "Instance constructors not allowed");
            } else {
                constructors.add(constructor);
            }
        }
    }

    public void add(Destructor destructor) {
        if (destructor.load()) {
            if (destructors.size() > 0) {
                cFile.erro(destructor.token, "Repeated destructor");
            } else if (isEnum() || isInterface()) {
                cFile.erro(destructor.token, "Destructors not allowed");
            } else {
                destructors.add(destructor);
            }
        }
    }

    public void add(MemberNative memberNative) {
        if (memberNative.load()) {
            memberNatives.add(memberNative);
        }
    }

    public FieldView getField(Token nameToken) {
        FieldView field = fields.get(nameToken);
        if (field == null && (parent != null && parent.type != null)) {
            return parent.type.getField(nameToken);
        } else {
            return null;
        }
    }

    public int getMethodsCount() {
        return methods.size() + (parent != null && parent.type != null ? parent.type.getMethodsCount() : 0);
    }

    public Method getMethod(int index) {
        if (index < methods.size()) {
            return methods.get(index);
        } else {
            return parent.type.getMethod(index - methods.size());
        }
    }

    public int getIndexersCount() {
        return indexers.size() + (parent != null && parent.type != null ? parent.type.getIndexersCount() : 0);
    }

    public Indexer getIndexer(int index) {
        if (index < indexers.size()) {
            return indexers.get(index);
        } else {
            return parent.type.getIndexer(index - indexers.size());
        }
    }

    public int getOperatorsCount() {
        return methods.size() + (parent != null && parent.type != null ? parent.type.getOperatorsCount() : 0);
    }

    public Operator getOperator(int index) {
        if (index < operators.size()) {
            return operators.get(index);
        } else {
            return parent.type.getOperator(index - operators.size());
        }
    }

    @Override
    public String toString() {
        return nameToken+"["+hashCode()+"]";
    }
}
