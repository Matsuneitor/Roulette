package me.matsubara.roulette.data;

import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Material;

public enum Part {
    // Borders of the table.
    BORDER_1("MATERIAL:SPRUCE_SLAB", -1.546425d, 0.3d, -0.59375d),
    BORDER_2("MATERIAL:SPRUCE_SLAB", -1.546425d, 0.3d, -1.1875d),

    BORDER_3_WEST("MATERIAL:SPRUCE_SLAB", -1.484375d, 0.3d, -1.546425d),
    BORDER_4_WEST("MATERIAL:SPRUCE_SLAB", -0.890625d, 0.3d, -1.546425d),
    BORDER_5_WEST("MATERIAL:SPRUCE_SLAB", -0.296875d, 0.3d, -1.546425d),
    BORDER_6_WEST("MATERIAL:SPRUCE_SLAB", 0.296875d, 0.3d, -1.546425d),
    BORDER_7_WEST("MATERIAL:SPRUCE_SLAB", 0.890625d, 0.3d, -1.546425d),
    BORDER_8_WEST("MATERIAL:SPRUCE_SLAB", 1.484375d, 0.3d, -1.546425d),
    BORDER_9_WEST("MATERIAL:SPRUCE_SLAB", 2.078125d, 0.3d, -1.546425d),
    BORDER_10_WEST("MATERIAL:SPRUCE_SLAB", 2.671875d, 0.3d, -1.546425d),
    BORDER_11_WEST("MATERIAL:SPRUCE_SLAB", 3.265625d, 0.3d, -1.546425d),
    BORDER_12_WEST("MATERIAL:SPRUCE_SLAB", 3.859375d, 0.3d, -1.546425d),

    BORDER_13_NORTH("MATERIAL:SPRUCE_SLAB", 3.921425d, 0.3d, -1.1875d),
    BORDER_14_NORTH("MATERIAL:SPRUCE_SLAB", 3.921425d, 0.3d, -0.59375d),
    BORDER_15_NORTH("MATERIAL:SPRUCE_SLAB", 3.921425d, 0.3d, 0.0d),

    BORDER_16_EAST("MATERIAL:SPRUCE_SLAB", 3.859375d, 0.3d, 0.358925d),
    BORDER_17_EAST("MATERIAL:SPRUCE_SLAB", 3.265625d, 0.3d, 0.358925d),
    BORDER_18_EAST("MATERIAL:SPRUCE_SLAB", 2.671875d, 0.3d, 0.358925d),
    BORDER_29_EAST("MATERIAL:SPRUCE_SLAB", 2.078125d, 0.3d, 0.358925d),
    BORDER_20_EAST("MATERIAL:SPRUCE_SLAB", 1.484375d, 0.3d, 0.358925d),
    BORDER_21_EAST("MATERIAL:SPRUCE_SLAB", 0.890625d, 0.3d, 0.358925d),
    BORDER_22_EAST("MATERIAL:SPRUCE_SLAB", 1.484375d, 0.3d, 0.358925d),
    BORDER_23_EAST("MATERIAL:SPRUCE_SLAB", 0.296875d, 0.3d, 0.358925d),
    BORDER_24_EAST("MATERIAL:SPRUCE_SLAB", -0.296875d, 0.3d, 0.358925d),

    BORDER_25("MATERIAL:SPRUCE_SLAB", -0.358925d, 0.3d, 0.0155d),

    BORDER_26_EAST("MATERIAL:SPRUCE_SLAB", -0.890625d, 0.3d, -0.234825d),
    BORDER_27_EAST("MATERIAL:SPRUCE_SLAB", -1.484375d, 0.3d, -0.234825d),

    // Foots of the table.
    FOOT_1("MATERIAL:SPRUCE_PLANKS", 0.0d, 0.325d, 0.0d),
    FOOT_2("MATERIAL:SPRUCE_PLANKS", 0.0d, 0.325d, -1.1875d),
    FOOT_3("MATERIAL:SPRUCE_PLANKS", 3.5625d, 0.325d, 0.0d),
    FOOT_4("MATERIAL:SPRUCE_PLANKS", 3.5625d, 0.325d, -1.1875d),

