package Model;

import Lexicon.Model.Lexicon;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author Andreas Novian
 */
public class Parser {

    //an object of a lexicon
    public Lexicon lexicon;

    private final ArrayList<String> parseResult;

    public Parser() throws IOException {
        this.lexicon = new Lexicon();
        this.parseResult = new ArrayList<>();
    }

    /**
     * Method to search whether a word is a valid root word in lexicon tree or
     * not
     *
     * @param word a word to check
     * @return true if and only if word is a valid root word in lexicon tree
     */
    private boolean isRootWord(String word) {
        return lexicon.searchInTree(word.toLowerCase());
    }

    /**
     * Check each component in tempResult, cross check with lexicon, delete from
     * tempResult if not valid based on lexicon
     *
     * @throws IOException
     */
    private void componentValidator() throws IOException {
        String rootWord;
        String component;
        String line;
        boolean valid;
        String[] words;

        for (int i = 0; i < this.parseResult.size(); i++) {
            component = "";
            line = this.parseResult.get(i);
            words = line.split("\\+");
            rootWord = words[0];
            for (int j = 1; j < words.length; j++) {
                if (words[j].charAt(0) != '$') {
                    component += words[j] + "+";
                }
            }

            if (!component.equalsIgnoreCase("")) {
                component = component.substring(0, component.length() - 1);
                valid = this.lexicon.searchInFile(rootWord, component);
                if (!valid) {
                    this.parseResult.remove(line);
                    i--;
                }
            }
        }
    }

    /**
     * To convert from String "malu+#per-kan+[me+$mu" to "Prefiks [me] + Bentuk
     * Dasar [malu] + Konfiks [per-kan] + Klitika [mu]"
     */
    private void convertToWord() throws IOException {
        String rootWord;
        String komposisi, reduplikasi, prefiks, sufiks, konfiks, proklitika, enklitika;
        String line, result;
        String[] words;
        String[] temp;

        for (int i = 0; i < this.parseResult.size(); i++) {
            komposisi = "";
            reduplikasi = "";
            prefiks = "";
            sufiks = "";
            konfiks = "";
            proklitika = "";
            enklitika = "";
            result = "";

            line = this.parseResult.get(i);
            if (line.contains("(")) {
                reduplikasi = line.substring(line.indexOf("("), line.indexOf(")") + 1);
                line = line.replace("+^" + reduplikasi, "");
                reduplikasi += "+";
            }
            words = line.split("\\+");
            rootWord = words[0];
            for (String word : words) {
                switch (word.charAt(0)) {
                    case '@':
                        komposisi += word.substring(1) + "+";
                        break;
                    case '^':
                        reduplikasi += word.substring(1) + "+";
                        break;
                    case '[':
                        prefiks += word.substring(1) + "+";
                        break;
                    case ']':
                        sufiks += word.substring(1) + "+";
                        break;
                    case '#':
                        konfiks += word.substring(1) + "+";
                        break;
                    case '$':
                        proklitika += word.substring(1) + "+";
                        break;
                    case '%':
                        enklitika += word.substring(1) + "+";
                        break;
                    default:
                        break;
                }
            }
            if (!prefiks.equalsIgnoreCase("")) {
                prefiks = prefiks.substring(0, prefiks.length() - 1);
                temp = prefiks.split("\\+");
                for (String word : temp) {
                    result = "Prefiks [" + word + "] + " + result;
                }
            }
            if (rootWord.charAt(0) == '!') {
                result += "Bentuk Asing [" + rootWord.substring(1) + "] + ";
            } else {
                result += "Bentuk Dasar [" + rootWord + "] + ";
            }
            if (!komposisi.equalsIgnoreCase("")) {
                komposisi = komposisi.substring(0, komposisi.length() - 1);
                temp = komposisi.split("\\+");
                for (String word : temp) {
                    result += "Komposisi [" + word + "] + ";
                }
            }
            if (!reduplikasi.equalsIgnoreCase("")) {
                reduplikasi = reduplikasi.substring(0, reduplikasi.length() - 1);
                if (reduplikasi.charAt(0) == '(') {
                    reduplikasi = reduplikasi.substring(1, reduplikasi.length() - 1);
                }
                result += "Reduplikasi [" + reduplikasi + "] + ";
            }
            if (!sufiks.equalsIgnoreCase("")) {
                sufiks = sufiks.substring(0, sufiks.length() - 1);
                temp = sufiks.split("\\+");
                for (String word : temp) {
                    result += "Sufiks [" + word + "] + ";
                }
            }
            if (!konfiks.equalsIgnoreCase("")) {
                konfiks = konfiks.substring(0, konfiks.length() - 1);
                temp = konfiks.split("\\+");
                for (String word : temp) {
                    result += "Konfiks [" + word + "] + ";
                }
            }
            if (!proklitika.equalsIgnoreCase("")) {
                proklitika = proklitika.substring(0, proklitika.length() - 1);
                temp = proklitika.split("\\+");
                for (String word : temp) {
                    result += "Proklitika [" + word + "] + ";
                }
            }
            if (!enklitika.equalsIgnoreCase("")) {
                enklitika = enklitika.substring(0, enklitika.length() - 1);
                temp = enklitika.split("\\+");
                for (String word : temp) {
                    result += "Enklitika [" + word + "] + ";
                }
            }

            result = result.substring(0, result.length() - 3);
            this.parseResult.remove(i);
            this.parseResult.add(i, result);
        }
    }

