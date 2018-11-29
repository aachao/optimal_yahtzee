package optimal_yahtzee;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.*;

public class Strategy {
    String BEST_CATEGORY_FILE_PATH = "/Users/AChao/Desktop/optimal_yahtzee/bestCategory.ser";
    String BEST_HOLD_FILE_PATH = "/Users/AChao/Desktop/optimal_yahtzee/bestHold.ser";

    public static class Key implements Serializable {
        String open_categories;
        String dice;
        int stage;

        public Key(String oc, String d, int s) {
            this.open_categories = oc;
            this.dice = d;
            this.stage = s;
        }

        @Override
        public int hashCode() {
            return open_categories.hashCode() + dice.hashCode() + stage;
        }

        @Override
        public boolean equals(Object obj) {
            Key k = (Key) obj;
            return k.open_categories.equals(open_categories) && k.dice.equals(dice) && (k.stage == stage);
        }

        @Override
        public String toString() {
            String s = "Open categories: " + this.open_categories + ", Dice: " + this.dice + ", Rolls left: " + this.stage;
            return s;
        }
    }

    // Expected value for every possible game state
    static Hashtable<Key,Double> evTable = new Hashtable<Key,Double>();

    // Best category to score a dice combination in a game state
    static Hashtable<Key,Character> bestCategory = new Hashtable<Key,Character>();

    // Best dice combination to hold before a reroll in a game state
    static Hashtable<Key,String> bestHold = new Hashtable<Key,String>();

    // Expected value of having a combination of categories open
    static Hashtable<String,Double> evOpenCategory = new Hashtable<String,Double>();

    // Contains ArrayList of all combinations of num_dice dice
    static ArrayList<String> diceCombos = new ArrayList<String>();

    // Contains ArrayList of all combinations of index + 1 categories
    static ArrayList<String>[] categoryCombos = new ArrayList[GameInfo.categories.length()];

    // load base case event tables from local
    @SuppressWarnings("unchecked")
    public static void loadTables() {
        try {
            FileInputStream fileIn = new FileInputStream(BEST_CATEGORY_FILE_PATH);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            bestCategory = (Hashtable<Key,Character>) in.readObject();
            in.close();
            fileIn.close();
            System.out.println("bestCategory loaded");
        }catch(IOException i) {
            i.printStackTrace();
            return;
        }catch(ClassNotFoundException c) {
            System.out.println("Not found");
            c.printStackTrace();
            return;
        }

        try {
            FileInputStream fileIn = new FileInputStream(BEST_HOLD_FILE_PATH);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            bestHold = (Hashtable<Key, String>) in.readObject();
            in.close();
            fileIn.close();
            System.out.println("bestHold loaded");
        }catch(IOException i) {
            i.printStackTrace();
            return;
        }catch(ClassNotFoundException c) {
            System.out.println("Not found");
            c.printStackTrace();
            return;
        }
    }

    // save base case event tables to local
    public static void saveTables() {
        System.out.println("hello");
        try {
            FileOutputStream fileOut = new FileOutputStream(BEST_CATEGORY_FILE_PATH);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(bestCategory);
            out.close();
            fileOut.close();
            System.out.printf("Serialized data is saved in " + BEST_CATEGORY_FILE_PATH);
        } catch(IOException i) {
            i.printStackTrace();
        }

        try {
            FileOutputStream fileOut = new FileOutputStream(BEST_HOLD_FILE_PATH);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(bestHold);
            out.close();
            fileOut.close();
            System.out.printf("Serialized data is saved in " + BEST_HOLD_FILE_PATH);
        } catch(IOException i) {
            i.printStackTrace();
        }
    }

    // Receive user input
    static void input() {
        @SuppressWarnings("resource")
        Scanner reader = new Scanner(System.in);
        while(true) {
            System.out.println("Enter open categories (e.g. abcdef): ");
            while(!reader.hasNext()) {}
            String oc = reader.nextLine();
            System.out.println("Enter a dice combination (e.g. 123): ");
            while(!reader.hasNext()) {}
            String d = reader.nextLine();
            d = GameInfo.orderString(d);
            System.out.println("Enter number of rerolls left (0 - 2): ");
            while(!reader.hasNext()) {}
            String s1 = reader.nextLine();
            int s = Integer.parseInt(s1);
            Key k = new Key(oc, d, s);
            if (s == 0) {
                System.out.print(bestCategory.get(k));
            } else if(s == 1 || s == 2) {
                System.out.println(bestHold.get(k));
            }
        }
    }