    // Rest of the table.
    TABLE_1_1("9c4ccb83813a3bcdcf72bfeed0cdcd710802f0782565497973023d115d82c9db", 0.0d, 0.0d, 0.0d),
    TABLE_1_2("ac82febeb5ecdc49aef9483120d9cc9223ff62c491c15186c86c726ca2e0021f", 0.59375d, 0.0d, 0.0d),
    TABLE_1_3("50e23e45e2bdff6f18145fd600e5fd0e73d4e79d648ecb2f69c349bca7c0a42c", 1.1875d, 0.0d, 0.0d),
    TABLE_1_4("fed81842d19ab80777ee2d0707a128cb58bcf7f561dbe6828739f8496ea15760", 1.78125d, 0.0d, 0.0d),
    TABLE_1_5("fd9688933422478b6bb936fbe05f132b1dfcbf0de743b3166a39abc77836f35f", 2.375d, 0.0d, 0.0d),
    TABLE_1_6("d9efd5586c2413847b5930e12c103eaf788b9bc9f13389b0b6164c1cf94315b1", 2.96875d, 0.0d, 0.0d),
    TABLE_1_7("6bd69fefd30930200eb2eb49738acb44c182f26f76ca1feea022655e75f8c4d5", 3.5625d, 0.0d, 0.0d),

    // Wheels of the table.
    WHEEL_1_1("7af91b2da78a013b79e112b8fbaa78bde299997cc2a0605c1eb54be793b4ca6f", -1.1875d, 0.0d, -0.59375d),
    WHEEL_1_2("2763a1c8abb7b2d41d851142ca71667f4e0b7af9839dac871b22737761fe15e7", -0.59375d, 0.0d, -0.59375d),

    // For american roulette.
    TABLE_2_1_AMERICAN("b088ab716d8d6607cba802984fc274eb3d9f67392e8c305a27810581bdab1181", 0.0d, 0.0d, -0.59375d),

    TABLE_2_1("99dcf9e97714e9294c92f2e0e4e38b0541523d8fcce35d93c0c3fd9513c97120", 0.0d, 0.0d, -0.59375d),
    TABLE_2_2("ad2d442a3d08f9aa9d07513bbc07e72b27dd598f991246412b07a8eaba7e5617", 0.59375d, 0.0d, -0.59375d),
    TABLE_2_3("158e22754a694251c82c5077dc6f0ddebc5f4905d45b8afa1886a12ccad4370b", 1.1875d, 0.0d, -0.59375d),
    TABLE_2_4("25debf11e12c1ec74bf4deb17ff55eba0eb4d7c23055de74ca5205798204ce0f", 1.78125d, 0.0d, -0.59375d),
    TABLE_2_5("ad2d442a3d08f9aa9d07513bbc07e72b27dd598f991246412b07a8eaba7e5617", 2.375d, 0.0d, -0.59375d),
    TABLE_2_6("158e22754a694251c82c5077dc6f0ddebc5f4905d45b8afa1886a12ccad4370b", 2.96875d, 0.0d, -0.59375d),
    TABLE_2_7("f0b05700dd0eefdbd595fd75400ba969605d62249fa3df333a653e814a3d7776", 3.5625d, 0.0d, -0.59375d),

    // Wheels of the table.
    WHEEL_2_1("35efc936873314d13ffd7ee5b305233e43fe3c404a6ee3c3b81f0e47d3a53367", -1.1875d, 0.0d, -1.1875d),
    WHEEL_2_2("5aac6a06d223aa6e96acfdf1a8898b0fcbca0dcce04e8b621aadcb7b32dd3d2e", -0.59375d, 0.0d, -1.1875d),

    // For american roulette.
    TABLE_3_1_AMERICAN("db90ad752b741d3fd96711db3840093a2422cd28373b0592cc23fda71aa369a5", 0.0d, 0.0d, -1.1875d),

    TABLE_3_1("dcd9bf37a36019a92e6bdca577bcc6601aed6317f1c5b09799f1f7fa00edcb7d", 0.0d, 0.0d, -1.1875d),
    TABLE_3_2("753197a604c3452b0210c601fb902ad045b0e98585ec1e563b63d77ac5b717c1", 0.59375d, 0.0d, -1.1875d),
    TABLE_3_3("c42ddacdfd9f389668bce1ca7f9840f80b9f23dcabee804e6839467879fd0704", 1.1875d, 0.0d, -1.1875d),
    TABLE_3_4("f52de0bbe26b67d13017f18b0d67259f48e3f14f7ec37389663e01f17846de83", 1.78125d, 0.0d, -1.1875d),
    TABLE_3_5("753197a604c3452b0210c601fb902ad045b0e98585ec1e563b63d77ac5b717c1", 2.375d, 0.0d, -1.1875d),
    TABLE_3_6("c42ddacdfd9f389668bce1ca7f9840f80b9f23dcabee804e6839467879fd0704", 2.96875d, 0.0d, -1.1875d),
    TABLE_3_7("4fc94663fc58921da8bcb196507701a711e2161f1fb2739a09a69aae2830958c", 3.5625d, 0.0d, -1.1875d),

