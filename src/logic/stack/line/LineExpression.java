package logic.stack.line;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.stack.Block;
import logic.stack.Context;
import logic.stack.Line;
import logic.stack.expression.Expression;

public class LineExpression extends Line {

    Expression expression;
    TokenGroup expressionGroup;

    public LineExpression(Block block, Token start, Token end) {
        super(block, start, end);
        System.out.println("EXPRESSION:");

        Token last = end;
        Token token = start;
        Token next;
        while (token != null && token != end) {
            next = token.getNext();
            if (next == end) {
                if (token.key != Key.SEMICOLON) {
                    cFile.erro(token, "Semicolon expected", this);
                } else {
                    last = token;
                }
            }
            token = next;
        }
        expressionGroup = new TokenGroup(start, last);
        expression = new Expression(this, start, last);
    }

    @Override
    public void load() {
        if (expression != null) {
            expression.load(new Context(stack));
            expression.requestGet(null);
            if (expression.isLiteral()) {
                cFile.erro(expressionGroup, "Must be a call", this);
            }
        }
    }
}
