package logic.stack;

import content.Token;
import content.TokenGroup;
import logic.Pointer;
import logic.member.Constructor;
import logic.member.Operator;
import logic.member.view.*;
import logic.stack.expression.CallGroup;
import logic.stack.expression.Expression;
import logic.typdef.Type;

import java.util.ArrayList;

public class Context {
    Stack stack;

    public Type type;
    public Pointer pointer;
    private boolean isStatic;
    private boolean isIncorrect;
    private boolean isBegin;

    public Context(Stack stack) {
        this.stack = stack;
        this.pointer = stack.getSourcePtr();
        this.isStatic = stack.isStatic();
        this.type = pointer.type;
        isBegin = true;
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
            this.pointer = stack.getSourcePtr();
            this.isStatic = stack.isStatic();
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

    public void resolve(Expression expression) {
        expression.load(new Context(stack));
        expression.request(null);
    }

    public Field findLocalField(Token nameToken) {
        if (!isBegin) return null;
        return stack.findField(nameToken);
    }

    public FieldView findField(Token nameToken) {
        if (type == null) return null;
        FieldView fieldView = type.getField(nameToken);
        if (fieldView != null) {
            if (isStatic && !fieldView.isStatic()) return null;
            if (!isStatic && pointer.pointers != null) fieldView = new FieldView(pointer, fieldView);
        }
        return fieldView;
    }

    public ArrayList<MethodView> findMethod(Token nameToken, ArrayList<Expression> arguments) {
        for (Expression arg : arguments) {
            arg.load(new Context(stack));
        }
        if (type != null) {
            ArrayList<MethodView> found = null;
            final int[] closer = new int[arguments.size()];
            final int[] result = new int[arguments.size()];

            ArrayList<MethodView> methods = type.getMethod(nameToken);
            for (MethodView mv : methods) {
                if (!isStatic && pointer.pointers != null) mv = new MethodView(pointer, mv);

                if (mv.getParams().getArgsCount() == arguments.size()) {
                    int ret = mv.getParams().verifyArguments(closer, result, arguments, found == null);
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
            if (found != null && found.size() == 1) {
                for (int i = 0; i < arguments.size(); i++) {
                    Expression arg = arguments.get(i);
                    arg.request(found.get(i).getParams().getArgTypePtr(i));
                }
                return found;
            }
        }
        for (Expression arg : arguments) {
            arg.request(null);
        }
        return null;
    }

    public ArrayList<IndexerView> findIndexer(ArrayList<Expression> arguments) {
        for (Expression arg : arguments) {
            arg.load(new Context(stack));
        }
        if (type != null) {
            ArrayList<IndexerView> found = null;
            final int[] closer = new int[arguments.size()];
            final int[] result = new int[arguments.size()];

            for (int it = 0; it < type.getIndexersCount(); it++) {
                IndexerView iv = type.getIndexer(it);
                if (!isStatic && pointer.pointers != null) iv = new IndexerView(pointer, iv);

                if (iv.getParams().getArgsCount() == arguments.size()) {
                    int ret = iv.getParams().verifyArguments(closer, result, arguments, found == null);
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
            if (found != null && found.size() == 1) {
                for (int i = 0; i < arguments.size(); i++) {
                    Expression arg = arguments.get(i);
                    arg.request(found.get(i).getParams().getArgTypePtr(i));
                }
                return found;
            }
        }
        for (Expression arg : arguments) {
            arg.request(null);
        }
        return null;
    }

    public ArrayList<ConstructorView> findConstructor(Pointer typePtr, ArrayList<Expression> arguments) {
        for (Expression arg : arguments) {
            arg.load(new Context(stack));
        }
        if (typePtr.type != null) {
            ArrayList<ConstructorView> found = null;
            final int[] closer = new int[arguments.size()];
            final int[] result = new int[arguments.size()];

            for (int it = 0; it < typePtr.type.getConstructorsCount(); it++) {
                Constructor constructor = typePtr.type.getConstructor(it);
                if (constructor.getParams().getCount() == arguments.size()) {
                    ConstructorView cv = new ConstructorView(typePtr, typePtr.type.getConstructor(it));

                    int ret = cv.getParams().verifyArguments(closer, result, arguments, found == null);
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
            if (found != null && found.size() == 1) {
                for (int i = 0; i < arguments.size(); i++) {
                    Expression arg = arguments.get(i);
                    arg.request(found.get(i).getParams().getArgTypePtr(i));
                }
                return found;
            }
        }
        for (Expression arg : arguments) {
            arg.request(null);
        }
        return null;
    }

    public OperatorView findOperator(CallGroup opToken, CallGroup exp) {
        exp.load(new Context(stack));
        exp.request(null);
        Pointer ptr = exp.getReturnType();

        if (opToken.isCastingOperator()) {
            Pointer castPtr = opToken.getCastPtr();
            if (castPtr.toLet().canReceive(ptr) != 0) return OperatorView.CAST;
            if (castPtr.type != null && castPtr.type.isInterface()) return OperatorView.CAST;
            if (ptr.type != null && ptr.type.casts.contains(castPtr)) return OperatorView.CAST;
            if (ptr.type != null && ptr.type.autoCast.contains(castPtr)) return OperatorView.CAST;
        } else if (ptr != null && ptr.type != null) {
            ArrayList<OperatorView> found = ptr.type.getOperator(opToken.getOperatorToken());
            if (found != null && found.size() == 1) {
                return found.get(0);
            }
        }
        return null;
    }

    public ArrayList<OperatorView> findOperator(CallGroup left, CallGroup opToken, CallGroup right) {
        left.load(new Context(stack));
        right.load(new Context(stack));
        left.request(null);

        Pointer ptr = left.getReturnType();
        if (ptr != null && ptr.type != null) {
            int repeat = 0;
            do {
                ArrayList<OperatorView> found = null;
                final int[] closer = new int[2];
                final int[] result = new int[2];

                ArrayList<OperatorView> operators = ptr.type.getOperator(opToken.getOperatorToken());
                for (OperatorView ov : operators) {
                    if (ov.getParams().getArgsCount() == 2) {
                        int ret = ov.getParams().verifyArguments(closer, result, left, right, found == null);
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
                if (found != null && found.size() == 1) {
                    right.request(found.get(0).getParams().getArgTypePtr(1));
                    return found;
                } else {
                    CallGroup swip = left;
                    left = right;
                    right = swip;
                    repeat += 1;
                }
            } while (repeat == 1);
        }
        right.request(null);
        return null;
    }
}
