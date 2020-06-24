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
    private TokenGroup typeToken;
    private Token operatorToken;

    private ArrayList<Call> calls = new ArrayList<>();
    private CallGroup left, right , center;
    private boolean setOperator;

    private OperatorView operatorView;
    private Pointer returnPtr;
    private Pointer castPtr;

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

    public Token getToken() {
        return operatorToken != null ? operatorToken : center != null ? center.getToken() :
                calls.size() > 0 ? calls.get(calls.size() - 1).getToken() : parent.getTokenGroup().start;
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

    public LiteralCall getLiteral() {
        return calls.size() == 1 && calls.get(0) instanceof LiteralCall ? (LiteralCall) calls.get(0) : null;
    }

    public void setOperator(Token token, TokenGroup typeToken) {
        this.operatorToken = token;
        this.typeToken = typeToken;
        if (typeToken != null) {
            castPtr = getStack().getPointer(typeToken, false);
        }
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
        return operatorToken != null && (operatorToken.key == Key.INC || operatorToken.key == Key.DEC ||
                operatorToken.key == Key.IS || operatorToken.key == Key.ISNOT);
    }

    public boolean isOperatorLeft() {
        return operatorToken != null && (operatorToken.key == Key.INC || operatorToken.key == Key.DEC ||
                operatorToken.key == Key.PARAM || operatorToken.key == Key.NOT || operatorToken.key == Key.BITNOT);
    }

    public boolean isOperatorBoth() {
        return operatorToken != null && operatorToken.key == Key.ADD || operatorToken.key == Key.SUB;
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
        if (left != null && center != null && right != null) {
            ArrayList<OperatorView> operatos = context.findOperator(left, center, right);
            setOperator = center.getOperatorToken() != null && center.getOperatorToken().key.priority == 11;

            if (operatos == null || operatos.size() == 0) {
                cFile.erro(center.operatorToken, "Operator Not Found", this);
            } else if (operatos.size() > 1) {
                cFile.erro(center.operatorToken, "Ambigous Operator Call", this);
                operatorView = operatos.get(0);
            } else {
                operatorView = operatos.get(0);
            }
        } else if (left != null && center != null) {
            OperatorView operator = context.findOperator(left, center);
            if (operator == null) {
                cFile.erro(left.operatorToken, "Operator Not Found", this);
            } else {
                operatorView = operator;
            }
        } else {
            for (int i = 0; i < calls.size(); i++) {
                Call call = calls.get(i);

                call.load(context);
                if (!context.isIncorrect() && i < calls.size() - 1) {
                    call.request(null);
                } else if (!context.isIncorrect()) {
                    if (call instanceof FieldCall) {
                        FieldCall field = (FieldCall) call;
                        if (field.isStaticCall()) {
                            cFile.erro(field.getToken(), "Unexpected identifier", this);
                        }
                    }
                }
            }
        }
    }

    public int verify(Pointer pointer) {
        if (operatorView != null) {
            if (pointer == null) return 1;

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
        if (returnPtr == null) {
            if (operatorView != null) {
                if (operatorView == OperatorView.OR || operatorView == OperatorView.AND ||
                        operatorView == OperatorView.IS || operatorView == OperatorView.ISNOT) {
                    returnPtr = cFile.langBoolPtr();
                } else if (operatorView == OperatorView.CAST) {
                    returnPtr = left.getCastPtr();
                } else if (operatorView == OperatorView.SET || setOperator) {
                    returnPtr = right.getReturnType();
                } else if (left != null && center != null && right == null && left.isOperatorBoth()) {
                    Key opKey = left.operatorToken.key;
                    LiteralCall literal = center.getLiteral();
                    if (literal != null) {
                        if (!literal.endL && literal.isLong && opKey == Key.SUB) {
                            if (literal.val <= 128) {
                                literal.isByte = true;
                            }
                            if (literal.val <= 32768) {
                                literal.isShort = true;
                            }
                            if (literal.val <= 2147483648L) {
                                literal.isInt = true;
                            }
                            if (literal.longLimit != null && literal.strVal.equals("9223372036854775808")) {
                                cFile.reverse(literal.longLimit);
                                literal.longLimit = null;
                            }
                        }
                        returnPtr = literal.request(pointer);
                        if (returnPtr != null) {
                            ArrayList<OperatorView> operators = returnPtr.type.getOperator(operatorToken);
                            for (OperatorView ov : operators) {
                                if (ov.getParams().getArgsCount() == 1) {
                                    operatorView = ov;
                                    break;
                                }
                            }
                        }
                    } else {
                        returnPtr = operatorView.getTypePtr();
                    }
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

    public Pointer requestSet(Pointer pointer) {
        if (calls.size() > 0) {
            returnPtr = calls.get(calls.size() - 1).requestSet(pointer);
            return returnPtr;
        } else {
            request(pointer);
            return null;
        }
    }
}
