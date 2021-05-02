package me.matsubara.roulette.game;

@SuppressWarnings("unused")
public enum GameRules {
    /**
     * Once a single zero is spun, the even-money bet will immediately be divided by two.
     * This way 50% of the bet will be recovered to the player, while the other 50% will be surrendered to the house.
     */
    LA_PARTAGE,
    /**
     * Gives players an opportunity to recover their even-money stakes after the zero is spun.
     * The stake remains on the losing even-money bet for the next spin and if the player wins the second time around,
     * they get their original stake back.
     */
    EN_PRISON,
    /**
     * It is basically the same as La Partage as it is enforced whenever 0 or 00 win,
     * in which case the player “surrenders” half of their original stake and retains the rest.
     */
    SURRENDER
}