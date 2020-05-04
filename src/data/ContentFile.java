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

public class ContentFile {

    public Library library;
    public String name;
    public String content;
    public Token token;

    public Namespace namespace;
    public ArrayList<Using> usings = new ArrayList<>();
    public ArrayList<Using> usingsDirect = new ArrayList<>();
    public ArrayList<Using> usingsStatic = new ArrayList<>();
    public ArrayList<Using> usingsStaticDirect = new ArrayList<>();
    public ArrayList<Type> types = new ArrayList<>();

    public ArrayList<Error> erros = new ArrayList<>();

    public ContentFile(Library library, String name, String content) {
        this.library = library;
        this.name = name;
        this.content = content;
    }

    public void read() {
        Lexer lexer = new Lexer(this);
        token = lexer.read();

        if (token != null) {
            Parser parser = new Parser(this);
            parser.parseWorkspace(token);

            for (Using using : usings) {
                using.preload();
            }
            for (Using using : usingsDirect) {
                using.preload();
            }
            for (Using using : usingsStatic) {
                using.preload();
            }
            for (Using using : usingsStaticDirect) {
                using.preload();
            }

            for (Type type : types) {
                type.preload();
            }
        }
    }

    public void load() {
        for (Type type : types) {
            type.load();
        }
    }

    public void cross() {
        for (Type type : types) {
            type.cross();
        }
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
            erro(keyToken == null ? start : keyToken, "Incorrect Namespace syntax : Name expected");
        } else {
            name = tokenName.toString();
            if (name.endsWith("::")) {
                erro(keyToken, "Incorrect Namespace syntax : Invalid name");

                name = name.substring(0, name.length() - 2);
            }
        }

        namespace = library.getNamespace(name);
    }

    public void add(Using using) {
        if (using.isDirect() && using.isStatic()) {
            usingsStaticDirect.add(using);
        } else if (using.isDirect()) {
            usingsDirect.add(using);
        } else if (using.isStatic()) {
            usingsStatic.add(using);
        } else {
            usings.add(using);
        }
    }

    public void add(Type type) {
        types.add(type);
        Type old = namespace.add(type);

        if (old != null && old.nameToken != null) {
            erro(old.nameToken, "Repeated type name");
        }
    }

    public Type findType(Token tokenType) {
        Type type;

        if (!tokenType.isComplex()) {
            for (Using using : usingsDirect) {
                type = using.findType(tokenType);
                if (type != null) {
                    return type;
                }
            }

            for (Using using : usings) {
                type = using.findType(tokenType);
                if (type != null) {
                    return type;
                }
            }

            type = namespace.findType(tokenType);
            if (type != null) {
                return type;
            }

            Library lang = Compiler.getLangLibrary();
            type = (lang == null ? library : lang).findType(tokenType);
        } else {
            type = Compiler.findType(tokenType);
        }

        return type;
    }

    public Pointer getPointer(Token typeToken, Token end) {
        return getPointer(typeToken, end, null, null);
    }

    public Pointer getPointer(Token typeToken, Token end, Type cycleOwner, GenericOwner genericOwner) {
        Pointer ptr = null;
        Type type = null;

        if (genericOwner != null) {
            ptr = genericOwner.findGeneric(typeToken);
        }

        if (ptr == null) {
            type = findType(typeToken);
            if (type == null) {
                type = Compiler.getLangObject();
                erro(typeToken, "Undefined type");
            }
        }

        if (type != null && cycleOwner != null) {
            if (type.cyclicVerify(cycleOwner)) {
                type = Compiler.getLangObject();
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

        // Todo - Check pointer generic validate

        if (ptr == null) {
            ptr = new Pointer(type, iPointers == null ? null : iPointers.toArray(new Pointer[0]));
        }
        for (int i = 0; i < arr; i++) {
            ptr = new Pointer(Compiler.getLangArray(), new Pointer[]{ptr});
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
}