    // Chairs.
    CHAIR_1_1("MATERIAL:SPRUCE_PLANKS", 0.0d, 0.32d, 1.21d),
    CHAIR_1_2("MATERIAL:SPRUCE_SLAB", 0.0d, 0.0d, 1.21d),
    CHAIR_1_3("MATERIAL:RED_CARPET", 0.0d, 0.3d, 1.21d),

    CHAIR_2_1("MATERIAL:SPRUCE_PLANKS", 1.17d, 0.32d, 1.21d),
    CHAIR_2_2("MATERIAL:SPRUCE_SLAB", 1.17d, 0.0d, 1.21d),
    CHAIR_2_3("MATERIAL:RED_CARPET", 1.17d, 0.3d, 1.21d),

    CHAIR_3_1("MATERIAL:SPRUCE_PLANKS", 2.34d, 0.32d, 1.21d),
    CHAIR_3_2("MATERIAL:SPRUCE_SLAB", 2.34d, 0.0d, 1.21d),
    CHAIR_3_3("MATERIAL:RED_CARPET", 2.34d, 0.3d, 1.21d),

    CHAIR_4_1("MATERIAL:SPRUCE_PLANKS", 3.55d, 0.32d, 1.21d),
    CHAIR_4_2("MATERIAL:SPRUCE_SLAB", 3.55d, 0.0d, 1.21d),
    CHAIR_4_3("MATERIAL:RED_CARPET", 3.55d, 0.3d, 1.21d),

    CHAIR_5_1("MATERIAL:SPRUCE_PLANKS", 4.76d, 0.32d, 0.0d),
    CHAIR_5_2("MATERIAL:SPRUCE_SLAB", 4.76d, 0.0d, 0.0d),
    CHAIR_5_3("MATERIAL:RED_CARPET", 4.76d, 0.3d, 0.0d),

    CHAIR_6_1("MATERIAL:SPRUCE_PLANKS", 4.76d, 0.32d, -1.17d),
    CHAIR_6_2("MATERIAL:SPRUCE_SLAB", 4.76d, 0.0d, -1.17d),
    CHAIR_6_3("MATERIAL:RED_CARPET", 4.76d, 0.3d, -1.17d),

    CHAIR_7_1("MATERIAL:SPRUCE_PLANKS", 3.55d, 0.32d, -2.38d),
    CHAIR_7_2("MATERIAL:SPRUCE_SLAB", 3.55d, 0.0d, -2.38d),
    CHAIR_7_3("MATERIAL:RED_CARPET", 3.55d, 0.3d, -2.38d),

    CHAIR_8_1("MATERIAL:SPRUCE_PLANKS", 2.34d, 0.32d, -2.38d),
    CHAIR_8_2("MATERIAL:SPRUCE_SLAB", 2.34d, 0.0d, -2.38d),
    CHAIR_8_3("MATERIAL:RED_CARPET", 2.34d, 0.3d, -2.38d),

    CHAIR_9_1("MATERIAL:SPRUCE_PLANKS", 1.17d, 0.32d, -2.38d),
    CHAIR_9_2("MATERIAL:SPRUCE_SLAB", 1.17d, 0.0d, -2.38d),
    CHAIR_9_3("MATERIAL:RED_CARPET", 1.17d, 0.3d, -2.38d),

    CHAIR_10_1("MATERIAL:SPRUCE_PLANKS", 0.0d, 0.32d, -2.38d),
    CHAIR_10_2("MATERIAL:SPRUCE_SLAB", 0.0d, 0.0d, -2.38d),
    CHAIR_10_3("MATERIAL:RED_CARPET", 0.0d, 0.3d, -2.38d),

