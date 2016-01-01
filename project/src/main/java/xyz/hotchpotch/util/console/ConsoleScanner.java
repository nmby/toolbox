package xyz.hotchpotch.util.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 標準入力から対話的にユーザ入力を取得するためのクラスです。<br>
 * <br>
 * 求める形式とは異なる入力をユーザが行った場合、{@code ConsoleScanner} はユーザに何度も再入力を求めます。<br>
 * 正しい形式の入力が得られたら、それを必要な形式（数値、クラス、列挙型等）に変換し、
 * クライアント・アプリケーションに返却します。<br>
 * <br>
 * 次の例では、1～12の範囲の整数を標準入力から対話的に取得します。
 * <pre>
 *     int n = ConsoleScanner.intBuilder(1, 12).build().get();
 * </pre>
 * 次の例では、列挙型 {@code MyEnum} の要素の中のひとつを選択するようユーザに要求し、選択された要素を取得します。<br>
 * <pre>
 *     MyEnum selected = ConsoleScanner.enumBuilder(MyEnum.class).build().get();
 * </pre>
 * このほか、正規表現を指定して入力を求めることなども可能です。<br>
 * 標準出力に表示するプロンプトや、要求とは異なる形式の入力をユーザが行った場合に表示するエラーメッセージを
 * カスタマイズすることができます。<br>
 * 詳細は各メソッドの説明を参照してください。<br>
 * <br>
 * このクラスのオブジェクトはスレッドセーフではありません。
 * このクラスのオブジェクトを複数のスレッドから利用することは避けてください。<br>
 * ただし、このクラスのオブジェクトが {@link #get()} を実行しユーザからの入力を待機しているときに、他のスレッドから割り込みを行うことができます。<br>
 * オブジェクトは、割り込みを検知すると入力待機を解除し、標準では {@code null} を返却して速やかに終了します。<br>
 * 割り込みを検知した際の動作はカスタマイズすることが可能です。詳細は {@link Builder#emergencyMeasure(Function)} の説明を参照してください。<br>
 * 
 * @param <T> 最終的にクライアント・アプリケーションに返却されるデータの型
 * @since 1.0.0
 * @author nmby
 */
public class ConsoleScanner<T> implements Supplier<T> {
    
    // [static members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    private static final String BR = System.lineSeparator();
    
    /**
     * {@link ConsoleScanner} オブジェクトを構築するためのビルダーです。
     * 
     * @param <T> 最終的にクライアント・アプリケーションに返却されるデータの型
     * @author nmby
     */
    public static class Builder<T> {
        private final Predicate<String> judge;
        private final Function<String, ? extends T> converter;
        private String prompt;
        private String complaint;
        private Function<Exception, ? extends T> emergencyMeasure;
        
        private Builder(
                Predicate<String> judge,
                Function<String, ? extends T> converter,
                String prompt,
                String complaint,
                Function<Exception, ? extends T> emergencyMeasure) {
                
            assert judge != null;
            assert converter != null;
            assert prompt != null;
            assert complaint != null;
            assert emergencyMeasure != null;
            
            this.judge = judge;
            this.converter = converter;
            this.prompt = prompt;
            this.complaint = complaint;
            this.emergencyMeasure = emergencyMeasure;
        }
        
        /**
         * 標準出力に表示するプロンプト文字列を指定します。<br>
         * 
         * @param prompt 標準出力に表示するプロンプト文字列
         * @return この {@code Builder} オブジェクト
         * @throws NullPointerException {@code prompt} が {@code null} の場合
         */
        public Builder<T> prompt(String prompt) {
            this.prompt = Objects.requireNonNull(prompt);
            return this;
        }
        
        /**
         * ユーザが要求とは異なる形式で入力した場合に標準出力に表示するエラー文字列を指定します。<br>
         * 
         * @param complaint 標準出力に表示するエラー文字列
         * @return この {@code Builder} オブジェクト
         * @throws NullPointerException {@code complaint} が {@code null} の場合
         */
        public Builder<T> complaint(String complaint) {
            this.complaint = Objects.requireNonNull(complaint);
            return this;
        }
        
        /**
         * 割り込みや入出力例外が発生した際の対処方法を指定します。<br>
         * {@link ConsoleScanner#get()} の実行中に入出力例外や他のスレッドからの割り込みが発生した場合、
         * 捕捉された例外オブジェクト {@code e} をパラメータとして {@code emergencyMeasure.apply(e)} が実行され、
         * その戻り値が {@code ConsoleScanner#get()} の呼び出し元に返されます。<br>
         * {@code emergencyMeasure.apply(e)} は何らかの値を返すこともできますし、実行時例外をスローすることもできます。<br>
         * 明示的に指定しない場合のデフォルトでは、{@code emergencyMeasure.apply(e)} は単に {@code null} を返します。<br>
         * 
         * @param emergencyMeasure 割り込みや入出力例外が発生した場合の対処方法
         * @return この {@code Builder} オブジェクト
         * @throws NullPointerException {@code emergencyMeasure} が {@code null} の場合
         */
        public Builder<T> emergencyMeasure(Function<Exception, ? extends T> emergencyMeasure) {
            this.emergencyMeasure = Objects.requireNonNull(emergencyMeasure);
            return this;
        }
        
        /**
         * 現在設定されているプロンプト文字列を返します。<br>
         * 
         * @return 現在設定されているプロンプト文字列
         */
        public String prompt() {
            return prompt;
        }
        
        /**
         * 現在設定されているエラー文字列を返します。<br>
         * 
         * @return 現在設定されているエラー文字列
         */
        public String complaint() {
            return complaint;
        }
        
        /**
         * {@link ConsoleScanner} オブジェクトを生成します。<br>
         * 
         * @return {@code ConsoleScanner} オブジェクト
         */
        public ConsoleScanner<T> build() {
            return new ConsoleScanner<>(judge, converter, prompt, complaint, emergencyMeasure);
        }
    }
    
    /**
     * {@code String} 型の入力値を取得するための {@code ConsoleScanner} のビルダーを返します。<br>
     * 
     * @param judge ユーザ入力値が要求形式に合致するかを判定する {@link Predicate}
     * @return {@link Builder} オブジェクト
     * @throws NullPointerException {@code judge} が {@code null} の場合
     */
    public static Builder<String> stringBuilder(Predicate<String> judge) {
        Objects.requireNonNull(judge);
        return new Builder<>(
                judge,
                Function.identity(),
                "> ",
                "入力形式が不正です。再入力してください ",
                e -> null);
    }
    
    /**
     * {@code String} 型の入力値を取得するための {@code ConsoleScanner} のビルダーを返します。<br>
     * 
     * @param pattern ユーザ入力値が要求形式に合致するかを判定する {@link Pattern}
     * @return {@link Builder} オブジェクト
     * @throws NullPointerException {@code pattern} が {@code null} の場合
     */
    public static Builder<String> stringBuilder(Pattern pattern) {
        Objects.requireNonNull(pattern);
        return stringBuilder(pattern.asPredicate());
    }
    
    /**
     * {@code String} 型の入力値を取得するための {@code ConsoleScanner} のビルダーを返します。<br>
     * 
     * @param regex ユーザ入力値が要求形式に合致するかを判定する正規表現文字列
     * @return {@link Builder} オブジェクト
     * @throws NullPointerException {@code regex} が {@code null} の場合
     * @throws PatternSyntaxException {@code regex} を {@link Pattern} にコンパイルできない場合
     * @see Pattern#compile(String)
     */
    public static Builder<String> stringBuilder(String regex) {
        Objects.requireNonNull(regex);
        return stringBuilder(Pattern.compile(regex));
    }
    
    /**
     * {@code Integer} 型の入力値を取得するための {@code ConsoleScanner} のビルダーを返します。<br>
     * 
     * @param lower 要求する範囲の下限値（この値を範囲に含みます）
     * @param upper 要求する範囲の上限値（この値を範囲に含みます）
     * @return {@link Builder} オブジェクト
     * @throws IllegalArgumentException {@code upper} よりも {@code lower} が大きい場合
     */
    public static Builder<Integer> intBuilder(int lower, int upper) {
        if (upper < lower) {
            throw new IllegalArgumentException(String.format("lower=%d, upper=%d", lower, upper));
        }
        Predicate<String> judge = s -> {
            try {
                int n = Integer.parseInt(s);
                return lower <= n && n <= upper;
            } catch (NumberFormatException e) {
                return false;
            }
        };
        Function<String, Integer> converter = Integer::valueOf;
        String prompt = String.format("%d～%dの範囲の値を指定してください > ", lower, upper);
        String complaint = "入力値が不正です。";
        return new Builder<>(judge, converter, prompt, complaint, e -> null);
    }
    
    /**
     * {@code Long} 型の入力値を取得するための {@code ConsoleScanner} のビルダーを返します。<br>
     * 
     * @param lower 要求する範囲の下限値（この値を範囲に含みます）
     * @param upper 要求する範囲の上限値（この値を範囲に含みます）
     * @return {@link Builder} オブジェクト
     * @throws IllegalArgumentException {@code upper} よりも {@code lower} が大きい場合
     */
    public static Builder<Long> longBuilder(long lower, long upper) {
        if (upper < lower) {
            throw new IllegalArgumentException(String.format("lower=%d, upper=%d", lower, upper));
        }
        Predicate<String> judge = s -> {
            try {
                long n = Long.parseLong(s);
                return lower <= n && n <= upper;
            } catch (NumberFormatException e) {
                return false;
            }
        };
        Function<String, Long> converter = Long::valueOf;
        String prompt = String.format("%d～%dの範囲の値を指定してください > ", lower, upper);
        String complaint = "入力値が不正です。";
        return new Builder<>(judge, converter, prompt, complaint, e -> null);
    }
    
    /**
     * リストの中から選択された要素を取得するための {@code ConsoleScanner} のビルダーを返します。<br>
     * 
     * @param <T> 最終的にクライアント・アプリケーションに返却されるデータの型
     * @param list 選択対象の要素が格納されたリスト
     * @return {@link Builder} オブジェクト
     * @throws NullPointerException {@code list} が {@code null} の場合
     * @throws IllegalArgumentException {@code list} の要素数が {@code 0} の場合
     */
    public static <T> Builder<T> listBuilder(List<? extends T> list) {
        Objects.requireNonNull(list);
        if (list.isEmpty()) {
            throw new IllegalArgumentException("list is empty.");
        }
        
        Predicate<String> judge = s -> {
            try {
                int idx = Integer.parseInt(s) - 1;
                return 0 <= idx && idx < list.size();
            } catch (NumberFormatException e) {
                return false;
            }
        };
        Function<String, T> converter = s -> {
            int idx = Integer.parseInt(s) - 1;
            return list.get(idx);
        };
        StringBuilder prompt = new StringBuilder();
        prompt.append("次の中から番号で指定してください。").append(BR);
        for (int i = 0; i < list.size(); i++) {
            prompt.append(String.format("\t%d : %s", i + 1, list.get(i))).append(BR);
        }
        prompt.append("> ");
        String complaint = "入力値が不正です。";
        return new Builder<>(judge, converter, prompt.toString(), complaint, e -> null);
    }
    
    /**
     * 列挙型の要素の中から選択された要素を取得するための {@code ConsoleScanner} のビルダーを返します。<br>
     * 
     * @param <E> 最終的にクライアント・アプリケーションに返却されるデータの型
     * @param type 列挙型クラス
     * @return {@link Builder} オブジェクト
     * @throws NullPointerException {@code type} が {@code null} の場合
     */
    public static <E extends Enum<E>> Builder<E> enumBuilder(Class<E> type) {
        Objects.requireNonNull(type);
        return listBuilder(Arrays.asList(type.getEnumConstants()));
    }
    
    /**
     * 任意の型の入力値を取得するための {@code ConsoleScanner} のビルダーを返します。<br>
     * 次の2つの呼び出しは同値です。
     * <pre>
     *     builder(judge, converter, prompt, complaint);
     *     builder(judge, converter, prompt, complaint, e -&gt; null);
     * </pre>
     * 
     * @param <T> 最終的にクライアント・アプリケーションに返却されるデータの型
     * @param judge ユーザ入力値が要求形式に合致するかを判定する {@link Predicate}
     * @param converter ユーザ入力文字列を {@code T} 型に変換するための {@link Function}
     * @param prompt 標準出力に表示するプロンプト文字列
     * @param complaint ユーザが要求とは異なる形式で入力した場合に標準出力に表示するエラー文字列
     * @return {@link Builder} オブジェクト
     * @throws NullPointerException {@code judge}、{@code converter}、{@code prompt}、{@code complaint}
     *                              のいずれかが {@code null} の場合
     */
    public static <T> Builder<T> builder(
            Predicate<String> judge,
            Function<String, ? extends T> converter,
            String prompt,
            String complaint) {
            
        return new Builder<>(
                Objects.requireNonNull(judge),
                Objects.requireNonNull(converter),
                Objects.requireNonNull(prompt),
                Objects.requireNonNull(complaint),
                e -> null);
    }
    
    /**
     * 任意の型の入力値を取得するための {@code ConsoleScanner} のビルダーを返します。<br>
     * 
     * @param <T> 最終的にクライアント・アプリケーションに返却されるデータの型
     * @param judge ユーザ入力値が要求形式に合致するかを判定する {@link Predicate}
     * @param converter ユーザ入力文字列を {@code T} 型に変換するための {@link Function}
     * @param prompt 標準出力に表示するプロンプト文字列
     * @param complaint ユーザが要求とは異なる形式で入力した場合に標準出力に表示するエラー文字列
     * @param emergencyMeasure 割り込みや入出力例外が発生した場合の対処方法
     *                         （詳細は {@link Builder#emergencyMeasure(Function)} の説明を参照してください）
     * @return {@link Builder} オブジェクト
     * @throws NullPointerException {@code judge}、{@code converter}、{@code prompt}、{@code complaint}、{@code emergencyMeasure}
     *                              のいずれかが {@code null} の場合
     */
    public static <T> Builder<T> builder(
            Predicate<String> judge,
            Function<String, ? extends T> converter,
            String prompt,
            String complaint,
            Function<Exception, ? extends T> emergencyMeasure) {
            
        return new Builder<>(
                Objects.requireNonNull(judge),
                Objects.requireNonNull(converter),
                Objects.requireNonNull(prompt),
                Objects.requireNonNull(complaint),
                Objects.requireNonNull(emergencyMeasure));
    }
    
    /**
     * ユーザが確認するまで待機するための {@code ConsoleScanner} を生成します。<br>
     * 生成される {@code ConsoleScanner} の {@link #get()} メソッドは、
     * 標準出力に「{@code 何か入力すると続行します > }」と表示し、
     * ユーザが何らかの入力を行うとその入力値を返します。<br>
     * 
     * @return ユーザが確認するまで待機するための {@code ConsoleScanner}
     */
    public static ConsoleScanner<String> waiter() {
        return waiter("何か入力すると続行します > ");
    }
    
    /**
     * ユーザが確認するまで待機するための {@code ConsoleScanner} を生成します。<br>
     * 生成される {@code ConsoleScanner} の {@link #get()} メソッドは、
     * 標準出力にプロンプト文字列を表示し、
     * ユーザが何らかの入力を行うとその入力値を返します。<br>
     * 
     * @param prompt 標準出力に表示するプロンプト文字列
     * @return ユーザが確認するまで待機するための {@code ConsoleScanner}
     * @throws NullPointerException {@code prompt} が {@code null} の場合
     */
    public static ConsoleScanner<String> waiter(String prompt) {
        Objects.requireNonNull(prompt);
        return new Builder<String>(s -> true, Function.identity(), prompt, "", e -> null).build();
    }
    
    // [instance members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    private final Predicate<String> judge;
    private final Function<String, ? extends T> converter;
    private final String prompt;
    private final String complaint;
    private final Function<Exception, ? extends T> emergencyMeasure;
    
    private ConsoleScanner(
            Predicate<String> judge,
            Function<String, ? extends T> converter,
            String prompt,
            String complaint,
            Function<Exception, ? extends T> emergencyMeasure) {
            
        assert judge != null;
        assert converter != null;
        assert prompt != null;
        assert complaint != null;
        assert emergencyMeasure != null;
        
        this.judge = judge;
        this.converter = converter;
        this.prompt = prompt;
        this.complaint = complaint;
        this.emergencyMeasure = emergencyMeasure;
    }
    
    /**
     * 標準入力から対話的にユーザ入力値を取得し、目的の型に変換して返します。<br>
     * 要求する形式の入力値が得られるまで、ユーザに何度も再入力を求めます。<br>
     * <br>
     * 入力待機中に割り込みを検知した場合は入力待機を解除し、
     * このオブジェクトの構築時に指定された方法に従って速やかに終了します。
     * （詳細は {@link Builder#emergencyMeasure(Function)} の説明を参照してください。）<br>
     * 
     * @return ユーザ入力値を変換した {@code T} 型の値
     */
    @Override
    public T get() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        boolean isFirst = true;
        String str;
        
        try {
            do {
                if (isFirst) {
                    isFirst = false;
                } else {
                    System.out.print(complaint);
                }
                System.out.print(prompt);
                
                // スレッド間の割り込み制御だとか入出力ストリーム処理だとかが理解できていない... orz
                // TODO: 要お勉強
                while (!reader.ready()) {
                    Thread.sleep(100);
                }
                str = reader.readLine();
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
            } while (!judge.test(str));
            
        } catch (InterruptedException | IOException e) {
            return emergencyMeasure.apply(e);
        }
        
        return converter.apply(str);
    }
}
