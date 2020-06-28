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

    private Pointer naturalPtr;
    private Pointer requestPtr;

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
                if (castPtr == null) {
                    castPtr = cFile.langObjectPtr();
                }

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
        return requestPtr;
    }

    public void load(Context context) {
        if (left != null && center != null && right != null && colon != null && option != null) {
            left.load(new Context(context));
            right.load(new Context(context));
            option.load(new Context(context));

            // [Literal Resolve]
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
                // [Literal Resolve]
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
                // [Literal Resolve]
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

                if (!context.isIncorrect() && !call.isTypeCall() && i < calls.size() - 1) {
                    call.requestGet(null);
                }
            }
            if (calls.size() == 1) {
                Call call = calls.get(0);
                if (call instanceof InnerCall) {
                    InnerCall innerCall = (InnerCall) call;
                    // [Literal Resolve]
                    if (innerCall.innerExpression.isLiteral()) {
                        LiteralCall literalCall = innerCall.innerExpression.getLiteral();
                        calls.set(0, LiteralResolver.resolve(this, literalCall));
                    }
                }
            }
            if (calls.size() > 0 && calls.get(calls.size() -1).isTypeCall()) {
                // erro ? [alredy done by requestGET/OWN/SET]
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

    public Pointer getNaturalPtr(Pointer convertFlag) {
        if (naturalPtr == null) {
            if (option != null) {
                Pointer a = right.getNaturalPtr(convertFlag);
                Pointer b = option.getNaturalPtr(convertFlag);
                if (a == null) {
                    naturalPtr = b;
                } else if (b == null) {
                    naturalPtr = a;
                } else if (a.canReceive(b) > 0) {
                    naturalPtr = a;
                } else if (b.canReceive(a) > 0) {
                    naturalPtr = b;
                }
            } else if (operatorView != null) {
                if (operatorView == OperatorView.OR || operatorView == OperatorView.AND ||
                        operatorView == OperatorView.IS || operatorView == OperatorView.ISNOT) {
                    naturalPtr = cFile.langBoolPtr();
                } else if (operatorView == OperatorView.CAST) {
                    naturalPtr = left.getCastPtr();
                } else if (operatorView == OperatorView.SET || setOperator) {
                    naturalPtr = right.getNaturalPtr(convertFlag);
                } else {
                    naturalPtr = operatorView.getTypePtr();
                }
            } else if (calls.size() > 0) {
                naturalPtr = calls.get(calls.size() - 1).getNaturalPtr(convertFlag);
            }
        }
        return naturalPtr;
    }

    private void setCastGet(Pointer pointer) {

    }

    private boolean setCastOwn(Pointer pointer) {
        if (pointer == null || castPtr == null) return true;

        return Pointer.OwnTable(pointer, naturalPtr) == 0;
    }

    public void requestGet(Pointer pointer) {
        if (pointer == null) pointer = getNaturalPtr(pointer);

        requestPtr = pointer;

        if (option != null) {
            left.requestGet(cFile.langBoolPtr());
            if (pointer != null) {
                pointer = pointer.toLet(); // [GET CONVERSION]

                right.requestGet(pointer);
                option.requestGet(pointer);
            } else {
                cFile.erro(getTokenGroup(), "Incompatible Ternary Members values", this);
            }
        } else if (calls.size() > 0) {
            calls.get(calls.size() - 1).requestGet(pointer);
        } else if (operatorView == OperatorView.CAST) {
            left.setCastGet(pointer);
            center.requestGet(null);
        } else {
            if (naturalPtr != null && naturalPtr != pointer && pointer.canReceive(naturalPtr) <= 0) {
                cFile.erro(getTokenGroup(), "Cannot cast [" + naturalPtr + "] to [" + pointer + "]", this);
            }
        }

    }

    public void requestOwn(Pointer pointer) {
        if (pointer == null) pointer = getNaturalPtr(pointer);

        requestPtr = pointer;

        if (option != null) {
            left.requestGet(cFile.langBoolPtr());
            if (pointer != null) {
                right.requestOwn(pointer);
                option.requestOwn(pointer);
            } else {
                cFile.erro(getTokenGroup(), "Incompatible Ternary Members values", this);
            }
        } else if (calls.size() > 0) {
            calls.get(calls.size() - 1).requestOwn(pointer);
        } else if (operatorView == OperatorView.CAST) {
            if (left.setCastOwn(pointer)) {
                center.requestOwn(null);
            } else {
                center.requestGet(null);
            }
        } else {
            if (naturalPtr != null && pointer.canReceive(naturalPtr) <= 0) {
                cFile.erro(getTokenGroup(), "Cannot cast [" + naturalPtr + "] to [" + pointer + "]", this);
            }
        }
    }

    public void requestSet() {
        if (calls.size() > 0) {
            calls.get(calls.size() - 1).requestSet();
        } else {
            cFile.erro(getTokenGroup(), "SET not allowed", this);
        }
    }

    public boolean isSetExpression() {
        return left != null && center != null && right != null && colon == null && option == null &&
                center.getOperatorToken() != null && center.getOperatorToken().key.isSet();
    }

    @Override
    public String toString() {
        return getTokenGroup().toString();
    }
}