    // Decorations.
    DECORATION("b0458d58c030cfabd8b19e4944bbe2860f6617a77ec6c9488593e2a473db6758", -1.8265d, 1.6d, -0.7465d),
    DECORATION_2("b0458d58c030cfabd8b19e4944bbe2860f6617a77ec6c9488593e2a473db6758", -1.8265d, 1.7328d, -0.7465d),
    DECORATION_3("b0458d58c030cfabd8b19e4944bbe2860f6617a77ec6c9488593e2a473db6758", -1.8265d, 1.8656d, -0.7465d),
    DECORATION_4("b0458d58c030cfabd8b19e4944bbe2860f6617a77ec6c9488593e2a473db6758", -1.8265d, 1.9984d, -0.7465d),
    DECORATION_5("1a316db9820b3e29c556318065d610231fbb5abce97dcb9b35ebf62ff3ba5d41", -1.8265d, 1.6d, -0.9465d),
    DECORATION_6("1a316db9820b3e29c556318065d610231fbb5abce97dcb9b35ebf62ff3ba5d41", -1.8265d, 1.7328d, -0.9465d),
    DECORATION_7("1a316db9820b3e29c556318065d610231fbb5abce97dcb9b35ebf62ff3ba5d41", -1.8265d, 1.8656d, -0.9465d),
    DECORATION_8("f780bf5789551bf051e74e36052706dd133fcd58ceadeed9ce2afd84c7c9a209", -1.8265d, 1.6d, -1.1465d),
    DECORATION_9("f780bf5789551bf051e74e36052706dd133fcd58ceadeed9ce2afd84c7c9a209", -1.8265d, 1.7328d, -1.1465d),
    DECORATION_10("8bad26ccb4f8937ffcfe9e0ef49b7cea7672c98f68ead7f8f901a22349029301", -1.8265d, 1.6d, -1.3465d),

    // Spinners of the table.
    SPINNER_1_1("EMPTY", -0.890625d, 1.585d, -0.890625d),
    SPINNER_1_2("EMPTY", -0.890625d, 1.585d, -0.890625d),

    // Target entity for the NPC.
    NPC_TARGET("MATERIAL:EMPTY", -1.275d, 2.1d, 0.0d),

    // The ball.
    BALL("MATERIAL:END_ROD", -0.890625d, -0.15d, -0.890625d),

    // Slots for the 0 & 00, if using an american table.
    SLOT_SINGLE_ZERO("8bad26ccb4f8937ffcfe9e0ef49b7cea7672c98f68ead7f8f901a22349029301", -0.3235d, 1.585d, -0.895d),
    SLOT_DOUBLE_ZERO("8bad26ccb4f8937ffcfe9e0ef49b7cea7672c98f68ead7f8f901a22349029301", -0.3235d, 1.585d, -1.489d),

    // Slots of the table, from 0 to 36.
    SLOT_0("EMPTY", -0.3235d, 1.585d, -1.192d),
    SLOT_1("EMPTY", -0.0265d, 1.585d, -0.7465d),
    SLOT_2("EMPTY", -0.0265d, 1.585d, -1.192d),
    SLOT_3("EMPTY", -0.0265d, 1.585d, -1.6375d),
    SLOT_4("EMPTY", 0.2705d, 1.585d, -0.7465d),
    SLOT_5("EMPTY", 0.2705d, 1.585d, -1.192d),
    SLOT_6("EMPTY", 0.2705d, 1.585d, -1.6375d),
    SLOT_7("EMPTY", 0.5675d, 1.585d, -0.7465d),
    SLOT_8("EMPTY", 0.5675d, 1.585d, -1.192d),
    SLOT_9("EMPTY", 0.5675d, 1.585d, -1.6375d),
    SLOT_10("EMPTY", 0.8645d, 1.585d, -0.7465d),
    SLOT_11("EMPTY", 0.8645d, 1.585d, -1.192d),
    SLOT_12("EMPTY", 0.8645d, 1.585d, -1.6375d),
    SLOT_13("EMPTY", 1.1615d, 1.585d, -0.7465d),
    SLOT_14("EMPTY", 1.1615d, 1.585d, -1.192d),
    SLOT_15("EMPTY", 1.1615d, 1.585d, -1.6375d),
    SLOT_16("EMPTY", 1.4585d, 1.585d, -0.7465d),
    SLOT_17("EMPTY", 1.4585d, 1.585d, -1.192d),
    SLOT_18("EMPTY", 1.4585d, 1.585d, -1.6375d),
    SLOT_19("EMPTY", 1.7555d, 1.585d, -0.7465d),
    SLOT_20("EMPTY", 1.7555d, 1.585d, -1.192d),
    SLOT_21("EMPTY", 1.7555d, 1.585d, -1.6375d),
    SLOT_22("EMPTY", 2.0525d, 1.585d, -0.7465d),
    SLOT_23("EMPTY", 2.0525d, 1.585d, -1.192d),
    SLOT_24("EMPTY", 2.0525d, 1.585d, -1.6375d),
    SLOT_25("EMPTY", 2.3495d, 1.585d, -0.7465d),
    SLOT_26("EMPTY", 2.3495d, 1.585d, -1.192d),
    SLOT_27("EMPTY", 2.3495d, 1.585d, -1.6375d),
    SLOT_28("EMPTY", 2.6465d, 1.585d, -0.7465d),
    SLOT_29("EMPTY", 2.6465d, 1.585d, -1.192d),
    SLOT_30("EMPTY", 2.6465d, 1.585d, -1.6375d),
    SLOT_31("EMPTY", 2.9435d, 1.585d, -0.7465d),
    SLOT_32("EMPTY", 2.9435d, 1.585d, -1.192d),
    SLOT_33("EMPTY", 2.9435d, 1.585d, -1.6375d),
    SLOT_34("EMPTY", 3.2405d, 1.585d, -0.7465d),
    SLOT_35("EMPTY", 3.2405d, 1.585d, -1.192d),
    SLOT_36("EMPTY", 3.2405d, 1.585d, -1.6375d),

