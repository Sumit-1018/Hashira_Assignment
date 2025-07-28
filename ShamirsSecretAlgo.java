import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * I have implemented a method to directly choose a JSON file containing test cases
 * and process it to find the secret constant of a polynomial.
 * So, you will need to select a JSON file with the required structure.
 * Sample Test Cases output:
 * > The calculated secret (c) is: 3
 * Valid share keys: [1, 2, 3, 6]
 * Invalid share keys: []
 *
 * --- Processing Test Case 2 from file: testcase2.json ---
 * => The calculated secret (c) is: 79836264049851
 * Valid share keys: [1, 2, 3, 4, 5, 6, 7, 9, 10]
 * Invalid share keys: [8]
 *
 */
public class ShamirsSecretAlgo {

    /**
     * Inner class to represent a single share, a point (x, y) on the polynomial.
     */
    static class Share {
        final BigInteger x;
        final BigInteger y;

        Share(BigInteger x, BigInteger y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "Share(x=" + x + ", y=" + y + ")";
        }
    }

    /**
     * A helper class to perform arithmetic on fractions of BigIntegers,
     * crucial for maintaining precision during Gaussian elimination.
     */
    static class Fraction {
        BigInteger num; // Numerator
        BigInteger den; // Denominator

        public Fraction(BigInteger number) {
            this.num = number;
            this.den = BigInteger.ONE;
        }

        public Fraction(BigInteger num, BigInteger den) {
            if (den.equals(BigInteger.ZERO)) {
                throw new IllegalArgumentException("Denominator cannot be zero.");
            }
            // Simplify the fraction by dividing by the greatest common divisor.
            BigInteger common = num.gcd(den);
            this.num = num.divide(common);
            this.den = den.divide(common);

            // Ensure the denominator is always positive for a standard representation.
            if (this.den.compareTo(BigInteger.ZERO) < 0) {
                this.num = this.num.negate();
                this.den = this.den.negate();
            }
        }

        public Fraction add(Fraction other) {
            BigInteger newNum = this.num.multiply(other.den).add(other.num.multiply(this.den));
            BigInteger newDen = this.den.multiply(other.den);
            return new Fraction(newNum, newDen);
        }

        public Fraction subtract(Fraction other) {
            BigInteger newNum = this.num.multiply(other.den).subtract(other.num.multiply(this.den));
            BigInteger newDen = this.den.multiply(other.den);
            return new Fraction(newNum, newDen);
        }

        public Fraction multiply(Fraction other) {
            return new Fraction(this.num.multiply(other.num), this.den.multiply(other.den));
        }

        public Fraction divide(Fraction other) {
            return new Fraction(this.num.multiply(other.den), this.den.multiply(other.num));
        }

        @Override
        public String toString() {
            return num + "/" + den;
        }
    }


