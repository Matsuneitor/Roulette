package me.matsubara.roulette.data;

import org.apache.commons.lang.ArrayUtils;

public enum Slot {
    // Slots of the table, from 0 to 36.
    SINGLE_0(0),

    // For american table.
    SINGLE_00(0, 0),

    SINGLE_1(1),
    SINGLE_2(2),
    SINGLE_3(3),
    SINGLE_4(4),
    SINGLE_5(5),
    SINGLE_6(6),
    SINGLE_7(7),
    SINGLE_8(8),
    SINGLE_9(9),
    SINGLE_10(10),
    SINGLE_11(11),
    SINGLE_12(12),
    SINGLE_13(13),
    SINGLE_14(14),
    SINGLE_15(15),
    SINGLE_16(16),
    SINGLE_17(17),
    SINGLE_18(18),
    SINGLE_19(19),
    SINGLE_20(20),
    SINGLE_21(21),
    SINGLE_22(22),
    SINGLE_23(23),
    SINGLE_24(24),
    SINGLE_25(25),
    SINGLE_26(26),
    SINGLE_27(27),
    SINGLE_28(28),
    SINGLE_29(29),
    SINGLE_30(30),
    SINGLE_31(31),
    SINGLE_32(32),
    SINGLE_33(33),
    SINGLE_34(34),
    SINGLE_35(35),
    SINGLE_36(36),

    // Columns.
    COLUMN_1(1, 4, 7, 10, 13, 16, 19, 22, 25, 28, 31, 34),
    COLUMN_2(2, 5, 8, 11, 14, 17, 20, 23, 26, 29, 32, 35),
    COLUMN_3(3, 6, 9, 12, 15, 18, 21, 24, 27, 30, 33, 36),

    // Dozens.
    DOZEN_1(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12),
    DOZEN_2(13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24),
    DOZEN_3(25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36),

    // Low numbers (1 to 18) and evens.
    LOW(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18),
    EVEN(2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 36),

    // Colors.
    RED(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36),
    BLACK(2, 4, 6, 8, 10, 11, 13, 15, 17, 20, 22, 24, 26, 28, 29, 31, 33, 35),

    // Odds and high numbers (19 to 36).
    ODD(1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31, 33, 35),
    HIGH(19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36);

    private final int[] ints;

    Slot(int... ints) {
        this.ints = ints;
    }

    public int[] getInts() {
        return ints;
    }

    public SlotColor getColor() {
        switch (this) {
            case RED:
            case SINGLE_1:
            case SINGLE_3:
            case SINGLE_5:
            case SINGLE_7:
            case SINGLE_9:
            case SINGLE_12:
            case SINGLE_14:
            case SINGLE_16:
            case SINGLE_18:
            case SINGLE_19:
            case SINGLE_21:
            case SINGLE_23:
            case SINGLE_25:
            case SINGLE_27:
            case SINGLE_30:
            case SINGLE_32:
            case SINGLE_34:
            case SINGLE_36:
                return SlotColor.RED;
            case BLACK:
                return SlotColor.BLACK;
            case SINGLE_0:
            case SINGLE_00:
                return SlotColor.GREEN;
        }
        if (ints.length > 1) {
            return SlotColor.MIXED;
        }
        return SlotColor.BLACK;
    }

    public int getMultiplier() {
        if (isSingle()) {
            return 36;
        }
        if (isLow() || isEven() || isRed() || isBlack() || isOdd() || isHigh()) {
            return 2;
        }
        return 3;
    }

    public String getChance(boolean isEuropean) {
        if (isSingle()) {
            return isEuropean ? "1/37 (27%)" : "1/38 (26%)";
        }
        if (isLow() || isEven() || isRed() || isBlack() || isOdd() || isHigh()) {
            return isEuropean ? "18/37 (48%)" : "18/38 (47%)";
        }
        return isEuropean ? "12/37 (32%)" : "12/38 (31%)";
    }

    public boolean isDoubleZero() {
        return this == SINGLE_00;
    }

    public boolean isSingle() {
        return ints.length == 1 || this == SINGLE_00;
    }

    public boolean isColumn() {
        return name().startsWith("COLUMN");
    }

    public boolean isDozen() {
        return name().startsWith("DOZEN");
    }

    public boolean isLow() {
        return this == LOW;
    }

    public boolean isEven() {
        return this == EVEN;
    }

    public boolean isOdd() {
        return this == ODD;
    }

    public boolean isHigh() {
        return this == HIGH;
    }

    public int getColumn() {
        return Integer.parseInt(name().substring(7));
    }

    public int getDozen() {
        return Integer.parseInt(name().substring(6));
    }

    public boolean isRed() {
        return getColor() == SlotColor.RED;
    }

    public boolean isBlack() {
        return getColor() == SlotColor.BLACK;
    }

    public static Slot[] getValues(boolean isEuropean) {
        return isEuropean ? (Slot[]) ArrayUtils.remove(values(), 1) : values();
    }

    public enum SlotColor {
        RED,
        BLACK,
        GREEN,
        // For columns, dozens, lows, evens, etc...
        MIXED
    }
}