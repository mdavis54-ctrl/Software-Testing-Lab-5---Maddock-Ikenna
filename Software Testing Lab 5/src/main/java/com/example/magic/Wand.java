package com.example.magic;

public class Wand {
    private final int powerBonus;
    private final int efficiencyRating;

    public Wand(int powerBonus, int efficiencyRating) {
        this.powerBonus = powerBonus;
        this.efficiencyRating = efficiencyRating;
    }

    public int getPowerBonus() { return powerBonus; }
    public int getEfficiencyRating() { return efficiencyRating; }
}