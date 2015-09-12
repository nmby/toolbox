package xyz.hotchpotch.util.stream;

import java.io.Serializable;
import java.util.Objects;

public class Pair<T, U> implements Serializable {
    
    // ++++++++++++++++ static members ++++++++++++++++
    
    private static final long serialVersionUID = 1L;
    
    public static <T, U> Pair<T, U> of(T m1, U m2) {
        return new Pair<>(m1, m2);
    }
    
    // ++++++++++++++++ instance members ++++++++++++++++
    
    private final T m1;
    private final U m2;
    
    private Pair(T m1, U m2) {
        this.m1 = m1;
        this.m2 = m2;
    }
    
    public T m1() {
        return m1;
    }
    
    public U m2() {
        return m2;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Pair) {
            Pair<?, ?> p = (Pair<?, ?>) obj;
            return Objects.equals(m1, p.m1) && Objects.equals(m2, p.m2);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(m1, m2);
    }
    
    @Override
    public String toString() {
        return String.format("(%s, %s)", m1, m2);
    }
}
