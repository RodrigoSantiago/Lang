package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.Pointer;
import logic.member.view.FieldView;
import logic.stack.Context;
import logic.stack.LocalVar;

public class FieldCall extends Call {

    FieldView field;

    public FieldCall(CallGroup group, Token start, Token end) {
        super(group, start, end);
        System.out.println("FIELD : "+ TokenGroup.toString(start, end));

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && (token.key == Key.WORD || token.key == Key.THIS || token.key == Key.SUPER)) {
                this.token = token;
                state = 1;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state == 0) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }
    }

    @Override
    public void load(Context context) {
        if (token == null) {
            context.jumpTo(null);
        } else {
            field = context.findField(token);
            if (field == null) {
                cFile.erro(token, "Field Not Found", this);
            }
            context.jumpTo(field == null ? null : field.getTypePtr());
        }
    }

    @Override
    public int verify(Pointer pointer) {
        return field == null ? -1 : pointer.canReceive(field.getTypePtr());
    }

    @Override
    public Pointer request(Pointer pointer) {
        if (field == null) return null;

        return field.getTypePtr();
    }
}
