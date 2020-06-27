package logic.member.view;

import content.Token;
import data.ContentFile;
import logic.Pointer;
import logic.member.Indexer;
import logic.typdef.Type;

public class IndexerView {

    public Indexer indexer;
    private ParamView paramView;
    private Pointer typePtr;

    public IndexerView srcGet, srcSet, srcOwn;

    public IndexerView(Pointer caller, Indexer indexer) {
        this.indexer = indexer;
        if (indexer.getTypePtr() != null) {
            typePtr = Pointer.byGeneric(indexer.getTypePtr(), caller);
        }
        paramView = new ParamView(caller, indexer.getParams());
    }

    public IndexerView(Pointer caller, IndexerView indexerView) {
        this.indexer = indexerView.indexer;
        if (indexerView.typePtr != null) {
            typePtr = Pointer.byGeneric(indexerView.getTypePtr(), caller);
        }
        paramView = new ParamView(caller, indexerView.getParams());
        srcGet = indexerView.srcGet;
        srcSet = indexerView.srcSet;
        srcOwn = indexerView.srcOwn;
    }

    public ContentFile getGetFile() {
        return srcGet != null && srcGet != this ? srcGet.getGetFile() : indexer.cFile;
    }

    public ContentFile getSetFile() {
        return srcSet != null && srcSet != this ? srcSet.getGetFile() : indexer.cFile;
    }

    public ContentFile getOwnFile() {
        return srcOwn != null && srcOwn != this ? srcOwn.getGetFile() : indexer.cFile;
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
        return hasGet() && (
                (isGetPrivate() && indexer.cFile == type.cFile) ||
                        (isGetPublic()) || (indexer.cFile.library == type.cFile.library));
    }

    public boolean canAcessSet(Type type) {
        return hasSet() && (
                (isSetPrivate() && indexer.cFile == type.cFile) ||
                        (isSetPublic()) || (indexer.cFile.library == type.cFile.library));
    }

    public boolean canAcessOwn(Type type) {
        return hasOwn() && (
                (isOwnPrivate() && indexer.cFile == type.cFile) ||
                        (isOwnPublic()) || (indexer.cFile.library == type.cFile.library));
    }

    public void setGetSource(IndexerView other) {
        srcGet = other.srcGet != null ? other.srcGet : other;
    }

    public void setSetSource(IndexerView other) {
        srcSet = other.srcSet != null ? other.srcSet : other;
    }

    public void setOwnSource(IndexerView other) {
        srcOwn = other.srcOwn != null ? other.srcOwn : other;
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
        return srcGet != null || indexer.hasGet();
    }

    public boolean isGetFinal() {
        return (srcGet != null && srcGet.isGetFinal()) || (indexer.hasGet() && indexer.isGetFinal());
    }

    public boolean isGetAbstract() {
        return  (srcGet != null && srcGet.isGetAbstract()) || (indexer.hasGet() && indexer.isGetAbstract());
    }

    public boolean isGetPublic() {
        return (srcGet != null && srcGet.isGetPublic()) || indexer.isGetPublic();
    }

    public boolean isGetPrivate() {
        return (srcGet != null && srcGet.isGetPrivate()) || indexer.isGetPrivate();
    }

    public boolean hasSet() {
        return srcSet != null || indexer.hasSet();
    }

    public boolean isSetFinal() {
        return (srcSet != null && srcSet.isSetFinal()) || (indexer.hasSet() && indexer.isSetFinal());
    }

    public boolean isSetAbstract() {
        return  (srcSet != null && srcSet.isSetAbstract()) || (indexer.hasSet() && indexer.isSetAbstract());
    }

    public boolean isSetPublic() {
        return (srcSet != null && srcSet.isSetPublic()) || indexer.isSetPublic();
    }

    public boolean isSetPrivate() {
        return (srcSet != null && srcSet.isSetPrivate()) || indexer.isSetPrivate();
    }

    public boolean hasOwn() {
        return srcOwn != null || indexer.hasOwn();
    }

    public boolean isOwnFinal() {
        return (srcOwn != null && srcOwn.isOwnFinal()) || (indexer.hasOwn() && indexer.isOwnFinal());
    }

    public boolean isOwnAbstract() {
        return  (srcOwn != null && srcOwn.isOwnAbstract()) || (indexer.hasOwn() && indexer.isOwnAbstract());
    }

    public boolean isOwnPublic() {
        return (srcOwn != null && srcOwn.isOwnPublic()) || indexer.isOwnPublic();
    }

    public boolean isOwnPrivate() {
        return (srcOwn != null && srcOwn.isOwnPrivate()) || indexer.isOwnPrivate();
    }
}
