package Model;

import Lexicon.Model.Lexicon;
import java.io.BufferedReader;
import java.io.FileReader;
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

    public Parser(Lexicon lexicon) throws IOException {
        this.lexicon = lexicon;
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
        if (!word.equalsIgnoreCase("")) {
            return lexicon.searchInTree(word.toLowerCase());
        } else {
            return false;
        }
    }

    /**
     * Check each component in tempResult, cross check with lexicon, delete from
     * tempResult if not valid based on lexicon
     *
     * @throws IOException
     */
    private void validateComponent() throws IOException {
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
                if (words[j].charAt(0) != '$' && words[j].charAt(0) != '%') {
                    component += words[j] + "+";
                }
            }

            if (!component.equalsIgnoreCase("")) {
                component = component.substring(0, component.length() - 1);
                if (isRootWord(rootWord)) {
                    valid = this.lexicon.searchInFile(rootWord, component);
                    if (!valid) {
                        this.parseResult.remove(line);
                        i--;
                    }
                } else {
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
        String line, result;
        String[] words;

        for (int i = 0; i < this.parseResult.size(); i++) {
            result = "";
            line = this.parseResult.get(i);
            words = line.split("\\+");

            for (String word : words) {
                switch (word.charAt(0)) {
                    case '@':
                        result += "Komposisi {" + word.substring(1) + "} + ";
                        break;
                    case '^':
                        result += "Reduplikasi {" + word.substring(1) + "} + ";
                        break;
                    case '[':
                        result += "Prefiks {" + word.substring(1) + "} + ";
                        break;
                    case ']':
                        result += "Sufiks {" + word.substring(1) + "} + ";
                        break;
                    case '#':
                        result += "Konfiks {" + word.substring(1) + "} + ";
                        break;
                    case '$':
                        result += "Proklitika {" + word.substring(1) + "} + ";
                        break;
                    case '%':
                        result += "Enklitika {" + word.substring(1) + "} + ";
                        break;
                    case '!':
                        result += "Bentuk Asing {" + word.substring(1) + "} + ";
                        break;
                    default:
                        result += "Bentuk Dasar {" + word + "} + ";
                        break;
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
     * Normalize text before entering parse process
     *
     * @param text
     * @return String[] containing each word in text but in lowercase, and only
     * character a..z, 0..9, and - allowed
     */
    private String[] normalizeInput(String text) {
        String[] tempArray;
        String tempWord;
        String input = "";
        String[] words;
        char c;

        text = text.toLowerCase();
        tempArray = text.split("\\s");
        for (String word : tempArray) {
            if (!word.equalsIgnoreCase("")) {
                tempWord = "";
                for (int i = 0; i < word.length(); i++) {
                    c = word.charAt(i);
                    //a..z
                    if (c >= 97 && c <= 122) {
                        tempWord += (char) c;
                    }
                    //0..9
                    if (c >= 48 && c <= 57) {
                        tempWord += (char) c;
                    }
                    //symbol - 
                    if (c == 45) {
                        tempWord += (char) c;
                    }
                }
                if (!tempWord.equalsIgnoreCase("")) {
                    input += tempWord + " ";
                }
            }
        }
        input = input.trim();
        //System.out.println(input);
        words = input.split("\\s");
        return words;
    }

    /**
     * Method to do parsing process a line of text by divide them into each word
     * separated by space character, then parse each word
     *
     * @param text line of text to parse
     * @param validator
     * @param converter
     * @return a list of all the possible parse of each word in text
     *
     * @throws java.io.IOException
     */
    public String process(String text, boolean validator, boolean converter) throws IOException {
        String result = "";
        String words[] = normalizeInput(text);
        String word;
        ArrayList<String> oneWord = new ArrayList<>(), twoWord = new ArrayList<>();

        for (int i = 0; i < words.length; i++) {
            oneWord.clear();
            twoWord.clear();
            this.parseResult.clear();

            word = words[i];
            if (!word.equalsIgnoreCase("")) {
                parse(word.toLowerCase());
                if (i < words.length - 1) {
                    this.checkKomposisi(words[i + 1]);
                }

                this.removeDuplicateResult();

                if (validator) {
                    this.validateComponent();
                }

                if (this.parseResult.isEmpty()) {
                    this.parseResult.add("!" + word);
                }

                for (int j = 0; j < this.parseResult.size(); j++) {
                    if (this.parseResult.get(j).contains("&")) {
                        twoWord.add(this.parseResult.get(j));
                    } else {
                        oneWord.add(this.parseResult.get(j));
                    }
                }

                this.parseResult.clear();
                this.parseResult.addAll(oneWord);
                this.parseResult.addAll(twoWord);

                if (converter) {
                    this.convertToWord();
                }

                if (!oneWord.isEmpty()) {
                    result += word.toUpperCase() + ":\n";
                    for (int j = 0; j < oneWord.size(); j++) {
                        result += this.parseResult.get(j) + ";\n";
                    }
                    result += "\n";
                }
                if (!twoWord.isEmpty()) {
                    result += word.toUpperCase() + " " + words[i + 1].toUpperCase() + ":\n";
                    for (int j = oneWord.size(); j < this.parseResult.size(); j++) {
                        result += this.parseResult.get(j) + ";\n";
                    }
                    result += "\n";
                }
            }
        }
        result = result.trim();
        return result;
    }

    /**
     * Method to do parsing process a line of text by divide them into each word
     * separated by space character, then parse each word
     *
     * @param filePath path to the input file
     * @return a list of all the possible parse of each word in text
     *
     * @throws java.io.IOException
     */
    public String readFile(String filePath) throws IOException {
        String text = "";

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String currentLine;
            while ((currentLine = br.readLine()) != null) {
                if (!currentLine.equalsIgnoreCase("")) {
                    text += currentLine;
                    text = text.trim();
                    text += " ";
                }
            }
        }

        return text.trim();

    }

    /**
     * Method to perform parse operation of a word
     *
     * @param word word to parse
     */
    private void parse(String word) throws IOException {
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
            this.checkAll(word, "", "");
        }

        if (this.parseResult.isEmpty()) {
            this.parseResult.add("!" + word);
        }
    }

    /**
     * Check all the possible prefiks, including klitika, even when combined
     *
     * @param word word to check
     * @param klitika any klitika found
     * @param prefiks any prefiks found previously
     */
    private void checkPrefiks(String word, String component, String klitika) throws IOException {
        //two letters prefiks
        if (word.length() > 2) {
            String c2 = word.substring(0, 2);
            String w2 = word.substring(2);

            if (c2.equalsIgnoreCase("be")) {
                checkPrefiksBer(w2, component, klitika);
            } else if (c2.equalsIgnoreCase("me")) {
                checkPrefiksMe(w2, component, klitika);
            } else if (c2.equalsIgnoreCase("di")) {
                checkPrefiksDi(w2, component, klitika);
            } else if (c2.equalsIgnoreCase("ke")) {
                checkPrefiksKe(w2, component, klitika);
            } else if (c2.equalsIgnoreCase("ku")) {
                checkProklitikaKu(w2, component, klitika);
            } else if (c2.equalsIgnoreCase("se")) {
                checkPrefiksSe(w2, component, klitika);
            } else if (c2.equalsIgnoreCase("pe")) {
                checkPrefiksPe(w2, component, klitika);
            } else if (c2.equalsIgnoreCase("te")) {
                checkPrefiksTer(w2, component, klitika);
            }
        }

        //three letters prefiks
        if (word.length() > 3) {
            String c3 = word.substring(0, 3);
            String w3 = word.substring(3);

            if (c3.equalsIgnoreCase("per") || c3.equalsIgnoreCase("pel")) {
                checkPrefiksPer(w3, component, klitika);
            } else if (c3.equalsIgnoreCase("kau")) {
                checkProklitikaKau(w3, component, klitika);
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
    private void checkSufiks(String word, String component, String klitika) throws IOException {
        if (word.length() > 2) {
            String c3 = word.substring(word.length() - 3);
            String w3 = word.substring(0, word.length() - 3);

            if (c3.equalsIgnoreCase("kan")) {
                checkSufiksKan(w3, component, klitika);
            } else if (c3.equalsIgnoreCase("nya")) {
                checkEnklitikaNya(w3, component, klitika);
            } else if (c3.equalsIgnoreCase("lah")) {
                checkEnklitikaLah(w3, component, klitika);
            } else if (c3.equalsIgnoreCase("pun")) {
                checkEnklitikaPun(w3, component, klitika);
            } else if (c3.equalsIgnoreCase("kah")) {
                checkEnklitikaKah(w3, component, klitika);
            }
        }
        if (word.length() > 1) {
            String c2 = word.substring(word.length() - 2);
            String w2 = word.substring(0, word.length() - 2);

            if (c2.equalsIgnoreCase("an")) {
                checkSufiksAn(w2, component, klitika);
            } else if (c2.equalsIgnoreCase("ku")) {
                checkEnklitikaKu(w2, component, klitika);
            } else if (c2.equalsIgnoreCase("mu")) {
                checkEnklitikaMu(w2, component, klitika);
            }
        }
        if (word.length() > 0) {
            String c1 = word.substring(word.length() - 1);
            String w1 = word.substring(0, word.length() - 1);

            if (c1.equalsIgnoreCase("i")) {
                checkSufiksI(w1, component, klitika);
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
    private void checkRedup(String word, String component, String klitika) throws IOException {
        String temp;
        boolean haveResult = false;
        if (word.contains("-")) {
            String[] words = word.split("-");

            if (words[0].equalsIgnoreCase(words[1])) {
                if (isRootWord(words[0])) {
                    temp = words[0] + "+^2" + component + klitika;
                    this.parseResult.add(temp);
                    haveResult = true;
                }
                //this.check(words[0], component + "+^2", klitika);
            } else {
                if (isRootWord(words[0])) {
                    String c1 = words[0].substring(0, 1); //first word first letter
                    String c2 = words[1].substring(0, 1); //second word first letter
                    String c3 = words[1].substring(0, 2); //second word first two letters
                    String w1 = words[1].substring(1); //second word minus c2
                    String w2 = words[1].substring(2); //second word minus c3

                    if (isRootWord(words[1])) {
                        temp = words[0] + "+^" + words[1] + component + klitika;
                        this.parseResult.add(temp);
                    } else if (c1.equalsIgnoreCase("k")) {
                        if (c3.equalsIgnoreCase("ng")) {
                            if (words[0].equalsIgnoreCase("k" + w2)) {
                                temp = words[0] + "+^2" + component + klitika;
                                this.parseResult.add(temp);
                                haveResult = true;
                            }
                        }
                    } else if (c1.equalsIgnoreCase("t")) {
                        if (c2.equalsIgnoreCase("n")) {
                            if (words[0].equalsIgnoreCase("t" + w1)) {
                                temp = words[0] + "+^2" + component + klitika;
                                this.parseResult.add(temp);
                                haveResult = true;
                            }
                        }
                    } else if (c1.equalsIgnoreCase("s")) {
                        if (c3.equalsIgnoreCase("ny")) {
                            if (words[0].equalsIgnoreCase("s" + w2)) {
                                temp = words[0] + "+^2" + component + klitika;
                                this.parseResult.add(temp);
                                haveResult = true;
                            }
                        }
                    } else if (c1.equalsIgnoreCase("p")) {
                        if (c2.equalsIgnoreCase("m")) {
                            if (words[0].equalsIgnoreCase("p" + w1)) {
                                temp = words[0] + "+^2" + component + klitika;
                                this.parseResult.add(temp);
                                haveResult = true;
                            }
                        }
                    }
                    
                    if (!haveResult) {
                        temp = words[0] + "+^" + words[1] + component + klitika;
                        this.parseResult.add(temp);
                    }
                }
            }
        }
    }

    private void checkAll(String word, String component, String klitika) throws IOException {
        //if word is found in lexicon
        if (isRootWord(word)) {
            this.parseResult.add(word + component + klitika);
        }
        //afixed word must be 3 or more letters
        if (word.length() > 3) {
            //prefiks check, including sufiks check
            checkPrefiks(word, component, klitika);

            //only sufiks check
            checkSufiks(word, component, klitika);

            //reduplication check
            checkRedup(word, component, klitika);
        }
    }

    private void checkPrefiksBer(String word, String component, String klitika) throws IOException {
        //ex. beragam
        this.checkAll(word, "+[ber" + component, klitika);

        if (word.length() > 3) {
            if (word.charAt(0) == 'r' || word.charAt(0) == 'l') {
                //ex. beranak, belajar
                word = word.substring(1);
                this.checkAll(word, "+[ber" + component, klitika);
            }
        }
    }

    private void checkPrefiksMe(String word, String component, String klitika) throws IOException {
        //me..
        this.checkAll(word, "+[me" + component, klitika);

        if (word.length() > 3) {
            //mem..;
            if (word.charAt(0) == 'm') {
                word = word.substring(1);
                this.checkAll(word, "+[me" + component, klitika);
                this.checkAll("p" + word, "+[me" + component, klitika);
            }

            //men..;
            if (word.charAt(0) == 'n') {
                word = word.substring(1);
                this.checkAll(word, "+[me" + component, klitika);
                this.checkAll("t" + word, "+[me" + component, klitika);

                if (word.length() > 3) {
                    //meng..;
                    if (word.charAt(0) == 'g') {
                        word = word.substring(1);
                        this.checkAll(word, "+[me" + component, klitika);
                        this.checkAll("k" + word, "+[me" + component, klitika);

                        if (word.length() > 3) {
                            //menge..
                            if (word.charAt(0) == 'e') {
                                word = word.substring(1);
                                this.checkAll(word, "+[me" + component, klitika);
                            }
                        }
                    }
                    //meny..;
                    if (word.charAt(0) == 'y') {
                        word = word.substring(1);
                        //this.check(word, "+[me" + component, klitika);
                        this.checkAll("s" + word, "+[me" + component, klitika);
                    }
                }
            }
        }
    }

    private void checkPrefiksDi(String word, String component, String klitika) throws IOException {
        this.checkAll(word, "+[di" + component, klitika);
    }

    private void checkPrefiksKe(String word, String component, String klitika) throws IOException {
        this.checkAll(word, "+[ke" + component, klitika);
    }

    private void checkProklitikaKu(String word, String component, String klitika) throws IOException {
        this.checkAll(word, component, klitika + "+$ku");
    }

    private void checkPrefiksSe(String word, String component, String klitika) throws IOException {
        this.checkAll(word, "+[se" + component, klitika);
    }

    private void checkPrefiksPe(String word, String component, String klitika) throws IOException {
        this.checkAll(word, "+[pe" + component, klitika);
        checkPrefiksPer(word, component, klitika);

        if (word.length() > 3) {
            //pem..;
            if (word.charAt(0) == 'm') {
                word = word.substring(1);
                this.checkAll(word, "+[pe" + component, klitika);
                this.checkAll("p" + word, "+[pe" + component, klitika);
            }

            //pen..;
            if (word.charAt(0) == 'n') {
                word = word.substring(1);
                this.checkAll(word, "+[pe" + component, klitika);
                this.checkAll("t" + word, "+[pe" + component, klitika);

                if (word.length() > 3) {
                    //peng..;
                    if (word.charAt(0) == 'g') {
                        word = word.substring(1);
                        this.checkAll(word, "+[pe" + component, klitika);
                        this.checkAll("k" + word, "+[pe" + component, klitika);

                        if (word.length() > 3) {
                            //penge..
                            if (word.charAt(0) == 'e') {
                                word = word.substring(1);
                                this.checkAll(word, "+[pe" + component, klitika);
                            }
                        }
                    }
                    //peny..;
                    if (word.charAt(0) == 'y') {
                        word = word.substring(1);
                        //this.check(word, "+[pe" + component, klitika);
                        this.checkAll("s" + word, "+[pe" + component, klitika);
                    }
                }
            }
        }
    }

    private void checkPrefiksPer(String word, String component, String klitika) throws IOException {
        this.checkAll(word, "+[per" + component, klitika);
    }

    private void checkPrefiksTer(String word, String component, String klitika) throws IOException {
        this.checkAll(word, "+[ter" + component, klitika);

        if (word.length() > 3) {
            if (word.charAt(0) == 'r') {
                word = word.substring(1);
                this.checkAll(word, "+[ter" + component, klitika);
            }
        }
    }

    private void checkProklitikaKau(String word, String component, String klitika) throws IOException {
        this.checkAll(word, component, klitika + "+$kau");
    }

    private void checkSufiksKan(String word, String component, String klitika) throws IOException {
        String temp;
        if (isRootWord(word)) {
            temp = word + "+]kan" + component + klitika;
            this.parseResult.add(temp);
            this.checkKonfiks();
        }
        this.checkKomposisi(word, "+]kan" + component, klitika);
        this.checkKonfiks();
        this.checkAll(word, "+]kan" + component, klitika);
    }

    private void checkSufiksAn(String word, String component, String klitika) throws IOException {
        String temp;
        if (isRootWord(word)) {
            temp = word + "+]an" + component + klitika;
            this.parseResult.add(temp);
            this.checkKonfiks();
        }
        this.checkKomposisi(word, "+]an" + component, klitika);
        this.checkKonfiks();
        this.checkAll(word, "+]an" + component, klitika);
    }

    private void checkSufiksI(String word, String component, String klitika) throws IOException {
        String temp;
        if (isRootWord(word)) {
            temp = word + "+]i" + component + klitika;
            this.parseResult.add(temp);
            this.checkKonfiks();
        }
        this.checkKomposisi(word, "+]i" + component, klitika);
        this.checkKonfiks();
        this.checkAll(word, "+]i" + component, klitika);
    }

    private void checkEnklitikaKu(String word, String component, String klitika) throws IOException {
        String temp;
        if (isRootWord(word)) {
            temp = word + component + "+%ku" + klitika;
            this.parseResult.add(temp);
        }
        this.checkAll(word, component, "+%ku" + klitika);
    }

    private void checkEnklitikaMu(String word, String component, String klitika) throws IOException {
        String temp;
        if (isRootWord(word)) {
            temp = word + component + "+%mu" + klitika;
            this.parseResult.add(temp);
        }
        this.checkAll(word, component, "+%mu" + klitika);
    }

    private void checkEnklitikaNya(String word, String component, String klitika) throws IOException {
        String temp;
        if (isRootWord(word)) {
            temp = word + component + "+%nya" + klitika;
            this.parseResult.add(temp);
        }
        this.checkAll(word, component, "+%nya" + klitika);
    }

    private void checkEnklitikaLah(String word, String component, String klitika) throws IOException {
        String temp;
        if (isRootWord(word)) {
            temp = word + component + "+%lah" + klitika;
            this.parseResult.add(temp);
        }
        this.checkAll(word, component, "+%lah" + klitika);
    }

    private void checkEnklitikaPun(String word, String component, String klitika) throws IOException {
        String temp;
        if (isRootWord(word)) {
            temp = word + component + "+%pun" + klitika;
            this.parseResult.add(temp);
        }
        this.checkAll(word, component, "+%pun" + klitika);
    }

    private void checkEnklitikaKah(String word, String component, String klitika) throws IOException {
        String temp;
        if (isRootWord(word)) {
            temp = word + component + "+%kah" + klitika;
            this.parseResult.add(temp);
        }
        this.checkAll(word, component, "+%kah" + klitika);
    }

    private void checkKonfiks() {
        String rootWord, component, line;
        for (int i = 0; i < this.parseResult.size(); i++) {
            rootWord = "";
            line = this.parseResult.get(i);
            if (line.contains("+")) {
                for (int j = 0; j < line.indexOf("+"); j++) {
                    rootWord += line.charAt(j);
                }
                component = line.substring(line.indexOf("+") + 1);

                String temp;
                if (component.contains("]an+[ber")) {
                    temp = component.replace("]an+[ber", "#ber-an");
                    this.parseResult.add(rootWord + "+" + temp);
                } else if (component.contains("]an+[ke")) {
                    temp = component.replace("]an+[ke", "#ke-an");
                    this.parseResult.add(rootWord + "+" + temp);
                } else if (component.contains("]an+[per")) {
                    temp = component.replace("]an+[per", "#per-an");
                    this.parseResult.add(rootWord + "+" + temp);
                } else if (component.contains("]an+[pe")) {
                    temp = component.replace("]an+[pe", "#pe-an");
                    this.parseResult.add(rootWord + "+" + temp);
                } else if (component.contains("]nya+[se")) {
                    temp = component.replace("]nya+[se", "#se-nya");
                    this.parseResult.add(rootWord + "+" + temp);
                }
            }
        }
    }

    /**
     * To check if a word is first komposisi then afixed ex. pertanggungjawaban
     *
     * @param word word to check
     * @param klitika any klitika found
     * @param prefiks any prefiks found previously
     */
    private void checkKomposisi(String word, String component, String klitika) {
        String rootWord = "";
        String temp;
        for (int i = 0; i < word.length() - 1; i++) {
            rootWord += word.charAt(i);
            temp = word.substring(i + 1);
            if (isRootWord(rootWord)) {
                if (isRootWord(temp)) {
                    temp = rootWord + "+@" + temp + component + klitika;
                    if (temp.contains("[") && temp.contains("]") && !temp.contains("-")) {
                        this.parseResult.add(temp);
                    }
                }
            }
        }
    }

    /**
     * To check if a word is first afixed then komposisi
     */
    private void checkKomposisi(String nextWord) {
        if (isRootWord(nextWord)) {
            String line, newLine;
            int size = this.parseResult.size();
            for (int i = 0; i < size; i++) {
                line = this.parseResult.get(i);
                newLine = line + "+@" + nextWord + "";
                this.parseResult.add(newLine);
            }
        }
    }
}
