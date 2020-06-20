package logic.typdef;

import content.Key;
import content.Parser;
import content.Token;
import content.TokenGroup;
import data.ContentFile;
import data.CppBuilder;
import logic.GenericOwner;
import logic.ViewList;
import logic.member.view.IndexerView;
import logic.member.view.MethodView;
import logic.member.view.ParamView;
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

    public ArrayList<Pointer> parents = new ArrayList<>();
    ArrayList<TokenGroup> parentTypeTokens = new ArrayList<>();

    ArrayList<Property> properties = new ArrayList<>();
    ArrayList<Variable> variables = new ArrayList<>();
    ArrayList<Num> nums = new ArrayList<>();

    private ArrayList<Method> methods = new ArrayList<>();
    private ArrayList<Indexer> indexers = new ArrayList<>();
    private ArrayList<Operator> operators = new ArrayList<>();
    private ArrayList<Constructor> constructors = new ArrayList<>();
    private ArrayList<Destructor> destructors = new ArrayList<>(1);
    private ArrayList<Native> natives = new ArrayList<>();
    private Constructor staticConstructor;

    private ArrayList<Type> inheritanceTypes = new ArrayList<>();
    private boolean isPrivate, isPublic, isAbstract, isFinal, isStatic;
    boolean isLoaded, isCrossed, isBase, isFunction, hasGeneric;

    private HashMap<Token, FieldView> fields = new HashMap<>();
    private ViewList<MethodView> methodView = new ViewList<>();
    private ArrayList<IndexerView> indexerView = new ArrayList<>();

    private ArrayList<MethodView> methodViewImpl = new ArrayList<>();
    private ArrayList<IndexerView> indexerGetViewImpl = new ArrayList<>();
    private ArrayList<IndexerView> indexerSetViewImpl = new ArrayList<>();
    private ArrayList<IndexerView> indexerOwnViewImpl = new ArrayList<>();
    private ArrayList<Property> propertyGetViewImpl = new ArrayList<>();
    private ArrayList<Property> propertySetViewImpl = new ArrayList<>();
    private ArrayList<Property> propertyOwnViewImpl = new ArrayList<>();

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
                    hasGeneric = true;
                    template = new Template(cFile, token, !isStruct());
                }
                state = 3;
            } else if ((state == 2 || state == 3) && token.key == Key.COLON) {
                state = 4;
            } else if (state == 4 && token.key == Key.WORD) {
                parentTypeTokens.add(new TokenGroup(token, next = TokenGroup.nextType(next, end)));
                state = 5;
            } else if (state == 5 && token.key == Key.COMMA) {
                state = 4;
            } else if ((state == 2 || state == 3 || state == 4 || state == 5) && token.key == Key.BRACE) {
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
        staticPathToken = new Token("_" + pathName);

        if (cFile.library == cFile.library.getCompiler().getLangLibrary()) {
            isBase = nameToken.equals("bool")
                    || nameToken.equals("byte")
                    || nameToken.equals("short")
                    || nameToken.equals("int")
                    || nameToken.equals("long")
                    || nameToken.equals("float")
                    || nameToken.equals("double")
                    || nameToken.equals("function");
            isFunction = isBase && nameToken.equals("function");
        }

        for (TokenGroup parentTypeToken : parentTypeTokens) {
            inheritanceType(parentTypeToken.start, parentTypeToken.end);
        }

        if (template != null) {
            template.preload(this);
        }
    }

    public void load() {
        for (Type parent : inheritanceTypes) {
            parent.load();
        }

        if (template != null) {
            template.load(this, null);
        }

        Pointer[] p = template == null ? null : new Pointer[template.generics.size()];
        if (p != null) {
            for (int i = 0; i < p.length; i++) {
                p[i] = template.generics.get(i).typePtr;
            }
        }
        self = new Pointer(this, p, false);
    }

    public void internal() {
        if (contentToken != null && contentToken.getChild() != null) {
            Parser.parseMembers(this, contentToken.getChild(), contentToken.getLastChild());
        }
    }

    public void cross() {
        if (isCrossed || !isClass()) return;
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

                MethodView impl = null;
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
                        mw.markOverrided(pMW);
                        impl = mw;
                        add = false;
                    } else if (!mw.canOverload(pMW)) {
                        cFile.erro(erro, "Incompatible signature [" + pMW.getName() + "]");
                        add = false;
                        break;
                    }
                }

                if (!isAbstract() && pMW.isAbstract() && impl == null) {
                    cFile.erro(parentTokens.get(i), "Abstract method not implemented [" + pMW.getName() + "]");
                } else if (!isAbstract() && pMW.isAbstract() && impl != null && impl.method.type != this) {
                    methodViewImpl.add(impl);
                } else if (!isAbstract() && parents.size() > 1 && pMW.original().type == cFile.langObject() &&
                        (impl == null || impl.method.type != this)) {

                    // lang::Object Exception
                    boolean has = false;
                    for (MethodView imw : methodViewImpl) {
                        if (imw.original().type == cFile.langObject() && imw.getName().equals(pMW.getName())) {
                            has = true;
                            break;
                        }
                    }
                    if (!has) {
                        methodViewImpl.add(impl == null ? pMW : impl);
                    }
                }

                if (add) {
                    hasGeneric = hasGeneric || pMW.getTemplate() != null;
                    methodView.put(pMW.getName(), pMW);
                }
            }
            for (IndexerView pIW : pPtr.type.indexerView) {
                pIW = new IndexerView(pPtr, pIW);

                IndexerView implIndexerGet = null, implIndexerSet = null, implIndexerOwn = null;
                boolean implGet = !pIW.hasGet() || !pIW.isGetAbstract();
                boolean implSet = !pIW.hasSet() || !pIW.isSetAbstract();
                boolean implOwn = !pIW.hasOwn() || !pIW.isOwnAbstract();
                boolean add = true;
                for (IndexerView iw : indexerView) {
                    Token erro = iw.isFrom(this) ? iw.getToken() : parentTokens.get(i);

                    if (iw.canOverride(pIW)) {
                        if (pIW.hasGet()) {
                            if (pIW.isGetFinal()) cFile.erro(erro, "Cannot override a final GET");
                            else if (!pIW.canAcessGet(this)) cFile.erro(erro, "Cannot acess GET");
                            else if (iw.hasGet() && (iw.isGetPrivate() || (!iw.isGetPublic() && pIW.isGetPublic()))) {
                                cFile.erro(erro, "Incompatible GET acess");
                            } else if (!iw.hasGet()) {
                                if (!pIW.isGetAbstract()) iw.setGetSource(pIW);
                            } else if (pIW.isGetAbstract()) {
                                implGet = true;
                                implIndexerGet = iw.srcGet;
                            }
                        }

                        if (pIW.hasSet()) {
                            if (pIW.isSetFinal()) cFile.erro(erro, "Cannot override a final SET");
                            else if (!pIW.canAcessSet(this)) cFile.erro(erro, "Cannot acess SET");
                            else if (iw.hasSet() && (iw.isSetPrivate() || (!iw.isSetPublic() && pIW.isSetPublic()))) {
                                cFile.erro(erro, "Incompatible SET acess");
                            } else if (!iw.hasSet()) {
                                if (!pIW.isSetAbstract()) iw.setSetSource(pIW);
                            } else if (pIW.isSetAbstract()) {
                                implSet = true;
                                implIndexerSet = iw.srcSet;
                            }
                        }
                        if (pIW.hasOwn()) {
                            if (pIW.isOwnFinal()) cFile.erro(erro, "Cannot override a final OWN");
                            else if (!pIW.canAcessOwn(this)) cFile.erro(erro, "Cannot acess OWN");
                            else if (iw.hasOwn() && (iw.isOwnPrivate() || (!iw.isOwnPublic() && pIW.isOwnPublic()))) {
                                cFile.erro(erro, "Incompatible OWN acess");
                            } else if (!iw.hasOwn()) {
                                if (!pIW.isOwnAbstract()) iw.setOwnSource(pIW);
                            } else if (pIW.isOwnAbstract()) {
                                implOwn = true;
                                implIndexerOwn = iw.srcOwn;
                            }
                        }
                        add = false;
                    } else if (!iw.canOverload(pIW)) {
                        cFile.erro(erro, "Incompatible signature [ " + pIW.getParams() + " ]");
                        add = false;
                        break;
                    }
                }

                if (!isAbstract() && pIW.hasGet() && pIW.isGetAbstract() && !implGet) {
                    cFile.erro(parentTokens.get(i), "Abstract indexer 'GET' not implemented [" + pIW.getParams() + "]");
                } else if (implIndexerGet != null && implIndexerGet.indexer.type != this) {
                    indexerGetViewImpl.add(implIndexerGet);
                }
                if (!isAbstract() && pIW.hasSet() && pIW.isSetAbstract() && !implSet) {
                    cFile.erro(parentTokens.get(i), "Abstract indexer 'SET' not implemented [" + pIW.getParams() + "]");
                } else if (implIndexerSet != null && implIndexerSet.indexer.type != this) {
                    indexerSetViewImpl.add(implIndexerSet);
                }
                if (!isAbstract() && pIW.hasOwn() && pIW.isOwnAbstract() && !implOwn) {
                    cFile.erro(parentTokens.get(i), "Abstract indexer 'OWN' not implemented [" + pIW.getParams() + "]");
                } else if (implIndexerOwn != null && implIndexerOwn.indexer.type != this) {
                    indexerOwnViewImpl.add(implIndexerOwn);
                }
                if (add) indexerView.add(pIW);
            }
            for (FieldView pFW : pPtr.type.fields.values()) {
                if (pFW.isStatic()) continue;
                pFW = new FieldView(pPtr, pFW);

                Property implFieldGet = null, implFieldSet = null, implFieldOwn = null;
                boolean implGet = !pFW.hasGet() || !pFW.isGetAbstract();
                boolean implSet = !pFW.hasSet() || !pFW.isSetAbstract();
                boolean implOwn = !pFW.hasOwn() || !pFW.isOwnAbstract();
                boolean add = true;
                FieldView fw = fields.get(pFW.getName());
                if (fw != null && !fw.isStatic()) {
                    Token erro = fw.isFrom(this) ? fw.getName() : parentTokens.get(i);
                    add = false;
                    if (fw.canOverride(pFW)) {
                        if (pFW.hasGet()) {
                            if (pFW.isGetFinal()) cFile.erro(erro, "Cannot override a final GET");
                            else if (!pFW.canAcessGet(this)) cFile.erro(erro, "Cannot acess GET");
                            else if (fw.hasGet() && (fw.isGetPrivate() || (!fw.isGetPublic() && pFW.isGetPublic()))) {
                                cFile.erro(erro, "Incompatible GET acess");
                            } else if (!fw.hasGet()) {
                                if (!pFW.isGetAbstract()) fw.setGetSource(pFW);
                            } else if (pFW.isGetAbstract()) {
                                implGet = true;
                                implFieldGet = fw.srcGet;
                            }
                        }

                        if (pFW.hasSet()) {
                            if (pFW.isSetFinal()) cFile.erro(erro, "Cannot override a final SET");
                            else if (!pFW.canAcessSet(this)) cFile.erro(erro, "Cannot acess SET");
                            else if (fw.hasSet() && (fw.isSetPrivate() || (!fw.isSetPublic() && pFW.isSetPublic()))) {
                                cFile.erro(erro, "Incompatible SET acess");
                            } else if (!fw.hasSet()) {
                                if (!pFW.isSetAbstract()) fw.setSetSource(pFW);
                            } else if (pFW.isSetAbstract()) {
                                implSet = true;
                                implFieldSet = fw.srcSet;
                            }
                        }
                        if (pFW.hasOwn()) {
                            if (pFW.isOwnFinal()) cFile.erro(erro, "Cannot override a final OWN");
                            else if (!pFW.canAcessOwn(this)) cFile.erro(erro, "Cannot acess OWN");
                            else if (fw.hasOwn() && (fw.isOwnPrivate() || (!fw.isOwnPublic() && pFW.isOwnPublic()))) {
                                cFile.erro(erro, "Incompatible OWN acess");
                            } else if (!fw.hasOwn()) {
                                if (!pFW.isOwnAbstract()) fw.setOwnSource(pFW);
                            } else if (pFW.isOwnAbstract()) {
                                implOwn = true;
                                implFieldOwn = fw.srcOwn;
                            }
                        }
                    } else if (!fw.canShadow(pFW)) {
                        cFile.erro(erro, "Incompatible signature [ " + pFW.getName() + " ]");
                    }
                }

                if (!isAbstract() && pFW.hasGet() && pFW.isGetAbstract() && !implGet) {
                    cFile.erro(parentTokens.get(i), "Abstract property 'GET' not implemented [" + pFW.getName() + "]");
                } else if (implFieldGet != null && implFieldGet.type != this){
                    propertyGetViewImpl.add(implFieldGet);
                }
                if (!isAbstract() && pFW.hasSet() && pFW.isSetAbstract() && !implSet) {
                    cFile.erro(parentTokens.get(i), "Abstract property 'SET' not implemented [" + pFW.getName() + "]");
                } else if (implFieldSet != null && implFieldSet.type != this){
                    propertySetViewImpl.add(implFieldSet);
                }
                if (!isAbstract() && pFW.hasOwn() && pFW.isOwnAbstract() && !implOwn) {
                    cFile.erro(parentTokens.get(i), "Abstract property 'OWN' not implemented [" + pFW.getName() + "]");
                } else if (implFieldOwn != null && implFieldOwn.type != this){
                    propertyOwnViewImpl.add(implFieldOwn);
                }
                if (add) fields.put(pFW.getName(), pFW);
            }
        }

        if (parent != null) {
            for (Constructor pC : parent.type.constructors) {
                if (pC.isDefault() && pC.isPublic()) {
                    boolean isImplemented = false;
                    ParamView pCW = new ParamView(parent, pC.getParams());
                    for (Constructor cC : constructors) {
                        if (!cC.isStatic() && cC.getParams().canOverride(pCW)) {
                            if (!cC.isPublic()) {
                                cFile.erro(cC.token, "A Default constructor implementation must be public");
                            }
                            cC.markDefault();
                            isImplemented = true;
                            break;
                        }
                    }
                    if (!isImplemented) {
                        cFile.erro(nameToken, "Default constructor not implemented");
                    }
                }
            }
        }
    }

    public void make() {
        for (Method method : methods) {
            if (!method.isAbstract()) {
                method.make();
            }
        }
    }

    public void build(CppBuilder cBuilder) {

        cBuilder.toHeader();
        cBuilder.add("// ").add(fileName).add(".h").ln()
                .ln()
                .add("#ifndef H_").add(fileName.toUpperCase()).ln()
                .add("#define H_").add(fileName.toUpperCase()).ln()
                .ln()
                .add("#include \"langCore.h\"").ln();
        cBuilder.markHeader();
        cBuilder.ln();

        // cBuild.toSource() || cBuilder.toGeneric()
        cBuilder.add("#define T_").add(fileName.toUpperCase()).add(" ");
        ArrayList<Pointer> allParents = new ArrayList<>(parents.size() * 2);
        cBuilder.path(self, false);
        if (!isInterface()) {
            recursiveGetParent(self, allParents);
            for (Pointer p : allParents) {
                cBuilder.add(", ").path(p, false);
            }
        }
        cBuilder.ln()
                .ln();
        allParents = null;

        cBuilder.toSource();
        cBuilder.add("// ").add(fileName).add(".cpp").ln();
        cBuilder.markSource();
        cBuilder.ln();

        if (hasGeneric()) {
            cBuilder.toGeneric();
            cBuilder.add("// ").add(fileName).add(".hpp").ln();
            cBuilder.markGeneric();
            cBuilder.ln();
        }

        cBuilder.toHeader();
        cBuilder.add(template)
                .add("class ").add(pathToken).add(isClass() || isInterface(), " :");
        if (parent == null || isInterface()) {
            cBuilder.add(" public IObject");
        }
        for (int i = 0; i < parents.size(); i++) {
            if (i == 0 && isInterface()) continue;

            cBuilder.add(i > 0 || isInterface(), ",").add(" public ").parent(parents.get(i));
        }
        cBuilder.add(" {").ln()
                .add("public :").ln();

        cBuilder.idt(1).add("// Type").ln()
                .idt(1).add("using P = ").add(isPointer(), "Ptr<").path(self, false).add(isPointer() ? ">;" : ";").ln()
                .idt(1).add("using L = ").add(isPointer(), "Let<").path(self, false).add(isPointer() ? ">;" : ";").ln()
                .idt(1).add("static lang::type* typeOf() { return getType<T_").add(fileName.toUpperCase()).add(">(); }").ln()
                .idt(1).add(isPointer(), "virtual lang::type* getTypeOf() { return typeOf(); }").ln();

        if (isClass()) cBuilder.idt(1).add("virtual void* self() { return &weak; }").ln();

        // Natives
        for (Native nat : natives) {
            if (!nat.isStatic()) {
                nat.build(cBuilder);
            }
        }

        // Variables
        for (Variable variable : variables) {
            if (!variable.isStatic()) {
                variable.build(cBuilder);
            }
        }

        // Properties
        for (Property property : properties) {
            if (!property.isStatic()) {
                property.build(cBuilder);
            }
        }
        for (Property fw : propertyGetViewImpl) {
            fw.buildImplGet(cBuilder, self);
        }
        for (Property fw : propertySetViewImpl) {
            fw.buildImplSet(cBuilder, self);
        }
        for (Property fw : propertyOwnViewImpl) {
            fw.buildImplOwn(cBuilder, self);
        }

        // Indexers
        for (Indexer indexer : indexers) {
            indexer.build(cBuilder);
        }
        for (IndexerView iw : indexerGetViewImpl) {
            iw.indexer.buildImplGet(cBuilder, self, iw);
        }
        for (IndexerView iw : indexerSetViewImpl) {
            iw.indexer.buildImplSet(cBuilder, self, iw);
        }
        for (IndexerView iw : indexerOwnViewImpl) {
            iw.indexer.buildImplOwn(cBuilder, self, iw);
        }

        // Constructors
        if (isValue()) {
            cBuilder.toHeader();
            cBuilder.idt(1).add(pathToken).add("();").ln();

            cBuilder.toSource(hasGeneric());
            cBuilder.path(self, false).add("::").add(pathToken).add("()");
            boolean first = true;
            for (Variable variable : variables) {
                if (!variable.isStatic()) {
                    cBuilder.add(first ? " : " : ", ").ln();
                    first = false;
                    variable.buildInit(cBuilder);
                }
            }
            cBuilder.add(" {\n}").ln()
                    .ln();
        }
        for (Constructor constructor : constructors) {
            if (!constructor.isStatic()) {
                constructor.build(cBuilder);
            }
        }

        // Destructor
        for (Destructor destructor : destructors) {
            destructor.build(cBuilder);
        }

         // Methods
        for (Method method : methods) {
            if (!method.isStatic()) {
                method.build(cBuilder);
            }
        }
        for (MethodView mw : methodViewImpl) {
            mw.method.buildImpl(cBuilder, self, mw);
        }

        // Operators
        for (Operator operator : operators) {
            operator.build(cBuilder);
        }

        cBuilder.toHeader();
        cBuilder.add("};").ln()
                .ln();

        // Static Members
        cBuilder.toHeader();
        cBuilder.add("class ").add(staticPathToken).add(" {").ln()
                .add("public :").ln()
                .idt(1).add("static void init();").ln()
                .idt(1).add("static bool initBlock();").ln();

        cBuilder.toSource();
        cBuilder.add("void ").path(self, true).add("::init() {").ln()
                .idt(1).add("static bool _init = initBlock();").ln()
                .add("}").ln()
                .ln();

        cBuilder.add("bool ").path(self, true).add("::initBlock() {").ln();
        for (Variable variable : variables) {
            if (variable.isStatic()) {
                variable.buildInit(cBuilder);
            }
        }
        if (staticConstructor != null) {
            staticConstructor.build(cBuilder);
        }
        cBuilder.idt(1).add("return true;").ln()
                .add("}").ln()
                .ln();

        for (Native nat : natives) {
            if (nat.isStatic()) {
                nat.build(cBuilder);
            }
        }
        for (Variable variable : variables) {
            if (variable.isStatic()) {
                variable.build(cBuilder);
            }
        }
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
        cBuilder.genericDependence();
        cBuilder.directDependence();

        cBuilder.toHeader();
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

    public boolean isValue() {
        return isStruct() || isEnum();
    }

    public boolean isPointer() {
        return isClass() || isInterface();
    }

    public boolean isLangBase() {
        return isBase;
    }

    public boolean isFunction() {
        return isFunction;
    }

    public boolean isAbsAllowed() {
        return (isClass() && isAbstract()) || isInterface();
    }

    public boolean isFinalAllowed() {
        return isClass();
    }

    public boolean hasGeneric() {
        return hasGeneric;
    }

    @Override
    public Pointer findGeneric(Token genericToken) {
        if (template != null) {
            return template.findGeneric(genericToken);
        }
        return null;
    }

    private void recursiveGetParent(Pointer caller, ArrayList<Pointer> pointers) {
        for (Pointer pointer : parents) {
            Pointer p = Pointer.byGeneric(pointer, caller);
            if (!pointers.contains(p)) {
                pointers.add(p);
            }
            p.type.recursiveGetParent(p, pointers);
        }
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

    public boolean cyclicVariableVerify(Type type) {
        if (type == this) return true;

        for (Variable variable : variables) {
            if (variable.getTypePtr().type != null && variable.getTypePtr().type.isStruct()) {
                if (variable.getTypePtr().type.cyclicVariableVerify(type)) {
                    return true;
                }
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
            } else if (isStruct()
                    && variable.getTypePtr().type != null
                    && variable.getTypePtr().type.isStruct()
                    && variable.getTypePtr().type.cyclicVariableVerify(this)) {
                cFile.erro(variable.getTypeToken().start, "Cyclic variable type");
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

            hasGeneric = hasGeneric || method.getTemplate() != null;
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
                } else if (constructor.isDefault()) {
                    cFile.erro(constructor.token, "A Default constructor cannot be static");
                } else {
                    if (constructor.isPrivate()) {
                        cFile.erro(constructor.token, "Static constructors are always public");
                    }
                    staticConstructor = constructor;
                }
            } else {
                if (constructor.isDefault() && !constructor.isPublic()) {
                    cFile.erro(constructor.token, "A Default constructor must be public");
                }
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
            } else if (!isClass()) {
                cFile.erro(destructor.token, "Destructors not allowed");
            } else {
                destructors.add(destructor);
            }
        }
    }

    public void add(Native nat) {
        if (nat.load()) {
            natives.add(nat);
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

    public int getVariablesCount() {
        return variables.size() + (parent != null && parent.type != null ? parent.type.getVariablesCount() : 0);
    }

    public Variable getVariable(int index) {
        if (index < variables.size()) {
            return variables.get(index);
        } else {
            return parent.type.getVariable(index - variables.size());
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
