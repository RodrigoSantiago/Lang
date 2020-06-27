package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.Pointer;
import logic.member.view.IndexerView;
import logic.stack.Context;

import java.util.ArrayList;

public class IndexerCall extends Call {

    private IndexerView indexerView;
    private ArrayList<Expression> arguments = new ArrayList<>();

    private boolean useGet, useSet, useOwn;

    public IndexerCall(CallGroup group, Token start, Token end) {
        super(group, start, end);

        System.out.println("INDEXER : "+ TokenGroup.toString(start, end));
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.INDEX && token.getChild() != null) {
                this.token = token;
                readArguments(token.getChild(), token.getLastChild());
                state = 1;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state == 0) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }
    }

    private void readArguments(Token start, Token end) {
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if ((state == 0 || state == 2) && token.key != Key.COMMA) {
                while (next != null && next != end && next.key != Key.COMMA) {
                    next = next.getNext();
                }
                arguments.add(new Expression(getLine(), token, next));
                state = 1;
            } else if (state == 1 && token.key == Key.COMMA) {
                state = 2;
            } else {
                cFile.erro(token, "Unexpected token", this);
            }
            if (next == end && state == 2) {
                cFile.erro(token, "Unexpected end of tokens", this);
            }
            token = next;
        }
    }

    @Override
    public void load(Context context) {
        ArrayList<IndexerView> indexers = context.findIndexer(arguments);
        if (indexers == null || indexers.size() == 0) {
            cFile.erro(token, "Indexer not found", this);
        } else if (indexers.size() > 1) {
            cFile.erro(token, "Ambigous Indexer Call", this);
            indexerView = indexers.get(0);
        } else {
            indexerView = indexers.get(0);
        }
        context.jumpTo(indexerView == null ? null : indexerView.getTypePtr());
    }

    @Override
    public int verify(Pointer pointer) {
        return indexerView == null ? 0 : pointer.canReceive(indexerView.getTypePtr());
    }

    @Override
    public void requestGet(Pointer pointer) {
        if (getNaturalPtr(pointer) == null) return;
        if (pointer == null) pointer = naturalPtr;
        pointer = pointer.toLet();

        requestPtr = pointer;

        if (pointer.canReceive(naturalPtr) <= 0) {
            cFile.erro(getToken(), "Cannot cast [" + naturalPtr + "] to [" + pointer + "]", this);
            return;
        }

        useGet = true;

        if (indexerView != null) {
            if (!indexerView.hasGet()) {
                cFile.erro(token, "GET member not defined", this); // [impossible ?]
            } else if (!indexerView.isGetPublic() && !indexerView.isGetPrivate()) {
                if (!getStack().cFile.library.equals(indexerView.getGetFile().library)) {
                    cFile.erro(token, "Cannot acess a Internal member from other Library", this);
                }
            } else if (indexerView.isGetPrivate()) {
                if (!getStack().cFile.equals(indexerView.getGetFile())) {
                    cFile.erro(token, "Cannot acess a Private member from other file", this);
                }
            }
        }
    }

    @Override
    public void requestOwn(Pointer pointer) {
        if (getNaturalPtr(pointer) == null) return;
        if (pointer == null) pointer = naturalPtr;

        requestPtr = pointer;

        if (pointer.canReceive(naturalPtr) <= 0) {
            cFile.erro(getToken(), "Cannot cast [" + naturalPtr + "] to [" + pointer + "]", this);
            return;
        }

        int table = Pointer.OwnTable(pointer, naturalPtr);

        if (table == 0) useOwn = true;
        else if (table == 1) useGet = true;
        else if (table == 2) useOwn = useGet = true;
        else cFile.erro(token, "Cannot convert a STRONG reference to a WEAK reference", this);

        if (indexerView != null) {
            if (useOwn && indexerView.hasOwn()) {
                if (!indexerView.isOwnPublic() && !indexerView.isOwnPrivate()) {
                    if (!getStack().cFile.library.equals(indexerView.getOwnFile().library)) {
                        if (useGet) {
                            useOwn = false;
                        } else {
                            cFile.erro(token, "Cannot acess a Internal member from other Library", this);
                        }
                    }
                } else if (indexerView.isOwnPrivate()) {
                    if (!getStack().cFile.equals(indexerView.getOwnFile())) {
                        if (useGet) {
                            useOwn = false;
                        } else {
                            cFile.erro(token, "Cannot acess a Private member from other file", this);
                        }
                    }
                }
            } else {
                if (useGet) {
                    useOwn = false;
                } else {
                    cFile.erro(token, "OWN member not defined", this);
                }
            }

            if (useGet && !useOwn) {
                if (!indexerView.hasGet()) {
                    cFile.erro(token, "GET member not defined", this); // [impossible ?]
                } else if (!indexerView.isGetPublic() && !indexerView.isGetPrivate()) {
                    if (!getStack().cFile.library.equals(indexerView.getGetFile().library)) {
                        cFile.erro(token, "Cannot acess a Internal member from other Library", this);
                    }
                } else if (indexerView.isGetPrivate()) {
                    if (!getStack().cFile.equals(indexerView.getGetFile())) {
                        cFile.erro(token, "Cannot acess a Private member from other file", this);
                    }
                }
            }
        }
    }

    @Override
    public void requestSet() {
        useSet = true;

        if (indexerView != null) {
            if (indexerView.hasSet()) {
                if (!indexerView.isSetPublic() && !indexerView.isSetPrivate()) {
                    if (!getStack().cFile.library.equals(indexerView.getSetFile().library)) {
                        cFile.erro(token, "Cannot acess a Internal member from other Library", this);
                    }
                } else if (indexerView.isSetPrivate()) {
                    if (!getStack().cFile.equals(indexerView.getSetFile())) {
                        cFile.erro(token, "Cannot acess a Private member from other file", this);
                    }
                }
            }  else {
                cFile.erro(token, "SET member not defined", this);
            }
        }
    }
}
