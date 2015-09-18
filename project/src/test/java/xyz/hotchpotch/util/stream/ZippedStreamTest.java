package xyz.hotchpotch.util.stream;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static xyz.hotchpotch.jutaime.throwable.RaiseMatchers.*;
import static xyz.hotchpotch.jutaime.throwable.Testee.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

public class ZippedStreamTest {
    
    @Test
    public void test01() {
        // ******** 基本的な挙動の確認 ********
        
        // 基本動作
        assertThat(ZippedStream.of(Stream.of(1, 2, 3, 4, 5), Stream.of("a", "b", "c", "d", "e"))
                .map(Pair::toString).collect(Collectors.joining(", ")),
                is("(1, a), (2, b), (3, c), (4, d), (5, e)"));
                
        // 短い方に合わせて切り詰められることの確認1-1
        assertThat(ZippedStream.of(Stream.of(1, 2, 3, 4, 5), Stream.of("a", "b", "c"))
                .map(Pair::toString).collect(Collectors.joining(", ")),
                is("(1, a), (2, b), (3, c)"));
                
        // 短い方に合わせて切り詰められることの確認1-2
        assertThat(ZippedStream.of(Stream.of(1, 2, 3), Stream.of("a", "b", "c", "d", "e"))
                .map(Pair::toString).collect(Collectors.joining(", ")),
                is("(1, a), (2, b), (3, c)"));
                
        // 短い方に合わせて切り詰められることの確認2-1
        assertThat(ZippedStream.of(Stream.of(1, 2, 3, 4, 5), Stream.<String> empty())
                .map(Pair::toString).collect(Collectors.joining(", ")),
                is(""));
                
        // 短い方に合わせて切り詰められることの確認2-2
        assertThat(ZippedStream.of(Stream.<Integer> empty(), Stream.of("a", "b", "c", "d", "e"))
                .map(Pair::toString).collect(Collectors.joining(", ")),
                is(""));
                
        // 短い方に合わせて切り詰められることの確認3
        assertThat(ZippedStream.of(Stream.<Integer> empty(), Stream.<String> empty())
                .map(Pair::toString).collect(Collectors.joining(", ")),
                is(""));
                
        // 無限ストリームが含まれる場合の挙動の確認1-1
        assertThat(ZippedStream.of(Stream.iterate(1, n -> n + 1), Stream.of(-1, -2, -3))
                .map(Pair::toString).collect(Collectors.joining(", ")),
                is("(1, -1), (2, -2), (3, -3)"));
                
        // 無限ストリームが含まれる場合の挙動の確認1-2
        assertThat(ZippedStream.of(Stream.of(1, 2, 3), Stream.iterate(-1, n -> n - 1))
                .map(Pair::toString).collect(Collectors.joining(", ")),
                is("(1, -1), (2, -2), (3, -3)"));
                
        // 無限ストリームが含まれる場合の挙動の確認2
        assertThat(ZippedStream.of(Stream.iterate(1, n -> n + 1), Stream.iterate(-1, n -> n - 1))
                .skip(99999).limit(1)
                .map(Pair::toString).collect(Collectors.joining(", ")),
                is("(100000, -100000)"));
    }
    
    @Test
    public void test02() {
        // ******** パラメータチェックの確認 ********
        
        // stream1 == null or stream2 == null : NullPointerException
        assertThat(of(() -> ZippedStream.of(null, Stream.of(1, 2, 3))), raise(NullPointerException.class));
        assertThat(of(() -> ZippedStream.of(Stream.of(1, 2, 3), null)), raise(NullPointerException.class));
        assertThat(of(() -> ZippedStream.of(null, null)), raise(NullPointerException.class));
        
        // stream1 == stream2 : IllegalArgumentException
        Stream<Integer> stream1 = Stream.of(1, 2, 3);
        assertThat(of(() -> ZippedStream.of(stream1, stream1)), raise(IllegalArgumentException.class));
        
        // これはOK
        assertThat(of(() -> ZippedStream.of(Stream.empty(), Stream.empty())), raiseNothing());
    }
    
