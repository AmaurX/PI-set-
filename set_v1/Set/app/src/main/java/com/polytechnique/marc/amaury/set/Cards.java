package com.polytechnique.marc.amaury.set;

/**
 * A Set! card is defined by its four characteristics: number, color,
 * filling and shape, any of which may take a value between 1 and 3.
 * <p>
 * This class contains utility methods for dealing with cards encoded
 * as their four characteristics into an integer. The four
 * characteristics are encoded as a single integer, with each
 * characteristic occupying 2 bits, from least significant to most
 * significant: number, color, filling. and shape.
 * <p>
 * Each characteristic may take a value between 1 and 3; 0 is
 * reserved.
 * <pre>
 * card = number | color &lt;&lt; 2 | filling &lt;&lt; 4 | shape &lt;&lt; 6
 * </pre>
 */
class Cards {
    /**
     * Encodes the specified individual characteristics into an
     * integer.
     *
     * @param number  the number characteristic
     * @param color   the color characteristic
     * @param filling the filling characteristic
     * @param shape   the shape characteristic
     * @returns the encoded card
     */
    static int valueOf(int number, int color, int filling, int shape) {
        if (number <= 0 || number > 3 ||
                color <= 0 || color > 3 ||
                filling <= 0 || filling > 3 ||
                shape <= 0 || shape > 3) {
            throw new IllegalArgumentException("Characteristics out of range.");
        }
        return number | (color << 2) | (filling << 4) | (shape << 6);
    }

    /**
     * @param card the characteristics to decode
     * @returns the number characteristic of {@code card}
     */
    static int numberOf(int card) {
        return card & 0x3;
    }

    /**
     * @param card the characteristics to decode
     * @returns the color characteristic of {@code card}
     */
    static int colorOf(int card) {
        return (card >> 2) & 0x3;
    }

    /**
     * @param card the characteristics to decode
     * @returns the filling characteristic of {@code card}
     */
    static int fillingOf(int card) {
        return (card >> 4) & 0x3;
    }

    /**
     * @param card the characteristics to decode
     * @returns the shape characteristic of {@code card}
     */
    static int shapeOf(int card) {
        return (card >> 6) & 0x3;
    }

    /**
     * Checks whether the given cards make a set.
     *
     * @param a an integer-encoded card to test
     * @param b an integer-encoded card to test
     * @param c an integer-encoded card to test
     * @returns whether we have a set
     */
    static boolean isSet(int a, int b, int c) {
        if (a == -1 || b == -1 || c == -1) return false;
        for (int i = 0; i < 4; ++i) {
            if (((a & 0x3) + (b & 0x3) + (c & 0x3)) % 3 != 0) {
                return false;
            }
            a >>= 2;
            b >>= 2;
            c >>= 2;
        }
        return true;
    }
}
