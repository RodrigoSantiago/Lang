package logic.line;

import content.Key;
import content.Token;
import logic.Pointer;

public class Stack {
    Pointer returnType;
    boolean isStatic;

    public void addBlock(Key key, Token start, Token end) {

    }

    public void addLine(Token start, Token end) {
        // if () {;}
        // while () {;}
        // for () {;}
        // switch () {;}
        // lock () {;}
        // native () {;}
        // do {;}
        // else {;}

        // return [yield][expression][;]
        // continue [;]
        // break [;]
        // case [expression] :
        // name.name[].name()
        // [expression] + [expression] [...]
        // (casting)[expression] + [expression] [...]
        // ([expression] + [expression] [...])
        // () -> {;}

        // block { block {lineA + lineB + (linec)lineD}}
    }
}