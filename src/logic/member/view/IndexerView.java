package logic.member.view;

import content.Token;
import logic.Pointer;
import logic.member.Indexer;
import logic.templates.Template;

public class IndexerView {
    public final Pointer caller;
    public final Indexer indexer;
    public final ParamView paramView;
    private Pointer ptr;
    private boolean hasGet, hasSet, hasOwn;
    private boolean hasGetAbstract, hasSetAbstract, hasOwnAbstract;
    private boolean hasGetImpl, hasSetImpl, hasOwnImpl;

    public IndexerView(Pointer caller, Indexer indexer) {
        this.caller = caller;
        this.indexer = indexer;
        if (indexer.typePtr != null) {
            ptr = Pointer.byGeneric(indexer.typePtr, caller);
        }
        paramView = new ParamView(indexer.params, caller);
    }

    public IndexerView(Pointer caller, IndexerView indexerView) {
        this.caller = caller;
        this.indexer = indexerView.indexer;
        if (indexerView.ptr != null) {
            ptr = Pointer.byGeneric(indexerView.getType(), caller);
        }
        paramView = new ParamView(indexerView.getParams(), caller);
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

    public void addOverriden(IndexerView indexerView) {
        hasGet = hasGet || indexerView.hasGet();
        hasSet = hasSet || indexerView.hasSet();
        hasOwn = hasOwn || indexerView.hasOwn();
        hasGetAbstract = hasGetAbstract && indexerView.isGetAbstract();
        hasSetAbstract = hasSetAbstract && indexerView.isSetAbstract();
        hasOwnAbstract = hasOwnAbstract && indexerView.isOwnAbstract();
    }

    public Pointer getType() {
        return ptr != null ? ptr : indexer.typePtr;
    }

    public ParamView getParams() {
        return paramView;
    }

    public boolean isPrivate() {
        return indexer.isPrivate();
    }

    public boolean isPublic() {
        return indexer.isPublic();
    }

    public boolean isFinal() {
        return indexer.isFinal();
    }

    public boolean isAbstract() {
        return indexer.isAbstract();
    }

    public boolean isStatic() {
        return indexer.isStatic();
    }

    public boolean isLet() {
        return indexer.isLet();
    }

    public boolean hasGet() {
        return indexer.hasGet() || hasGet;
    }

    public boolean isGetFinal() {
        return indexer.isGetFinal();
    }

    public boolean isGetAbstract() {
        return indexer.isGetAbstract() || hasGetAbstract;
    }

    public boolean isGetPublic() {
        return indexer.isGetPublic();
    }

    public boolean isGetPrivate() {
        return indexer.isGetPrivate();
    }

    public boolean hasSet() {
        return indexer.hasSet() || hasSet;
    }

    public boolean isSetFinal() {
        return indexer.isSetFinal();
    }

    public boolean isSetAbstract() {
        return indexer.isSetAbstract() || hasSetAbstract;
    }

    public boolean isSetPublic() {
        return indexer.isSetPublic();
    }

    public boolean isSetPrivate() {
        return indexer.isSetPrivate();
    }

    public boolean hasOwn() {
        return indexer.hasOwn() || hasOwn;
    }

    public boolean isOwnFinal() {
        return indexer.isOwnFinal();
    }

    public boolean isOwnAbstract() {
        return indexer.isOwnAbstract() || hasOwnAbstract;
    }

    public boolean isOwnPublic() {
        return indexer.isOwnPublic();
    }

    public boolean isOwnPrivate() {
        return indexer.isOwnPrivate();
    }
}