    // Columns.
    SLOT_COLUMN_1("EMPTY", 3.4995d, 1.585d, -0.7465d),
    SLOT_COLUMN_2("EMPTY", 3.4995d, 1.585d, -1.192d),
    SLOT_COLUMN_3("EMPTY", 3.4995d, 1.585d, -1.6375d),

    // Dozens.
    SLOT_DOZEN_1("EMPTY", 0.419d, 1.585d, -0.4135d),
    SLOT_DOZEN_2("EMPTY", 1.606d, 1.585d, -0.4135d),
    SLOT_DOZEN_3("EMPTY", 2.794d, 1.585d, -0.4135d),

    // 1 to 18.
    SLOT_1_TO_18("EMPTY", 0.122d, 1.585d, -0.2275d),

    // Evens.
    SLOT_EVEN("EMPTY", 0.716d, 1.585d, -0.2275d),

    // Colors.
    SLOT_RED("EMPTY", 1.31d, 1.585d, -0.2275d),
    SLOT_BLACK("EMPTY", 1.904d, 1.585d, -0.2275d),

    // Odds.
    SLOT_ODD("EMPTY", 2.498d, 1.585d, -0.2275d),

    // 19 to 36.
    SLOT_19_TO_36("EMPTY", 3.092d, 1.585d, -0.2275d);

    private final String url;
    private final double offsetX;
    private final double offsetY;
    private final double offsetZ;

    private final static Part[] EUROPEAN, AMERICAN;

    Part(String url, double offsetX, double offsetY, double offsetZ) {
        this.url = url;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    static {
        EUROPEAN = removeParts(TABLE_3_1_AMERICAN, TABLE_2_1_AMERICAN, SLOT_SINGLE_ZERO, SLOT_DOUBLE_ZERO);
        AMERICAN = removeParts(TABLE_3_1, TABLE_2_1, SLOT_0);
    }

    private static Part[] removeParts(Part... parts) {
        Part[] values = values();
        for (Part part : parts) {
            values = (Part[]) ArrayUtils.removeElement(values, part);
        }
        return values;
    }

    public boolean isMaterial() {
        return url.startsWith("MATERIAL");
    }

    public boolean isSpinner() {
        return name().startsWith("SPINNER");
    }

    public boolean isSlot() {
        return name().startsWith("SLOT");
    }

    public boolean isChair() {
        return name().startsWith("CHAIR");
    }

    public boolean isDecoration() {
        return name().startsWith("DECORATION");
    }

    public boolean isFirstSpinner() {
        return Character.getNumericValue(name().charAt(name().length() - 1)) == 1;
    }

    public boolean isNPCTarget() {
        return this == NPC_TARGET;
    }

    public boolean isBall() {
        return this == BALL;
    }

    public String getUrl() {
        return isMaterial() ? url.substring(9) : url;
    }

    public Material getMaterial() {
        if (!isMaterial()) return null;
        try {
            return Material.valueOf(getUrl());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static int getSize(boolean isEuropean) {
        return isEuropean ? EUROPEAN.length : AMERICAN.length;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public double getOffsetZ() {
        return offsetZ;
    }

    public static Part[] getValues(boolean isEuropean) {
        return isEuropean ? EUROPEAN : AMERICAN;
    }
}