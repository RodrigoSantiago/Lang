package logic.member.view;

import content.Token;
import logic.Pointer;
import logic.member.Indexer;
import logic.templates.Template;
import logic.typdef.Type;

public class IndexerView {
    public final Pointer caller;
    public final Indexer indexer;
    public final ParamView paramView;
    private Pointer ptr;
    private boolean hasGetAbs, hasSetAbs, hasOwnAbs;
    private boolean hasGetImpl, hasSetImpl, hasOwnImpl;
    public int getAcess, setAcess, ownAcess;

    public IndexerView(Pointer caller, Indexer indexer) {
        this.caller = caller;
        this.indexer = indexer;
        if (indexer.typePtr != null) {
            ptr = Pointer.byGeneric(indexer.typePtr, caller);
        }
        paramView = new ParamView(indexer.params, caller);
        getAcess = !indexer.hasGet() || indexer.isGetPrivate() ? 0 : indexer.isGetPublic() ? 3 : 2;
        setAcess = !indexer.hasSet() || indexer.isSetPrivate() ? 0 : indexer.isSetPublic() ? 3 : 2;
        ownAcess = !indexer.hasOwn() || indexer.isOwnPrivate() ? 0 : indexer.isOwnPublic() ? 3 : 2;
    }

    public IndexerView(Pointer caller, IndexerView indexerView) {
        this.caller = caller;
        this.indexer = indexerView.indexer;
        if (indexerView.ptr != null) {
            ptr = Pointer.byGeneric(indexerView.getType(), caller);
        }
        paramView = new ParamView(indexerView.getParams(), caller);
        hasGetAbs = indexerView.hasGetAbs;
        hasSetAbs = indexerView.hasSetAbs;
        hasOwnAbs = indexerView.hasOwnAbs;
        hasGetImpl = indexerView.hasGetImpl;
        hasSetImpl = indexerView.hasSetImpl;
        hasOwnImpl = indexerView.hasOwnImpl;
        getAcess = indexerView.getAcess;
        setAcess = indexerView.setAcess;
        ownAcess = indexerView.ownAcess;
    }

    public boolean isFrom(Type type) {
        return indexer.type == type;
    }

    public Token getToken() {
        return indexer.token;
    }

    public boolean canOverload(IndexerView other) {
        return getParams().canOverload(other.getParams());
    }

    public boolean canOverride(IndexerView other) {
        if (getType().equals(other.getType())) {
            return getParams().canOverride(other.getParams());
        }

        return false;
    }

    public boolean canAcess(IndexerView other) {
        return !((other.isGetPrivate() && indexer.cFile != other.indexer.cFile) ||
                (!other.isGetPublic() && indexer.cFile.library != other.indexer.cFile.library));
    }

    public boolean canOverrideAcess(IndexerView other) {
        return !((isGetPrivate() && !other.isGetPrivate()) || (!isGetPublic() && other.isGetPublic()));
    }

    public void addOverriden(IndexerView other) {
        if (!hasGet()) getAcess = other.getAcess;
        if (!hasGet() && other.hasGet()) {
            if (other.isGetAbstract()) {
                hasGetAbs = true;
            } else {
                hasGetImpl = true;
            }
        } else if (hasGet() && isGetAbstract() && other.hasGet() && !other.isGetAbstract()) {
            hasGetImpl = true;
        }

        if (!hasSet()) setAcess = other.setAcess;
        if (!hasSet() && other.hasSet()) {
            if (other.isSetAbstract()) {
                hasSetAbs = true;
            } else {
                hasSetImpl = true;
            }
        } else if (hasSet() && isSetAbstract() && other.hasSet() && !other.isSetAbstract()) {
            hasSetImpl = true;
        }

        if (!hasOwn()) ownAcess = other.ownAcess;
        if (!hasOwn() && other.hasOwn()) {
            if (other.isOwnAbstract()) {
                hasOwnAbs = true;
            } else {
                hasOwnImpl = true;
            }
        } else if (hasOwn() && isOwnAbstract() && other.hasOwn() && !other.isOwnAbstract()) {
            hasOwnImpl = true;
        }
    }

    public Pointer getType() {
        return ptr != null ? ptr : indexer.typePtr;
    }

    public ParamView getParams() {
        return paramView;
    }

    public boolean isStatic() {
        return indexer.isStatic();
    }

    public boolean isLet() {
        return indexer.isLet();
    }

    public boolean hasGet() {
        return indexer.hasGet() || hasGetAbs || hasGetImpl;
    }

    public boolean isGetFinal() {
        return indexer.isGetFinal();
    }

    public boolean isGetAbstract() {
        return !hasGetImpl && (hasGetAbs || indexer.isGetAbstract());
    }

    public boolean isGetPublic() {
        return indexer.isGetPublic() || getAcess == 3;
    }

    public boolean isGetPrivate() {
        return getAcess == 0;
    }

    public boolean hasSet() {
        return indexer.hasSet() || hasSetAbs || hasSetImpl;
    }

    public boolean isSetFinal() {
        return indexer.isSetFinal();
    }

    public boolean isSetAbstract() {
        return !hasSetImpl && (hasSetAbs || indexer.isSetAbstract());
    }

    public boolean isSetPublic() {
        return indexer.isSetPublic() || setAcess == 3;
    }

    public boolean isSetPrivate() {
        return setAcess == 0;
    }

    public boolean hasOwn() {
        return indexer.hasOwn() || hasOwnAbs || hasOwnImpl;
    }

    public boolean isOwnFinal() {
        return indexer.isOwnFinal();
    }

    public boolean isOwnAbstract() {
        return !hasOwnImpl && (hasOwnAbs || indexer.isOwnAbstract());
    }

    public boolean isOwnPublic() {
        return indexer.isOwnPublic() || ownAcess == 3;
    }

    public boolean isOwnPrivate() {
        return ownAcess == 0;
    }
}
