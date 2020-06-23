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
    private Token operatorToken;

    public ArrayList<Call> calls = new ArrayList<>();
    public CallGroup left, right , center;
    OperatorView operatorView;
    Pointer returnPtr;
    Pointer castPtr;

    public CallGroup(Expression expression) {
        this.cFile = expression.cFile;
        this.parent = expression;
    }

    public CallGroup(Expression expression, CallGroup left, CallGroup right) {
        this.cFile = expression.cFile;
        this.parent = expression;
        if (right.isOperator()) {
            this.left = right;
            this.center = right;
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
                calls.size() > 0 ? calls.get(calls.size() - 1).getToken() : parent.start;
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

    public void load(Context context) {
        if (left != null && center != null && right != null) {
            ArrayList<OperatorView> operatos = context.findOperator(left, center, right);
            if (operatos.size() == 0) {
                cFile.erro(left.operatorToken, "Operator Not Found", this);
            } else if (operatos.size() > 1) {
                cFile.erro(left.operatorToken, "Ambigous Operator Call", this);
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
                }
            }
        }
    }

    public int verify(Pointer pointer) {
        if (operatorView != null) {
            if (operatorView == OperatorView.OR || operatorView == OperatorView.AND) {
                return pointer.canReceive(cFile.langBoolPtr());
            } else if (operatorView == OperatorView.CAST) {
                return pointer.canReceive(castPtr);
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
                if (operatorView == OperatorView.OR || operatorView == OperatorView.AND) {
                    returnPtr = cFile.langBoolPtr();
                } else if (operatorView == OperatorView.CAST) {
                    returnPtr = castPtr;
                } else {
                    returnPtr = operatorView.getTypePtr();
                }
            } else if (calls.size() > 0) {
                returnPtr = calls.get(calls.size() - 1).request(pointer);
            }
        }
        if (returnPtr == null && pointer != null) {
            returnPtr = pointer.canReceive(returnPtr) > 0 ? pointer : null;
        }
        return returnPtr;
    }

    public Pointer requestSet(Pointer pointer) {
        if (returnPtr == null) {
            if (operatorView != null) {
                if (operatorView == OperatorView.OR || operatorView == OperatorView.AND) {
                    returnPtr = cFile.langBoolPtr();
                } else if (operatorView == OperatorView.CAST) {
                    returnPtr = castPtr;
                } else {
                    returnPtr = operatorView.getTypePtr();
                }
                // erro
            } else if (calls.size() > 0) {
                returnPtr = calls.get(calls.size() - 1).requestSet(pointer);
            }
        }
        if (returnPtr == null && pointer != null) {
            returnPtr = pointer.canReceive(returnPtr) > 0 ? pointer : null;
        }
        return returnPtr;
    }

    public boolean isEmpty() {
        return calls.isEmpty();
    }

    public void setOperator(Token token) {
        operatorToken = token;
    }

    public void setCastingOperator(Token token) {
        operatorToken = token;
        Token next;
        Token start = token.getChild();
        Token end = token.getLastChild();
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
        return operatorToken.key == Key.INC || operatorToken.key == Key.DEC;
    }

    public boolean isOperatorLeft() {
        return castPtr != null || operatorToken.key == Key.INC || operatorToken.key == Key.DEC ||
                operatorToken.key == Key.NOT || operatorToken.key == Key.BITNOT;
    }

    public boolean isOperatorBoth() {
        return operatorToken.key == Key.ADD || operatorToken.key == Key.SUB;
    }

    public boolean isOperatorCenter() {
        return !isOperatorLeft();
    }

    public int getOperatorPriority() {
        return operatorToken.key.priority;
    }

    public boolean isField() {
        return calls.size() == 1 && calls.get(0) instanceof FieldCall;
    }

    public void add(Call call) {
        calls.add(call);
    }

    public Pointer getReturnType() {
        return returnPtr;
    }
}
