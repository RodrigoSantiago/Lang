package logic.stack.line;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.stack.Block;
import logic.stack.Line;
import logic.stack.Stack;
import logic.stack.expression.Expression;

public class LineExpression extends Line {

    Expression expression;

    public LineExpression(Block block, Token start, Token end) {
        super(block, start, end);
        System.out.print("EXPRESSION:");
        System.out.println(TokenGroup.toString(start, end));

        Token last = end;
        Token token = start;
        Token next;
        while (token != null && token != end) {
            next = token.getNext();
            if (next == end) {
                if (token.key != Key.SEMICOLON) {
                    cFile.erro(token, "Semicolon expected");
                } else {
                    last = token;
                }
            }
            token = next;
        }
        expression = new Expression(this, start, last);
        // expression.make();
    }
}