    @Test
    public void test03() {
        // ******** 遅延バインディングの確認 ********
        
        // 終端操作開始前までのものは反映されることの確認
        List<Integer> src1a = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<String> src1b = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        Stream<Pair<Integer, String>> zipped1 = ZippedStream.of(src1a.stream(), src1b.stream());
        
        src1a.add(4);
        src1b.remove(1);
        
        assertThat(zipped1.map(Pair::toString).collect(Collectors.joining(", ")),
                is("(1, a), (2, c), (3, d), (4, e)"));
                
        // 終端操作開始後の変更に対しては元の iterator のフェイル・ファストにより例外がスローされることの確認
        List<Integer> src2a = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<String> src2b = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        Stream<Pair<Integer, String>> zipped2 = ZippedStream.of(src2a.stream(), src2b.stream());
        
        assertThat(of(() -> zipped2.map(p -> {
            if (p.m1().equals(1)) {
                src2a.add(4);
                src2b.remove(1);
            }
            return p.toString();
        }).collect(Collectors.joining(", "))),
                raise(ConcurrentModificationException.class));
                
        // 大元のソースが並行操作のために設計されたものであれば、例外がスローされることなく処理が行われることの確認1
        List<Integer> src3a = new CopyOnWriteArrayList<>(Arrays.asList(1, 2, 3));
        List<String> src3b = new CopyOnWriteArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        Stream<Pair<Integer, String>> zipped3 = ZippedStream.of(src3a.stream(), src3b.stream());
        
        assertThat(zipped3.map(p -> {
            if (p.m1().equals(1)) {
                src3a.add(4);
                src3b.remove(1);
            }
            return p.toString();
        }).collect(Collectors.joining(", ")),
                is("(1, a), (2, b), (3, c)"));
                
        // 大元のソースが並行操作のために設計されたものであれば、例外がスローされることなく処理が行われることの確認2
        Set<Integer> src4a = new ConcurrentSkipListSet<>(Arrays.asList(1, 2, 3));
        Set<String> src4b = new ConcurrentSkipListSet<>(Arrays.asList("a", "b", "c", "d", "e"));
        Stream<Pair<Integer, String>> zipped4 = ZippedStream.of(src4a.stream(), src4b.stream());
        
        assertThat(zipped4.map(p -> {
            if (p.m1().equals(1)) {
                src4a.add(4);
                src4b.remove("b");
            }
            return p.toString();
        }).collect(Collectors.joining(", ")),
                is("(1, a), (2, c), (3, d), (4, e)"));
    }
    
    @Test
    public void test04() {
        // ******** ストリームのプロパティの確認 ********
        
        // 順次／並列の確認1
        Stream<Integer> stream1a = Arrays.asList(1, 2, 3).stream();
        Stream<String> stream1b = Arrays.asList("a", "b", "c").stream();
        assertThat(stream1a.isParallel(), is(false));
        assertThat(stream1b.isParallel(), is(false));
        
        Stream<Pair<Integer, String>> zipped1 = ZippedStream.of(stream1a, stream1b);
        assertThat(zipped1.isParallel(), is(false));
        
        // 順次／並列の確認2 ： ソースストリームがどうであれ、順次ストリームが生成される
        Stream<Integer> stream2a = Arrays.asList(1, 2, 3).stream().unordered().parallel();
        Stream<String> stream2b = Arrays.asList("a", "b", "c").stream().parallel().unordered();
        assertThat(stream2a.isParallel(), is(true));
        assertThat(stream2b.isParallel(), is(true));
        
        Stream<Pair<Integer, String>> zipped2 = ZippedStream.of(stream2a, stream2b);
        assertThat(zipped2.isParallel(), is(false));
    }
    