    /**
     * Main entry point of the program.
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setUndecorated(true);
        frame.setVisible(false);
        frame.setLocationRelativeTo(null);

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select a JSON Test Case File");
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files", "json"));
        fileChooser.setMultiSelectionEnabled(true);

        int result = fileChooser.showOpenDialog(frame);
        int testNum = 1;

        if (result == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            for (File file : selectedFiles) {
                processFile(file, testNum++);
            }
        } else {
            System.out.println("No file selected. Exiting program.");
        }
        
        frame.dispose();
    }
    
    /**
     * Processes a single JSON file to find the secret.
     */
    private static void processFile(File file, int testNum) {
        System.out.println("--- Processing Test Case " + testNum + " from file: " + file.getName() + " ---");
        try {
            String jsonInput = new String(Files.readAllBytes(file.toPath()));
            Map<String, Object> parsedData = parseJsonAndDecodeShares(jsonInput);
            int n = (int) parsedData.get("n");
            int k = (int) parsedData.get("k");
            @SuppressWarnings("unchecked")
            List<Share> allShares = (List<Share>) parsedData.get("shares");

            List<int[]> combinations = new ArrayList<>();
            generateCombinations(n, k, 0, new int[k], 0, combinations);

            Map<BigInteger, Integer> secretFrequencies = new HashMap<>();
            Map<BigInteger, List<int[]>> secretToCombinations = new HashMap<>();

            for (int[] comboIndices : combinations) {
                Share[] currentShares = new Share[k];
                for (int i = 0; i < k; i++) {
                    currentShares[i] = allShares.get(comboIndices[i]);
                }

                try {
                    // Use the new Gaussian elimination method
                    BigInteger secret = reconstructSecretWithGauss(currentShares);
                    secretFrequencies.put(secret, secretFrequencies.getOrDefault(secret, 0) + 1);
                    secretToCombinations.computeIfAbsent(secret, key -> new ArrayList<>()).add(comboIndices);
                } catch (ArithmeticException e) {
                    // Inconsistent shares or non-integer result, ignore.
                }
            }

            if (secretFrequencies.isEmpty()) {
                System.out.println("Could not find a consistent secret.");
                return;
            }

            Map.Entry<BigInteger, Integer> majoritySecretEntry = Collections.max(secretFrequencies.entrySet(), Map.Entry.comparingByValue());
            BigInteger finalSecret = majoritySecretEntry.getKey();

            Set<BigInteger> validShareKeys = new HashSet<>();
            List<int[]> validCombinations = secretToCombinations.get(finalSecret);
            for (int[] combo : validCombinations) {
                for (int index : combo) {
                    validShareKeys.add(allShares.get(index).x);
                }
            }

            Set<BigInteger> allShareKeys = allShares.stream().map(s -> s.x).collect(Collectors.toSet());
            Set<BigInteger> invalidShareKeys = new HashSet<>(allShareKeys);
            invalidShareKeys.removeAll(validShareKeys);
            
            System.out.println("=> The calculated secret (c) is: " + finalSecret);
            System.out.println("   Valid share keys: " + validShareKeys.stream().sorted().collect(Collectors.toList()));
            System.out.println("   Invalid share keys: " + invalidShareKeys.stream().sorted().collect(Collectors.toList()));

        } catch (IOException e) {
            System.err.println("Error reading file " + file.getName() + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("An error occurred while processing " + file.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }

    /**
     * Solves the system of linear equations
     * using Gaussian elimination.
     */
    public static BigInteger reconstructSecretWithGauss(Share[] shares) {
        int k = shares.length;
        // The augmented matrix for the system Ax = b, where x are the coefficients.
        Fraction[][] matrix = new Fraction[k][k + 1];

        // Populate the Vandermonde matrix A and the vector b.
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < k; j++) {
                // A[i][j] = x_i^j
                matrix[i][j] = new Fraction(shares[i].x.pow(j));
            }
            // b[i] = y_i
            matrix[i][k] = new Fraction(shares[i].y);
        }

        // Forward elimination to get row-echelon form.
        for (int i = 0; i < k; i++) {
            // Find pivot (non-zero element).
            int pivot = i;
            while (pivot < k && matrix[pivot][i].num.equals(BigInteger.ZERO)) {
                pivot++;
            }
            if (pivot == k) continue; // No pivot found, matrix is singular.

            // Swap pivot row with current row.
            Fraction[] temp = matrix[i];
            matrix[i] = matrix[pivot];
            matrix[pivot] = temp;

            // Normalize pivot row.
            Fraction pivotValue = matrix[i][i];
            for (int j = i; j <= k; j++) {
                matrix[i][j] = matrix[i][j].divide(pivotValue);
            }

            // Eliminate other entries in the column.
            for (int row = 0; row < k; row++) {
                if (row != i) {
                    Fraction factor = matrix[row][i];
                    for (int col = i; col <= k; col++) {
                        matrix[row][col] = matrix[row][col].subtract(factor.multiply(matrix[i][col]));
                    }
                }
            }
        }

        Fraction secretFraction = matrix[0][k];

        // The secret must be an integer.
        if (!secretFraction.den.equals(BigInteger.ONE)) {
            throw new ArithmeticException("Secret is not an integer, shares are inconsistent.");
        }

        return secretFraction.num;
    }

    /**
     * Recursive helper to generate all combinations of k indices from n items.
     */
    private static void generateCombinations(int n, int k, int start, int[] currentCombo, int index, List<int[]> combinations) {
        if (index == k) {
            combinations.add(currentCombo.clone());
            return;
        }
        for (int i = start; i <= n - (k - index); i++) {
            currentCombo[index] = i;
            generateCombinations(n, k, i + 1, currentCombo, index + 1, combinations);
        }
    }

    /**
     * A JSON parser that handles the specific structure of the input.
     */
    private static Map<String, Object> parseJsonAndDecodeShares(String json) {
        Map<String, Object> data = new HashMap<>();
        
        Pattern keysPattern = Pattern.compile("\"keys\"\\s*:\\s*\\{\\s*\"n\"\\s*:\\s*(\\d+)\\s*,\\s*\"k\"\\s*:\\s*(\\d+)\\s*\\}");
        Matcher keysMatcher = keysPattern.matcher(json);
        if (keysMatcher.find()) {
            data.put("n", Integer.parseInt(keysMatcher.group(1)));
            data.put("k", Integer.parseInt(keysMatcher.group(2)));
        } else {
            throw new IllegalArgumentException("Could not find 'keys' object with 'n' and 'k' in JSON input.");
        }

        List<Share> shares = new ArrayList<>();
        Pattern sharePattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\\{\\s*\"base\"\\s*:\\s*\"(\\d+)\"\\s*,\\s*\"value\"\\s*:\\s*\"([a-zA-Z0-9]+)\"\\s*\\}");
        Matcher shareMatcher = sharePattern.matcher(json);
        while (shareMatcher.find()) {
            try {
                BigInteger x = new BigInteger(shareMatcher.group(1));
                int base = Integer.parseInt(shareMatcher.group(2));
                String encodedY = shareMatcher.group(3);
                BigInteger y = new BigInteger(encodedY, base);
                shares.add(new Share(x, y));
            } catch (NumberFormatException e) {
                // This will skip non-share keys like "keys".
            }
        }
        data.put("shares", shares);
        return data;
    }
}
