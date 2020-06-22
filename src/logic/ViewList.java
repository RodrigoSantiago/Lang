package logic;

import content.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class ViewList<T> implements Iterable<T> {

    private ArrayList<T> empty = new ArrayList<>();
    private HashMap<Token, ArrayList<T>> source = new HashMap<>();
    private int size;

    public ViewList() {

    }

    public ArrayList<T> get(Token token) {
        ArrayList<T> list = source.get(token);
        if (list != null) {
            return list;
        } else {
            return empty;
        }
    }

    public void put(Token token, T value) {
        size++;

        ArrayList<T> list = source.get(token);
        if (list == null) {
            list = new ArrayList<>(1);
            source.put(token, list);
        }
        list.add(value);
    }

    public int size() {
        return size;
    }

    @Override
    public Iterator<T> iterator() {
        return new It<>(source);
    }

    public static class It<T> implements Iterator<T> {
        private Iterator<Token> itName;
        private Iterator<T> itValues;
        private HashMap<Token, ArrayList<T>> source;

        public It(HashMap<Token, ArrayList<T>> source) {
            this.source = source;
            itName = source.keySet().iterator();
            if (itName.hasNext()) {
                itValues = source.get(itName.next()).iterator();
            }
        }

        @Override
        public boolean hasNext() {
            if (itValues == null) {
                return false;
            } else if (itValues.hasNext()) {
                return true;
            } else if (itName.hasNext()) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public T next() {
            if (itValues.hasNext()) {
                return itValues.next();
            } else {
                itValues = source.get(itName.next()).iterator();
            }
            return itValues.next();
        }
    }
}
