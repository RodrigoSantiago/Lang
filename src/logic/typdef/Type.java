package logic.typdef;

import content.Key;
import content.Parser;
import content.Token;
import content.TokenGroup;
import data.ContentFile;
import data.CppBuilder;
import logic.GenericOwner;
import logic.ViewList;
import logic.member.view.*;
import logic.templates.Template;
import logic.Pointer;
import logic.member.*;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class Type implements GenericOwner {

    public final ContentFile cFile;

    public Token nameToken;
    public Token pathToken;
    public Token staticPathToken;
    public String fileName;

    public Template template;
    public TokenGroup contentToken;

    public Pointer self;
    public Pointer parent;

    ArrayList<Token> parentTokens = new ArrayList<>();

    public ArrayList<Pointer> parents = new ArrayList<>();
    public ArrayList<Pointer> autoCast = new ArrayList<>();
    public ArrayList<Pointer> casts = new ArrayList<>();
    ArrayList<TokenGroup> parentTypeTokens = new ArrayList<>();

    public ArrayList<Property> properties = new ArrayList<>();
    public ArrayList<Variable> variables = new ArrayList<>();
    public ArrayList<Num> nums = new ArrayList<>();

    private ArrayList<Method> methods = new ArrayList<>();
    private ArrayList<Indexer> indexers = new ArrayList<>();
    private ArrayList<Operator> operators = new ArrayList<>();
    private ArrayList<Constructor> constructors = new ArrayList<>();
    private ArrayList<Native> natives = new ArrayList<>();
    private Destructor destructor;
    private Constructor staticConstructor;
    private Constructor emptyConstructor;
    private Constructor parentEmptyConstructor;

    private ArrayList<Type> inheritanceTypes = new ArrayList<>();
    private boolean isPrivate, isPublic, isAbstract, isFinal, isStatic;
    boolean isLoaded, isCrossed, isBase, isFunction, hasGeneric, hasStaticInit;

    private HashMap<Token, FieldView> fields = new HashMap<>();
    private ViewList<MethodView> methodView = new ViewList<>();
    private ViewList<OperatorView> operatorView = new ViewList<>();
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
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();

            if (state == 0 && token.key == key) {
                state = 1;
            } else if (state == 0 && token.key.isAttribute) {
                if (token.key == Key.PUBLIC || token.key == Key.PRIVATE) {
                    if (isPublic || isPrivate) {
                        cFile.erro(token, "Repeated acess modifier", this);
                    } else {
                        isPublic = (token.key == Key.PUBLIC);
                        isPrivate = (token.key == Key.PRIVATE);
                    }
                } else if (token.key == Key.ABSTRACT || token.key == Key.FINAL) {
                    if (!isClass()) {
                        cFile.erro(token, "Unexpected modifier", this);
                    } else if (isAbstract || isFinal) {
                        cFile.erro(token, "Repeated inheritance modifier", this);
                    } else {
                        isAbstract = (token.key == Key.ABSTRACT);
                        isFinal = (token.key == Key.FINAL);
                    }
                } else if (token.key == Key.STATIC) {
                    if (!isStruct()) {
                        cFile.erro(token, "Unexpected modifier", this);
                    } else if (isStatic) {
                        cFile.erro(token, "Repeated modifier", this);
                    } else {
                        isStatic = true;
                    }
                } else {
                    cFile.erro(token, "Unexpected modifier", this);
                }
            } else if (state == 1 && token.key == Key.WORD) {
                if (token.startsWith('_')) {
                    cFile.erro(nameToken, "Type names cannot start with underline (_)", this);
                } else if (token.isComplex()) {
                    cFile.erro(token, "Complex names are not allowed", this);
                }
                nameToken = token;
                state = 2;
            } else if (state == 2 && token.key == Key.GENERIC && token.getChild() != null) {
                if (isEnum()) {
                    cFile.erro(token, "A enum cannot have genrics", this);
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
                state = 6;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state != 6) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
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
            template.load(this);
        }

        Pointer[] p = template == null ? null : new Pointer[template.getCount()];
        if (p != null) {
            for (int i = 0; i < p.length; i++) {
                p[i] = template.getTypePtr(i);
            }
        }
        self = new Pointer(this, p, false);
    }

    public void internal() {
        if (contentToken != null) {
            Parser.parseMembers(this, contentToken.start, contentToken.end);

            if (staticConstructor != null) {
                hasStaticInit = true;
            } else {
                for (Property property : properties) {
                    if (property.isInitialized()) {
                        hasStaticInit = true;
                    }
                }
                if (!hasStaticInit) {
                    loop:
                    for (Variable variable : variables) {
                        for (int i = 0; i < variable.count(); i++) {
                            if (variable.isInitialized(i) && !variable.isLiteral(i)) {
                                hasStaticInit = true;
                                break loop;
                            }
                        }
                    }
                }
            }
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
                        if (pMW.isFinal()) cFile.erro(erro, "Cannot override a final method", this);
                        if (!pMW.canAcess(this)) {
                            pMW.canAcess(this);
                            cFile.erro(erro, "Cannot acess", this);
                        }
                        if ((pMW.isPublic() && !mw.isPublic()) || (!pMW.isPrivate() && mw.isPrivate())) {
                            cFile.erro(erro, "Incompatible acess", this);
                        }
                        mw.markOverrided(pMW);
                        impl = mw;
                        add = false;
                    } else if (!mw.canOverload(pMW)) {
                        cFile.erro(erro, "Incompatible signature [" + pMW.getName() + "]", this);
                        add = false;
                        break;
                    }
                }

                if (!isAbstract() && pMW.isAbstract() && impl == null) {
                    cFile.erro(parentTokens.get(i), "Abstract method not implemented [" + pMW.getName() + "]", this);
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
                            if (pIW.isGetFinal()) cFile.erro(erro, "Cannot override a final GET", this);
                            else if (!pIW.canAcessGet(this)) cFile.erro(erro, "Cannot acess GET", this);
                            else if (iw.hasGet() && (iw.isGetPrivate() || (!iw.isGetPublic() && pIW.isGetPublic()))) {
                                cFile.erro(erro, "Incompatible GET acess", this);
                            } else if (!iw.hasGet()) {
                                if (!pIW.isGetAbstract()) iw.setGetSource(pIW);
                            } else if (pIW.isGetAbstract()) {
                                implGet = true;
                                implIndexerGet = iw.srcGet;
                            }
                        }

                        if (pIW.hasSet()) {
                            if (pIW.isSetFinal()) cFile.erro(erro, "Cannot override a final SET", this);
                            else if (!pIW.canAcessSet(this)) cFile.erro(erro, "Cannot acess SET", this);
                            else if (iw.hasSet() && (iw.isSetPrivate() || (!iw.isSetPublic() && pIW.isSetPublic()))) {
                                cFile.erro(erro, "Incompatible SET acess", this);
                            } else if (!iw.hasSet()) {
                                if (!pIW.isSetAbstract()) iw.setSetSource(pIW);
                            } else if (pIW.isSetAbstract()) {
                                implSet = true;
                                implIndexerSet = iw.srcSet;
                            }
                        }
                        if (pIW.hasOwn()) {
                            if (pIW.isOwnFinal()) cFile.erro(erro, "Cannot override a final OWN", this);
                            else if (!pIW.canAcessOwn(this)) cFile.erro(erro, "Cannot acess OWN", this);
                            else if (iw.hasOwn() && (iw.isOwnPrivate() || (!iw.isOwnPublic() && pIW.isOwnPublic()))) {
                                cFile.erro(erro, "Incompatible OWN acess", this);
                            } else if (!iw.hasOwn()) {
                                if (!pIW.isOwnAbstract()) iw.setOwnSource(pIW);
                            } else if (pIW.isOwnAbstract()) {
                                implOwn = true;
                                implIndexerOwn = iw.srcOwn;
                            }
                        }
                        add = false;
                    } else if (!iw.canOverload(pIW)) {
                        cFile.erro(erro, "Incompatible signature [ " + pIW.getParams() + " ]", this);
                        add = false;
                        break;
                    }
                }

                if (!isAbstract() && pIW.hasGet() && pIW.isGetAbstract() && !implGet) {
                    cFile.erro(parentTokens.get(i), "Abstract indexer 'GET' not implemented [" + pIW.getParams() + "]", this);
                } else if (implIndexerGet != null && implIndexerGet.indexer.type != this) {
                    indexerGetViewImpl.add(implIndexerGet);
                }
                if (!isAbstract() && pIW.hasSet() && pIW.isSetAbstract() && !implSet) {
                    cFile.erro(parentTokens.get(i), "Abstract indexer 'SET' not implemented [" + pIW.getParams() + "]", this);
                } else if (implIndexerSet != null && implIndexerSet.indexer.type != this) {
                    indexerSetViewImpl.add(implIndexerSet);
                }
                if (!isAbstract() && pIW.hasOwn() && pIW.isOwnAbstract() && !implOwn) {
                    cFile.erro(parentTokens.get(i), "Abstract indexer 'OWN' not implemented [" + pIW.getParams() + "]", this);
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
                if (fw != null) {
                    Token erro = fw.isFrom(this) ? fw.getName() : parentTokens.get(i);
                    add = false;
                    if (fw.canOverride(pFW) && !fw.isStatic()) {
                        if (pFW.hasGet()) {
                            if (pFW.isGetFinal()) cFile.erro(erro, "Cannot override a final GET", this);
                            else if (!pFW.canAcessGet(this)) cFile.erro(erro, "Cannot acess GET", this);
                            else if (fw.hasGet() && (fw.isGetPrivate() || (!fw.isGetPublic() && pFW.isGetPublic()))) {
                                cFile.erro(erro, "Incompatible GET acess", this);
                            } else if (!fw.hasGet()) {
                                if (!pFW.isGetAbstract()) fw.setGetSource(pFW);
                            } else if (pFW.isGetAbstract()) {
                                implGet = true;
                                implFieldGet = fw.srcGet;
                            }
                        }

                        if (pFW.hasSet()) {
                            if (pFW.isSetFinal()) cFile.erro(erro, "Cannot override a final SET", this);
                            else if (!pFW.canAcessSet(this)) cFile.erro(erro, "Cannot acess SET", this);
                            else if (fw.hasSet() && (fw.isSetPrivate() || (!fw.isSetPublic() && pFW.isSetPublic()))) {
                                cFile.erro(erro, "Incompatible SET acess", this);
                            } else if (!fw.hasSet()) {
                                if (!pFW.isSetAbstract()) fw.setSetSource(pFW);
                            } else if (pFW.isSetAbstract()) {
                                implSet = true;
                                implFieldSet = fw.srcSet;
                            }
                        }
                        if (pFW.hasOwn()) {
                            if (pFW.isOwnFinal()) cFile.erro(erro, "Cannot override a final OWN", this);
                            else if (!pFW.canAcessOwn(this)) cFile.erro(erro, "Cannot acess OWN", this);
                            else if (fw.hasOwn() && (fw.isOwnPrivate() || (!fw.isOwnPublic() && pFW.isOwnPublic()))) {
                                cFile.erro(erro, "Incompatible OWN acess", this);
                            } else if (!fw.hasOwn()) {
                                if (!pFW.isOwnAbstract()) fw.setOwnSource(pFW);
                            } else if (pFW.isOwnAbstract()) {
                                implOwn = true;
                                implFieldOwn = fw.srcOwn;
                            }
                        }
                    } else /*if (!fw.canShadow(pFW))*/ {
                        cFile.erro(erro, "Incompatible signature [ " + pFW.getName() + " ]", this);
                    }
                }

                if (!isAbstract() && pFW.hasGet() && pFW.isGetAbstract() && !implGet) {
                    cFile.erro(parentTokens.get(i), "Abstract property 'GET' not implemented [" + pFW.getName() + "]", this);
                } else if (implFieldGet != null && implFieldGet.type != this){
                    propertyGetViewImpl.add(implFieldGet);
                }
                if (!isAbstract() && pFW.hasSet() && pFW.isSetAbstract() && !implSet) {
                    cFile.erro(parentTokens.get(i), "Abstract property 'SET' not implemented [" + pFW.getName() + "]", this);
                } else if (implFieldSet != null && implFieldSet.type != this){
                    propertySetViewImpl.add(implFieldSet);
                }
                if (!isAbstract() && pFW.hasOwn() && pFW.isOwnAbstract() && !implOwn) {
                    cFile.erro(parentTokens.get(i), "Abstract property 'OWN' not implemented [" + pFW.getName() + "]", this);
                } else if (implFieldOwn != null && implFieldOwn.type != this){
                    propertyOwnViewImpl.add(implFieldOwn);
                }
                if (add) fields.put(pFW.getName(), pFW);
            }
        }

        if (parent != null && isClass()) {
              if (parentEmptyConstructor == null) {
                    Constructor parentEmpty = parent.type.emptyConstructor;
                    if (parentEmpty == null && parent.type.constructors.size() == 0) {
                        parentEmpty = parent.type.parentEmptyConstructor;
                    }

                    if (parentEmpty != null && parentEmpty.isPublic()) {
                        parentEmptyConstructor = parentEmpty;
                    }
                if (parentEmptyConstructor == null && constructors.size() == 0) {
                    cFile.erro(nameToken, "The class should implement a valid constructor", this);
                }
            }
            for (Constructor pC : parent.type.constructors) {
                if (pC.isDefault() && pC.isPublic()) {
                    boolean isImplemented = false;
                    ParamView pCW = new ParamView(parent, pC.getParams());
                    for (Constructor cC : constructors) {
                        if (!cC.isStatic() && cC.getParams().canOverride(pCW)) {
                            if (!cC.isPublic()) {
                                cFile.erro(cC.token, "A Default constructor implementation must be public", this);
                            }
                            cC.markDefault();
                            isImplemented = true;
                            break;
                        }
                    }
                    if (!isImplemented) {
                        cFile.erro(nameToken, "Default constructor not implemented", this);
                    }
                }
            }
        }
    }

    public void make() {
        for (Constructor constructor : constructors) {
            constructor.make();
        }
        if (staticConstructor != null) {
            staticConstructor.make();
        }
        if (destructor != null) {
            destructor.make();
        }

        for (Method method : methods) {
            if (!method.isAbstract()) {
                method.make();
            }
        }
        for (Variable variable : variables) {
            variable.make();
        }
        for (Property property : properties) {
            property.make();
        }
        for (Indexer indexer : indexers) {
            indexer.make();
        }
        for (Operator operator : operators) {
            operator.make();
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

        if (hasGenericFile()) {
            cBuilder.toGeneric();
            cBuilder.add("// ").add(fileName).add(".hpp").ln()
                    .ln()
                    .add("#ifndef HPP_").add(fileName.toUpperCase()).ln()
                    .add("#define HPP_").add(fileName.toUpperCase()).ln();
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
                .idt(1).add("using L = ").add(isPointer(), "Let<").path(self, false).add(isPointer() ? ">;" : ";").ln();
        if (isValue()) {
            cBuilder.idt(1).add("using W = ").path(parent, false).add(";").ln();
        }
        cBuilder.idt(1).add("static lang::type* typeOf() { return getType<T_").add(fileName.toUpperCase()).add(">(); }").ln()
                .idt(1).add(isPointer(), "virtual lang::type* getTypeOf() { return typeOf(); }").ln();

        if (isClass()) cBuilder.idt(1).add("virtual lang_Object* self() { return this; }").ln();

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
                    variable.buildDefault(cBuilder);
                }
            }
            cBuilder.add(" {\n}").ln()
                    .ln();
        }
        for (Constructor constructor : constructors) {
            constructor.build(cBuilder);
        }

        // Destructor
        if (destructor != null) {
            destructor.build(cBuilder);
        } else if (isClass() && parent != null) {
            cBuilder.idt(1).add("virtual void destroy() { ").path(parent, false).add("::destroy(); }").ln();
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
            if (!operator.isCasting()) {
                operator.build(cBuilder);
            }
        }

        cBuilder.toHeader();
        cBuilder.add("};").ln()
                .ln();

        for (Operator operator : operators) {
            if (operator.isCasting()) {
                operator.build(cBuilder);
            }
        }

        // Static Members
        cBuilder.toHeader();
        cBuilder.add("class ").add(staticPathToken).add(" {").ln()
                .add("public :").ln();
        if (hasStaticInit()) {
            cBuilder.idt(1).add("static void init();").ln()
                    .idt(1).add("static bool initBlock();").ln();

            cBuilder.toSource();
            cBuilder.add("void ").path(self, true).add("::init() {").ln()
                    .idt(1).add("static bool _init = initBlock();").ln()
                    .add("}").ln()
                    .ln();

            cBuilder.add("bool ").path(self, true).add("::initBlock() ").in(1);
            for (Variable variable : variables) {
                if (variable.isStatic()) {
                    variable.buildInit(cBuilder);
                }
            }
            for (Property property : properties) {
                if (property.isStatic()) {
                    property.buildInit(cBuilder);
                }
            }
            if (staticConstructor != null) {
                staticConstructor.build(cBuilder);
            }
            cBuilder.idt(1).add("return true;").ln()
                    .out().ln()
                    .ln();
        }

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

        if (hasGenericFile()) {
            cBuilder.toGeneric();
            cBuilder.add("#endif");
        }
    }

    public boolean hasStaticInit() {
        return hasStaticInit;
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

    public boolean hasGenericFile() {
        return hasGeneric || autoCast.size() > 0  || casts.size() > 0;
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
                            inheritanceType(iToken, iNext = TokenGroup.nextType(iNext, iEnd));
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
                cFile.erro(variable.token, "Instance variables not allowed", this);
            } else if (isStruct()
                    && variable.getTypePtr().type != null
                    && variable.getTypePtr().type.isStruct()
                    && variable.getTypePtr().type.cyclicVariableVerify(this)) {
                cFile.erro(variable.getTypeToken().start, "Cyclic variable type", this);
            } else if (!variable.isStatic() && isValue() && isStatic() && !variable.getTypePtr().type.isStatic()) {
                cFile.erro(variable.getTypeToken().start, "A Static Type can only have Static Types as it's field", this);
            } else {
                for (FieldView field : variable.getFields()) {
                    if (fields.containsKey(field.nameToken)) {
                        cFile.erro(field.nameToken, "Repeated field name", this);
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
                cFile.erro(property.token, "A abstract property GET cannot have a stronger acess modifier", this);
            }
            if (property.hasSet() && property.isSetAbstract() &&
                    ((property.isSetPrivate() && !isPrivate()) || (!property.isSetPublic() && isPublic()))) {
                cFile.erro(property.token, "A abstract property SET cannot have a strong acess modifier", this);
            }
            if (property.hasOwn() && property.isOwnAbstract() &&
                    ((property.isOwnPrivate() && !isPrivate()) || (!property.isOwnPublic() && isPublic()))) {
                cFile.erro(property.token, "A abstract property OWN cannot have a strong acess modifier", this);
            }
            FieldView field = property.getField();
            if (fields.containsKey(field.nameToken)) {
                cFile.erro(field.nameToken, "Repeated field name", this);
            } else {
                fields.put(field.nameToken, field);
            }
            properties.add(property);
        }
    }

    public void add(Num num) {
        if (num.load()) {
            if (!isEnum()) {
                cFile.erro(num.token, "Unexpected enumeration", this);
            } else {
                for (FieldView field : num.getFields()) {
                    if (fields.containsKey(field.nameToken)) {
                        cFile.erro(field.nameToken, "Repeated field name", this);
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
                cFile.erro(method.getName(), "A abstract method cannot have a strong acess modifier than the type", this);
            }
            for (MethodView methodB : methodView.get(method.getName())) {
                if (!method.getParams().canOverload(methodB.method.getParams())) {
                    cFile.erro(method.getName(), "Invalid overloading", this);
                    return;
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
                cFile.erro(indexer.token, "A abstract indexer GET cannot have a stronger acess modifier", this);
            }
            if (indexer.hasSet() && indexer.isSetAbstract() &&
                    ((indexer.isSetPrivate() && !isPrivate()) || (!indexer.isSetPublic() && isPublic()))) {
                cFile.erro(indexer.token, "A abstract indexer SET cannot have a strong acess modifier", this);
            }
            if (indexer.hasOwn() && indexer.isOwnAbstract() &&
                    ((indexer.isOwnPrivate() && !isPrivate()) || (!indexer.isOwnPublic() && isPublic()))) {
                cFile.erro(indexer.token, "A abstract indexer OWN cannot have a strong acess modifier", this);
            }
            for (Indexer indexerB : indexers) {
                if (!indexer.getParams().canOverload(indexerB.getParams())) {
                    cFile.erro(indexer.token, "Invalid overloading", this);
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
                cFile.erro(operator.token, "Operators not allowed", this);
            } else {
                for (Operator operatorB : operators) {
                    if (operator.op == operatorB.op
                            || (operator.op == Key.CAST && operatorB.op == Key.AUTO)
                            || (operator.op == Key.AUTO && operatorB.op == Key.CAST)) {

                        if (operator.op == Key.CAST || operator.op == Key.AUTO) {
                            if (operator.typePtr.equals(operatorB.typePtr)) {
                                cFile.erro(operator.token, "Invalid overloading", this);
                                return;
                            }
                        } else if (!operator.params.canOverload(operatorB.params)) {
                            cFile.erro(operator.token, "Invalid overloading", this);
                            return;
                        }
                    }
                }
                operators.add(operator);

                if (operator.op == Key.AUTO) {
                    autoCast.add(operator.getTypePtr());
                } else if (operator.op == Key.CAST) {
                    casts.add(operator.getTypePtr());
                } else {
                    operatorView.put(operator.getOperator(), new OperatorView(self, operator));
                }
            }
        }
    }

    public void add(Constructor constructor) {
        if (constructor.load()) {
            if (isInterface() && !constructor.isStatic()) {
                cFile.erro(constructor.token, "Instance constructors not allowed", this);
            } else if (constructor.isStatic()) {
                if (staticConstructor != null) {
                    cFile.erro(constructor.token, "Repeated static constructor", this);
                } else if (constructor.isDefault()) {
                    cFile.erro(constructor.token, "A Default constructor cannot be static", this);
                } else {
                    if (constructor.isPrivate()) {
                        cFile.erro(constructor.token, "Static constructors are always public", this);
                    }
                    staticConstructor = constructor;
                }
            } else {
                if (constructor.isDefault() && !constructor.isPublic()) {
                    cFile.erro(constructor.token, "A Default constructor must be public", this);
                }
                for (Constructor constructorB : constructors) {
                    if (!constructor.getParams().canOverload(constructorB.getParams())) {
                        cFile.erro(constructor.token, "Invalid overloading", this);
                        return;
                    }
                }

                if (constructor.getParams().getCount() == 0) {
                    emptyConstructor = constructor;
                }
                constructors.add(constructor);
            }
        }
    }

    public void add(Destructor destructor) {
        if (destructor.load()) {
            if (this.destructor != null) {
                cFile.erro(destructor.token, "Repeated destructor", this);
            } else if (!isClass()) {
                cFile.erro(destructor.token, "Destructors not allowed", this);
            } else {
                this.destructor = destructor;
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
            return field;
        }
    }

    public ArrayList<MethodView> getMethod(Token nameToken) {
        return methodView.get(nameToken);
    }

    public int getIndexersCount() {
        return indexerView.size();
    }

    public IndexerView getIndexer(int index) {
        return indexerView.get(index);
    }

    public int getOperatorsCount() {
        return operators.size();
    }

    public ArrayList<OperatorView> getOperator(Token operatorToken) {
        return operatorView.get(operatorToken);
    }

    public Operator getOperator(int index) {
        if (index < operators.size()) {
            return operators.get(index);
        } else {
            return null;
        }
    }

    public int getConstructorsCount() {
        return constructors.size();
    }

    public Constructor getConstructor(int index) {
        if (index < constructors.size()) {
            return constructors.get(index);
        } else {
            return null;
        }
    }

    public Constructor getEmptyConstructor() {
        return emptyConstructor;
    }

    public Constructor getParentEmptyConstructor() {
        return parentEmptyConstructor;
    }

    @Override
    public String toString() {
        return nameToken == null ? "[empty]" : nameToken.toString();
    }
}
