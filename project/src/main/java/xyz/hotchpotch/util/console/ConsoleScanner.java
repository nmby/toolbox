package xyz.hotchpotch.util.console;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
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
 * 次の例では、0～12の範囲の整数を標準入力から対話的に取得します。
 * <pre>
 *     int n = ConsoleScanner.intBuilder(0, 12).build().get();
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
 * このクラスのオブジェクトはスレッドセーフではありません。<br>
 * このクラスのオブジェクトを複数のスレッドで共有しないでください。<br>
 * 
 * @param <T> 最終的にクライアント・アプリケーションに返却されるデータの型
 * @author nmby
 */
public class ConsoleScanner<T> implements Supplier<T> {
    
    // ++++++++++++++++ static members ++++++++++++++++
    
    private static final String BR = System.lineSeparator();
    
    /**
     * {@link ConsoleScanner} オブジェクトを構築するためのビルダーです。
     * 
     * @param <T> 最終的にクライアント・アプリケーションに返却されるデータの型
     * @author nmby
     */
    public static class Builder<T> {
        private final Predicate<String> judge;
        private final Function<String, T> converter;
        private String prompt;
        private String complaint;
        
        private Builder(
                Predicate<String> judge,
                Function<String, T> converter,
                String prompt,
                String complaint) {
                
            this.judge = judge;
            this.converter = converter;
            this.prompt = prompt;
            this.complaint = complaint;
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
            return new ConsoleScanner<>(judge, converter, prompt, complaint);
        }
    }
    
    /**
     * {@code String} 型の入力を取得するための {@code ConsoleScanner} のビルダーを返します。<br>
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
                "入力形式が不正です。再入力してください ");
    }
    
    /**
     * {@code String} 型の入力を取得するための {@code ConsoleScanner} のビルダーを返します。<br>
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
     * {@code String} 型の入力を取得するための {@code ConsoleScanner} のビルダーを返します。<br>
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
     * {@code Integer} 型の入力を取得するための {@code ConsoleScanner} のビルダーを返します。<br>
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
        return new Builder<>(judge, converter, prompt, complaint);
    }
    
    /**
     * {@code Long} 型の入力を取得するための {@code ConsoleScanner} のビルダーを返します。<br>
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
        return new Builder<>(judge, converter, prompt, complaint);
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
    public static <T> Builder<T> listBuilder(List<T> list) {
        Objects.requireNonNull(list);
        if (list.size() == 0) {
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
        return new Builder<>(judge, converter, prompt.toString(), complaint);
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
     * 任意の型の入力を取得するための {@code ConsoleScanner} のビルダーを返します。<br>
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
            Function<String, T> converter,
            String prompt,
            String complaint) {
            
        return new Builder<>(
                Objects.requireNonNull(judge),
                Objects.requireNonNull(converter),
                Objects.requireNonNull(prompt),
                Objects.requireNonNull(complaint));
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
        return new Builder<String>(s -> true, Function.identity(), prompt, null).build();
    }
    
    // ++++++++++++++++ instance members ++++++++++++++++
    
    private final Predicate<String> judge;
    private final Function<String, T> converter;
    private final String prompt;
    private final String complaint;
    
    private ConsoleScanner(
            Predicate<String> judge,
            Function<String, T> converter,
            String prompt,
            String complaint) {
            
        this.judge = judge;
        this.converter = converter;
        this.prompt = prompt;
        this.complaint = complaint;
    }
    
    /**
     * 標準入力から対話的にユーザ入力値を取得し、目的の型に変換して返します。<br>
     * 要求する形式の入力値が得られるまで、ユーザに何度も再入力を求めます。<br>
     * 
     * @return ユーザ入力値を変換した {@code T} 型の値
     */
    @Override
    public T get() {
        // System.in をクローズしてはダメ。
        @SuppressWarnings("resource")
        Scanner sc = new Scanner(System.in);
        
        System.out.print(prompt);
        String str = sc.nextLine();
        
        while (!judge.test(str)) {
            System.out.print(complaint);
            System.out.print(prompt);
            str = sc.nextLine();
        }
        return converter.apply(str);
    }
}
