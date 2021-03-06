package logic.stack.line;

import content.Key;
import content.Token;
import builder.CppBuilder;
import logic.stack.Block;
import logic.stack.Line;

public class LineContinue extends Line {

    Block continueSource;
    Token label;

    public LineContinue(Block block, Token start, Token end) {
        super(block, start, end);

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.CONTINUE) {
                state = 1;
            } else if (state == 1 && token.key == Key.WORD) {
                if (token.isComplex()) cFile.erro(token, "Complex names are not allowed", this);
                label = token;
                state = 2;
            } else if ((state == 1 || state == 2) && token.key == Key.SEMICOLON) {
                state = 3;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end) {
                if (state == 0) {
                    cFile.erro(token, "Unexpected end of tokens", this);
                } else if (state !=3) {
                    cFile.erro(token, "Semicolon expected", this);
                }
            }
            token = next;
        }

        continueSource = parent == null ? null : (Block)parent.isContinuable(label);
        if (continueSource == null) {
            if (label == null) {
                cFile.erro(start, "A Continue Statment should be inside a Loop or Switch Statment", this);
            } else {
                cFile.erro(start, "A Continue Statment should be inside a Named Loop or Switch  [" + label + "]", this);
            }
        }
    }

    @Override
    public void build(CppBuilder cBuilder, int idt, int off) {
        if (label != null) {
            cBuilder.idt(off).add("goto continue_").add(continueSource.getLabelID()).add(";");
        } else {
            cBuilder.idt(off).add("continue").add(";");
        }
        if (off > 0) cBuilder.ln();
    }
}
