package xyz.hotchpotch.util.stream;

import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;

/**
 * 2つのソーススプリッテレータの要素をペアリングして得られる要素をソースとするスプリッテレータを提供します。<br>
 * 
 * @see #of(Spliterator, Spliterator)
 * @author nmby
 */
/* package */ class ZippedSpliterator {
    
    // ++++++++++++++++ static members ++++++++++++++++
    
    /**
     * 2つのソーススプリッテレータの要素をペアリングして得られる要素をソースとするスプリッテレータを生成します。<br>
     * 生成されるスプリッテレータの特性は、2つのソーススプリッテレータの特性を反映したものとなります。
     * （詳細は {@link #zippedCharacteristics(Spliterator, Spliterator)} の説明を参照してください。）<br>
     * <br>
     * 
     * @param sp1 ソーススプリッテレータ1
     * @param sp2 ソーススプリッテレータ2
     * @return 2つのスプリッテレータの要素をペアリングして得られる要素をソースとするスプリッテレータ
     * @throws NullPointerException {@code sp1}、{@code sp2} のいずれかが {@code null} の場合
     * @throws IllegalArgumentException {@code sp1} と {@code sp2} が同一インスタンスの場合
     *         （但し、ともに {@link Spliterators#emptySpliterator()} と同一の場合は例外をスローしません）
     * @see #zippedCharacteristics(Spliterator, Spliterator)
     */
    /* package */ static <T, U> Spliterator<Pair<T, U>> of(
            Spliterator<? extends T> sp1,
            Spliterator<? extends U> sp2) {
            
        Objects.requireNonNull(sp1);
        Objects.requireNonNull(sp2);
        if (sp1 == sp2 && sp1 != Spliterators.emptySpliterator()) {
            throw new IllegalArgumentException();
        }
        
        long size = Long.min(sp1.estimateSize(), sp2.estimateSize());
        int characteristics = zippedCharacteristics(sp1, sp2);
        
        Iterator<? extends T> itr1 = Spliterators.iterator(sp1);
        Iterator<? extends U> itr2 = Spliterators.iterator(sp2);
        Iterator<Pair<T, U>> zippedItr = ZippedIterator.of(itr1, itr2);
        
        if (size < Long.MAX_VALUE) {
            return Spliterators.spliterator(zippedItr, size, characteristics);
        } else {
            return Spliterators.spliteratorUnknownSize(zippedItr, characteristics);
        }
    }
    
    /**
     * 2つのソーススプリッテレータをペアリングして生成されるスプリッテレータの特性値を返します。<br>
     * 生成されるスプリッテレータは、以下に示す場合に各特性値を報告します。
     * <ul>
     *   <li>{@link Spliterator#ORDERED ORDERED} 特性 ： sp1, sp2 がともに {@code ORDERED} 特性を報告する場合</li>
     *   <li>{@link Spliterator#DISTINCT DISTINCT} 特性 ： sp1, sp2 のいずれかが {@code DISTINCT} 特性を報告する場合</li>
     *   <li>{@link Spliterator#SIZED SIZED} 特性、{@link Spliterator#SIZED SUBSIZED} 特性 ：
     *       sp1, sp2 がともに {@code SIZED} 特性を報告する場合</li>
     *   <li>{@link Spliterator#NONNULL NONNULL} 特性 ： sp1, sp2 の特性に関わらず常に</li>
     *   <li>{@link Spliterator#IMMUTABLE IMMUTABLE} 特性 ： sp1, sp2 がともに {@code IMMUTABLE} 特性を報告する場合</li>
     *   <li>{@link Spliterator#CONCURRENT CONCURRENT} 特性 ： sp1, sp2 がともに {@code CONCURRENT} 特性を報告する場合<br>
     *       もしくは、一方が {@code CONCURRENT} 特性を報告し他方が {@code IMMUTABLE} 特性を報告する場合</li>
     *   <li></li>
     * </ul>
     * 
     * @param sp1 ソーススプリッテレータ1
     * @param sp2 ソーススプリッテレータ2
     * @return 2つのソーススプリッテレータをペアリングして生成されるスプリッテレータの特性値
     * @throws NullPointerException {@code sp1}、{@code sp2} のいずれかが {@code null} の場合
     */
    /* package */ static <T, U> int zippedCharacteristics(
            Spliterator<? extends T> sp1,
            Spliterator<? extends T> sp2) {
            
        Objects.requireNonNull(sp1);
        Objects.requireNonNull(sp2);
        
        int c1 = sp1.characteristics();
        int c2 = sp2.characteristics();
        
        int ordered = Spliterator.ORDERED & (c1 & c2);
        int distinct = Spliterator.DISTINCT & (c1 | c2);
        int sorted = 0;
        int sized = Spliterator.SIZED & (c1 & c2);
        int nonnull = Spliterator.NONNULL;
        int immutable = Spliterator.IMMUTABLE & (c1 & c2);
        int concurrent;
        if ((Spliterator.CONCURRENT & c1 & c2) != 0) {
            concurrent = Spliterator.CONCURRENT;
        } else if ((Spliterator.CONCURRENT & c1) != 0) {
            concurrent = (Spliterator.IMMUTABLE & c2) != 0 ? Spliterator.CONCURRENT : 0;
        } else if ((Spliterator.CONCURRENT & c2) != 0) {
            concurrent = (Spliterator.IMMUTABLE & c1) != 0 ? Spliterator.CONCURRENT : 0;
        } else {
            concurrent = 0;
        }
        int subsized = sized == 0 ? 0 : Spliterator.SUBSIZED;
        
        assert(Long.min(sp1.estimateSize(), sp2.estimateSize()) < Long.MAX_VALUE) == (sized == Spliterator.SIZED);
        assert sized == 0 || concurrent == 0;
        assert immutable == 0 || concurrent == 0;
        
        return ordered | distinct | sorted | sized | nonnull | immutable | concurrent | subsized;
    }
    
    // ++++++++++++++++ instance members ++++++++++++++++
    
    private ZippedSpliterator() {
    }
}
