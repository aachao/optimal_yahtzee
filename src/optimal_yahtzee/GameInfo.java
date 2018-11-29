package optimal_yahtzee;

import java.util.ArrayList;
import java.util.Arrays;

public class GameInfo {
    public static final int num_dice = 5;

    public static final String categories = "abcdefghijklm";

    /* Categories (5 dice)
     * 		a - Ones 		f - Sixes				k - Large Straight
     * 		b - Twos		g - Three of a Kind		l - Yahtzee
     * 		c - Threes		h - Four of a Kind		m - Chance
     * 		d - Fours		i - Full House
     * 		e - Fives		j - Small Straight
     */

    /*
     *		s = 0: 0 rerolls remaining (choosing category to score in)
     *		s = 1: 1 reroll remaining (choose holding combination)
     *		s = 2: 2 rerolls remaining (choose holding combination)
     *		s = 3: 3 rolls remaining (expected value of having set of categories open)
     */

    public static final int FULLHOUSE = 25;
    public static final int SLSTRAIGHT = 50;
    public static final int LGSTRAIGHT = 30;
    public static final int YAHTZEE = 50;

    // Scoring function
    public static int scoreDice(String dice, char category) throws CustomException {
        int category_num = category - 'a' + 1;
        dice = orderString(dice);

        // Check number of dice
        if (dice.length() != num_dice) throw new CustomException("Invalid number of dice");
        else {
            // Score upper category (1 - 6)
            if (category_num > 0 && category_num < 7) {
                return upper(dice,Character.forDigit(category_num, 10));
            }
            // Score three of a kind category (7)
            else if (category_num == 7) return threekind(dice);
                // Score four of a kind category (8)
            else if (category_num == 8) return fourkind(dice);
                // Score full house category (9)
            else if (category_num == 9 && fullhouse(dice)) return FULLHOUSE;
                // Score small straight category (10)
            else if (category_num == 10 && slstraight(dice)) return SLSTRAIGHT;
                // Score large straight category (11)
            else if (category_num == 11 && lgstraight(dice)) return LGSTRAIGHT;
                // Score yahtzee category (12)
            else if (category_num == 12 && yahtzee(dice)) return YAHTZEE;
                // Score chance category (13)
            else if (category_num == 13) return chance(dice);
        }
        return 0;
    }

    // Upper category scoring function (adds dice matching number)
    private static int upper(String dice, char num) throws CustomException {
        int i = Character.getNumericValue(num);
        if (i > 6 || i < 1) throw new CustomException("Invalid number for upper scoring");
        String s = Character.toString(num);
        int count = dice.length() - dice.replace(s, "").length();
        return count * i;
    }

    // Chance scoring function (sums dice)
    private static int chance(String dice) {
        int sum = 0;
        for(char c: dice.toCharArray()) {
            int i = (int) c - 48;
            sum += i;
        }
        return sum;
    }

    // Three of a kind scoring function
    private static int threekind(String dice) {
        int[] diceCount = new int[6];
        for (char c: dice.toCharArray()) {
            diceCount[Character.getNumericValue(c) - 1] += 1;
        }
        for(int i = 0; i < 6; i++) {
            if(diceCount[i] == 3) {
                return chance(dice);
            }
        }
        return 0;
    }

    // Full House scoring function
    private static boolean fullhouse(String dice) {
        int[] diceCount = new int[6];
        for (char c: dice.toCharArray()) {
            diceCount[Character.getNumericValue(c) - 1] += 1;
        }
        boolean hastwo = false;
        boolean hasthree = false;
        for(int i = 0; i < 6; i++) {
            if(diceCount[i] == 3) {
                hasthree = true;
            }
            if(diceCount[i] == 2) {
                hastwo = true;
            }
        }
        return hastwo && hasthree;
    }

    // Four of a kind scoring function
    private static int fourkind(String dice) {
        int[] diceCount = new int[6];
        for (char c: dice.toCharArray()) {
            diceCount[Character.getNumericValue(c) - 1] += 1;
        }
        for(int i = 0; i < 6; i++) {
            if(diceCount[i] == 4) {
                return chance(dice);
            }
        }
        return 0;
    }

    // Small straight scoring function
    private static boolean slstraight(String dice) {
        for(int i = 0; i < dice.length(); i++) {
            StringBuilder sb = new StringBuilder(dice);
            sb.deleteCharAt(i);
            if(sb.toString().equals("1234") || sb.toString().equals("2345") || sb.toString().equals("3456")) {
                return true;
            }
        }
        return false;
    }

    // Large straight scoring function
    private static boolean lgstraight(String dice) {
        return dice.equals("12345");
    }

    // Yahtzee scoring function (checks all dice are the same)
    private static boolean yahtzee(String dice) {
        String first = Character.toString(dice.charAt(0));
        int count = dice.replace(first, "").length();
        return count == 0;
    }

    // Calculates probability of rolling dice given held dice
    public static double probability(String dice, String hold) {
        dice = orderString(dice);
        hold = orderString(hold);
        int[] diceCount = new int[6];
        for (char c: dice.toCharArray()) {
            diceCount[Character.getNumericValue(c) - 1] += 1;
        }
        for (char c: hold.toCharArray()) {
            diceCount[Character.getNumericValue(c) - 1] -= 1;
        }
        int total = 0;
        for(int i = 0; i < 6; i++) {
            if(diceCount[i] < 0) {
                return 0;
            }
            total += diceCount[i];
        }
        double probability = factorial(total) / Math.pow(6, (double) total);
        for (int i: diceCount) {
            probability /= factorial(i);
        }
        return probability;
    }

    // return all possible holds for a dice roll
    public static ArrayList<String> getHolds(String dice) {
        char[] d = dice.toCharArray();
        int n = dice.length();
        ArrayList<String> output = new ArrayList<String>();
        StringBuilder dummy = new StringBuilder();

        // Run a loop for printing all 2^n
        // subsets one by one
        for (int i = 1; i < (1<<n); i++) {

            // Print current subset
            for (int j = 0; j < n; j++) {
                // (1<<j) is a number with jth bit 1
                // so when we 'and' them with the
                // subset number we get which numbers
                // are present in the subset and which
                // are not
                if ((i & (1 << j)) > 0) {
                    dummy.append(d[j]);
                }
            }
            output.add(dummy.toString());
            dummy = new StringBuilder();
        }
        return output;
    }

    // Calculates a number's factorial
    public static double factorial(int i) {
        double factorial = 1;
        for (double j = 1; j <= i; j++) factorial *= j;
        return factorial;
    }

    // Order characters in a string
    public static String orderString(String s) {
        char[] charArray = s.toCharArray();
        Arrays.sort(charArray);
        return new String(charArray);
    }

    public static void main(String[] Args) throws CustomException {
    }
}
