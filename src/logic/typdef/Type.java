package logic.typdef;

import content.Key;
import content.Parser;
import content.Token;
import content.TokenGroup;
import data.ContentFile;
import builder.CppBuilder;
import logic.GenericOwner;
import logic.ViewList;
import logic.member.view.*;
import logic.stack.expression.LambdaCall;
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
    private Constructor syncConstructor;
    private Constructor emptyConstructor;
    private Constructor parentEmptyConstructor;
    private Operator equal, different;

    private ArrayList<Type> inheritanceTypes = new ArrayList<>();
    private boolean isPrivate, isPublic, isAbstract, isFinal, isSync;
    boolean isLoaded, isCrossed, isBase, isFunction, hasGeneric, hasInstanceVars,
            hasInstanceInit, hasStaticInit, hasSyncInit, hasStatic;

    private HashMap<Token, FieldView> fields = new HashMap<>();
    private ViewList<MethodView> methodView = new ViewList<>();
    private ViewList<OperatorView> operatorView = new ViewList<>();
    private ArrayList<IndexerView> indexerView = new ArrayList<>();
    private ArrayList<Token> overloadMethods = new ArrayList<>();
    private boolean overloadGet, overloadSet, overloadOwn;

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
                } else if (token.key == Key.SYNC) {
                    if (!isStruct()) {
                        cFile.erro(token, "Unexpected modifier", this);
                    } else if (isSync) {
                        cFile.erro(token, "Repeated modifier", this);
                    } else {
                        isSync = true;
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
            isSync = true;
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
            isFunction = nameToken.equals("function");
            isBase = nameToken.equals("bool")
                    || nameToken.equals("byte")
                    || nameToken.equals("short")
                    || nameToken.equals("int")
                    || nameToken.equals("long")
                    || nameToken.equals("float")
                    || nameToken.equals("double")
                    || isFunction;
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
            }
            if (syncConstructor != null || nums.size() > 0) {
                hasSyncInit = true;
            }
            for (Property property : properties) {
                hasInstanceVars = hasInstanceVars || (!property.isStatic() && property.isAuto());
                if (property.isInitialized()) {
                    hasInstanceInit = hasInstanceInit || !property.isStatic();
                    hasStaticInit = hasStaticInit || property.isStatic();
                }
            }
            for (Variable variable : variables) {
                hasInstanceVars = hasInstanceVars || !variable.isStatic();
                for (int i = 0; i < variable.getCount(); i++) {
                    if (variable.isInitialized(i) && !variable.isLiteral(i)) {
                        hasInstanceInit = hasInstanceInit || !variable.isStatic();
                        hasStaticInit = hasStaticInit || (variable.isStatic() && !variable.isSync());
                        hasSyncInit = hasSyncInit || variable.isSync();
                    }
                }
            }
        }
    }

    public void cross() {
        if (isCrossed) return;
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
                    } else {
                        if (!overloadMethods.contains(mw.getName())) overloadMethods.add(mw.getName());
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
                                implIndexerGet = iw.getSourceGet();
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
                                implIndexerSet = iw.getSourceSet();
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
                                implIndexerOwn = iw.getSourceOwn();
                            }
                        }
                        add = false;
                    } else if (!iw.canOverload(pIW)) {
                        cFile.erro(erro, "Incompatible signature [ " + pIW.getParams() + " ]", this);
                        add = false;
                        break;
                    } else {
                        overloadGet = overloadGet || iw.hasGet();
                        overloadSet = overloadSet || iw.hasSet();
                        overloadOwn = overloadOwn || iw.hasOwn();
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
                                if (!pFW.isGetAbstract()) fw.setSourceGet(pFW);
                            } else if (pFW.isGetAbstract()) {
                                implGet = true;
                                implFieldGet = fw.getSourceGet();
                            }
                        }

                        if (pFW.hasSet()) {
                            if (pFW.isSetFinal()) cFile.erro(erro, "Cannot override a final SET", this);
                            else if (!pFW.canAcessSet(this)) cFile.erro(erro, "Cannot acess SET", this);
                            else if (fw.hasSet() && (fw.isSetPrivate() || (!fw.isSetPublic() && pFW.isSetPublic()))) {
                                cFile.erro(erro, "Incompatible SET acess", this);
                            } else if (!fw.hasSet()) {
                                if (!pFW.isSetAbstract()) fw.setSourceSet(pFW);
                            } else if (pFW.isSetAbstract()) {
                                implSet = true;
                                implFieldSet = fw.getSourceSet();
                            }
                        }
                        if (pFW.hasOwn()) {
                            if (pFW.isOwnFinal()) cFile.erro(erro, "Cannot override a final OWN", this);
                            else if (!pFW.canAcessOwn(this)) cFile.erro(erro, "Cannot acess OWN", this);
                            else if (fw.hasOwn() && (fw.isOwnPrivate() || (!fw.isOwnPublic() && pFW.isOwnPublic()))) {
                                cFile.erro(erro, "Incompatible OWN acess", this);
                            } else if (!fw.hasOwn()) {
                                if (!pFW.isOwnAbstract()) fw.setSourceOwn(pFW);
                            } else if (pFW.isOwnAbstract()) {
                                implOwn = true;
                                implFieldOwn = fw.getSourceOwn();
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
                            cC.toDefault();
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

        if (isStruct() && !isFunction()) {
            FieldView field = parent.type.getField(new Token("value"));
            if (field == null || !field.isVariable() || field.isStatic() ||
                    !field.srcVar.isPublic() || !Pointer.byGeneric(field.getTypePtr(), parent).equals(self)) {
                cFile.erro(parentTokens.get(0), "The Wrapper Must have a public local variable named 'value' of this Struct as Type", this);
            }

            boolean found = false;
            for (int i = 0; i < parent.type.getConstructorsCount(); i++) {
                Constructor constructor = parent.type.getConstructor(i);
                if (constructor.getParams().getCount() == 1 &&
                        Pointer.byGeneric(constructor.getParams().getTypePtr(i), parent).equals(self)) {
                    found = true;
                    if (!constructor.isPublic()) {
                        cFile.erro(parentTokens.get(0), "The Wrapper Must have a public constructor as this Struct as unic parameter", this);
                    }
                }
            }
            if (!found) {
                cFile.erro(parentTokens.get(0), "The Wrapper Must have a public constructor as this Struct as unic parameter", this);
            }
        }

        for (Operator operator : operators) {
            if (operator.getParams().getCount() == 2) {
                Pointer ptrA = operator.getParams().getTypePtr(0);
                Pointer ptrB = operator.getParams().getTypePtr(1);
                Pointer find = ptrA.type != this ? ptrA : ptrB.type != this ? ptrB : null;
                if (find != null) {
                    for (OperatorView other : find.type.getOperator(operator.token)) {
                        if (other.getParams().getArgsCount() == 2) {
                            ParamView pv = new ParamView(find, other.getParams());
                            if (pv.getArgTypePtr(0).equalsIgnoreLet(operator.getParams().getTypePtr(0)) &&
                                    pv.getArgTypePtr(1).equalsIgnoreLet(operator.getParams().getTypePtr(1))) {
                                cFile.erro(operator.token, "Ambigous operator between Structs [" + find.type + "]", this);
                            }
                        }
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
        for (Num num : nums) {
            num.make();
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
                .add("#ifndef H_").up(fileName).ln()
                .add("#define H_").up(fileName).ln()
                .ln()
                .add("#include \"langCore.h\"").ln();
        cBuilder.markHeader();
        cBuilder.ln();

        cBuilder.add("#define T_").up(fileName).add(" ");
        ArrayList<Pointer> allParents = new ArrayList<>(parents.size() * 2);
        cBuilder.path(self);
        if (!isInterface()) {
            recursiveGetParent(self, allParents);
            for (Pointer p : allParents) {
                cBuilder.add(", ").path(p);
            }
        }
        cBuilder.ln()
                .ln();
        allParents = null;

        for (Native nat : natives) {
            if (nat.isMacro()) nat.build(cBuilder);
        }

        cBuilder.toSource();
        cBuilder.add("// ").add(fileName).add(".cpp").ln();
        cBuilder.markSource();
        cBuilder.ln();

        if (hasGenericFile()) {
            cBuilder.toGeneric();
            cBuilder.add("// ").add(fileName).add(".hpp").ln()
                    .ln()
                    .add("#ifndef HPP_").up(fileName).ln()
                    .add("#define HPP_").up(fileName).ln();
            cBuilder.markGeneric();
            cBuilder.ln();
        }

        // Class Statment
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

        // Type Definitions
        cBuilder.idt(1).add("// Type").ln()
                .idt(1).add("using P = ").add(isPointer(), "Ptr<").path(self).add(isPointer() ? ">;" : ";").ln()
                .idt(1).add("using L = ").add(isPointer(), "Let<").path(self).add(isPointer() ? ">;" : ";").ln();
        if (isValue()) {
            cBuilder.idt(1).add("using W = ").path(parent).add(";").ln();
        }
        cBuilder.idt(1).add("static lang::type* typeOf() { return getType<T_").up(fileName).add(">(); }").ln()
                .idt(1).add(isPointer(), "virtual "). add("lang::type* getTypeOf() { return typeOf(); }").ln();
        if (isClass()) cBuilder.idt(1).add("virtual lang_Object* self() { return this; }").ln();

        // Pointer Transference
        if (!isInterface()) {
            cBuilder.toHeader();
            cBuilder.idt(1).add(isPointer(), "virtual ").add("void transferOut();").ln()
                    .idt(1).add(isPointer(), "virtual ").add("void transferIn();").ln()
                    .idt(1).add(isPointer(), "virtual ").add("void clear();").ln();

            cBuilder.toSource(template != null);

            // Transfer OUT [Strong[+Open], Non-Sync Struct]
            cBuilder.add(template)
                    .add("void ").path(self).add("::transferOut() {").ln();
            for (FieldView field : fields.values()) {
                Pointer ptr = field.getTypePtr();
                if (!field.isStatic() && (field.isVariable() || !field.isAutoProperty()) && !ptr.let && !ptr.isSync()) {
                    if (ptr.isOpen()) {
                        cBuilder.idt(1).add("transfer<").add(ptr).add(">::out(").nameField(field.getName()).add(");").ln();
                    } else {
                        cBuilder.idt(1).add("this->").nameField(field.getName()).add(".transferOut();").ln();
                    }
                }
            }
            cBuilder.add("}").ln();

            // Transfer In [Strong[+Open], Non-Sync Struct]
            cBuilder.add(template)
                    .add("void ").path(self).add("::transferIn() {").ln();
            for (FieldView field : fields.values()) {
                Pointer ptr = field.getTypePtr();
                if (!field.isStatic() && (field.isVariable() || !field.isAutoProperty()) && !ptr.let && !ptr.isSync()) {
                    if (ptr.isOpen()) {
                        cBuilder.idt(1).add("transfer<").add(ptr).add(">::in(").nameField(field.getName()).add(");").ln();
                    } else {
                        cBuilder.idt(1).add("this->").nameField(field.getName()).add(".transferIn();").ln();
                    }
                }
            }
            cBuilder.add("}").ln();

            // Transfer Clear [Strong/Weak[+Open], Non-Sync Struct]
            cBuilder.add(template)
                    .add("void ").path(self).add("::clear() {").ln();
            for (FieldView field : fields.values()) {
                Pointer ptr = field.getTypePtr();
                if (!field.isStatic() && (field.isVariable() || !field.isAutoProperty()) && !ptr.isSync()) {
                    if (ptr.isOpen()) {
                        cBuilder.idt(1).add("transfer<").add(ptr).add(">::clear(").nameField(field.getName()).add(");").ln();
                    } else {
                        cBuilder.idt(1).add("this->").nameField(field.getName()).add(".clear();").ln();
                    }
                }
            }
            cBuilder.add("}").ln();
        }

        // Instance Init
        if (hasInstanceInit()) {
            cBuilder.toHeader();
            cBuilder.idt(1).add("void init();").ln();

            cBuilder.toSource(template != null);
            cBuilder.add(template)
                    .add("void ").path(self).add("::init() ").in(1);
            for (Variable variable : variables) {
                if (!variable.isStatic()) {
                    variable.buildLambdas(cBuilder);    // lambda Fix
                }
            }
            for (Property property : properties) {
                if (!property.isStatic()) {
                    property.buildLambdas(cBuilder);    // lambda Fix
                }
            }
            for (Variable variable : variables) {
                if (!variable.isStatic()) {
                    variable.buildInit(cBuilder);
                }
            }
            for (Property property : properties) {
                if (!property.isStatic()) {
                    property.buildInit(cBuilder);
                }
            }
            cBuilder.out().ln()
                    .ln();
        }

        // Natives
        for (Native nat : natives) {
            if (nat.isSource() || nat.isHeader()) {
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
        if (hasInstanceVars() || isValue()) {
            cBuilder.toHeader();
            cBuilder.idt(1).add(pathToken).add("();").ln();

            cBuilder.toSource(hasGeneric());
            cBuilder.add(template)
                    .path(self).add("::").add(pathToken).add("()");
            boolean first = true;
            for (Variable variable : variables) {
                if (!variable.isStatic()) {
                    cBuilder.add(first ? " : " : ", ").ln();
                    first = false;
                    variable.buildDefault(cBuilder);
                }
            }
            for (Property property : properties) {
                if (!property.isStatic() && property.isAuto()) {
                    cBuilder.add(first ? " : " : ", ").ln();
                    first = false;
                    property.buildDefault(cBuilder);
                }
            }
            cBuilder.add(" {\n}").ln()
                    .ln();
        }

        if (isEnum()) {
            cBuilder.toHeader();
            cBuilder.idt(1).add(cFile.langIntPtr()).add(" value;").ln();
            cBuilder.idt(1).path(self).add("(empty e, ").add(cFile.langIntPtr()).add(" v) : value(v) {}").ln();
        }

        for (Constructor constructor : constructors) {
            constructor.build(cBuilder);
        }
        if (constructors.size() == 0 && parentEmptyConstructor != null) {
            parentEmptyConstructor.buildEmpty(this, cBuilder);
        }

        // Destructor
        if (destructor != null) {
            destructor.build(cBuilder);
        } else if (isClass() && parent != null && parents.size() > 1) {
            cBuilder.toHeader();
            cBuilder.idt(1).add("virtual void destroy() { ").path(parent).add("::destroy(); }").ln();
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
        if (isValue()) {
            Operator.buildAutomatic(cBuilder, this, equal, different);

            for (Operator operator : operators) {
                operator.build(cBuilder);
            }
        }

        // Overload Name Hide Fix [Method]
        cBuilder.toHeader();
        for (Token token : overloadMethods) {
            cBuilder.idt(1).add("using ").path(parent).add("::").nameMethod(token).add(";").ln();
        }

        // Overload Name Hide Fix [Indexer]
        if (overloadGet) cBuilder.idt(1).add("using ").path(parent).add("::").nameGet().add(";").ln();
        if (overloadSet) cBuilder.idt(1).add("using ").path(parent).add("::").nameSet().add(";").ln();
        if (overloadOwn) cBuilder.idt(1).add("using ").path(parent).add("::").nameOwn().add(";").ln();

        cBuilder.toHeader();
        cBuilder.add("};").ln()
                .ln();

        // Static Members
        if (hasStatic) {
            cBuilder.toHeader();
            cBuilder.add("class ").add(staticPathToken).add(" {").ln()
                    .add("public :").ln();

            // Static Constructors
            if (hasStaticInit()) {
                cBuilder.toHeader();
                cBuilder.idt(1).add("static void init();").ln()
                        .idt(1).add("static bool initBlock();").ln();

                cBuilder.toSource();
                cBuilder.add("void ").path(self, true).add("::init() {").ln()
                        .idt(1).add("static thread_local bool _init = initBlock();").ln()
                        .add("}").ln()
                        .ln();

                cBuilder.add("bool ").path(self, true).add("::initBlock() ").in(1);
                for (Variable variable : variables) {
                    if (variable.isStatic() && !variable.isSync()) {
                        variable.buildLambdas(cBuilder);    // lambda Fix
                    }
                }
                for (Property property : properties) {
                    if (property.isStatic()) {
                        property.buildLambdas(cBuilder);    // lambda Fix
                    }
                }
                for (Variable variable : variables) {
                    if (variable.isStatic() && !variable.isSync()) {
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
                cBuilder.toSource();
                cBuilder.idt(1).add("return true;").ln()
                        .out().ln()
                        .ln();
            }

            // Static Sync Constructors
            if (hasSyncInit) {
                cBuilder.toHeader();
                cBuilder.idt(1).add("static void syncInit();").ln()
                        .idt(1).add("static bool syncInitBlock();").ln();

                cBuilder.toSource();
                cBuilder.add("void ").path(self, true).add("::syncInit() {").ln()
                        .idt(1).add("static bool _init = syncInitBlock();").ln()
                        .add("}").ln()
                        .ln();

                cBuilder.add("bool ").path(self, true).add("::syncInitBlock() ").in(1);
                for (Num num : nums) {
                    num.buildLambdas(cBuilder);
                }
                for (Variable variable : variables) {
                    if (variable.isSync()) {
                        variable.buildLambdas(cBuilder);    // lambda Fix
                    }
                }
                for (Num num : nums) {
                    num.buildInit(cBuilder);
                }
                for (Variable variable : variables) {
                    if (variable.isSync()) {
                        variable.buildInit(cBuilder);
                    }
                }
                if (syncConstructor != null) {
                    syncConstructor.build(cBuilder);
                }

                cBuilder.toSource();
                cBuilder.idt(1).add("return true;").ln()
                        .out().ln()
                        .ln();
            }

            for (Native nat : natives) {
                if (nat.isStatic()) {
                    nat.build(cBuilder);
                }
            }
            for (Num num : nums) {
                num.build(cBuilder);
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
            cBuilder.add("};").ln()
                    .ln();
        }

        // External Operators [Casting, Equal, Different]
        if (isValue()) {
            Operator.buildAutomaticOperator(cBuilder, this, equal, different);

            for (Operator operator : operators) {
                operator.buildOperator(cBuilder);
            }
        }

        for (Native nat : natives) {
            if (nat.isExtra()) nat.build(cBuilder);
        }

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

    public boolean hasSyncInit() {
        return hasSyncInit;
    }

    public boolean hasInstanceInit() {
        return hasInstanceInit;
    }

    public boolean hasInstanceVars() {
        return hasInstanceVars;
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

    public boolean isSync() {
        return isSync;
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
            } else if (!variable.isStatic() && isSync() && !variable.getTypePtr().type.isSync()) {
                cFile.erro(variable.getTypeToken().start, "A Sync Type can only have Sync Types as local variables", this);
            } else {
                if (variable.isStatic()) hasStatic = true;

                for (FieldView field : variable.getFields()) {
                    if (fields.containsKey(field.getName())) {
                        cFile.erro(field.getName(), "Repeated field name", this);
                    } else {
                        fields.put(field.getName(), field);
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
            if (property.isStatic()) hasStatic = true;

            FieldView field = property.getField();
            if (fields.containsKey(field.getName())) {
                cFile.erro(field.getName(), "Repeated field name", this);
            } else {
                fields.put(field.getName(), field);
            }
            properties.add(property);
        }
    }

    public void add(Num num) {
        if (num.load()) {
            hasStatic = true;

            if (!isEnum()) {
                cFile.erro(num.token, "Unexpected enumeration", this);
            } else {
                for (FieldView field : num.getFields()) {
                    if (fields.containsKey(field.getName())) {
                        cFile.erro(field.getName(), "Repeated field name", this);
                    } else {
                        fields.put(field.getName(), field);
                    }
                }
                nums.add(num);
            }
        }
    }

    public void add(Method method) {
        if (method.load()) {
            if (method.isStatic()) hasStatic = true;

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
                    if (operator.getOp() == operatorB.getOp()
                            || (operator.getOp() == Key.CAST && operatorB.getOp() == Key.AUTO)
                            || (operator.getOp() == Key.AUTO && operatorB.getOp() == Key.CAST)) {

                        if (operator.getOp() == Key.CAST || operator.getOp() == Key.AUTO) {
                            if (operator.getTypePtr().equals(operatorB.getTypePtr())) {
                                cFile.erro(operator.token, "Invalid overloading", this);
                                return;
                            }
                        } else if (!operator.getParams().canOverload(operatorB.getParams())) {
                            cFile.erro(operator.token, "Invalid overloading", this);
                            return;
                        }
                    }
                }
                operators.add(operator);

                if (operator.getOp() == Key.EQUAL) {
                    if (operator.getParams().getCount() == 2 &&
                            operator.getParams().getTypePtr(0).equals(self) &&
                            operator.getParams().getTypePtr(1).equals(self)) {
                        equal = operator;
                    }
                }

                if (operator.getOp() == Key.DIF) {
                    if (operator.getParams().getCount() == 2 &&
                            operator.getParams().getTypePtr(0).equals(self) &&
                            operator.getParams().getTypePtr(1).equals(self)) {
                        different = operator;
                    }
                }

                if (operator.getOp() == Key.AUTO) {
                    autoCast.add(operator.getTypePtr());
                } else if (operator.getOp() == Key.CAST) {
                    casts.add(operator.getTypePtr());
                } else {
                    operatorView.put(operator.getOperator(), new OperatorView(self, operator));
                }
            }
        }
    }

    public void add(Constructor constructor) {
        if (constructor.load()) {
            if (constructor.isStatic()) hasStatic = true;

            if (isInterface() && !constructor.isStatic()) {
                cFile.erro(constructor.token, "Instance constructors not allowed", this);
            } else if (constructor.isSync()) {
                if (syncConstructor != null) {
                    cFile.erro(constructor.token, "Repeated sync constructor", this);
                } else if (constructor.isDefault()) {
                    cFile.erro(constructor.token, "A Default constructor cannot be sync", this);
                } else {
                    if (constructor.isPrivate()) {
                        cFile.erro(constructor.token, "Sync constructors are always public", this);
                    }
                    syncConstructor = constructor;
                }
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
                if (constructor.isDefault() && isValue()) {
                    cFile.erro(constructor.token, "A Value Type should not have default constructors", this);
                } else if (constructor.isDefault() && !constructor.isPublic()) {
                    cFile.erro(constructor.token, "A Default constructor must be public", this);
                }
                if (isEnum()) {
                    if (constructor.isPublic()) {
                        cFile.erro(constructor.token, "A Enum constructor are always private", this);
                    }
                    constructor.toPrivate();
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
            if (nat.isStatic()) hasStatic = true;
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
