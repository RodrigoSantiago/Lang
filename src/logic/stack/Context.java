package logic.stack;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.Pointer;
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
        }
        return fieldView;
    }

    private ArrayList<MethodView> findFunctionRun(Token nameToken, ArrayList<Expression> arguments) {
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
        found.add(new MethodView(stack.cFile.langFunction().getMethod(new Token("run")).get(0).method, pointer));
        return found;
    }

    public ArrayList<MethodView> findMethod(Token nameToken, ArrayList<Expression> arguments) {
        for (Expression arg : arguments) {
            arg.load(new Context(this));
        }

        if (type != null && type.isFunction() && !isStatic && nameToken.equals("run")) {
            return findFunctionRun(nameToken, arguments);
        }
        if (type != null) {
            ArrayList<MethodView> found = null;
            final int[] closer = new int[arguments.size()];
            final int[] result = new int[arguments.size()];

            ArrayList<MethodView> methods = type.getMethod(nameToken);
            for (MethodView mv : methods) {
                if (mv.getParams().getArgsCount() == arguments.size()) {
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
                }
            }
            if (found != null) {
                for (int i = 0; i < arguments.size(); i++) {
                    Expression arg = arguments.get(i);
                    arg.requestOwn(found.get(0).getParams().getArgTypePtr(i));
                }
                return found;
            }
        }
        for (Expression arg : arguments) {
            arg.requestOwn(null);
        }
        return null;
    }

    public ArrayList<IndexerView> findIndexer(ArrayList<Expression> arguments) {
        for (Expression arg : arguments) {
            arg.load(new Context(this));
        }
        if (type != null) {
            ArrayList<IndexerView> found = null;
            final int[] closer = new int[arguments.size()];
            final int[] result = new int[arguments.size()];

            for (int it = 0; it < type.getIndexersCount(); it++) {
                IndexerView iv = type.getIndexer(it);
                if (iv.getParams().getArgsCount() == arguments.size()) {
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

            for (int it = 0; it < typePtr.type.getConstructorsCount(); it++) {
                Constructor constructor = typePtr.type.getConstructor(it);
                if (constructor.getParams().getCount() == arguments.size()) {
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
            if (found == null && arguments.size() == 0 && typePtr.type.getParentEmptyConstructor() != null) {
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
            } else {
                if (castPtr.canReceive(ptr) > 0 || ptr.canReceive(castPtr) > 0) return OperatorView.CAST;
            }
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
            Pointer lPointer = left.getNaturalPtr(null);
            Pointer rPointer = right.getNaturalPtr(null);
            if (lPointer != null && rPointer != null &&
                    lPointer != Pointer.voidPointer && rPointer != Pointer.voidPointer) {

                if ((lPointer == Pointer.nullPointer && rPointer == Pointer.nullPointer) ||
                        (lPointer.isOpen() && rPointer.isOpen()) ||
                        (lPointer == Pointer.nullPointer && rPointer.type != null && rPointer.type.isPointer()) ||
                        (rPointer == Pointer.nullPointer && lPointer.type != null && lPointer.type.isPointer()) ||
                        (lPointer.type != null && lPointer.type.isPointer() &&
                                rPointer.type != null && rPointer.type.isPointer())) {
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

                ArrayList<OperatorView> operators = ptr.type.getOperator(opToken);
                for (OperatorView ov : operators) {
                    if (ov.getParams().getArgsCount() == 2) {
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

    public boolean isBegin() {
        return isBegin;
    }
}
