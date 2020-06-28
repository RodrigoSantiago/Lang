package logic.stack.expression;

import content.Key;

public class LiteralResolver {

    public static LiteralCall resolve(CallGroup group, LiteralCall lCall, LiteralCall rCall, Key op) {
        int lType = lCall.getLiteralType();
        int rType = rCall.getLiteralType();

        if (lType == LiteralCall.BOOL && rType == LiteralCall.BOOL) {
            boolean d = false;
            boolean ld = lCall.resultBool;
            boolean rd = rCall.resultBool;
            if (op == Key.OR) d = ld || rd;
            else if (op == Key.AND) d = ld && rd;
            else if (op == Key.EQUAL) d = ld == rd;
            else if (op == Key.DIF) d = ld != rd;
            return new LiteralCall(group,
                    lCall.getToken(), rCall.getToken(),
                    LiteralCall.BOOL, d, 0, 0, null,
                    false, false, false);
        } else if (lType == LiteralCall.STRING || rType == LiteralCall.STRING) {
            String d = "";
            String ld = lCall.resultStr;
            String rd = rCall.resultStr;
            if (op == Key.ADD) d = ld + rd;
            else if (op == Key.EQUAL || op == Key.DIF || op == Key.MORE || op == Key.LESS ||
                    op == Key.EMORE || op == Key.ELESS) {
                boolean v = false;
                if (op == Key.EQUAL) v = ld.equals(rd);
                else if (op == Key.DIF) v = !ld.equals(rd);
                else if (op == Key.MORE) v = ld.compareTo(rd) > 0;
                else if (op == Key.LESS) v = ld.compareTo(rd) < 0;
                else if (op == Key.EMORE) v = ld.compareTo(rd) >= 0;
                else if (op == Key.ELESS) v = ld.compareTo(rd) <= 0;

                return new LiteralCall(group,
                        lCall.getToken(), rCall.getToken(),
                        LiteralCall.BOOL, v, 0, 0, null,
                        false, false, false);
            }
            return new LiteralCall(group,
                    lCall.getToken(), rCall.getToken(),
                    LiteralCall.STRING, false, 0, 0, d,
                    false, false, false);
        } else if (lType == LiteralCall.DOUBLE && rType == LiteralCall.DOUBLE) {
            double d = 0;
            double ld = lCall.resultDouble;
            double rd = rCall.resultDouble;
            if (op == Key.ADD) d = ld + rd;
            else if (op == Key.SUB) d = ld - rd;
            else if (op == Key.DIV) d = ld / rd;
            else if (op == Key.MUL) d = ld * rd;
            else if (op == Key.EQUAL || op == Key.DIF || op == Key.MORE || op == Key.LESS ||
                    op == Key.EMORE || op == Key.ELESS) {
                boolean v = false;
                if (op == Key.EQUAL) v = ld == rd;
                else if (op == Key.DIF) v = ld != rd;
                else if (op == Key.MORE) v = ld > rd;
                else if (op == Key.LESS) v = ld < rd;
                else if (op == Key.EMORE) v = ld >= rd;
                else if (op == Key.ELESS) v = ld <= rd;

                return new LiteralCall(group,
                        lCall.getToken(), rCall.getToken(),
                        LiteralCall.BOOL, v, 0, 0, null,
                        false, false, false);
            }
            return new LiteralCall(group,
                    lCall.getToken(), rCall.getToken(),
                    LiteralCall.DOUBLE, false, 0, d, null,
                    false, (lCall.isFloat || rCall.isFloat) && !(lCall.isDouble || rCall.isDouble),
                    lCall.isDouble || rCall.isDouble);
        } else if (lType == LiteralCall.LONG && rType == LiteralCall.DOUBLE) {
            double d = 0;
            long ld = lCall.resultNum;
            double rd = rCall.resultDouble;
            if (op == Key.ADD) d = ld + rd;
            else if (op == Key.SUB) d = ld - rd;
            else if (op == Key.DIV) d = ld / rd;
            else if (op == Key.MUL) d = ld * rd;
            else if (op == Key.EQUAL || op == Key.DIF || op == Key.MORE || op == Key.LESS ||
                    op == Key.EMORE || op == Key.ELESS) {
                boolean v = false;
                if (op == Key.EQUAL) v = ld == rd;
                else if (op == Key.DIF) v = ld != rd;
                else if (op == Key.MORE) v = ld > rd;
                else if (op == Key.LESS) v = ld < rd;
                else if (op == Key.EMORE) v = ld >= rd;
                else if (op == Key.ELESS) v = ld <= rd;

                return new LiteralCall(group,
                        lCall.getToken(), rCall.getToken(),
                        LiteralCall.BOOL, v, 0, 0, null,
                        false, false, false);
            }
            return new LiteralCall(group,
                    lCall.getToken(), rCall.getToken(),
                    LiteralCall.DOUBLE, false, 0, d, null,
                    false, (lCall.isFloat || rCall.isFloat) && !(lCall.isDouble || rCall.isDouble),
                    lCall.isDouble || rCall.isDouble);
        } else if (lType == LiteralCall.DOUBLE && rType == LiteralCall.LONG) {
            double d = 0;
            double ld = lCall.resultDouble;
            long rd = rCall.resultNum;
            if (op == Key.ADD) d = ld + rd;
            else if (op == Key.SUB) d = ld - rd;
            else if (op == Key.DIV) d = ld / rd;
            else if (op == Key.MUL) d = ld * rd;
            else if (op == Key.EQUAL || op == Key.DIF || op == Key.MORE || op == Key.LESS ||
                    op == Key.EMORE || op == Key.ELESS) {
                boolean v = false;
                if (op == Key.EQUAL) v = ld == rd;
                else if (op == Key.DIF) v = ld != rd;
                else if (op == Key.MORE) v = ld > rd;
                else if (op == Key.LESS) v = ld < rd;
                else if (op == Key.EMORE) v = ld >= rd;
                else if (op == Key.ELESS) v = ld <= rd;

                return new LiteralCall(group,
                        lCall.getToken(), rCall.getToken(),
                        LiteralCall.BOOL, v, 0, 0, null,
                        false, false, false);
            }
            return new LiteralCall(group,
                    lCall.getToken(), rCall.getToken(),
                    LiteralCall.DOUBLE, false, 0, d, null,
                    false, (lCall.isFloat || rCall.isFloat) && !(lCall.isDouble || rCall.isDouble),
                    lCall.isDouble || rCall.isDouble);
        } else if (lType == LiteralCall.LONG && rType == LiteralCall.LONG) {
            long d = 0;
            long ld = lCall.resultNum;
            long rd = rCall.resultNum;
            if (op == Key.ADD) d = ld + rd;
            else if (op == Key.SUB) d = ld - rd;
            else if (op == Key.DIV) d = ld / rd;
            else if (op == Key.MUL) d = ld * rd;
            else if (op == Key.BITAND) d = ld & rd;
            else if (op == Key.BITOR) d = ld | rd;
            else if (op == Key.BITXOR) d = ld ^ rd;
            else if (op == Key.RSHIFT) d = ld >> rd;
            else if (op == Key.LSHIFT) d = ld << rd;
            else if (op == Key.EQUAL || op == Key.DIF || op == Key.MORE || op == Key.LESS ||
                    op == Key.EMORE || op == Key.ELESS) {
                boolean v = false;
                if (op == Key.EQUAL) v = ld == rd;
                else if (op == Key.DIF) v = ld != rd;
                else if (op == Key.MORE) v = ld > rd;
                else if (op == Key.LESS) v = ld < rd;
                else if (op == Key.EMORE) v = ld >= rd;
                else if (op == Key.ELESS) v = ld <= rd;

                return new LiteralCall(group,
                        lCall.getToken(), rCall.getToken(),
                        LiteralCall.BOOL, v, 0, 0, null,
                        false, false, false);
            }
            return new LiteralCall(group,
                    lCall.getToken(), rCall.getToken(),
                    LiteralCall.LONG, false, d, 0, null,
                    lCall.isLong || rCall.isLong, false, false);
        }
        return null;
    }

