package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import data.ContentFile;
import builder.CppBuilder;
import builder.Temp;
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
    private boolean rightOperator;
    private boolean directRequest;

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
            rightOperator = true;
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

    public Call getLastCall() {
        return calls.get(calls.size() - 1);
    }

    public boolean isEmpty() {
        return calls.isEmpty();
    }

    public boolean isTypeCall() {
        return calls.size() == 1 && calls.get(0).isTypeCall();
    }

    public boolean isLineCall() {
        if (calls.size() <= 1) return false;
        if (calls.size() == 2) {
            return !calls.get(0).isTypeCall() && !calls.get(0).isDirectCall();
        }
        return true;
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

    private void setCastGet(Pointer pointer) {
        if (castPtr != null) castPtr = castPtr.toLet(true);
    }

    private boolean setCastOwn(Pointer pointer) {
        if (pointer == null || castPtr == null) return true;
        if (Pointer.OwnTable(pointer, castPtr) == 0) {
            return true;
        } else {
            castPtr = castPtr.toLet(true);
            return false;
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

    public void markArgument() {
        if (operatorView != OperatorView.CAST) {
            if (left != null) left.markArgument();
            if (center != null) center.markArgument();
            if (right != null) right.markArgument();
            if (option != null) option.markArgument();
        } else if (calls.size() > 0) {
            calls.get(calls.size() - 1).markArgument();
        }
    }

    public void add(Call call) {
        calls.add(call);
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

                if (!context.isIncorrect() && i < calls.size() - 1) {
                    if (call.isTypeCall()) {
                        if (call.getTypePtr() != null) {
                            context.jumpTo(call.getTypePtr().type, true);
                        } else {
                            context.jumpTo(null);
                        }
                    } else {
                        call.requestGet(null);
                        context.jumpTo(call.getNaturalPtr());
                    }
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
            if (calls.size() > 0 && calls.get(calls.size() - 1).isTypeCall()) {
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
                    operatorView == OperatorView.IS || operatorView == OperatorView.ISNOT ||
                    operatorView == OperatorView.EQUAL || operatorView == OperatorView.DIF) {
                return pointer.canReceive(cFile.langBoolPtr());
            } else if (operatorView == OperatorView.CAST) {
                return left.getCastPtr() == null ? 0 : pointer.canReceive(left.getCastPtr());
            } else if (operatorView == OperatorView.SET) {
                return right.getNaturalPtr() == null ? 0 : pointer.canReceive(right.getNaturalPtr());
            } else {
                return pointer.canReceive(operatorView.getTypePtr());
            }
        } else if (calls.size() > 0) {
            return calls.get(calls.size() - 1).verify(pointer);
        }
        return 0;
    }

    public Pointer getLineRequestPtr() {
        return calls.get(calls.size() - 1).requestPtr;
    }

    public Pointer getRequestPtr() {
        return requestPtr;
    }

    public Pointer getNaturalPtr() {
        return naturalPtr;
    }

    public Pointer getNaturalPtr(Pointer convertFlag) {
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
                    operatorView == OperatorView.IS || operatorView == OperatorView.ISNOT ||
                    operatorView == OperatorView.EQUAL || operatorView == OperatorView.DIF) {
                naturalPtr = cFile.langBoolPtr();
            } else if (operatorView == OperatorView.CAST) {
                naturalPtr = left.getCastPtr();
            } else if (operatorView == OperatorView.SET) {
                naturalPtr = right.getNaturalPtr();
            } else {
                naturalPtr = operatorView.getTypePtr();
            }
        } else if (calls.size() > 0) {
            naturalPtr = calls.get(calls.size() - 1).getNaturalPtr(convertFlag);
        }
        return naturalPtr;
    }

    public void request() {
        directRequest = true;
        requestGet(null);
    }

    public void requestGet(Pointer pointer) {
        getNaturalPtr(pointer);
        if (pointer == null) pointer = getNaturalPtr(pointer);
        pointer = pointer.toLet();

        requestPtr = pointer;

        if (option != null) {
            left.requestGet(cFile.langBoolPtr());
            if (pointer != null) {
                right.requestGet(pointer);
                option.requestGet(pointer);
            } else {
                cFile.erro(getTokenGroup(), "Incompatible Ternary Members values", this);
            }
        } else if (operatorView == OperatorView.CAST) {
            left.setCastGet(pointer);
            center.requestGet(center.getNaturalPtr(left.getCastPtr()));
            naturalPtr = left.getCastPtr();
        } else if (calls.size() > 0) {
            calls.get(calls.size() - 1).requestGet(pointer);
        } else {
            if (naturalPtr != null && naturalPtr != pointer && pointer.canReceive(naturalPtr) <= 0) {
                cFile.erro(getTokenGroup(), "Cannot cast [" + naturalPtr + "] to [" + pointer + "]", this);
            }
        }
    }

    public void requestOwn(Pointer pointer) {
        getNaturalPtr(pointer);
        if (pointer == null) pointer = naturalPtr;
        requestPtr = pointer;

        if (option != null) {
            left.requestGet(cFile.langBoolPtr());
            if (pointer != null) {
                right.requestOwn(pointer);
                option.requestOwn(pointer);
            } else {
                cFile.erro(getTokenGroup(), "Incompatible Ternary Members values", this);
            }
        } else if (operatorView == OperatorView.CAST) {
            if (left.setCastOwn(pointer)) {
                center.requestOwn(center.getNaturalPtr(left.getCastPtr()));
            } else {
                center.requestGet(center.getNaturalPtr(left.getCastPtr()));
            }
            if (naturalPtr != null && pointer.canReceive(naturalPtr) <= 0) {
                cFile.erro(getTokenGroup(), "Cannot cast [" + naturalPtr + "] to [" + pointer + "]", this);
            }
        } else if (calls.size() > 0) {
            calls.get(calls.size() - 1).requestOwn(pointer);
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

    public void build(CppBuilder cBuilder, int idt) {
        boolean autocast = requestPtr != null && !requestPtr.equalsIgnoreLet(naturalPtr) &&
                naturalPtr != Pointer.nullPointer && (!requestPtr.isLangBase() || !naturalPtr.isLangBase());
        if (autocast) {
            if (naturalPtr.type != null && naturalPtr.isDerivedFrom(requestPtr) > 0) {
                cBuilder.add(requestPtr).add("(");
            } else {
                cBuilder.add("cast<").add(naturalPtr).add(", ").add(requestPtr).add(">::as(");
            }
        }

        if (left != null && center != null && right != null && colon != null && option != null) {
            cBuilder.add(left, idt).add(" ? ").add(right, idt).add(" : ").add(option, idt);
        } else if (left != null && center != null && right != null) {
            if (operatorView == OperatorView.EQUAL || operatorView == OperatorView.DIF ||
                    operatorView == OperatorView.AND || operatorView == OperatorView.OR) {
                cBuilder.add(left, idt).add(" ").add(center.operatorToken).add(" ").add(right, idt);
            } else if (operatorView == OperatorView.IS || operatorView == OperatorView.ISNOT) {
                cBuilder.add("cast<").add(left.getNaturalPtr()).add(", ");
                right.getLastCall().build(cBuilder, idt, false);
                cBuilder.add(">::is(").add(left, idt).add(")");
            } else if (center.operatorToken.key.isSet()) {
                if (center.operatorToken.key == Key.SETVAL) {
                    buildSet(cBuilder, idt);
                } else {
                    buildComposite(cBuilder, idt);
                }
            } else if (operatorView.operator != null && operatorView.operator.type.isLangBase()) {
                cBuilder.add(left, idt).add(" ").add(center.operatorToken).add(" ").add(right, idt);
            } else {
                cBuilder.path(operatorView.caller, false).add("::").nameOp(operatorView.operator.getOp(), null)
                        .add("(").add(left, idt).add(", ").add(right, idt).add(")");
            }
        } else if (left != null && left.isCastingOperator() && center != null) {
            cBuilder.cast(center.getRequestPtr(), left.getCastPtr(), center, idt);
        } else if (left != null && center != null) {
            if (center.getRequestPtr().isLangBase()) {
                if (rightOperator) {
                    cBuilder.add(center, idt).add(left.operatorToken);
                } else {
                    cBuilder.add(left.operatorToken).add(center, idt);
                }
            } else {
                // TODO - LEFT RIGHT IMPLEMENTATION
            }
        } else if (calls.size() > 0) {
            for (int i = 0; i < calls.size(); i++) {
                Call call = calls.get(i);
                call.build(cBuilder, idt, i <  calls.size() - 1);
            }
        }

        if (autocast) {
            cBuilder.add(")");
        }
    }

    public void buildLine(CppBuilder cBuilder, int idt) {
        if (isLineCall()) {
            for (int i = 0; i < calls.size() - 1; i++) {
                Call call = calls.get(i);
                call.build(cBuilder, idt, i <  calls.size() - 2);
            }
        }
    }

    public void buildCall(CppBuilder cBuilder, int idt, boolean set) {
        if (isLineCall()) {
            cBuilder.add(calls.get(calls.size() - 2).next());
            if (set) {
                calls.get(calls.size() - 1).buildSet(cBuilder, idt);
            } else {
                calls.get(calls.size() - 1).build(cBuilder, idt, false);
            }
        } else {
            for (int i = 0; i < calls.size(); i++) {
                Call call = calls.get(i);
                if (i <  calls.size() - 1) {
                    call.build(cBuilder, idt, true);
                } else {
                    if (set) {
                        call.buildSet(cBuilder, idt);
                    } else {
                        call.build(cBuilder, idt, false);
                    }
                }
            }
        }
    }

    public boolean isMethodSetter() {
        return calls.get(calls.size() - 1).isMethodSetter();
    }

    public void buildSet(CppBuilder cBuilder, int idt) {
        if (directRequest) {
            if (left.isMethodSetter()) {
                left.build(cBuilder, idt);
                right.build(cBuilder, idt);
                cBuilder.add(")");
            } else {
                left.build(cBuilder, idt);
                cBuilder.add(" = ");
                right.build(cBuilder, idt);
            }
        } else {
            Temp t1 = cBuilder.temp(right.getRequestPtr().toLet());
            cBuilder.add("(");
            left.build(cBuilder, idt);
            if (!left.isMethodSetter()) {
                cBuilder.add(" = ");
            }
            cBuilder.add(t1).add(" = ").add(right, idt);
            if (left.isMethodSetter()) {
                cBuilder.add(")");
            }
            cBuilder.add(", ").add(t1).add(")");
        }
    }
    public void buildComposite(CppBuilder cBuilder, int idt) {
        Key op = center.getOperatorToken().key;
        op = Key.getComposite(op);

        if (directRequest && operatorView.operator.type.isLangBase() && !left.isMethodSetter() &&
                left.getNaturalPtr().equals(right.getNaturalPtr())) {
            left.buildLine(cBuilder, idt);
            left.buildCall(cBuilder, idt, true);
            cBuilder.add(" ").add(center.operatorToken.key).add(" ").add(right, idt);
            return;
        }

        // (t = left.line, t.callSET = t2 = op(t.callGOW, right.line), t2)
        Temp tl = null;
        Temp tr = null ;
        if (left.isLineCall()) tl = cBuilder.temp(left.getLineRequestPtr().toLet());
        if (!directRequest) tr = cBuilder.temp(right.getRequestPtr());
        if (tl != null || tr != null) {
            cBuilder.add("(");
        }
        if (tl != null) {
            cBuilder.add(tl).add(" = ");
            left.buildLine(cBuilder, idt);
            cBuilder.add(", ");
        }
        if (tl != null) {
            cBuilder.add(tl);
        }
        left.buildCall(cBuilder, idt, true);
        if (!left.isMethodSetter()) {
            cBuilder.add(" = ");
        }
        if (tr != null) {
            cBuilder.add("(").add(tr).add(" = ");
        }

        if (operatorView.operator == null || operatorView.operator.type.isLangBase()) {
            if (tl != null) {
                cBuilder.add(tl);
            }
            left.buildCall(cBuilder, idt, false);
            cBuilder.add(" ").add(op).add(" ").add(right, idt);
        } else {
            cBuilder.path(operatorView.caller, false).add("::").nameOp(operatorView.operator.getOp(), null);
            cBuilder.add("(");
            if (tl != null) {
                cBuilder.add(tl);
            }
            left.buildCall(cBuilder, idt, false);
            cBuilder.add(", ").add(right, idt).add(")");
        }
        if (tr != null) {
            cBuilder.add(")");
        }
        if (left.isMethodSetter()) {
            cBuilder.add(")");
        }
        if (tr != null) {
            cBuilder.add(", ").add(tr);
        }
        if (tl != null || tr != null) {
            cBuilder.add(")");
        }
    }

    @Override
    public String toString() {
        return getTokenGroup().toString();
    }
}
