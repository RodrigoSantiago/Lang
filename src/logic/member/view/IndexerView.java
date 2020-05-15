package logic.member.view;

import content.Token;
import logic.Pointer;
import logic.member.Indexer;
import logic.typdef.Type;

public class IndexerView {

    private Indexer indexer;
    private ParamView paramView;
    private Pointer typePtr;

    private boolean hasGetAbs, hasSetAbs, hasOwnAbs;
    private boolean hasGetImpl, hasSetImpl, hasOwnImpl;
    public int getAcess, setAcess, ownAcess;

    public IndexerView(Pointer caller, Indexer indexer) {
        this.indexer = indexer;
        if (indexer.getTypePtr() != null) {
            typePtr = Pointer.byGeneric(indexer.getTypePtr(), caller);
        }
        paramView = new ParamView(caller, indexer.getParams());
        getAcess = !indexer.hasGet() || indexer.isGetPrivate() ? 0 : indexer.isGetPublic() ? 2 : 1;
        setAcess = !indexer.hasSet() || indexer.isSetPrivate() ? 0 : indexer.isSetPublic() ? 2 : 1;
        ownAcess = !indexer.hasOwn() || indexer.isOwnPrivate() ? 0 : indexer.isOwnPublic() ? 2 : 1;
    }

    public IndexerView(Pointer caller, IndexerView indexerView) {
        this.indexer = indexerView.indexer;
        if (indexerView.typePtr != null) {
            typePtr = Pointer.byGeneric(indexerView.getTypePtr(), caller);
        }
        paramView = new ParamView(caller, indexerView.getParams());
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
        if (getTypePtr().equals(other.getTypePtr())) {
            return getParams().canOverride(other.getParams());
        }

        return false;
    }

    public boolean canAcessGet(Type type) {
        return (getAcess == 0 && indexer.cFile == type.cFile) ||
                (getAcess == 1 && indexer.cFile.library == type.cFile.library) || (getAcess == 2);
    }

    public boolean canAcessSet(Type type) {
        return (setAcess == 0 && indexer.cFile == type.cFile) ||
                (setAcess == 1 && indexer.cFile.library == type.cFile.library) || (setAcess == 2);
    }

    public boolean canAcessOwn(Type type) {
        return (ownAcess == 0 && indexer.cFile == type.cFile) ||
                (ownAcess == 1 && indexer.cFile.library == type.cFile.library) || (ownAcess == 2);
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

    public Pointer getTypePtr() {
        return typePtr != null ? typePtr : indexer.getTypePtr();
    }

    public ParamView getParams() {
        return paramView;
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
        return indexer.isGetPublic() || getAcess == 2;
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
        return indexer.isSetPublic() || setAcess == 2;
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
        return indexer.isOwnPublic() || ownAcess == 2;
    }

    public boolean isOwnPrivate() {
        return ownAcess == 0;
    }
}