    // Generates base case Event objects
    public static void genBase() throws CustomException {
        genDiceCombos("", GameInfo.num_dice);
        genCategoryCombos("", GameInfo.categories.length());
        // iterates through category sets of length 1
        for(String c: categoryCombos[0]) {
            double total = 0;
            // iterates through all combinations of dice
            for(String d: diceCombos) {
                // create a new key for each category and dice combo at stage 0
                Key k = new Key(c, d, 0);
                // score the dice combo in the category
                double s = GameInfo.scoreDice(d, c.charAt(0));
                // put the key and score into evTable
                evTable.put(k, s);
                // update total
                total += s;
            }
            // put average ev for category sets of length 1 into evOpenCategory
            evOpenCategory.put(c, total / diceCombos.size());
        }
    }

    // Generates table of values for dynamic programming
    public static void genTables() throws CustomException {
        genBase();
        // iterate through the set of all subsets of categories
        for(int i = 0; i < GameInfo.categories.length(); i++) {
            System.out.println(i);
            for(String c: categoryCombos[i]) {
                // iterate through stages
                for(int j = 0; j < 4; j++) {
                    if (i == 0 && j == 0) continue;
                    if (j == 0) calcBestCategory(c);
                    else if (j == 1 || j == 2) {
                        for(String d: diceCombos) {
                            Key k = new Key(c, d, j);
                            calcHold(k);
                        }
                    }
                    else if (j == 3) calcOpenCategory(c);
                }
            }
        }
        saveTables();
    }

    // Calculate best category to score every possible roll given a set of open categories
    public static void calcBestCategory(String c) throws CustomException {
        for(String d: diceCombos){
            double max = 0;
            char best_c = '\n';
            for(char a: c.toCharArray()) {
                String new_c = c.substring(0, c.indexOf(a)) + c.substring(c.indexOf(a) + 1, c.length());
                double ev = GameInfo.scoreDice(d, a) + evOpenCategory.get(new_c);
                if (ev > max) {
                    max = ev;
                    best_c = a;
                }
            }
            Key k = new Key(c, d, 0);
            bestCategory.put(k, best_c);
            Key ks = new Key(c, d, 0);
            evTable.put(ks,max);

        }
    }

    /* Calculate best combination of dice to hold
     * Open categories, dice, number reroll -> dice to hold
     *
     * for each holding combination:
     *   - iterate through all possible dice_combos:
     *       - 1 reroll left: P(dice_combo | holding combo) * evTable.get(open_categories, dice_combo, 1)
     *       - 2 rerolls left: P(dice_combo | holding combo) * evTable.get(open_categories, dice_combo, 2)
     */
    public static void calcHold(Key k) throws CustomException {
        ArrayList<String> holds = GameInfo.getHolds(k.dice);
        double max = 0;
        String maxHold = "";
        for (String hold: holds) {
            double total = 0;
            for (String dc: diceCombos) {
                double prob = GameInfo.probability(dc, hold);
                Key new_k = new Key(k.open_categories, dc, k.stage - 1);
                double score = evTable.get(new_k);
                total += prob*score;
            }
            if (total > max) {
                max = total;
                maxHold = hold;
            }
        }
        evTable.put(k, max);
        bestHold.put(k, maxHold);
    }

    /* Calculate expected value of an open category combination
     *
     * Summation of all possible dice combos:
     *   - P(dice_combo) * evTable.get(open_categories, dice_combo, 2)
     */
    public static void calcOpenCategory(String c) throws CustomException {
        double total = 0;
        for(String d: diceCombos) {
            double prob = GameInfo.probability(d, "");
            Key k = new Key(c, d, 2);
            double score = evTable.get(k);
            total += prob * score;
        }
        evOpenCategory.put(c, total);
    }

    // Generates and saves all combinations of dice (start with n = 6)
    public static void genDiceCombos(String last, int n) {
        if (n == 0) {
            diceCombos.add(last);
            return;
        }
        int lastDie = 1;
        if (last.length() != 0) {
            lastDie = Character.getNumericValue(last.charAt(last.length() - 1));
        }
        for(int i = lastDie; i <= 6; i++) {
            String s = Integer.toString(i);
            genDiceCombos(last + s, n - 1);
        }
    }

    // Generates and saves all combinations of categories (start with n = number of categories)
    public static void genCategoryCombos(String last, int n) {
        if (n == 0) return;
        int lastDie = 0;
        if (last.length() != 0) {
            lastDie = GameInfo.categories.indexOf(last.charAt(last.length() - 1)) + 1;
        }
        int num = GameInfo.categories.length() - n;
        if (categoryCombos[num] == null) {
            categoryCombos[num] = new ArrayList<String>();
        }
        for(int i = lastDie; i <= GameInfo.categories.length() - 1; i++) {
            String s = String.valueOf((char)(i + 97));
            categoryCombos[num].add(last + s);
            genCategoryCombos(last + s, n - 1);
        }
    }

    public static void main(String[] Args) throws CustomException {
        loadTables();
        input();
    }
}
