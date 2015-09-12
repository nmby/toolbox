package xyz.hotchpotch.util.stream;

import java.util.Objects;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 2つのソースストリームの要素をペアリングして得られる要素をソースとする順次ストリームを提供します。<br>
 * 
 * @see #of(Stream, Stream)
 * @author nmby
 */
public class ZippedStream {
    
    // ++++++++++++++++ static members ++++++++++++++++
    
    /**
     * 2つのソースストリームの要素をペアリングして得られる要素をソースとする順次ストリームを生成します。<br>
     * 例を示します。<br>
     * <ul>
     *   <li>ソースストリーム1 : {@code 1, 2, 3, ...}</li>
     *   <li>ソースストリーム2 : {@code "a", "b", "c", ...}</li>
     *   <li>生成されるストリーム : {@code (1, "a"), (2, "b"), (3, "c"), ...}</li>
     * </ul>
     * <br>
     * 生成されるストリームの長さは、2つのソースストリームの短い方に合わせて切り詰められます。
     * 2つのソースストリームがともに無限ストリームの場合、生成されるストリームも無限ストリームになります。<br>
     * <br>
     * 2つのソースストリームがともに順序付けされている場合、生成されるストリームも順序付けされ、その要素は決定論的になります。<br>
     * 反対に、2つのソースストリームのいずれかまたは両方が順序付けされていない場合、生成されるストリームは順序付けされず、
     * その要素の値および並び順は非決定論的になります。
     * 例えば、ソースストリーム1が順序付けされていない場合、{@code (1, "a"), (2, "b"), (3, "c"), ...} というストリームのみならず、
     * {@code (2, "a"), (3, "b"), (1, "c"), ...} というストリームも生成され得ます。これらはいずれも正当な結果とみなされます。
     * このことが好ましくない場合は、{@link Stream#sorted()} 等により得られる順序付けされたストリームをソースとするようにしてください。<br>
     * <br>
     * 生成されるストリームは、2つのソースストリームに遅延バインディングでバインドします。
     * 従って、ソースストリーム自体も遅延バインディングであり、かつ、生成されるストリームに対して終端操作が開始される前までに行われたものであれば、
     * 大元のソースに対する変更は生成されるストリームの要素に反映されます。<br>
     * 但し、ソースストリームのいずれかが {@code null} の場合や2つのソースストリームが同一インスタンスの場合にスローされる例外は、
     * 構築時に直ちにスローされます。<br>
     * <br>
     * 終端操作開始後に大元のソースに対する構造的干渉が検出された場合の動作ポリシーは、ソースストリームのそれに従います。
     * すなわち、ソースストリームがフェイル・ファストであれば、生成されるストリームもフェイル・ファストになります。<br>
     * <br>
     * このメソッドの実行により、新たなストリームの構築のためにソースストリームに対する終端操作が行われ、ソースストリームを再利用することはできなくなります。
     * 但し、新たなストリームに対する終端操作が行われるまで、大元のソースが消費されることはありません。<br>
     * <br>
     * 生成されるストリームに対するクローズ操作は、ソースストリームに対するクローズ操作と連動しません。
     * ソースストリームが入出力チャネルをソースとする場合は、ソースストリームに対して個別にクローズ操作を行ってください。<br>
     * <br>
     * 
     * @param <T> ソースストリーム1の要素の型
     * @param <U> ソースストリーム2の要素の型
     * @param stream1 ソースストリーム1
     * @param stream2 ソースストリーム2
     * @return 2つのソースストリームの要素をペアリングして得られる要素をソースとする順次ストリーム
     * @throws NullPointerException {@code stream1}、{@code stream2} のいずれかが {@code null} の場合
     * @throws IllegalArgumentException {@code stream1} と {@code stream2} が同一インスタンスの場合
     * @see java.util.stream.Stream
     * @see java.util.Spliterator
     */
    public static <T, U> Stream<Pair<T, U>> of(Stream<? extends T> stream1, Stream<? extends U> stream2) {
        Objects.requireNonNull(stream1);
        Objects.requireNonNull(stream2);
        
        if (stream1 == stream2) {
            throw new IllegalArgumentException("Two different streams are required.");
        }
        
        Spliterator<? extends T> sp1 = stream1.spliterator();
        Spliterator<? extends U> sp2 = stream2.spliterator();
        
        return StreamSupport.stream(
                () -> ZippedSpliterator.of(sp1, sp2),
                ZippedSpliterator.zippedCharacteristics(sp1, sp2),
                false);
    }
    
    // ++++++++++++++++ instance members ++++++++++++++++
    
    private ZippedStream() {
    }
}
