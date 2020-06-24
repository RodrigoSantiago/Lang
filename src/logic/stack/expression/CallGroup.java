package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.ContentFile;
import logic.Pointer;
import logic.member.view.OperatorView;
import logic.stack.Context;
import logic.stack.Stack;

import java.util.ArrayList;

public class CallGroup {

    public final ContentFile cFile;
    public final Expression parent;

    // Line Calls
    private final ArrayList<Call> calls = new ArrayList<>();

    // Operator
    private CallGroup left, center, right, colon, option;
    private Token operatorToken;
    private OperatorView operatorView;
    private Pointer castPtr;
    private boolean setOperator;

    private Pointer returnPtr;

    public CallGroup(Expression expression) {
        this.cFile = expression.cFile;
        this.parent = expression;
    }

    public CallGroup(Expression expression, CallGroup left, CallGroup right) {
        this.cFile = expression.cFile;
        this.parent = expression;
        if (right.isOperator()) {
            this.left = right;
            this.center = left;
        } else {
            this.left = left;
            this.center = right;
        }
    }

    public CallGroup(Expression expression, CallGroup left, CallGroup center, CallGroup right) {
        this.cFile = expression.cFile;
        this.parent = expression;
        this.left = left;
        this.center = center;
        this.right = right;
    }

    public CallGroup(Expression expression, CallGroup left, CallGroup center, CallGroup right,
                     CallGroup colon, CallGroup option) {
        this.cFile = expression.cFile;
        this.parent = expression;
        this.left = left;
        this.center = center;
        this.right = right;
        this.colon = colon;
        this.option = option;
    }

    public TokenGroup getTokenGroup() {
        if (left != null && option != null) {
            return new TokenGroup(left.getTokenGroup().start, option.getTokenGroup().end);
        } else if (left != null && right != null) {
            return new TokenGroup(left.getTokenGroup().start, right.getTokenGroup().end);
        } else if (left != null && center != null) {
            TokenGroup lGroup = left.getTokenGroup();
            TokenGroup cGroup = center.getTokenGroup();
            if (lGroup.start.start > cGroup.start.start) {
                return new TokenGroup(cGroup.start, lGroup.end);
            } else {
                return new TokenGroup(cGroup.end, lGroup.start);
            }
        } else if (operatorToken != null) {
            return new TokenGroup(operatorToken);
        } else if (calls.size() > 0) {
            return new TokenGroup(calls.get(calls.size() - 1).getToken());
        } else {
            return parent.getTokenGroup();
        }
    }

    public Token getOperatorToken() {
        return operatorToken;
    }

    public Pointer getCastPtr() {
        return castPtr;
    }

    public Stack getStack() {
        return parent.stack;
    }

    public boolean isEmpty() {
        return calls.isEmpty();
    }

    public boolean isTypeCall() {
        return calls.size() == 1 && calls.get(0).isTypeCall();
    }

    public boolean isLiteral() {
        if (left != null && center != null && right != null && option != null) {
            return left.isLiteral() && right.isLiteral() && option.isLiteral();
        } else if (left != null && center != null && right != null) {
            return left.isLiteral() && right.isLiteral();
        } else if (calls.size() > 0) {
            return calls.get(0).isLiteral();
        }
        return false;
    }

    public LiteralCall getLiteral() {
        return calls.size() == 1 && calls.get(0) instanceof LiteralCall ? (LiteralCall) calls.get(0) : null;
    }

    public int getLiteralType() {
        return getLiteral().getLiteralType();
    }

    public void setOperator(Token token) {
        this.operatorToken = token;
    }