    private void removeDuplicateResult() {
        String l1, l2;

        //remove same item
        for (int i = 0; i < this.parseResult.size(); i++) {
            l1 = this.parseResult.get(i);
            for (int j = i + 1; j < this.parseResult.size(); j++) {
                l2 = this.parseResult.get(j);
                if (l1.equalsIgnoreCase(l2)) {
                    this.parseResult.remove(j);
                    if (i > 0) {
                        i--;
                    }
                    j--;
                }
            }
        }
    }

    /**
     * Method to do parsing process a line of text by divide them into each word
     * separated by space character, then parse each word
     *
     * @param text line of text to parse
     * @return a list of all the possible parse of each word in text
     *
     * @throws java.io.IOException
     */
    public String process(String text) throws IOException {
        String result = "";
        String word[] = text.split(" ");

        for (String word1 : word) {
            this.parseResult.clear();
            parse(word1.toLowerCase());
            //this.componentValidator();
            //this.convertToWord();
            this.removeDuplicateResult();
            result += word1.toUpperCase() + ":\n";
            for (int i = 0; i < this.parseResult.size(); i++) {
                result += this.parseResult.get(i) + ";\n";
            }
            result += "\n";
        }
        result = result.trim();
        return result;
    }

    /**
     * Method to perform parse operation of a word
     *
     * @param word word to parse
     */
    private ArrayList<String> parse(String word) throws IOException {
        boolean isAWord = true;
        for (int i = 0; i < word.length(); i++) {
            int c = (int) word.charAt(i);
            if (c < 97 || c > 122) {
                isAWord = false;
            }
            if (c == 45) {
                isAWord = true;
            }
        }

        if (isAWord) {
            this.check(word, "", "");
        }

        if (this.parseResult.isEmpty()) {
            this.parseResult.add("!" + word);
        }

        ArrayList<String> result = this.parseResult;
        return result;
    }