    public static LiteralCall resolve(CallGroup group, LiteralCall rCall, Key op) {
        int rType = rCall.getLiteralType();
        if (rType == LiteralCall.LONG) {
            long rd = rCall.resultNum;
            if (op == Key.ADD) rd = +rd;
            else if (op == Key.SUB) rd = -rd;
            else if (op == Key.BITNOT) rd = ~rd;

            return new LiteralCall(group,
                    rCall.getToken(), null,
                    rCall.getLiteralType(), false, rd, 0, null,
                    rCall.isLong, false, false);
        } else if (rType == LiteralCall.DOUBLE) {
            double rd = rCall.resultDouble;
            if (op == Key.ADD) rd = +rd;
            else if (op == Key.SUB) rd = -rd;
            if (!rCall.isFloat && !rCall.isDouble && rd == -9223372036854775808D) {
                return new LiteralCall(group,
                        rCall.getToken(), null,
                        LiteralCall.LONG, false, -9223372036854775808L, 0, null,
                        rCall.isLong, false, false);
            } else {
                return new LiteralCall(group,
                        rCall.getToken(), null,
                        rCall.getLiteralType(), false, 0, rd, null,
                        rCall.isLong, rCall.isFloat, rCall.isDouble);
            }
        } else if (rType == LiteralCall.BOOL) {
            boolean rd = rCall.resultBool;
            if (op == Key.NOT) rd = !rd;
            return new LiteralCall(group,
                    rCall.getToken(), null,
                    rCall.getLiteralType(), rd, 0, 0, null,
                    rCall.isLong, rCall.isFloat, rCall.isDouble);
        }
        return null;
    }

    public static LiteralCall resolve(CallGroup group, LiteralCall inner) {
        return new LiteralCall(group,
                inner.getToken(), null,
                inner.getLiteralType(), inner.resultBool, inner.resultNum, inner.resultDouble, null,
                inner.isLong, inner.isFloat, inner.isDouble);
    }
}