    @Test
    public void test05() {
        // ******** Spliterator のプロパティの確認1 ： ORDERED 特性 ********
        
        // (false, false) -> false
        Stream<Integer> stream1a = new HashSet<>(Arrays.asList(1, 2, 3, 1, 2, 3)).stream();
        Stream<String> stream1b = new HashSet<>(Arrays.asList("a", "b", "c", "a", "b", "c")).stream();
        assertThat(stream1a.spliterator().hasCharacteristics(Spliterator.ORDERED), is(false));
        assertThat(stream1b.spliterator().hasCharacteristics(Spliterator.ORDERED), is(false));
        
        stream1a = new HashSet<>(Arrays.asList(1, 2, 3, 1, 2, 3)).stream();
        stream1b = new HashSet<>(Arrays.asList("a", "b", "c", "a", "b", "c")).stream();
        Stream<Pair<Integer, String>> zipped1 = ZippedStream.of(stream1a, stream1b);
        assertThat(zipped1.spliterator().hasCharacteristics(Spliterator.ORDERED), is(false));
        
        // (false, true) -> false
        Stream<Integer> stream2a = new HashSet<>(Arrays.asList(1, 2, 3, 1, 2, 3)).stream();
        Stream<String> stream2b = Arrays.asList("a", "b", "c", "a", "b", "c").stream();
        assertThat(stream2a.spliterator().hasCharacteristics(Spliterator.ORDERED), is(false));
        assertThat(stream2b.spliterator().hasCharacteristics(Spliterator.ORDERED), is(true));
        
        stream2a = new HashSet<>(Arrays.asList(1, 2, 3, 1, 2, 3)).stream();
        stream2b = Arrays.asList("a", "b", "c", "a", "b", "c").stream();
        Stream<Pair<Integer, String>> zipped2 = ZippedStream.of(stream2a, stream2b);
        assertThat(zipped2.spliterator().hasCharacteristics(Spliterator.ORDERED), is(false));
        
        // (true, false) -> false
        Stream<Integer> stream3a = Arrays.asList(1, 2, 3, 1, 2, 3).stream();
        Stream<String> stream3b = new HashSet<>(Arrays.asList("a", "b", "c", "a", "b", "c")).stream();
        assertThat(stream3a.spliterator().hasCharacteristics(Spliterator.ORDERED), is(true));
        assertThat(stream3b.spliterator().hasCharacteristics(Spliterator.ORDERED), is(false));
        
        stream3a = Arrays.asList(1, 2, 3, 1, 2, 3).stream();
        stream3b = new HashSet<>(Arrays.asList("a", "b", "c", "a", "b", "c")).stream();
        Stream<Pair<Integer, String>> zipped3 = ZippedStream.of(stream3a, stream3b);
        assertThat(zipped3.spliterator().hasCharacteristics(Spliterator.ORDERED), is(false));
        
        // (true, true) -> true
        Stream<Integer> stream4a = Arrays.asList(1, 2, 3, 1, 2, 3).stream();
        Stream<String> stream4b = Arrays.asList("a", "b", "c", "a", "b", "c").stream();
        assertThat(stream4a.spliterator().hasCharacteristics(Spliterator.ORDERED), is(true));
        assertThat(stream4b.spliterator().hasCharacteristics(Spliterator.ORDERED), is(true));
        
        stream4a = Arrays.asList(1, 2, 3, 1, 2, 3).stream();
        stream4b = Arrays.asList("a", "b", "c", "a", "b", "c").stream();
        Stream<Pair<Integer, String>> zipped4 = ZippedStream.of(stream4a, stream4b);
        assertThat(zipped4.spliterator().hasCharacteristics(Spliterator.ORDERED), is(true));
    }
    
    @Test
    public void test06() {
        // ******** Spliterator のプロパティの確認2 ： DISTINCT 特性 ********
        
        // (false, false) -> false
        Stream<Integer> stream1a = Arrays.asList(1, 2, 3, 1, 2, 3).stream();
        Stream<String> stream1b = Arrays.asList("a", "b", "c", "a", "b", "c").stream();
        assertThat(stream1a.spliterator().hasCharacteristics(Spliterator.DISTINCT), is(false));
        assertThat(stream1b.spliterator().hasCharacteristics(Spliterator.DISTINCT), is(false));
        
        stream1a = Arrays.asList(1, 2, 3, 1, 2, 3).stream();
        stream1b = Arrays.asList("a", "b", "c", "a", "b", "c").stream();
        Stream<Pair<Integer, String>> zipped1 = ZippedStream.of(stream1a, stream1b);
        assertThat(zipped1.spliterator().hasCharacteristics(Spliterator.DISTINCT), is(false));
        
        // (false, true) -> true
        Stream<Integer> stream2a = Arrays.asList(1, 2, 3, 1, 2, 3).stream();
        Stream<String> stream2b = new HashSet<>(Arrays.asList("a", "b", "c")).stream();
        assertThat(stream2a.spliterator().hasCharacteristics(Spliterator.DISTINCT), is(false));
        assertThat(stream2b.spliterator().hasCharacteristics(Spliterator.DISTINCT), is(true));
        
        stream2a = Arrays.asList(1, 2, 3, 1, 2, 3).stream();
        stream2b = new HashSet<>(Arrays.asList("a", "b", "c")).stream();
        Stream<Pair<Integer, String>> zipped2 = ZippedStream.of(stream2a, stream2b);
        assertThat(zipped2.spliterator().hasCharacteristics(Spliterator.DISTINCT), is(true));
        
        // (true, false) -> true
        Stream<Integer> stream3a = new HashSet<>(Arrays.asList(1, 2, 3)).stream();
        Stream<String> stream3b = Arrays.asList("a", "b", "c", "a", "b", "c").stream();
        assertThat(stream3a.spliterator().hasCharacteristics(Spliterator.DISTINCT), is(true));
        assertThat(stream3b.spliterator().hasCharacteristics(Spliterator.DISTINCT), is(false));
        
        stream3a = new HashSet<>(Arrays.asList(1, 2, 3)).stream();
        stream3b = Arrays.asList("a", "b", "c", "a", "b", "c").stream();
        Stream<Pair<Integer, String>> zipped3 = ZippedStream.of(stream3a, stream3b);
        assertThat(zipped3.spliterator().hasCharacteristics(Spliterator.DISTINCT), is(true));
        
        // (true, true) -> true
        Stream<Integer> stream4a = new HashSet<>(Arrays.asList(1, 2, 3)).stream();
        Stream<String> stream4b = new HashSet<>(Arrays.asList("a", "b", "c")).stream();
        assertThat(stream4a.spliterator().hasCharacteristics(Spliterator.DISTINCT), is(true));
        assertThat(stream4b.spliterator().hasCharacteristics(Spliterator.DISTINCT), is(true));
        
        stream4a = new HashSet<>(Arrays.asList(1, 2, 3)).stream();
        stream4b = new HashSet<>(Arrays.asList("a", "b", "c")).stream();
        Stream<Pair<Integer, String>> zipped4 = ZippedStream.of(stream4a, stream4b);
        assertThat(zipped4.spliterator().hasCharacteristics(Spliterator.DISTINCT), is(true));
    }
    
