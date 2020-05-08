package data;

import content.Key;
import content.Lexer;
import content.Parser;
import content.Token;
import logic.GenericOwner;
import logic.Namespace;
import logic.Pointer;
import logic.Using;
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

    public ArrayList<Error> erros = new ArrayList<>();

    private int state;
    private boolean invalid;
    private HashSet<ContentFile> usedBy = new HashSet<>();

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
            Parser parser = new Parser();
            parser.parseWorkspace(this, token);

            if (namespace == null) {
                namespace = library.getNamespace(null);
                namespace.mark(this);
            }

            for (Using using : usings) {
                using.preload();
            }

            for (Type type : types) {
                type.preload();
            }
        }

        invalid = false;
        state = 1;
    }

    public void load() {
        for (Type type : types) {
            type.load();
        }

        state = 2;
    }

    public void cross() {
        for (Type type : types) {
            type.cross();
        }

        state = 3;
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
        Token keyToken = null;
        Token tokenName = null;

        int state = 0;
        Token next;
        Token token = start;
        while (token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.NAMESPACE) {
                keyToken = token;
                state = 1;
            } else if (state == 1 && token.key == Key.WORD) {
                tokenName = token;
                state = 2;
            } else if (state == 2 && token.key == Key.SEMICOLON) {
                state = 3;
            } else {
                erro(token, "Unexpected token");
            }
            if (next == end && state != 3) {
                erro(token, "Unexpected end of tokens");
            }

            token = next;
        }

        String name = null;
        if (tokenName == null) {
            erro(keyToken == null ? start : keyToken, "Invalid Namespace : Name expected");
        } else {
            name = tokenName.toString();
            if (tokenName.endsWith("::")) {
                name = null;
                erro(tokenName, "Invalid Namespace : Invalid name");
            }
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

    public Pointer langObject() {
        return new Pointer(mark(getCompiler().getLangObject()));
    }

    public Pointer langArray(Pointer pointer) {
        return new Pointer(mark(getCompiler().getLangArray()), new Pointer[]{pointer});
    }

    public Pointer langWrapper(Type type) {
        return new Pointer(mark(getCompiler().getLangWrapper()), new Pointer[]{new Pointer(type)});
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
        types.add(type);
        if (type.nameToken != null) {
            Type old = namespace.add(type);

            if (old == type) {
                erro(old.nameToken, "Repeated type name (Insensitive case exception)");
            } else if (old != null) {
                erro(old.nameToken, "Repeated type name");
            }
        }
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

    public Pointer getPointer(Token typeToken, Token end) {
        return getPointer(typeToken, end, null, null);
    }

    public Pointer getPointer(Token typeToken, Token end, Type cycleOwner, GenericOwner genericOwner) {
        Pointer ptr = null;
        Type type = null;

        if (typeToken.getNext() == end && typeToken.equals("void")) {
            return Pointer.voidPointer;
        }

        if (genericOwner != null) {
            ptr = genericOwner.findGeneric(typeToken);
        }

        if (ptr == null) {
            type = findType(typeToken);
            if (type == null) {
                type = getCompiler().getLangObject();
                erro(typeToken, "Undefined type");
            }
        }

        if (type != null && cycleOwner != null) {
            if (type.cyclicVerify(cycleOwner)) {
                type = getCompiler().getLangObject();
                erro(typeToken, "Cyclic reference");
            }
        }

        int arr = 0;
        ArrayList<Pointer> iPointers = null;

        int state = 0;
        Token token = typeToken.getNext();
        while (token != end) {
            Token next = token.getNext();

            if (ptr != null && state == 0 && token.key == Key.GENERIC) {
                erro(token, "Generic types could not apply generic variance");
            } else if (state == 0 && token.key == Key.GENERIC) {
                iPointers = new ArrayList<>();

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
                        iPointers.add(getPointer(iToken, iNext, cycleOwner, genericOwner));
                        iState = 1;
                    } else if (iState == 1 && iToken.key == Key.COMMA) {
                        iState = 0;
                    } else {
                        erro(iToken, "Unexpected token");
                    }
                    iToken = iNext;
                }
                state = 1;
            } else if (token.key == Key.INDEX
                    && token.getChild() != null
                    && token.getLastChild() == token.getChild()) {
                arr++;
                state = 1;
            } else {
                erro(token, "Unexpected token");
            }
            token = next;
        }

        // Todo - validate generic pointer

        if (ptr == null) {
            ptr = new Pointer(type, iPointers == null ? null : iPointers.toArray(new Pointer[0]));
        }
        for (int i = 0; i < arr; i++) {
            ptr = new Pointer(getCompiler().getLangArray(), new Pointer[]{ptr});
        }

        return ptr;
    }

    public void erro(int start, int end, String message) {
        erros.add(new Error(Error.ERROR, start, end, message));
    }

    public void erro(Token token, String message) {
        erros.add(new Error(Error.ERROR, token.start, token.end, message));
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
}