    /**
     * Check all the possible prefiks, including klitika, even when combined
     *
     * @param word word to check
     * @param klitika any klitika found
     */
    private void prefiksCheck(String word, String klitika, String prefiks) throws IOException {
        if (word.length() > 2) {
            String c2 = word.substring(0, 2);
            String w2 = word.substring(2);

            if (c2.equalsIgnoreCase("be")) {
                prefiksBer(w2, klitika, prefiks);
            } else if (c2.equalsIgnoreCase("me")) {
                prefiksMe(w2);
            } else if (c2.equalsIgnoreCase("di")) {
                prefiksDi(w2);
            } else if (c2.equalsIgnoreCase("ke")) {
                prefiksKe(w2);
            } else if (c2.equalsIgnoreCase("ku")) {
                prefiksKu(w2);
            } else if (c2.equalsIgnoreCase("se")) {
                prefiksSe(w2);
            } else if (c2.equalsIgnoreCase("pe")) {
                prefiksPe(w2);
            } else if (c2.equalsIgnoreCase("te")) {
                prefiksTer(w2);
            }
        }

        if (word.length() > 3) {
            String c3 = word.substring(0, 3);
            String w3 = word.substring(3);

            if (c3.equalsIgnoreCase("per")) {
                prefiksPer(w3);
            } else if (c3.equalsIgnoreCase("kau")) {
                prefiksKau(w3);
            }
        }
    }

    /**
     * Check all the possible sufiks, including klitika, even when combined ex.
     * makananmu
     *
     * @param word word to check
     * @param klitika any klitika found
     * @param prefiks any prefiks found previously
     */
    private void sufiksCheck(String word, String klitika, String prefiks) throws IOException {
        String temp;

        if (word.length() > 2) {
            String c3 = word.substring(word.length() - 3);
            String w3 = word.substring(0, word.length() - 3);

            if (c3.equalsIgnoreCase("kan")) {
                temp = sufiksKan(w3);
                if (!temp.equalsIgnoreCase("")) {
                    temp = w3 + prefiks + temp + klitika;
                    this.parseResult.add(temp);
                }
                this.check(w3, klitika + "+]kan", prefiks);
            }
            if (c3.equalsIgnoreCase("nya")) {
                temp = sufiksNya(w3);
                if (!temp.equalsIgnoreCase("")) {
                    temp = w3 + temp;
                    this.parseResult.add(temp);
                }
                this.check(w3, klitika + "+%nya", prefiks);
            }
            if (c3.equalsIgnoreCase("lah")) {
                temp = sufiksLah(w3);
                if (!temp.equalsIgnoreCase("")) {
                    temp = w3 + temp;
                    this.parseResult.add(temp);
                }
                this.check(w3, klitika + "+%lah", prefiks);
            }
        }
        if (word.length() > 1) {
            String c2 = word.substring(word.length() - 2);
            String w2 = word.substring(0, word.length() - 2);

            if (c2.equalsIgnoreCase("an")) {
                temp = sufiksAn(w2);
                if (!temp.equalsIgnoreCase("")) {
                    temp = w2 + prefiks + temp + klitika;
                    this.parseResult.add(temp);
                }
                this.check(w2, klitika + "+]an", prefiks);
            }
            if (c2.equalsIgnoreCase("ku")) {
                temp = sufiksKu(w2);
                if (!temp.equalsIgnoreCase("")) {
                    temp = w2 + temp;
                    this.parseResult.add(temp);
                }
                this.check(w2, klitika + "+%ku", prefiks);
            }
            if (c2.equalsIgnoreCase("mu")) {
                temp = sufiksMu(w2);
                if (!temp.equalsIgnoreCase("")) {
                    temp = w2 + temp;
                    this.parseResult.add(temp);
                }
                this.check(w2, klitika + "+%mu", prefiks);
            }
        }
        if (word.length() > 0) {
            String c1 = word.substring(word.length() - 1);
            String w1 = word.substring(0, word.length() - 1);

            if (c1.equalsIgnoreCase("i")) {
                temp = sufiksI(w1);
                if (!temp.equalsIgnoreCase("")) {
                    temp = w1 + prefiks + temp + klitika;
                    this.parseResult.add(temp);
                }
                this.check(w1, klitika + "+]i", prefiks);
            }
        }
    }