    @Test
    public void test07() {
        // ******** Spliterator のプロパティの確認3 ： SORTED 特性 ********
        
        // Pair 同士の大小関係は定義されていないため、ソースストリームがともに SORTED であっても、生成されたストリームは SORTED 特性を報告しない。
        Stream<Integer> stream1a = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5)).stream();
        Stream<String> stream1b = new TreeSet<>(Arrays.asList("a", "b", "c", "d", "e")).stream();
        assertThat(stream1a.spliterator().hasCharacteristics(Spliterator.SORTED), is(true));
        assertThat(stream1b.spliterator().hasCharacteristics(Spliterator.SORTED), is(true));
        
        stream1a = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5)).stream();
        stream1b = new TreeSet<>(Arrays.asList("a", "b", "c", "d", "e")).stream();
        Stream<Pair<Integer, String>> zipped1 = ZippedStream.of(stream1a, stream1b);
        assertThat(zipped1.spliterator().hasCharacteristics(Spliterator.SORTED), is(false));
    }
    
    @Test
    public void test08() {
        // ******** Spliterator のプロパティの確認4 ： SIZED 特性、SUBSIZED 特性 ********
        
        // (false, false) -> false
        Stream<Integer> stream1a = Stream.iterate(1, n -> n + 1);
        Stream<Integer> stream1b = Stream.iterate(-1, n -> n - 1);
        Spliterator<Integer> sp1a = stream1a.spliterator();
        Spliterator<Integer> sp1b = stream1b.spliterator();
        assertThat(sp1a.hasCharacteristics(Spliterator.SIZED), is(false));
        assertThat(sp1b.hasCharacteristics(Spliterator.SIZED), is(false));
        assertThat(sp1a.hasCharacteristics(Spliterator.SUBSIZED), is(false));
        assertThat(sp1b.hasCharacteristics(Spliterator.SUBSIZED), is(false));
        
        stream1a = Stream.iterate(1, n -> n + 1);
        stream1b = Stream.iterate(-1, n -> n - 1);
        Stream<Pair<Integer, Integer>> zipped1 = ZippedStream.of(stream1a, stream1b);
        Spliterator<Pair<Integer, Integer>> sp1z = zipped1.spliterator();
        assertThat(sp1z.hasCharacteristics(Spliterator.SIZED), is(false));
        assertThat(sp1z.hasCharacteristics(Spliterator.SUBSIZED), is(false));
        
        // (false, true) -> true
        Stream<Integer> stream2a = Stream.iterate(1, n -> n + 1);
        Stream<Integer> stream2b = Arrays.asList(-1, -2, -3, -4, -5).stream();
        Spliterator<Integer> sp2a = stream2a.spliterator();
        Spliterator<Integer> sp2b = stream2b.spliterator();
        assertThat(sp2a.hasCharacteristics(Spliterator.SIZED), is(false));
        assertThat(sp2b.hasCharacteristics(Spliterator.SIZED), is(true));
        assertThat(sp2a.hasCharacteristics(Spliterator.SUBSIZED), is(false));
        assertThat(sp2b.hasCharacteristics(Spliterator.SUBSIZED), is(true));
        
        stream2a = Stream.iterate(1, n -> n + 1);
        stream2b = Arrays.asList(-1, -2, -3, -4, -5).stream();
        Stream<Pair<Integer, Integer>> zipped2 = ZippedStream.of(stream2a, stream2b);
        Spliterator<Pair<Integer, Integer>> sp2z = zipped2.spliterator();
        assertThat(sp2z.hasCharacteristics(Spliterator.SIZED), is(true));
        assertThat(sp2z.hasCharacteristics(Spliterator.SUBSIZED), is(true));
        
        // (true, false) -> true
        Stream<Integer> stream3a = Arrays.asList(1, 2, 3, 4, 5).stream();
        Stream<Integer> stream3b = Stream.iterate(-1, n -> n - 1);
        Spliterator<Integer> sp3a = stream3a.spliterator();
        Spliterator<Integer> sp3b = stream3b.spliterator();
        assertThat(sp3a.hasCharacteristics(Spliterator.SIZED), is(true));
        assertThat(sp3b.hasCharacteristics(Spliterator.SIZED), is(false));
        assertThat(sp3a.hasCharacteristics(Spliterator.SUBSIZED), is(true));
        assertThat(sp3b.hasCharacteristics(Spliterator.SUBSIZED), is(false));
        
        stream3a = Arrays.asList(1, 2, 3, 4, 5).stream();
        stream3b = Stream.iterate(-1, n -> n - 1);
        Stream<Pair<Integer, Integer>> zipped3 = ZippedStream.of(stream3a, stream3b);
        Spliterator<Pair<Integer, Integer>> sp3z = zipped3.spliterator();
        assertThat(sp3z.hasCharacteristics(Spliterator.SIZED), is(true));
        assertThat(sp3z.hasCharacteristics(Spliterator.SUBSIZED), is(true));
        
        // (true, true) -> true
        Stream<Integer> stream4a = Arrays.asList(1, 2, 3, 4, 5).stream();
        Stream<Integer> stream4b = Arrays.asList(-1, -2, -3, -4, -5).stream();
        Spliterator<Integer> sp4a = stream4a.spliterator();
        Spliterator<Integer> sp4b = stream4b.spliterator();
        assertThat(sp4a.hasCharacteristics(Spliterator.SIZED), is(true));
        assertThat(sp4b.hasCharacteristics(Spliterator.SIZED), is(true));
        
        stream4a = Arrays.asList(1, 2, 3, 4, 5).stream();
        stream4b = Arrays.asList(-1, -2, -3, -4, -5).stream();
        Stream<Pair<Integer, Integer>> zipped4 = ZippedStream.of(stream4a, stream4b);
        Spliterator<Pair<Integer, Integer>> sp4z = zipped4.spliterator();
        assertThat(sp4z.hasCharacteristics(Spliterator.SIZED), is(true));
        assertThat(sp4z.hasCharacteristics(Spliterator.SUBSIZED), is(true));
    }
    
    @Test
    public void test09() {
        // ******** Spliterator のプロパティの確認5 ： NONNULL 特性 ********
        
        // ソースストリームが null 要素を含むとしても、生成されたストリームは常に Pair インスタンスを返すため、常に NONNULL 特性を報告する。
        Stream<Integer> stream1a = Arrays.asList(1, null, 3, null, 5).stream();
        Stream<String> stream1b = Arrays.asList("a", "b", null, null, "e").stream();
        assertThat(stream1a.spliterator().hasCharacteristics(Spliterator.NONNULL), is(false));
        assertThat(stream1b.spliterator().hasCharacteristics(Spliterator.NONNULL), is(false));
        
        stream1a = Arrays.asList(1, null, 3, null, 5).stream();
        stream1b = Arrays.asList("a", "b", null, null, "e").stream();
        Stream<Pair<Integer, String>> zipped1 = ZippedStream.of(stream1a, stream1b);
        assertThat(zipped1.spliterator().hasCharacteristics(Spliterator.NONNULL), is(true));
        
        stream1a = Arrays.asList(1, null, 3, null, 5).stream();
        stream1b = Arrays.asList("a", "b", null, null, "e").stream();
        zipped1 = ZippedStream.of(stream1a, stream1b);
        assertThat(zipped1.map(Pair::toString).collect(Collectors.joining(", ")),
                is("(1, a), (null, b), (3, null), (null, null), (5, e)"));
    }
    
    @Test
    public void test10() {
        // ******** Spliterator のプロパティの確認6 ： IMMUTABLE 特性 ********
        
        // (false, false) -> false
        Stream<Integer> stream1a = Arrays.asList(1, 2, 3).stream();
        Stream<String> stream1b = Arrays.asList("a", "b", "c").stream();
        assertThat(stream1a.spliterator().hasCharacteristics(Spliterator.IMMUTABLE), is(false));
        assertThat(stream1b.spliterator().hasCharacteristics(Spliterator.IMMUTABLE), is(false));
        
        stream1a = Arrays.asList(1, 2, 3).stream();
        stream1b = Arrays.asList("a", "b", "c").stream();
        Stream<Pair<Integer, String>> zipped1 = ZippedStream.of(stream1a, stream1b);
        assertThat(zipped1.spliterator().hasCharacteristics(Spliterator.IMMUTABLE), is(false));
        
        // (false, true) -> false
        Stream<Integer> stream2a = Arrays.asList(1, 2, 3).stream();
        Stream<String> stream2b = new CopyOnWriteArrayList<>(Arrays.asList("a", "b", "c")).stream();
        assertThat(stream2a.spliterator().hasCharacteristics(Spliterator.IMMUTABLE), is(false));
        assertThat(stream2b.spliterator().hasCharacteristics(Spliterator.IMMUTABLE), is(true));
        
        stream2a = Arrays.asList(1, 2, 3).stream();
        stream2b = new CopyOnWriteArrayList<>(Arrays.asList("a", "b", "c")).stream();
        Stream<Pair<Integer, String>> zipped2 = ZippedStream.of(stream2a, stream2b);
        assertThat(zipped2.spliterator().hasCharacteristics(Spliterator.IMMUTABLE), is(false));
        
        // (true, false) -> false
        Stream<Integer> stream3a = new CopyOnWriteArrayList<>(Arrays.asList(1, 2, 3)).stream();
        Stream<String> stream3b = Arrays.asList("a", "b", "c").stream();
        assertThat(stream3a.spliterator().hasCharacteristics(Spliterator.IMMUTABLE), is(true));
        assertThat(stream3b.spliterator().hasCharacteristics(Spliterator.IMMUTABLE), is(false));
        
        stream3a = new CopyOnWriteArrayList<>(Arrays.asList(1, 2, 3)).stream();
        stream3b = Arrays.asList("a", "b", "c").stream();
        Stream<Pair<Integer, String>> zipped3 = ZippedStream.of(stream3a, stream3b);
        assertThat(zipped3.spliterator().hasCharacteristics(Spliterator.IMMUTABLE), is(false));
        
        // (true, true) -> true
        Stream<Integer> stream4a = new CopyOnWriteArrayList<>(Arrays.asList(1, 2, 3)).stream();
        Stream<String> stream4b = new CopyOnWriteArrayList<>(Arrays.asList("a", "b", "c")).stream();
        assertThat(stream4a.spliterator().hasCharacteristics(Spliterator.IMMUTABLE), is(true));
        assertThat(stream4b.spliterator().hasCharacteristics(Spliterator.IMMUTABLE), is(true));
        
        stream4a = new CopyOnWriteArrayList<>(Arrays.asList(1, 2, 3)).stream();
        stream4b = new CopyOnWriteArrayList<>(Arrays.asList("a", "b", "c")).stream();
        Stream<Pair<Integer, String>> zipped4 = ZippedStream.of(stream4a, stream4b);
        assertThat(zipped4.spliterator().hasCharacteristics(Spliterator.IMMUTABLE), is(true));
    }
    
    @Test
    public void test11() {
        // ******** Spliterator のプロパティの確認7 ： CONCURRENT 特性 ********
        
        // CONCURRENT:(false, false), IMMUTABLE:(false, false) -> false
        Stream<Integer> stream1a = Arrays.asList(1, 2, 3).stream();
        Stream<String> stream1b = Arrays.asList("a", "b", "c").stream();
        Spliterator<Integer> sp1a = stream1a.spliterator();
        Spliterator<String> sp1b = stream1b.spliterator();
        assertThat(sp1a.hasCharacteristics(Spliterator.CONCURRENT), is(false));
        assertThat(sp1b.hasCharacteristics(Spliterator.CONCURRENT), is(false));
        assertThat(sp1a.hasCharacteristics(Spliterator.IMMUTABLE), is(false));
        assertThat(sp1b.hasCharacteristics(Spliterator.IMMUTABLE), is(false));
        
        stream1a = Arrays.asList(1, 2, 3).stream();
        stream1b = Arrays.asList("a", "b", "c").stream();
        Stream<Pair<Integer, String>> zipped1 = ZippedStream.of(stream1a, stream1b);
        assertThat(zipped1.spliterator().hasCharacteristics(Spliterator.CONCURRENT), is(false));
        
        // CONCURRENT:(false, true), IMMUTABLE:(false, false) -> false
        Stream<Integer> stream2a = Arrays.asList(1, 2, 3).stream();
        Stream<String> stream2b = new ConcurrentSkipListSet<>(Arrays.asList("a", "b", "c")).stream();
        Spliterator<Integer> sp2a = stream2a.spliterator();
        Spliterator<String> sp2b = stream2b.spliterator();
        assertThat(sp2a.hasCharacteristics(Spliterator.CONCURRENT), is(false));
        assertThat(sp2b.hasCharacteristics(Spliterator.CONCURRENT), is(true));
        assertThat(sp2a.hasCharacteristics(Spliterator.IMMUTABLE), is(false));
        assertThat(sp2b.hasCharacteristics(Spliterator.IMMUTABLE), is(false));
        
        stream2a = Arrays.asList(1, 2, 3).stream();
        stream2b = new ConcurrentSkipListSet<>(Arrays.asList("a", "b", "c")).stream();
        Stream<Pair<Integer, String>> zipped2 = ZippedStream.of(stream2a, stream2b);
        assertThat(zipped2.spliterator().hasCharacteristics(Spliterator.CONCURRENT), is(false));
        
        // CONCURRENT:(false, true), IMMUTABLE:(true, false) -> true
        Stream<Integer> stream3a = new CopyOnWriteArrayList<>(Arrays.asList(1, 2, 3)).stream();
        Stream<String> stream3b = new ConcurrentSkipListSet<>(Arrays.asList("a", "b", "c")).stream();
        Spliterator<Integer> sp3a = stream3a.spliterator();
        Spliterator<String> sp3b = stream3b.spliterator();
        assertThat(sp3a.hasCharacteristics(Spliterator.CONCURRENT), is(false));
        assertThat(sp3b.hasCharacteristics(Spliterator.CONCURRENT), is(true));
        assertThat(sp3a.hasCharacteristics(Spliterator.IMMUTABLE), is(true));
        assertThat(sp3b.hasCharacteristics(Spliterator.IMMUTABLE), is(false));
        
        stream3a = new CopyOnWriteArrayList<>(Arrays.asList(1, 2, 3)).stream();
        stream3b = new ConcurrentSkipListSet<>(Arrays.asList("a", "b", "c")).stream();
        Stream<Pair<Integer, String>> zipped3 = ZippedStream.of(stream3a, stream3b);
        assertThat(zipped3.spliterator().hasCharacteristics(Spliterator.CONCURRENT), is(true));
        
        // CONCURRENT:(true, false), IMMUTABLE:(false, false) -> false
        Stream<Integer> stream4a = new ConcurrentSkipListSet<>(Arrays.asList(1, 2, 3)).stream();
        Stream<String> stream4b = Arrays.asList("a", "b", "c").stream();
        Spliterator<Integer> sp4a = stream4a.spliterator();
        Spliterator<String> sp4b = stream4b.spliterator();
        assertThat(sp4a.hasCharacteristics(Spliterator.CONCURRENT), is(true));
        assertThat(sp4b.hasCharacteristics(Spliterator.CONCURRENT), is(false));
        assertThat(sp4a.hasCharacteristics(Spliterator.IMMUTABLE), is(false));
        assertThat(sp4b.hasCharacteristics(Spliterator.IMMUTABLE), is(false));
        
        stream4a = new ConcurrentSkipListSet<>(Arrays.asList(1, 2, 3)).stream();
        stream4b = Arrays.asList("a", "b", "c").stream();
        Stream<Pair<Integer, String>> zipped4 = ZippedStream.of(stream4a, stream4b);
        assertThat(zipped4.spliterator().hasCharacteristics(Spliterator.CONCURRENT), is(false));
        
        // CONCURRENT:(true, false), IMMUTABLE:(false, true) -> true
        Stream<Integer> stream5a = new ConcurrentSkipListSet<>(Arrays.asList(1, 2, 3)).stream();
        Stream<String> stream5b = new CopyOnWriteArrayList<>(Arrays.asList("a", "b", "c")).stream();
        Spliterator<Integer> sp5a = stream5a.spliterator();
        Spliterator<String> sp5b = stream5b.spliterator();
        assertThat(sp5a.hasCharacteristics(Spliterator.CONCURRENT), is(true));
        assertThat(sp5b.hasCharacteristics(Spliterator.CONCURRENT), is(false));
        assertThat(sp5a.hasCharacteristics(Spliterator.IMMUTABLE), is(false));
        assertThat(sp5b.hasCharacteristics(Spliterator.IMMUTABLE), is(true));
        
        stream5a = new ConcurrentSkipListSet<>(Arrays.asList(1, 2, 3)).stream();
        stream5b = new CopyOnWriteArrayList<>(Arrays.asList("a", "b", "c")).stream();
        Stream<Pair<Integer, String>> zipped5 = ZippedStream.of(stream5a, stream5b);
        assertThat(zipped5.spliterator().hasCharacteristics(Spliterator.CONCURRENT), is(true));
        
        // CONCURRENT:(true, true), IMMUTABLE:(false, false) -> true
        Stream<Integer> stream6a = new ConcurrentSkipListSet<>(Arrays.asList(1, 2, 3)).stream();
        Stream<String> stream6b = new ConcurrentSkipListSet<>(Arrays.asList("a", "b", "c")).stream();
        Spliterator<Integer> sp6a = stream6a.spliterator();
        Spliterator<String> sp6b = stream6b.spliterator();
        assertThat(sp6a.hasCharacteristics(Spliterator.CONCURRENT), is(true));
        assertThat(sp6b.hasCharacteristics(Spliterator.CONCURRENT), is(true));
        assertThat(sp6a.hasCharacteristics(Spliterator.IMMUTABLE), is(false));
        assertThat(sp6b.hasCharacteristics(Spliterator.IMMUTABLE), is(false));
        
        stream6a = new ConcurrentSkipListSet<>(Arrays.asList(1, 2, 3)).stream();
        stream6b = new ConcurrentSkipListSet<>(Arrays.asList("a", "b", "c")).stream();
        Stream<Pair<Integer, String>> zipped6 = ZippedStream.of(stream6a, stream6b);
        assertThat(zipped6.spliterator().hasCharacteristics(Spliterator.CONCURRENT), is(true));
    }
    
    @Test
    public void test12() {
        // ******** 大元のソースデータを共有する場合も正常に動くことの確認 ********
        
        // 読み取るだけの場合
        List<String> source1 = Arrays.asList("a", "b", "c");
        
        assertThat(
                ZippedStream.of(source1.stream(), source1.stream())
                .map(Pair::toString)
                .collect(Collectors.joining(", ")),
                is("(a, a), (b, b), (c, c)"));
        
        // 更新を伴う場合1
        Set<Integer> source2 = new ConcurrentSkipListSet<>(Arrays.asList(1, 2, 3));
        
        assertThat(
                ZippedStream.of(source2.stream(), source2.stream().skip(1))
                .peek(p -> source2.add(p.m1() + p.m2() * 2))
                .map(p -> p.m1().toString())
                .limit(10)
                .collect(Collectors.joining(", ")),
                is("1, 2, 3, 5, 8, 13, 21, 34, 55, 89"));
        
        // 更新を伴う場合2 ： 手動同期が必要な iterator を介すため、これはダメ
        // これはこのクラスの問題ではなく、Collections#synchronizedList が返す iterator が手動同期を必要とすることが原因。
        // -> ちょっと微妙な事象なので、javadoc に注意事項を明記することとした。
        List<Integer> source3 = Collections.synchronizedList(new ArrayList<>(Arrays.asList(1, 2, 3)));
        
        assertThat(of(() -> {
                ZippedStream.of(source3.stream(), source3.stream().skip(1))
                .peek(p -> source3.add(p.m1() + p.m2() * 2))
                .map(p -> p.m1().toString())
                .limit(10)
                .collect(Collectors.joining(", ")); }),
                raise(ConcurrentModificationException.class));
    }
    
    @Test
    public void test99() {
        // ******** その他の検証 ********
        
        // ZippedStream#of の実行によりソースストリームが再利用不可になることの確認
        Stream<Integer> stream1a = Arrays.asList(1, 2, 3).stream();
        Stream<String> stream1b = Arrays.asList("a", "b", "c").stream();
        ZippedStream.of(stream1a, stream1b);
        
        assertThat(of(() -> stream1a.forEach(System.out::println)), raise(IllegalStateException.class));
        assertThat(of(() -> stream1b.forEach(System.out::println)), raise(IllegalStateException.class));
    }
}
