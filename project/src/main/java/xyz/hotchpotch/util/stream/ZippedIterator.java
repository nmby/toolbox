package xyz.hotchpotch.util.stream;

import java.util.Iterator;
import java.util.Objects;

class ZippedIterator<T, U> implements Iterator<Pair<T, U>> {
    
    // ++++++++++++++++ static members ++++++++++++++++
    
    static <T, U> Iterator<Pair<T, U>> of(Iterator<? extends T> itr1, Iterator<? extends U> itr2) {
        Objects.requireNonNull(itr1);
        Objects.requireNonNull(itr2);
        if (itr1 == itr2) {
            throw new IllegalArgumentException("itr1 == itr2");
        }
        
        return new ZippedIterator<>(itr1, itr2);
    }
    
    // ++++++++++++++++ instance members ++++++++++++++++
    
    private final Iterator<? extends T> itr1;
    private final Iterator<? extends U> itr2;
    
    private ZippedIterator(Iterator<? extends T> itr1, Iterator<? extends U> itr2) {
        this.itr1 = itr1;
        this.itr2 = itr2;
    }
    
    @Override
    public boolean hasNext() {
        return itr1.hasNext() && itr2.hasNext();
    }
    
    @Override
    public Pair<T, U> next() {
        return Pair.of(itr1.next(), itr2.next());
    }
}