    /**
     * Check if word is reduplication or not based on the present of "-"
     *
     * @param word word to check
     * @param klitika any klitika found
     * @param prefiks any prefiks found previously
     * @throws IOException
     */
    private void redupCheck(String word, String klitika, String prefiks) throws IOException {
        String temp;
        if (word.contains("-")) {
            String[] words = word.split("-");

            if (words[0].equalsIgnoreCase(words[1])) {
                if (isRootWord(words[0])) {
                    temp = words[0] + prefiks + klitika + "+^2";
                    this.parseResult.add(temp);
                }
                this.check(words[0], klitika + "+^2", prefiks);
            } else {
                if (isRootWord(words[0])) {
                    temp = words[0] + prefiks + klitika + "+^" + words[1];
                    this.parseResult.add(temp);
                }
                
                //to do parse on the second word and check each word on the result
                ArrayList<String> list = new Parser().parse(words[1]);
                for (String w : list) {
                    if (w.charAt(0) != '!') {
                        this.check(words[0], klitika + "+^(" + w + ")", prefiks);
                    }
                }
            }
        }
    }

    private void check(String word, String klitika, String prefiks) throws IOException {
        //if word is found in lexicon
        if (isRootWord(word)) {
            this.parseResult.add(word + prefiks + klitika);
        }
        //afixed word must be 3 or more letters
        if (word.length() > 3) {
            //prefiks check, including sufiks check
            prefiksCheck(word, klitika, prefiks);

            //only sufiks check
            sufiksCheck(word, klitika, prefiks);

            //reduplication check
            redupCheck(word, klitika, prefiks);
        }
    }

    private void prefiksBer(String word, String klitika, String prefiks) throws IOException {
        //ex. beragam
        this.check(word, klitika, "+[ber" + prefiks);

        //ex. beranak, belajar
        word = word.substring(1);
        this.check(word, klitika, "+[ber" + prefiks);
    }

    private void prefiksMe(String word) {
        String result = "";

        if (isRootWord(word)) {
            result = "+[me";
        }
    }

    private void prefiksDi(String word) {
        String result = "";

        if (isRootWord(word)) {
            result = "+[di";
        }
    }

    private void prefiksKe(String word) {
        String result = "";

        if (isRootWord(word)) {
            result = "+[ke";
        }
    }

    private void prefiksKu(String word) throws IOException {
        String result;

        if (isRootWord(word)) {
            result = "+$ku";
            result = word + result;
            this.parseResult.add(result);
        }

        prefiksCheck(word, "+$ku", "");
        sufiksCheck(word, "+$ku", "");
    }

    private void prefiksSe(String word) {
        String result = "";

        if (isRootWord(word)) {
            result = "+[se";
        }
    }

    private void prefiksPe(String word) {
        String result = "";

        if (isRootWord(word)) {
            result = "+[pe";
        }
    }

    private void prefiksPer(String word) {
        String result = "";

        if (isRootWord(word)) {
            result = "+[per";
        }
    }

    private void prefiksTer(String word) {
        String result = "";

        if (isRootWord(word)) {
            result = "+[ter";
        }
    }

    private void prefiksKau(String word) throws IOException {
        String result;

        if (isRootWord(word)) {
            result = "+$kau";
            result = word + result;
            this.parseResult.add(result);
        }

        prefiksCheck(word, "+$kau", "");
        sufiksCheck(word, "+$kau", "");
    }

    private String sufiksKan(String word) {
        String result = "";

        if (isRootWord(word)) {
            result = "+]kan";
        }

        return result;
    }

    private String sufiksAn(String word) {
        String result = "";

        if (isRootWord(word)) {
            result = "+]an";
        }

        return result;
    }

    private String sufiksI(String word) {
        String result = "";

        if (isRootWord(word)) {
            result = "+]i";
        }

        return result;
    }

    private String sufiksKu(String word) {
        String result = "";

        if (isRootWord(word)) {
            result = "+%ku";
        }

        return result;
    }

    private String sufiksMu(String word) {
        String result = "";

        if (isRootWord(word)) {
            result = "+%mu";
        }

        return result;
    }

    private String sufiksNya(String word) {
        String result = "";

        if (isRootWord(word)) {
            result = "+%nya";
        }

        return result;
    }

    private String sufiksLah(String word) {
        String result = "";

        if (isRootWord(word)) {
            result = "+%lah";
        }

        return result;
    }
}
