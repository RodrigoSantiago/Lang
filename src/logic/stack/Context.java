package logic.stack;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.Pointer;
import logic.Using;
import logic.member.Constructor;
import logic.member.view.*;
import logic.stack.expression.CallGroup;
import logic.stack.expression.Expression;
import logic.typdef.Type;

import java.util.ArrayList;

public class Context {
    Stack stack;

    public Type type;
    public Pointer pointer;
    public Pointer innerThis;
    private boolean isStatic;
    private boolean isBegin;
    private boolean isIncorrect;

    public Context(Stack stack) {
        this(stack, null);
    }

    public Context(Stack stack, Pointer innerThis) {
        this.stack = stack;
        this.pointer = innerThis != null ? innerThis : stack.getSourcePtr();
        this.isStatic = innerThis == null && stack.isStatic();
        this.type = pointer.type;
        this.innerThis = innerThis;
        isBegin = true;
    }

    public Context(Context context) {
        this(context.stack, context.innerThis);
    }

    public void jumpTo(Type type, boolean isStatic) {
        this.isStatic = isStatic;
        this.pointer = type.self;
        this.type = type;
        isBegin = false;
    }

    public void jumpTo(Pointer pointer) {
        if (pointer == null) {
            isIncorrect = true;
            this.pointer = innerThis != null ? innerThis : stack.getSourcePtr();
            this.isStatic = innerThis == null && stack.isStatic();
        } else {
            this.isStatic = false;
            this.pointer = pointer;
            this.type = pointer.type;
        }
        isBegin = false;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isIncorrect() {
        return isIncorrect;
    }

    public boolean isBegin() {
        return isBegin;
    }

    public Pointer getPointer(TokenGroup typeToken) {
        return getPointer(typeToken, false);
    }

    public Pointer getPointer(TokenGroup typeToken, boolean isLet) {
        return stack.cFile.getPointer(typeToken.start, typeToken.end, null, null, isLet);
    }

    public Type findType(Token typeName) {
        return stack.cFile.findType(typeName);
    }

    public Field findLocalField(Token nameToken) {
        if (!isBegin) return null;

        return stack.findField(nameToken);
    }

    public FieldView findField(Token nameToken) {
        if (type == null) return null;

        FieldView fieldView = type.getField(nameToken);
        if (fieldView != null) {
            if (!isStatic && pointer.pointers != null) fieldView = new FieldView(pointer, fieldView);
        } else if (isBegin) {
            for (Using using : stack.cFile.usingsStaticDirect) {
                if (using.getMemberToken().equals(nameToken)) {
                    FieldView sField = using.getStaticType().getField(nameToken);
                    if (sField != null && sField.isStatic()) {
                        return sField;
                    }
                }
            }
            for (Using using : stack.cFile.usingsStatic) {
                FieldView sField = using.getStaticType().getField(nameToken);
                if (sField != null && sField.isStatic()) {
                    return sField;
                }
            }
        }
        return fieldView;
    }

    private ArrayList<MethodView> findFunctionCall(Token nameToken, ArrayList<Expression> arguments) {
        Pointer[] ptrs = pointer.pointers;
        int len = ptrs.length - 1;
        if (len > arguments.size()) {
            len = arguments.size();
            stack.cFile.erro(nameToken, "Missing arguments", this);
        } else if (len < arguments.size()) {
            stack.cFile.erro(nameToken, "Too much arguments", this);
        }
        for (int i = 0; i < arguments.size(); i++) {
            arguments.get(i).requestOwn(i > len ? null : ptrs[i + 1]);
        }

        ArrayList<MethodView> found = new ArrayList<>();
        found.add(new MethodView(stack.cFile.langFunction().getMethod(new Token("call")).get(0).method, pointer));
        return found;
    }

    public ArrayList<MethodView> findMethod(Token nameToken, ArrayList<Expression> arguments) {
        for (Expression arg : arguments) {
            arg.load(new Context(this));
        }

        if (type != null && type.isFunction() && !isStatic && nameToken.equals("call")) {
            return findFunctionCall(nameToken, arguments);
        }
        if (type != null) {
            ArrayList<MethodView> found = null;
            final int[] closer = new int[arguments.size()];
            final int[] result = new int[arguments.size()];

            // TODO - With no method found, try the closer method with name
            int markArgs = 0;

            ArrayList<MethodView> methods = type.getMethod(nameToken);
            for (MethodView mv : methods) {
                if (mv.getParams().getArgsCount() == arguments.size()) {
                    markArgs++;
                    found = find(mv, closer, result, arguments, found);
                }
            }
            if (isBegin) {
                for (Using using : stack.cFile.usingsStaticDirect) {
                    if (using.getMemberToken().equals(nameToken)) {
                        methods = using.getStaticType().getMethod(nameToken);
                        for (MethodView mv : methods) {
                            if (mv.getParams().getArgsCount() == arguments.size()) {
                                markArgs++;
                                found = find(mv, closer, result, arguments, found);
                            }
                        }
                    }
                }
                for (Using using : stack.cFile.usingsStatic) {
                    methods = using.getStaticType().getMethod(nameToken);
                    for (MethodView mv : methods) {
                        if (mv.getParams().getArgsCount() == arguments.size()) {
                            markArgs++;
                            found = find(mv, closer, result, arguments, found);
                        }
                    }
                }
            }

            if (found != null) {
                for (int i = 0; i < arguments.size(); i++) {
                    Expression arg = arguments.get(i);
                    arg.requestOwn(found.get(0).getParams().getArgTypePtr(i));
                    if (markArgs > 1) arg.markArgument();
                }
                return found;
            }
        }
        for (Expression arg : arguments) {
            arg.requestOwn(null);
        }
        return null;
    }

    private ArrayList<MethodView> find(MethodView mv, int[] closer, int[] result, ArrayList<Expression> arguments,
                                       ArrayList<MethodView> found) {
        if (!isStatic && pointer.pointers != null) mv = new MethodView(pointer, mv);
        if (mv.getTemplate() != null) mv = MethodView.byTemplate(arguments, mv);

        int ret = mv.getParams().verifyArguments(closer, result, arguments, found != null);
        if (ret == 0) {
            // invalid
        } else if (ret == 1) {
            if (found == null) found = new ArrayList<>();
            found.clear();
            found.add(mv);
        } else if (ret == 2) {
            if (found == null) found = new ArrayList<>();
            found.add(mv);
        }
        return found;
    }

    public ArrayList<IndexerView> findIndexer(ArrayList<Expression> arguments) {
        for (Expression arg : arguments) {
            arg.load(new Context(this));
        }
        if (type != null) {
            ArrayList<IndexerView> found = null;
            final int[] closer = new int[arguments.size()];
            final int[] result = new int[arguments.size()];

            int markArgs = 0;
            for (int it = 0; it < type.getIndexersCount(); it++) {
                IndexerView iv = type.getIndexer(it);
                if (iv.getParams().getArgsCount() == arguments.size()) {
                    markArgs ++;
                    if (!isStatic && pointer.pointers != null) iv = new IndexerView(pointer, iv);

                    int ret = iv.getParams().verifyArguments(closer, result, arguments, found != null);
                    if (ret == 0) {
                        // invalid
                    } else if (ret == 1) {
                        if (found == null) found = new ArrayList<>();
                        found.clear();
                        found.add(iv);
                    } else if (ret == 2) {
                        if (found == null) found = new ArrayList<>();
                        found.add(iv);
                    }
                }
            }
            if (found != null) {
                for (int i = 0; i < arguments.size(); i++) {
                    Expression arg = arguments.get(i);
                    arg.requestOwn(found.get(0).getParams().getArgTypePtr(i));
                    if (markArgs > 1) arg.markArgument();
                }
                return found;
            }
        }
        for (Expression arg : arguments) {
            arg.requestOwn(null);
        }
        return null;
    }

    public ArrayList<ConstructorView> findConstructor(Pointer typePtr, ArrayList<Expression> arguments) {
        for (Expression arg : arguments) {
            arg.load(new Context(this));
        }
        if (typePtr.type != null) {
            ArrayList<ConstructorView> found = null;
            final int[] closer = new int[arguments.size()];
            final int[] result = new int[arguments.size()];
            int markArgs = 0;

            for (int it = 0; it < typePtr.type.getConstructorsCount(); it++) {
                Constructor constructor = typePtr.type.getConstructor(it);
                if (constructor.getParams().getCount() == arguments.size()) {
                    markArgs++;
                    ConstructorView cv = new ConstructorView(typePtr, typePtr.type.getConstructor(it));

                    int ret = cv.getParams().verifyArguments(closer, result, arguments, found != null);
                    if (ret == 0) {
                        // invalid
                    } else if (ret == 1) {
                        if (found == null) found = new ArrayList<>();
                        found.clear();
                        found.add(cv);
                    } else if (ret == 2) {
                        if (found == null) found = new ArrayList<>();
                        found.add(cv);
                    }
                }
            }
            if (found == null && arguments.size() == 0 && typePtr.type.isStruct()) {
                found = new ArrayList<>();
                found.add(ConstructorView.structInit);
            } else if (found == null && arguments.size() == 0 && typePtr.type.getParentEmptyConstructor() != null) {
                ConstructorView cv = new ConstructorView(typePtr, typePtr.type.getParentEmptyConstructor());

                int ret = cv.getParams().verifyArguments(closer, result, arguments, false);
                if (ret == 1) {
                    found = new ArrayList<>();
                    found.add(cv);
                }
            }
            if (found != null) {
                for (int i = 0; i < arguments.size(); i++) {
                    Expression arg = arguments.get(i);
                    arg.requestOwn(found.get(0).getParams().getArgTypePtr(i));
                    if (markArgs > 1) arg.markArgument();
                }
                return found;
            }
        }
        for (Expression arg : arguments) {
            arg.requestOwn(null);
        }
        return null;
    }

    public OperatorView findOperator(CallGroup left, CallGroup center) {
        center.load(new Context(this));

        Pointer ptr = center.getNaturalPtr(null);

        if (left.isCastingOperator()) {
            Pointer castPtr = left.getCastPtr();
            if (castPtr == null || ptr == null) return null;
            if (castPtr.isOpen() || ptr.isOpen()) return OperatorView.CAST;
            if (castPtr.type != null && castPtr.type.isPointer()) return OperatorView.CAST;
            if (castPtr.type != null && ptr.type != null && castPtr.type.isValue() && ptr.type.isValue()) {
                if (ptr.type != null && ptr.type.casts.contains(castPtr)) return OperatorView.CAST;
                if (ptr.type != null && ptr.type.autoCast.contains(castPtr)) return OperatorView.CAST;
            }
            if (center.verify(castPtr) > 0) return OperatorView.CAST;
            return null;
        } else {

            Token opToken = left.getOperatorToken();
            if (opToken.key == Key.INC || opToken.key == Key.DEC) {
                center.requestSet();
            }

            if (ptr != null && ptr.type != null) {
                ArrayList<OperatorView> operators = ptr.type.getOperator(opToken);
                for (OperatorView ov : operators) {
                    if (ov.getParams().getArgsCount() == 1) {
                        center.requestOwn(ov.getParams().getArgTypePtr(0));
                        return ov;
                    }
                }
            }
        }
        center.requestOwn(null);
        return null;
    }

    public ArrayList<OperatorView> findOperator(CallGroup left, CallGroup center, CallGroup right) {
        left.load(new Context(this));
        right.load(new Context(this));

        boolean isComposite = false;
        Token opToken = center.getOperatorToken();

        if (opToken.key == Key.EQUAL || opToken.key == Key.DIF) {
            Pointer lptr = left.getNaturalPtr(null);
            Pointer rptr = right.getNaturalPtr(null);
            if (lptr != null && rptr != null && lptr != Pointer.voidPointer && rptr != Pointer.voidPointer) {
                if ((lptr == Pointer.nullPointer && rptr == Pointer.nullPointer) ||
                        (lptr.isOpen() && rptr.isOpen()) ||
                        (lptr.isOpen() && rptr != Pointer.nullPointer) ||
                        (lptr != Pointer.nullPointer && rptr.isOpen()) ||
                        (lptr.isPointer() && rptr.isPointer()) ||
                        (lptr.isPointer() && rptr == Pointer.nullPointer) ||
                        (lptr == Pointer.nullPointer && rptr.isPointer())) {
                    left.requestGet(null);
                    right.requestGet(null);

                    ArrayList<OperatorView> operators = new ArrayList<>();
                    operators.add(opToken.key == Key.EQUAL ? OperatorView.EQUAL : OperatorView.DIF);
                    return operators;
                }
            }
        } else if (opToken.key == Key.AND || opToken.key == Key.OR) {
            left.requestGet(stack.cFile.langBoolPtr());
            right.requestGet(stack.cFile.langBoolPtr());

            ArrayList<OperatorView> operators = new ArrayList<>();
            operators.add(opToken.key == Key.AND ? OperatorView.AND : OperatorView.OR);
            return operators;
        } else if (opToken.key == Key.IS || opToken.key == Key.ISNOT) {
            left.requestGet(null);
            // right.requestGet(null); [TYPE CALL]

            if (!right.isTypeCall()) {
                right.requestGet(null);
                stack.cFile.erro(left.getTokenGroup(), "Type expected", this);
            }
            ArrayList<OperatorView> operators = new ArrayList<>();
            operators.add(opToken.key == Key.IS ? OperatorView.IS : OperatorView.ISNOT);
            return operators;
        } else if (opToken.key == Key.SETVAL) {
            left.requestSet();
            right.requestOwn(left.getNaturalPtr(null));

            ArrayList<OperatorView> operators = new ArrayList<>();
            operators.add(OperatorView.SET);
            return operators;
        }

        if (Key.getComposite(opToken.key) != opToken.key) {
            left.requestSet();

            isComposite = true;
            Key key = Key.getComposite(opToken.key);
            opToken = new Token(key.string, 0, key.string.length(), key, false);
        }

        int repeat = 0;
        Pointer ptr = left.getNaturalPtr(null);
        do {
            if (ptr != null && ptr.type != null) {
                ArrayList<OperatorView> found = null;
                final int[] closer = new int[2];
                final int[] result = new int[2];
                int markArgs = 0;

                ArrayList<OperatorView> operators = ptr.type.getOperator(opToken);
                for (OperatorView ov : operators) {
                    if (ov.getParams().getArgsCount() == 2) {
                        markArgs ++;
                        if (ptr.pointers != null) ov = new OperatorView(ptr, ov);

                        int ret = ov.getParams().verifyArguments(closer, result, left, right, found != null);
                        if (ret == 0) {
                            // invalid
                        } else if (ret == 1) {
                            if (found == null) found = new ArrayList<>();
                            found.clear();
                            found.add(ov);
                        } else if (ret == 2) {
                            if (found == null) found = new ArrayList<>();
                            found.add(ov);
                        }
                    }
                }
                if (found != null) {
                    left.requestOwn(found.get(0).getParams().getArgTypePtr(0));
                    right.requestOwn(found.get(0).getParams().getArgTypePtr(1));
                    if (markArgs > 1 && !found.get(0).operator.type.isLangBase()) {
                        left.markArgument();
                        right.markArgument();
                    }

                    if (isComposite) {
                        Pointer returnType = found.get(0).getTypePtr();

                        if (ptr.canReceive(returnType) <= 0) {
                            stack.cFile.erro(left.getTokenGroup(), "Cannot cast [" + returnType + "] to [" + ptr + "]", this);
                        } else if (Pointer.OwnTable(returnType, ptr) == -1) {
                            stack.cFile.erro(left.getTokenGroup(), "Cannot convert a STRONG reference to a WEAK reference", this);
                        }
                    }
                    return found;
                }
            }
            if (!isComposite) {
                ptr = right.getNaturalPtr(null);
                repeat += 1;
            }
        } while (repeat == 1);

        right.requestOwn(null);
        return null;
    }
}
