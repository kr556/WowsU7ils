package org.w7ls.utils;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import org.jetbrains.annotations.Nullable;
import org.w7ls.main.APIQueryNames;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public final class WordTable {
    // table1 > table0を優先順位とする
    private static final HashMap<Character, String> table0;
    private static final HashMap<String, String> table1;
    private static final List<Character> keyl0;
    private static final List<String> keyl1;
    private static final Pattern p = Pattern.compile("[キシチニヒミリギジビピ][ャ-ョ]");
    private static final Pattern kana = Pattern.compile("[ア-ン]|[ァ-ォ]");
    private static final Pattern alfbt = Pattern.compile("[A-Z]|[a-z]|[0-9]");
    private static final Pattern escape = Pattern.compile("\\\\[a-z]|[A-Z]");

    static {
        table0 = new HashMap<>();
        table1 = new HashMap<>();
        table0.put('ア', "a");
        table0.put('イ', "i");
        table0.put('ウ', "u");
        table0.put('エ', "e");
        table0.put('オ', "o");
        table0.put('カ', "ka");
        table0.put('キ', "ki");
        table0.put('ク', "ku");
        table0.put('ケ', "ke");
        table0.put('コ', "ko");
        table0.put('サ', "sa");
        table0.put('シ', "shi");
        table0.put('ス', "su");
        table0.put('セ', "se");
        table0.put('ソ', "so");
        table0.put('タ', "ta");
        table0.put('チ', "chi");
        table0.put('ツ', "tsu");
        table0.put('テ', "te");
        table0.put('ト', "to");
        table0.put('ナ', "na");
        table0.put('ニ', "ni");
        table0.put('ヌ', "nu");
        table0.put('ネ', "ne");
        table0.put('ノ', "no");
        table0.put('ハ', "ha");
        table0.put('ヒ', "hi");
        table0.put('フ', "fu");
        table0.put('ヘ', "he");
        table0.put('ホ', "ho");
        table0.put('マ', "ma");
        table0.put('ミ', "mi");
        table0.put('ム', "mu");
        table0.put('メ', "me");
        table0.put('モ', "mo");
        table0.put('ヤ', "ya");
        table0.put('ユ', "yu");
        table0.put('ヨ', "yo");
        table0.put('ラ', "ra");
        table0.put('リ', "ri");
        table0.put('ル', "ru");
        table0.put('レ', "re");
        table0.put('ロ', "ro");
        table0.put('ワ', "wa");
        table0.put('ヰ', "i");
        table0.put('ヱ', "e");
        table0.put('ヲ', "o");
        table0.put('ン', "n");
        table0.put('ガ', "ga");
        table0.put('ギ', "gi");
        table0.put('グ', "gu");
        table0.put('ゲ', "ge");
        table0.put('ゴ', "go");
        table0.put('ザ', "za");
        table0.put('ジ', "ji");
        table0.put('ズ', "zu");
        table0.put('ゼ', "ze");
        table0.put('ゾ', "zo");
        table0.put('ダ', "da");
        table0.put('ヂ', "ji");
        table0.put('ヅ', "zu");
        table0.put('デ', "de");
        table0.put('ド', "do");
        table0.put('バ', "ba");
        table0.put('ビ', "bi");
        table0.put('ブ', "bu");
        table0.put('ベ', "be");
        table0.put('ボ', "bo");
        table0.put('パ', "pa");
        table0.put('ピ', "pi");
        table0.put('プ', "pu");
        table0.put('ペ', "pe");
        table0.put('ポ', "po");
        table0.put('ァ', "a");
        table0.put('ィ', "i");
        table0.put('ゥ', "u");
        table0.put('ェ', "e");
        table0.put('ォ', "o");
        table0.put('ヴ', "bu");
        table1.put("キャ", "kya");
        table1.put("キュ", "kyu");
        table1.put("キョ", "kyo");
        table1.put("シャ", "sha");
        table1.put("シュ", "shu");
        table1.put("ショ", "sho");
        table1.put("チャ", "cha");
        table1.put("チュ", "chu");
        table1.put("チョ", "cho");
        table1.put("ニャ", "nya");
        table1.put("ニュ", "nyu");
        table1.put("ニョ", "nyo");
        table1.put("ヒャ", "hya");
        table1.put("ヒュ", "hyu");
        table1.put("ヒョ", "hyo");
        table1.put("ミャ", "mya");
        table1.put("ミュ", "myu");
        table1.put("ミョ", "myo");
        table1.put("リャ", "rya");
        table1.put("リュ", "ryu");
        table1.put("リョ", "ryo");
        table1.put("ギャ", "gya");
        table1.put("ギュ", "gyu");
        table1.put("ギョ", "gyo");
        table1.put("ジャ", "ja");
        table1.put("ジュ", "ju");
        table1.put("ジョ", "jo");
        table1.put("ビャ", "bya");
        table1.put("ビュ", "byu");
        table1.put("ビョ", "byo");
        table1.put("ピャ", "pya");
        table1.put("ピュ", "pyu");
        table1.put("ピョ", "pyo");
        keyl0 = new ArrayList<>(table0.keySet());
        keyl1 = new ArrayList<>(table1.keySet());
    }

    private WordTable() {}

    private static String tablingJA(String reading) {
        StringBuilder re = new StringBuilder();
        String[] g1 = p.matcher(reading).results()
                .map(r -> table1.get(r.group()))
                .toArray(String[]::new);
        char[] cs = new char[reading.length() + 1];
        String[] g0 = Arrays.stream(p.split(reading))
                .map(s -> {
                    // table0の文字の置き換え処理
                    s.getChars(0, s.length(), cs, 0);
                    StringBuilder re_ = new StringBuilder();
                    for (int i = 0; i < s.length(); i++) {
                        char c = cs[i];
                        if (c == 'ッ')
                            re_.append(table0.get(cs[i + 1]).charAt(0));
                        else if(c == 'ー');
                        else if(alfbt.matcher(String.valueOf(c)).find())
                            re_.append(c);
                        else if(kana.matcher(String.valueOf(c)).find())
                            re_.append(table0.get(c));
                        else
                            re_.append(c);
                    }
                    return re_.toString();
                })
                .toArray(String[]::new);
        int cnt = g1.length + g0.length;

        for (int i = 0; i < cnt; i++) {
            if (i % 2 == 0 && g0.length != 0)
                re.append(g0[i / 2]);
            else if (g1.length != 0)
                re.append(g1[(i - 1) / 2]);
        }

        return re.toString();
    }

    /**
     * @return null if lang is not supported language.
     */
    public static @Nullable String getENPronunciation(String str, APIQueryNames.Language lang) {
        return switch (lang) {
            case RU, TH, EN, PL, DE, FR, ES, ZH_CN, ZH_TW, TR, CS, PT_BR, ES_MX -> null;
            case JA -> {
                List<Token> tkl = new Tokenizer().tokenize(str);
                StringBuilder sb = new StringBuilder();
                for (Token tk : tkl) {
                    String surface = tk.getSurface();
                    if (alfbt.matcher(surface).find() || escape.matcher(surface).find())
                        sb.append(surface);
                    else if (surface.equals(" "))
                        sb.append(" ");
                    else if (kana.matcher(surface).find())
                        sb.append(tablingJA(surface));
                    else
                        sb.append(tablingJA(tk.getPronunciation()));
                }
                yield sb.toString();
            }
        };
    }
}
