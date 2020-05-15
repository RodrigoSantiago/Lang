package logic.typdef;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.ContentFile;
import data.CppBuilder;
import logic.GenericOwner;
import logic.ViewList;
import logic.member.view.IndexerView;
import logic.member.view.MethodView;
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
    public Token staticPathToken;
    public String fileName;

    public Template template;
    public Token contentToken;

    public Pointer self;
    public Pointer parent;

    ArrayList<Token> parentTokens = new ArrayList<>();

    ArrayList<Pointer> parents = new ArrayList<>();
    ArrayList<TokenGroup> parentTypeTokens = new ArrayList<>();

    private ArrayList<Property> properties = new ArrayList<>();
    private ArrayList<Variable> variables = new ArrayList<>();
    private ArrayList<Num> nums = new ArrayList<>();

    private ArrayList<Method> methods = new ArrayList<>();
    private ArrayList<Indexer> indexers = new ArrayList<>();
    private ArrayList<Operator> operators = new ArrayList<>();
    private ArrayList<Constructor> constructors = new ArrayList<>();
    private ArrayList<Destructor> destructors = new ArrayList<>(1);
    private ArrayList<MemberNative> memberNatives = new ArrayList<>();
    private Constructor staticConstructor;

    private ArrayList<Type> inheritanceTypes = new ArrayList<>();
    private boolean isPrivate, isPublic, isAbstract, isFinal, isStatic;
    private boolean isCrossed, isBase;

    private HashMap<Token, FieldView> fields = new HashMap<>();
    private ViewList<MethodView> methodView = new ViewList<>();
    private ArrayList<IndexerView> indexerView = new ArrayList<>();

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
                if (token.startsWith('_')) {
                    cFile.erro(nameToken, "Type names cannot start with underline (_)");
                }
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
                parentTypeTokens.add(new TokenGroup(token, next = TokenGroup.nextType(next, end)));
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

        if (isInterface()) {
            isAbstract = true;
        }
    }

    public void preload() {
        String pathName = (cFile.namespace.name + "::" + nameToken).replace("_", "__").replace("::", "_");
        fileName = (isClass() ? "c_" : isStruct() ? "s_" : isEnum() ? "e_" : "i_") + pathName;
        pathToken = new Token(pathName);
        staticPathToken = new Token("_"+pathName);

        if (cFile.library == cFile.library.getCompiler().getLangLibrary()) {
            isBase = nameToken.equals("bool")
                    || nameToken.equals("byte")
                    || nameToken.equals("short")
                    || nameToken.equals("int")
                    || nameToken.equals("long")
                    || nameToken.equals("float")
                    || nameToken.equals("double")
                    || nameToken.equals("function");
        }

        for (TokenGroup parentTypeToken : parentTypeTokens) {
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
        if (isCrossed || isStruct() || isEnum()) return;
        isCrossed = true;

        for (Pointer parent : parents) {
            Type pType = parent.type;
            pType.cross();
        }

        for (int i = 0; i < parents.size(); i++) {
            Pointer pPtr = parents.get(i);
            for (MethodView pMW : pPtr.type.methodView) {
                if (pMW.isStatic()) continue;
                pMW = new MethodView(pPtr, pMW);

                boolean impl = false;
                boolean add = true;
                for (MethodView mw : methodView.get(pMW.getName())) {
                    if (mw.isStatic()) continue;
                    Token erro = mw.isFrom(this) ? mw.getName() : parentTokens.get(i);

                    if (mw.canOverride(pMW)) {
                        if (pMW.isFinal()) cFile.erro(erro, "Cannot override a final method");
                        if (!pMW.canAcess(this)) {
                            pMW.canAcess(this);
                            cFile.erro(erro, "Cannot acess");
                        }
                        if ((pMW.isPublic() && !mw.isPublic()) || (!pMW.isPrivate() && mw.isPrivate())) {
                            cFile.erro(erro, "Incompatible acess");
                        }

                        impl = true;
                        add = false;
                        break;
                    } else if (!mw.canOverload(pMW)) {
                        cFile.erro(erro, "Incompatible signature [" + pMW.getName() + "]");
                        add = false;
                        break;
                    }
                }

                if (!isAbstract() && pMW.isAbstract() && !impl) {
                    cFile.erro(parentTokens.get(i), "Abstract method not implemented [" + pMW.getName() + "]");
                }
                if (add) methodView.put(pMW.getName(), pMW);
            }
            for (IndexerView pIW : pPtr.type.indexerView) {
                pIW = new IndexerView(pPtr, pIW);

                boolean implGet = !pIW.hasGet() || !pIW.isGetAbstract();
                boolean implSet = !pIW.hasSet() || !pIW.isSetAbstract();
                boolean implOwn = !pIW.hasOwn() || !pIW.isOwnAbstract();
                boolean add = true;
                for (IndexerView iw : indexerView) {
                    Token erro = iw.isFrom(this) ? iw.getToken() : parentTokens.get(i);

                    if (iw.canOverride(pIW)) {
                        iw.addOverriden(pIW);
                        if (pIW.hasGet()) {
                            if (pIW.isGetFinal()) cFile.erro(erro, "Cannot override a final GET");
                            if (!pIW.canAcessGet(this)) cFile.erro(erro, "Cannot acess GET");
                            if (pIW.getAcess > iw.getAcess) {
                                cFile.erro(erro, "Incompatible GET acess");
                                iw.getAcess = pIW.getAcess;
                            }
                        }

                        if (pIW.hasSet()) {
                            if (pIW.isSetFinal()) cFile.erro(erro, "Cannot override a final SET");
                            if (!pIW.canAcessSet(this)) cFile.erro(erro, "Cannot acess SET");
                            if (pIW.setAcess > iw.setAcess) {
                                cFile.erro(erro, "Incompatible SET acess");
                                iw.setAcess = pIW.setAcess;
                            }
                        }
                        if (pIW.hasOwn()) {
                            if (pIW.isOwnFinal()) cFile.erro(erro, "Cannot override a final OWN");
                            if (!pIW.canAcessOwn(this)) cFile.erro(erro, "Cannot acess OWN");
                            if (pIW.ownAcess > iw.ownAcess) {
                                cFile.erro(erro, "Incompatible OWN acess");
                                iw.ownAcess = pIW.ownAcess;
                            }
                        }

                        implGet = implGet || !iw.hasGet() || !iw.isGetAbstract();
                        implSet = implSet || !iw.hasSet() || !iw.isSetAbstract();
                        implOwn = implOwn || !iw.hasOwn() || !iw.isOwnAbstract();
                        add = false;
                    } else if (!iw.canOverload(pIW)) {
                        cFile.erro(erro, "Incompatible signature [ " + pIW.getParams() + " ]");
                        add = false;
                        break;
                    }
                }

                if (!isAbstract() && !implGet) {
                    cFile.erro(parentTokens.get(i), "Abstract indexer 'GET' not implemented [" + pIW.getParams() + "]");
                }
                if (!isAbstract() && !implSet) {
                    cFile.erro(parentTokens.get(i), "Abstract indexer 'SET' not implemented [" + pIW.getParams() + "]");
                }
                if (!isAbstract() && !implOwn) {
                    cFile.erro(parentTokens.get(i), "Abstract indexer 'OWN' not implemented [" + pIW.getParams() + "]");
                }
                if (add) indexerView.add(pIW);
            }
            for (FieldView pFW : pPtr.type.fields.values()) {
                if (pFW.isStatic()) continue;
                pFW = new FieldView(pPtr, pFW);

                boolean implGet = !pFW.hasGet() || !pFW.isGetAbstract();
                boolean implSet = !pFW.hasSet() || !pFW.isSetAbstract();
                boolean implOwn = !pFW.hasOwn() || !pFW.isOwnAbstract();
                boolean add = true;
                FieldView fw = fields.get(pFW.getName());
                if (fw != null && !fw.isStatic()) {
                    Token erro = fw.isFrom(this) ? fw.getName() : parentTokens.get(i);
                    add = false;
                    if (fw.canOverride(pFW)) {
                        fw.addOverriden(pFW);
                        if (pFW.hasGet()) {
                            if (pFW.isGetFinal()) cFile.erro(erro, "Cannot override a final GET");
                            if (!pFW.canAcessGet(this)) cFile.erro(erro, "Cannot acess GET");
                            if (pFW.getAcess > fw.getAcess) {
                                cFile.erro(erro, "Incompatible GET acess");
                                fw.getAcess = pFW.getAcess;
                            }
                        }

                        if (pFW.hasSet()) {
                            if (pFW.isSetFinal()) cFile.erro(erro, "Cannot override a final SET");
                            if (!pFW.canAcessSet(this)) cFile.erro(erro, "Cannot acess SET");
                            if (pFW.setAcess > fw.setAcess) {
                                cFile.erro(erro, "Incompatible SET acess");
                                fw.setAcess = pFW.setAcess;
                            }
                        }
                        if (pFW.hasOwn()) {
                            if (pFW.isOwnFinal()) cFile.erro(erro, "Cannot override a final OWN");
                            if (!pFW.canAcessOwn(this)) cFile.erro(erro, "Cannot acess OWN");
                            if (pFW.ownAcess > fw.ownAcess) {
                                cFile.erro(erro, "Incompatible OWN acess");
                                fw.ownAcess = pFW.ownAcess;
                            }
                        }

                        implGet = implGet || !fw.hasGet() || !fw.isGetAbstract();
                        implSet = implSet || !fw.hasSet() || !fw.isSetAbstract();
                        implOwn = implOwn || !fw.hasOwn() || !fw.isOwnAbstract();
                    } else if (!fw.canShadow(pFW)) {
                        cFile.erro(erro, "Incompatible signature [ " + pFW.getName() + " ]");
                    }
                }

                if (!isAbstract() && !implGet) {
                    cFile.erro(parentTokens.get(i), "Abstract property 'GET' not implemented [" + pFW.getName() + "]");
                }
                if (!isAbstract() && !implSet) {
                    cFile.erro(parentTokens.get(i), "Abstract property 'SET' not implemented [" + pFW.getName() + "]");
                }
                if (!isAbstract() && !implOwn) {
                    cFile.erro(parentTokens.get(i), "Abstract property 'OWN' not implemented [" + pFW.getName() + "]");
                }
                if (add) fields.put(pFW.getName(), pFW);
            }
        }
    }

    public void build(CppBuilder cBuilder) {

        cBuilder.toHeader();
        cBuilder.add("//").add(fileName).add(".h").ln()
                .add("#ifndef H_").add(fileName).ln()
                .add("#define H_").add(fileName).ln()
                .add("#include \"langCore.h\"").ln();
        cBuilder.markHeader();
        cBuilder.ln();

        cBuilder.toSource();
        cBuilder.add("//").add(fileName).add(".cpp").ln();
        cBuilder.markSource();
        cBuilder.ln();

        cBuilder.toHeader();
        cBuilder.add(template)
                .add("class ").add(pathToken).add(isClass() || isInterface(), " :");
        if (parent == null || isInterface()) {
            cBuilder.add(" public IObject");
        }
        for (int i = 0; i < parents.size(); i++) {
            cBuilder.add(i > 0 || isInterface(), ",").add(" public ").parent(parents.get(i));
        }
        cBuilder.add(" {").ln()
                .add("public :").ln();

        for (Property property : properties) {
            if (!property.isStatic()) {
                property.build(cBuilder);
            }
        }
        for (Indexer indexer : indexers) {
            indexer.build(cBuilder);
        }
        for (Method method : methods) {
            if (!method.isStatic()) {
                method.build(cBuilder);
            }
        }
        for (Operator operator : operators) {
            operator.build(cBuilder);
        }

        cBuilder.toHeader();
        cBuilder.add("};").ln()
                .ln();

        // Static Members
        cBuilder.add("class ").add(staticPathToken).add(" {").ln()
                .add("public :").ln()
                .idt(1).add("static void init();").ln()
                .idt(1).add("static bool initBlock();").ln();

        cBuilder.toSource();
        cBuilder.add("void ").path(self, true).add("::init() {").ln()
                .idt(1).add("static bool _init = initBlock();").ln()
                .add("}").ln()
                .ln();
        cBuilder.add("bool ").path(self, true).add("::initBlock() {").ln()
                .idt(1).add("return true;").ln()
                .add("}").ln()
                .ln();

        for (Property property : properties) {
            if (property.isStatic()) {
                property.build(cBuilder);
            }
        }
        for (Method method : methods) {
            if (method.isStatic()) {
                method.build(cBuilder);
            }
        }

        cBuilder.toHeader();

        cBuilder.add("};").ln();

        cBuilder.headerDependence();
        cBuilder.sourceDependence();
        cBuilder.directDependence();

        cBuilder.add("#endif");
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
        return isBase;
    }

    public boolean isAbsAllowed() {
        return (isClass() && isAbstract()) || isInterface();
    }

    public boolean isFinalAllowed() {
        return isClass();
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
            if (property.hasGet() && property.isGetAbstract() &&
                    ((property.isGetPrivate() && !isPrivate()) || (!property.isGetPublic() && isPublic()))) {
                cFile.erro(property.token, "A abstract property GET cannot have a stronger acess modifier");
            }
            if (property.hasSet() && property.isSetAbstract() &&
                    ((property.isSetPrivate() && !isPrivate()) || (!property.isSetPublic() && isPublic()))) {
                cFile.erro(property.token, "A abstract property SET cannot have a strong acess modifier");
            }
            if (property.hasOwn() && property.isOwnAbstract() &&
                    ((property.isOwnPrivate() && !isPrivate()) || (!property.isOwnPublic() && isPublic()))) {
                cFile.erro(property.token, "A abstract property OWN cannot have a strong acess modifier");
            }
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
            if (method.isAbstract() && ((method.isPrivate() && !isPrivate()) || (!method.isPublic() && isPublic()))) {
                cFile.erro(method.getName(), "A abstract method cannot have a strong acess modifier than the type");
            }
            for (Method methodB : methods) {
                if (method.getName().equals(methodB.getName())) {
                    if (!method.getParams().canOverload(methodB.getParams())) {
                        cFile.erro(method.getName(), "Invalid overloading");
                        return;
                    }
                }
            }

            methods.add(method);
            methodView.put(method.getName(), new MethodView(self, method));
        }
    }

    public void add(Indexer indexer) {
        if (indexer.load()) {
            if (indexer.hasGet() && indexer.isGetAbstract() &&
                    ((indexer.isGetPrivate() && !isPrivate()) || (!indexer.isGetPublic() && isPublic()))) {
                cFile.erro(indexer.token, "A abstract indexer GET cannot have a stronger acess modifier");
            }
            if (indexer.hasSet() && indexer.isSetAbstract() &&
                    ((indexer.isSetPrivate() && !isPrivate()) || (!indexer.isSetPublic() && isPublic()))) {
                cFile.erro(indexer.token, "A abstract indexer SET cannot have a strong acess modifier");
            }
            if (indexer.hasOwn() && indexer.isOwnAbstract() &&
                    ((indexer.isOwnPrivate() && !isPrivate()) || (!indexer.isOwnPublic() && isPublic()))) {
                cFile.erro(indexer.token, "A abstract indexer OWN cannot have a strong acess modifier");
            }
            for (Indexer indexerB : indexers) {
                if (!indexer.getParams().canOverload(indexerB.getParams())) {
                    cFile.erro(indexer.token, "Invalid overloading");
                    return;
                }
            }

            indexers.add(indexer);
            indexerView.add(new IndexerView(self, indexer));
        }
    }

    public void add(Operator operator) {
        if (operator.load()) {
            if (!isStruct()) {
                cFile.erro(operator.token, "Operators not allowed");
            } else {
                for (Operator operatorB : operators) {
                    if (operator.op == operatorB.op
                            || (operator.op == Key.CAST && operatorB.op == Key.AUTO)
                            || (operator.op == Key.AUTO && operatorB.op == Key.CAST)) {

                        if (operator.op == Key.CAST || operator.op == Key.AUTO) {
                            if (operator.typePtr.equals(operatorB.typePtr)) {
                                cFile.erro(operator.token, "Invalid overloading");
                                return;
                            }
                        } else if (!operator.params.canOverload(operatorB.params)) {
                            cFile.erro(operator.token, "Invalid overloading");
                            return;
                        }
                    }
                }

                operators.add(operator);
            }
        }
    }

    public void add(Constructor constructor) {
        if (constructor.load()) {
            if (isInterface() && !constructor.isStatic()) {
                cFile.erro(constructor.token, "Instance constructors not allowed");
            } else if (constructor.isStatic()) {
                if (staticConstructor != null) {
                    cFile.erro(constructor.token, "Repeated static constructor");
                } else {
                    if (constructor.isPrivate()) {
                        cFile.erro(constructor.token, "Static constructors are always public");
                    }
                    staticConstructor = constructor;
                }
            } else {
                for (Constructor constructorB : constructors) {
                    if (!constructor.getParams().canOverload(constructorB.getParams())) {
                        cFile.erro(constructor.token, "Invalid overloading");
                        return;
                    }
                }

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

    public IndexerView getIndexerView(int index) {
        return indexerView.get(index);
    }

    public int getOperatorsCount() {
        return operators.size();
    }

    public Operator getOperator(int index) {
        if (index < operators.size()) {
            return operators.get(index);
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return nameToken+"["+hashCode()+"]";
    }
}
