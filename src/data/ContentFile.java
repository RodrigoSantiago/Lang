package data;

import content.*;
import logic.GenericOwner;
import logic.Namespace;
import logic.Pointer;
import logic.Using;
import logic.member.Method;
import logic.typdef.Type;

import java.util.ArrayList;
import java.util.HashSet;

public class ContentFile {

    public Library library;
    public String name;
    public String content;
    public Token token;

    public Namespace namespace;
    public ArrayList<Type> types = new ArrayList<>();
    public ArrayList<Using> usings = new ArrayList<>();
    public ArrayList<Using> usingsNormal = new ArrayList<>();
    public ArrayList<Using> usingsDirect = new ArrayList<>();
    public ArrayList<Using> usingsStatic = new ArrayList<>();
    public ArrayList<Using> usingsStaticDirect = new ArrayList<>();

    public Method method;
    public TokenGroup mainMethodToken;

    public ArrayList<Error> erros = new ArrayList<>();

    private int state;
    private boolean invalid;
    private final HashSet<ContentFile> usedBy = new HashSet<>();

    public ContentFile(Library library, String name, String content) {
        this.library = library;
        this.name = name;
        this.content = content;
        invalidate();
    }

    public void read() {
        if (token == null) {
            Lexer lexer = new Lexer(this);
            token = lexer.read();
        }

        if (token != null) {
            Parser.parseWorkspace(this, token);

            if (namespace == null) {
                namespace = library.getNamespace(null);
                namespace.mark(this);
            }
        }

        invalid = false;
        state = 1;
    }

    public void preload() {
        for (int i = 0; i < usings.size(); i++) {
            Using using = usings.get(i);
            using.preload();
            if (!using.isUsable()) {
                if (using.isDirect() && using.isStatic()) {
                    usingsStaticDirect.remove(using);
                } else if (using.isDirect()) {
                    usingsDirect.remove(using);
                } else if (using.isStatic()) {
                    usingsStatic.remove(using);
                } else {
                    usingsNormal.remove(using);
                }
                usings.remove(i--);
            }
        }

        for (Type type : types) {
            type.preload();
        }

        state = 2;
    }

    public void load() {
        for (Type type : types) {
            type.load();
        }

        state = 3;
    }

    public void internal() {
        for (Type type : types) {
            type.internal();
        }

        if (mainMethodToken != null) {
            method = new Method(langObject(), this, mainMethodToken.start, mainMethodToken.end);
            method.toStatic();
            if (method.getName() != null && !method.getName().equals("main")) {
                erro(method.getName(), "The Main Method should be named 'main'", this);
            }
        }
        state = 4;
    }

    public void cross() {
        for (Type type : types) {
            type.cross();
        }

        if (method != null) {
            if (!method.load()) {
                method = null;
            } else {
                if (method.getTypePtr() != Pointer.voidPointer) {
                    erro(method.getTypeToken(), "The Main Method should return void", this);
                }
                if (!method.getParams().isEmpty()) {
                    erro(method.getParams().token, "The Main Method cannot have parameters", this);
                }
            }
        }
        state = 5;
    }

    public void make() {
        for (Type type : types) {
            type.make();
        }

        if (method != null) {
            method.make();
        }
        state = 5;
    }

    public void unload() {
        for (Type type : types) {
            namespace.remove(type);
        }

        if (namespace.unmark(this)) {
            library.delNamespace(namespace.name);
        }
        namespace = null;

        usings.clear();
        usingsNormal.clear();
        usingsDirect.clear();
        usingsStatic.clear();
        usingsStaticDirect.clear();
        erros.clear();

        types.clear();

        state = 0;
    }

    public void invalidate() {
        if (!invalid) {
            invalid = true;

            for (ContentFile cFile : usedBy) {
                cFile.invalidate();
            }

            if (namespace != null) {
                namespace.invalidate();
            }
        }
    }

    public boolean isInvalided() {
        return invalid;
    }

    public int getState() {
        return state;
    }

