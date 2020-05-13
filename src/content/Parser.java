package content;

import data.ContentFile;
import logic.Using;
import logic.line.Stack;
import logic.member.*;
import logic.typdef.Class;
import logic.typdef.Enum;
import logic.typdef.Interface;
import logic.typdef.Struct;
import logic.typdef.Type;


public class Parser {

    public Parser() {

    }

    public void parseWorkspace(ContentFile cFile, Token init) {
        int state = 0;

        Key typeKey = Key.NOONE;
        Token token = init;
        Token start = token;
        while (token != null) {
            Token next = token.getNext();

            if (state == 0 && token.key.isNamespace) {
                typeKey = token.key;
                state = 1;
            }
            if (state == 1 &&
                    (token.key == Key.SEMICOLON || token.key == Key.BRACE || next == null || next.key.isNamespace)) {
                if (typeKey == Key.NAMESPACE) {
                    cFile.setNamespace(start, next);
                } else if (typeKey == Key.USING) {
                    cFile.add(new Using(cFile, start, next));
                } else if (typeKey == Key.CLASS) {
                    cFile.add(new Class(cFile, start, next));
                } else if (typeKey == Key.INTERFACE) {
                    cFile.add(new Interface(cFile, start, next));
                } else if (typeKey == Key.STRUCT) {
                    cFile.add(new Struct(cFile, start, next));
                } else if (typeKey == Key.ENUM) {
                    cFile.add(new Enum(cFile, start, next));
                }
                typeKey = Key.NOONE;
                start = next;
                state = 0;
            } else if (state == 0 && (token.key == Key.SEMICOLON || token.key == Key.BRACE || next == null)) {
                // undefined scope
                cFile.add(new Class(cFile, start, next));
                start = next;
                state = 0;
            }
            token = next;
        }
    }

    public void parseMembers(Type type, Token init, Token end) {
        boolean destructor = false, constructor = false;
        boolean prevThis = false, prevDest = false;
        int state = 0;

        Token prev = null;
        Token token = init;
        Token start = token;
        while (token != end) {
            Token next = token.getNext();

            if (type.isEnum() && state == 0) {
                if (token.key == Key.WORD && (next == end || next.key == Key.COMMA || next.key == Key.SEMICOLON)) {
                    state = 6;  // enum
                } else if (prev != null && prev.key == Key.WORD && token.key == Key.PARAM &&
                        (next == end || next.key == Key.COMMA || next.key == Key.SEMICOLON)) {
                    state = 6;  // enum + constructor
                }
            }

            if (state == 0 && (token.key == Key.SETVAL || token.key == Key.COMMA)) {
                state = 1;  // variable

            } else if (state == 0 && (token.key == Key.PARAM)) {
                state = 2;  // method, constructor, destructor
                constructor = prevThis && !prevDest;
                destructor = prevDest;

            } else if (state == 0 && (prevThis && token.key == Key.INDEX)) {
                state = 3;  // indexer

            } else if (state == 0 && (token.key == Key.BRACE && next != end && next.key == Key.SETVAL)) {
                state = 4;  // property

            } else if (state == 0 && (token.key == Key.OPERATOR)) {
                state = 5;  // operator

            } else if (state == 0 && (token.key == Key.NATIVE)) {
                state = 7;  // native
            }

            boolean reset = true;

            if (state == 0 && (token.key == Key.BRACE)) {
                if (prev == null) {
                    type.add(new Method(type, start, next));  // unexpected (empty brace)
                } else {
                    type.add(new Property(type, start, next));
                }
            } else if ((state == 0 || state == 1) && (token.key == Key.SEMICOLON || next == end)) {
                if (start.getNext() != next) {
                    type.add(new Variable(type, start, next));
                }

            } else if (state == 2 && (token.key == Key.SEMICOLON || token.key == Key.BRACE || next == end)) {
                if (constructor) {
                    type.add(new Constructor(type, start, next));
                } else if (destructor) {
                    type.add(new Destructor(type, start, next));
                } else {
                    type.add(new Method(type, start, next));
                }
            } else if (state == 3 && (token.key == Key.SEMICOLON || token.key == Key.BRACE || next == end)) {
                type.add(new Indexer(type, start, next));

            } else if (state == 4 && (token.key == Key.SEMICOLON || next == end)) {
                type.add(new Property(type, start, next));

            } else if (state == 5 && (token.key == Key.SEMICOLON || token.key == Key.BRACE || next == end)) {
                type.add(new Operator(type, start, next));

            } else if (state == 6 && (token.key == Key.SEMICOLON || next == end)) {
                type.add(new Num(type, start, next));

            } else if (state == 7 && (token.key == Key.SEMICOLON || token.key == Key.BRACE || next == end)) {
                type.add(new MemberNative(type, start, next));

            } else {
                reset = false;
            }

            if (reset) {
                start = next;
                state = 0;
                constructor = destructor = false;
            }

            prevThis = (token.key == Key.THIS);
            prevDest = prevThis && token.getPrev() != null && token.getPrev().key == Key.BITNOT;
            prev = token;
            token = next;
        }
    }

    public void parseLines(Stack stack, Token init, Token end) {
        int state = 0;

        Token token = init;
        Token start = token;
        while (token != end) {
            Token next = token.getNext();
            if (state == 0 && token.key == Key.BRACE) {
                // break; clean block [{}]
                stack.addLine(start, token);
                start = next;
            } else if (state == 0 && token.key.isBlock) {
                state = 1; // [if]

            } else if (state == 0 && next != end) {
                state = 4; // [...]

            } else if (state == 1 && token.key == Key.PARAM) {
                state = 2; // [if][()]

            } else if ((state == 1 || state == 2) && token.key == Key.BRACE) {
                // break; [if][()][{}] || [else][{}]
                stack.addLine(start, token);
                start = next;
                state = 0;

            } else if (token.key == Key.SEMICOLON || next == end) {
                // break; (0)[;] || (1)[else][...][;] || (2)[if][()][;] || (3)[if][()][...][;] || (4)[...][;]
                stack.addLine(start, token);
                start = next;
                state = 0;

            } else if ((state == 1 || state == 2) && token.key.isBlock) {
                state = 1; // [for][()][if]

            } else if (state == 1 || state == 2) {
                state = 3; // [if][()][...]

            } else if (token.key.isBlock) {
                // break; unexpected [...][if]
                stack.addLine(start, token.getPrev());
                start = token;
                state = 1;
            }
            token = next;
        }
    }

}
