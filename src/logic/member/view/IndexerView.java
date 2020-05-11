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
        return indexer.hasGet();
    }

    public boolean isGetFinal() {
        return indexer.isGetFinal();
    }

    public boolean isGetAbstract() {
        return indexer.isGetAbstract();
    }

    public boolean isGetPublic() {
        return indexer.isGetPublic();
    }

    public boolean isGetPrivate() {
        return indexer.isGetPrivate();
    }

    public boolean hasSet() {
        return indexer.hasSet();
    }

    public boolean isSetFinal() {
        return indexer.isSetFinal();
    }

    public boolean isSetAbstract() {
        return indexer.isSetAbstract();
    }

    public boolean isSetPublic() {
        return indexer.isSetPublic();
    }

    public boolean isSetPrivate() {
        return indexer.isSetPrivate();
    }

    public boolean hasOwn() {
        return indexer.hasOwn();
    }

    public boolean isOwnFinal() {
        return indexer.isOwnFinal();
    }

    public boolean isOwnAbstract() {
        return indexer.isOwnAbstract();
    }

    public boolean isOwnPublic() {
        return indexer.isOwnPublic();
    }

    public boolean isOwnPrivate() {
        return indexer.isOwnPrivate();
    }
}