    public void setNamespace(Token start, Token end) {
        String name = null;

        int state = 0;
        Token next;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.NAMESPACE) {
                state = 1;
            } else if (state == 1 && token.key == Key.WORD) {
                if (token.endsWith("::")) {
                    erro(token, "A namespace section cannot be empty", this);
                } else {
                    name = token.toString();
                    String[] parts = name.split("::");
                    for (String part : parts) {
                        if (part.startsWith("_")) {
                            erro(token, "A namespace section cannot start with underline", this);
                            name = null;
                            break;
                        } else if (part.length() < 3) {
                            erro(token, "A namespace section shold have at least 3 characters", this);
                            name = null;
                            break;
                        } else if (part.charAt(0) >= '0' && part.charAt(0) < '9') {
                            erro(token, "A namespace section cannot start with a number", this);
                            name = null;
                            break;
                        }
                    }
                }
                state = 2;
            } else if (state == 2 && token.key == Key.SEMICOLON) {
                state = 3;
            } else {
                erro(token, "Unexpected token", this);
            }
            if (next == end && state != 3) {
                erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }

        namespace = library.getNamespace(name);
        namespace.mark(this);
    }

    public Type mark(Type type) {
        if (type != null) {
            type.cFile.usedBy.add(this);
        }
        return type;
    }

    public void link(Namespace namespace) {
        namespace.link(this);
    }

    public Type langBool() {
        return getCompiler().getLangBool();
    }

    public Type langByte() {
        return getCompiler().getLangByte();
    }

    public Type langShort() {
        return getCompiler().getLangShort();
    }

    public Type langInt() {
        return getCompiler().getLangInt();
    }

    public Type langLong() {
        return getCompiler().getLangLong();
    }

    public Type langFloat() {
        return getCompiler().getLangFloat();
    }

    public Type langDouble() {
        return getCompiler().getLangDouble();
    }

    public Type langString() {
        return getCompiler().getLangString();
    }

    public Type langFunction() {
        return getCompiler().getLangFunction();
    }

    public Type langObject() {
        return getCompiler().getLangObject();
    }

    public Type langWrapper() {
        return getCompiler().getLangWrapper();
    }

    public Type langIterable() {
        return getCompiler().getLangIterable();
    }

    public Type langIterator() {
        return getCompiler().getLangIterator();
    }

    public Type langArray() {
        return getCompiler().getLangArray();
    }

    public Type langLocker() {
        return getCompiler().getLangLocker();
    }

    public Pointer langBoolPtr() {
        return new Pointer(mark(getCompiler().getLangBool()));
    }

    public Pointer langBytePtr() {
        return new Pointer(mark(getCompiler().getLangByte()));
    }

    public Pointer langShortPtr() {
        return new Pointer(mark(getCompiler().getLangShort()));
    }

    public Pointer langIntPtr() {
        return new Pointer(mark(getCompiler().getLangInt()));
    }

    public Pointer langLongPtr() {
        return new Pointer(mark(getCompiler().getLangLong()));
    }

    public Pointer langFloatPtr() {
        return new Pointer(mark(getCompiler().getLangFloat()));
    }

    public Pointer langDoublePtr() {
        return new Pointer(mark(getCompiler().getLangDouble()));
    }

    public Pointer langStringPtr() {
        return new Pointer(mark(getCompiler().getLangString()));
    }

    public Pointer langFunctionPtr(Pointer[] pointers) {
        return new Pointer(mark(getCompiler().getLangFunction()), pointers, false);
    }

    public Pointer langObjectPtr() {
        return new Pointer(mark(getCompiler().getLangObject()));
    }

    public Pointer langObjectPtr(boolean isLet) {
        return new Pointer(mark(getCompiler().getLangObject()), null, null, isLet);
    }

    public Pointer langWrapperPtr(Type type) {
        return new Pointer(mark(getCompiler().getLangWrapper()), new Pointer[]{new Pointer(type)}, false);
    }

    public Pointer langIterablePtr(Pointer pointer) {
        return new Pointer(mark(getCompiler().getLangIterable()), new Pointer[]{pointer}, false);
    }

    public Pointer langIteratorPtr(Pointer pointer) {
        return new Pointer(mark(getCompiler().getLangIterator()), new Pointer[]{pointer}, false);
    }

    public Pointer langArrayPtr(Pointer pointer) {
        return new Pointer(mark(getCompiler().getLangArray()), new Pointer[]{pointer}, false);
    }

    public Pointer langLockerPtr() {
        return new Pointer(mark(getCompiler().getLangLocker()));
    }

    public Compiler getCompiler() {
        return library.getCompiler();
    }

    public void add(Using using) {
        usings.add(using);

        if (using.isDirect() && using.isStatic()) {
            usingsStaticDirect.add(using);
        } else if (using.isDirect()) {
            usingsDirect.add(using);
        } else if (using.isStatic()) {
            usingsStatic.add(using);
        } else {
            usingsNormal.add(using);
        }
    }

    public void add(Type type) {
        if (type.nameToken != null) {
            types.add(type);
            Type old = namespace.add(type);

            if (old == type) {
                erro(old.nameToken, "Repeated type name (Insensitive case exception)", this);
            } else if (old != null) {
                erro(old.nameToken, "Repeated type name", this);
            }
        }
    }

    public void add(TokenGroup mainMethodToken) {
        if (this.mainMethodToken != null) {
            erro(mainMethodToken, "Unexpected statment", this);
        } else {
            this.mainMethodToken = mainMethodToken;
            if (!library.setMainFile(this)) {
                erro(mainMethodToken, "Duplicated Main Method", this);
            }
        }
    }

    public Method getMainMethod() {
        return method;
    }

    public Type findType(Token typeToken) {
        Type type;

        if (!typeToken.isComplex()) {
            for (Using using : usingsDirect) {
                type = using.findType(typeToken);
                if (type != null) {
                    return type; // type marked
                }
            }

            for (Using using : usingsNormal) {
                type = using.findType(typeToken);
                if (type != null) {
                    return mark(type);
                }
            }

            type = namespace.findType(typeToken);
            if (type != null) {
                return mark(type);
            }

            type = getCompiler().getLangType(typeToken);
        } else {
            type = getCompiler().findType(library, typeToken);
        }

        return mark(type);
    }

    public Pointer getPointer(Token typeToken, Token end, Type cycleOwner, GenericOwner genericOwner, boolean isLet) {
        Pointer ptr = null;
        Type type = null;

        if (genericOwner != null) {
            ptr = genericOwner.findGeneric(typeToken);
        }

        if (ptr == null) {
            type = findType(typeToken);
            if (type == null) {
                erro(typeToken, "Undefined type", this);
                return null;
            } else if (cycleOwner != null && type.cyclicVerify(cycleOwner)) {
                erro(typeToken, "Cyclic reference", this);
                return null;
            }
        }


        int arr = 0;
        ArrayList<Pointer> iPointers = null;

        int state = 0;
        Token token = typeToken.getNext();
        while (token != end) {
            Token next = token.getNext();

            if (state == 0 && token.key == Key.GENERIC && token.getChild() != null && ptr != null) {
                erro(token, "Generic types could not apply generic variance", this);
            } else if (state == 0 && token.key == Key.GENERIC && token.getChild() != null && type.template == null) {
                erro(token, "Unexpected generic variance", this);
            } else if (state == 0 && token.key == Key.GENERIC && token.getChild() != null && type != null) {
                iPointers = new ArrayList<>(type.template.getCount());

                boolean hasLet = false;
                int iState = 0;
                Token iToken = token.getChild();
                Token iEnd = token.getLastChild();
                while (iToken != null && iToken != iEnd) {
                    Token iNext = iToken.getNext();

                    if (iState == 0 && iToken.key == Key.LET) {
                        if (type.isFunction()) {
                            hasLet = true;
                        } else {
                            erro(iToken, "Let is not allowed here", this);
                        }
                        iState = 1;
                    } else if ((iState == 0 || iState == 1) && iToken.key == Key.WORD) {
                        iNext = TokenGroup.nextType(iNext, iEnd);

                        int index = iPointers.size();
                        if (index >= type.template.getCount() && !type.isFunction()) {
                            erro(iToken, iNext, "Unexpected generic", this);
                        } else {
                            Pointer genPtr = getPointer(iToken, iNext, cycleOwner, genericOwner, hasLet);
                            if (genPtr == null) {
                                genPtr = type.isFunction() ? langObjectPtr() : type.template.getDefaultPtr(index);
                            } else if (!type.isFunction() && genPtr.canSpecify(type.template.getBasePtr(index)) < 0) {
                                genPtr = type.template.getDefaultPtr(index);
                                erro(iToken, iNext, "Invalid generic", this);
                            }
                            iPointers.add(genPtr);
                        }
                        iState = 2;
                    } else if ((iState == 0 || iState == 1) && iToken.key == Key.VOID) {

                        int index = iPointers.size();
                        if (index >= type.template.getCount() && !type.isFunction()) {
                            erro(iToken, iNext, "Unexpected generic", this);
                        } else {
                            Pointer genPtr = Pointer.voidPointer;
                            if (index > 0 || !type.isFunction()) {
                                genPtr = type.isFunction() ? langObjectPtr() : type.template.getDefaultPtr(index);
                                erro(iToken, "Void not allowed here", this);
                            }
                            iPointers.add(genPtr);
                        }
                        iState = 2;
                    } else if (iState == 2 && iToken.key == Key.COMMA) {
                        iState = 0;
                    } else {
                        erro(iToken, "Unexpected token", this);
                    }
                    if (iToken == iNext && iPointers.size() < type.template.getCount()) {
                        while (iPointers.size() < type.template.getCount()) {
                            iPointers.add(type.template.getDefaultPtr(iPointers.size()));
                        }
                        erro(iToken, "Missing generics", this);
                    }
                    iToken = iNext;
                }
                state = 1;
            } else if (token.key == Key.INDEX && token.isEmptyParent()) {
                arr++;
                state = 1;
            } else {
                erro(token, "Unexpected token", this);
            }
            token = next;
        }

        if (ptr == null) {
            Pointer[] pointers = null;
            if (iPointers != null) {
                pointers = iPointers.toArray(new Pointer[0]);
            } else if (type.template != null) {
                pointers = new Pointer[type.template.getCount()];
                for (int i = 0; i < pointers.length; i++) {
                    pointers[i] = type.template.getDefaultPtr(i);
                }
            }
            ptr = new Pointer(type, pointers, isLet && arr == 0);
        }
        for (int i = 0; i < arr; i++) {
            ptr = new Pointer(langArray(), new Pointer[]{ptr}, isLet && (i + 1 == arr));
        }

        return ptr.toLet(isLet);
    }

    public void erro(int start, int end, String message, Object sender) {
        erros.add(new Error(Error.ERROR, start, end, message + " > from " + sender.getClass().getSimpleName()));
    }

    public void erro(Token token, String message, Object sender) {
        erros.add(new Error(Error.ERROR, token.start, token.end, message + " > from " + sender.getClass().getSimpleName()));
    }

    public void erro(TokenGroup token, String message, Object sender) {
        erro(token.start, token.end, message, sender);
    }

    public void erro(Token token, Token tokenEnd, String message, Object sender) {
        int start = token.start;
        int end = start;
        while (token != null && token != tokenEnd) {
            end = token.end;
            token = token.getNext();
        }
        erros.add(new Error(Error.ERROR, start, end, message + " > from " + sender.getClass().getSimpleName()));
    }

    public void warning(int start, int end, String message) {
        erros.add(new Error(Error.WARNING, start, end, message));
    }

    public void warning(Token token, String message) {
        erros.add(new Error(Error.WARNING, token.start, token.end, message));
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (obj instanceof ContentFile) {
            return ((ContentFile) obj).name.equals(name);
        }
        return false;
    }

    @Override
    public String toString() {
        return "ContentFile{" + name + "}";
    }
}
