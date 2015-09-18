package xyz.hotchpotch.util.stream;

import java.io.Serializable;
import java.util.Objects;

/**
 * 2つのオブジェクトのペアを表すクラスです。<br>
 * 
 * @param <T> オブジェクト1の型
 * @param <U> オブジェクト2の型
 * @author nmby
 */
public class Pair<T, U> implements Serializable {
    
    // ++++++++++++++++ static members ++++++++++++++++
    
    private static final long serialVersionUID = 1L;
    
    /**
     * {@code Pair} オブジェクトを生成します。<br>
     * 
     * @param <T> メンバオブジェクト1の型
     * @param <U> メンバオブジェクト2の型
     * @param m1 メンバオブジェクト1（{@code null} が許容されます）
     * @param m2 メンバオブジェクト2（{@code null} が許容されます）
     * @return {@code (m1, m2)} から成る {@code Pair} オブジェクト
     */
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
    
    /**
     * メンバオブジェクト1を返します。<br>
     * 
     * @return メンバオブジェクト1
     */
    public T m1() {
        return m1;
    }
    
    /**
     * メンバオブジェクト2を返します。<br>
     * 
     * @return メンバオブジェクト2
     */
    public U m2() {
        return m2;
    }
    
    /**
     * {@inheritDoc}
     */
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
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(m1, m2);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("(%s, %s)", m1, m2);
    }
}