    public void setCastingOperator(Token operatorToken) {
        this.operatorToken = operatorToken;
        Token next;
        Token token = operatorToken.getChild();
        Token end = operatorToken.getLastChild();
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.WORD) {
                TokenGroup tokenGroup = new TokenGroup(token, next = TokenGroup.nextType(next, end));
                castPtr = getStack().getPointer(tokenGroup, false);
                state = 1;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state != 1) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }
    }

    public boolean isOperator() {
        return operatorToken != null;
    }

    public boolean isCastingOperator() {
        return operatorToken != null && operatorToken.key == Key.PARAM;
    }

    public boolean isOperatorRight() {
        return operatorToken != null && (operatorToken.key == Key.INC || operatorToken.key == Key.DEC);
    }

    public boolean isOperatorLeft() {
        return operatorToken != null && (operatorToken.key == Key.INC || operatorToken.key == Key.DEC ||
                operatorToken.key == Key.PARAM || operatorToken.key == Key.NOT || operatorToken.key == Key.BITNOT);
    }

    public boolean isOperatorBoth() {
        return operatorToken != null && (operatorToken.key == Key.ADD || operatorToken.key == Key.SUB);
    }

    public int getOperatorPriority() {
        return operatorToken.key.priority;
    }

    public void add(Call call) {
        calls.add(call);
    }

    public Pointer getReturnType() {
        return returnPtr;
    }


    public void load(Context context) {
        if (left != null && center != null && right != null && colon != null && option != null) {
            left.load(new Context(getStack()));
            right.load(new Context(getStack()));
            option.load(new Context(getStack()));

            if (left.request(cFile.langBoolPtr()) == null) {
                cFile.erro(center.operatorToken, "The ternary condition must be a bool", this);
            }
            if (left.isLiteral() && right.isLiteral() && option.isLiteral()) {

            }
        } else if (left != null && center != null && right != null) {
            ArrayList<OperatorView> operatos = context.findOperator(left, center, right);
            setOperator = center.getOperatorToken() != null && center.getOperatorToken().key.priority == 11;

            if (operatos == null || operatos.size() == 0) {
                cFile.erro(center.operatorToken, "Operator Not Found", this);
            } else if (operatos.size() > 1) {
                cFile.erro(center.operatorToken, "Ambigous Operator Call", this);
                operatorView = operatos.get(0);
            } else {
                operatorView = operatos.get(0);
                if (left.isLiteral() && right.isLiteral()) {
                    LiteralCall lCall = left.getLiteral();
                    LiteralCall rCall = right.getLiteral();
                    Key op = center.getOperatorToken().key;
                    LiteralCall result = LiteralResolver.resolve(this, lCall, rCall, op);
                    if (result != null) {
                        left = null;
                        center = null;
                        right = null;
                        operatorView = null;
                        calls.add(result);
                    }
                }
            }
        } else if (left != null && center != null) {
            OperatorView operator = context.findOperator(left, center);
            if (operator == null) {
                cFile.erro(left.operatorToken, "Operator Not Found", this);
            } else {
                operatorView = operator;
                // [Literal Absortion]
                if (center.isLiteral()) {
                    LiteralCall cCall = center.getLiteral();
                    Key op = left.getOperatorToken().key;
                    LiteralCall result = LiteralResolver.resolve(this, cCall, op);
                    if (result != null) {
                        left = null;
                        center = null;
                        operatorView = null;
                        calls.add(result);
                    }
                }
            }
        } else {
            for (int i = 0; i < calls.size(); i++) {
                Call call = calls.get(i);

                call.load(context);
                if (!context.isIncorrect() && i < calls.size() - 1) {
                    call.request(null);
                }
            }
        }
    }

    public int verify(Pointer pointer) {
        if (colon != null) {
            int r = right.verify(pointer);
            int o = option.verify(pointer);
            if (r > 0 && o > 0) {
                return Math.max(r, o);
            }
        } else if (operatorView != null) {
            if (operatorView == OperatorView.OR || operatorView == OperatorView.AND ||
                    operatorView == OperatorView.IS || operatorView == OperatorView.ISNOT) {
                return pointer.canReceive(cFile.langBoolPtr());
            } else if (operatorView == OperatorView.CAST) {
                return left.getCastPtr() == null ? 0 : pointer.canReceive(left.getCastPtr());
            } else if (operatorView == OperatorView.SET) {
                return right.getReturnType() == null ? 0 : pointer.canReceive(right.getReturnType());
            } else {
                return pointer.canReceive(operatorView.getTypePtr());
            }
        } else if (calls.size() > 0) {
            return calls.get(calls.size() - 1).verify(pointer);
        }
        return 0;
    }

    public Pointer request(Pointer pointer) {
        if (colon != null) {
            Pointer a = right.request(pointer);
            Pointer b = option.request(pointer);
            if (a == null || b == null) {
                returnPtr = null;
            } else if (a.canReceive(b) > 0) {
                returnPtr = a;
            } else if (b.canReceive(a) > 0) {
                returnPtr = b;
            } else {
                returnPtr = cFile.langObjectPtr();
            }
        } else if (returnPtr == null) {
            if (operatorView != null) {
                if (operatorView == OperatorView.OR || operatorView == OperatorView.AND ||
                        operatorView == OperatorView.IS || operatorView == OperatorView.ISNOT) {
                    returnPtr = cFile.langBoolPtr();
                } else if (operatorView == OperatorView.CAST) {
                    returnPtr = left.getCastPtr();
                } else if (operatorView == OperatorView.SET || setOperator) {
                    returnPtr = right.getReturnType();
                } else {
                    returnPtr = operatorView.getTypePtr();
                }
            } else if (calls.size() > 0) {
                returnPtr = calls.get(calls.size() - 1).request(pointer);
            }
        }
        if (returnPtr != null && pointer != null) {
            returnPtr = pointer.canReceive(returnPtr) > 0 ? pointer : null;
        }
        return returnPtr;
    }

    public boolean requestSet(Pointer pointer) {
        if (calls.size() > 0) {
            boolean canSet = calls.get(calls.size() - 1).requestSet(pointer);
            returnPtr = calls.get(calls.size() - 1).getReturnType();
            return canSet;
        } else {
            request(pointer);
            return false;
        }
    }

    @Override
    public String toString() {
        return getTokenGroup().toString();
    }
}
