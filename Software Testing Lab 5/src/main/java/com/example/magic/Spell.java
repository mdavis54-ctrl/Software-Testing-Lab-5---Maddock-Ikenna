package com.example.magic;

public class Spell {
    private final String name;
    private final int baseManaCost;
    private final int minLevelRequired;

    public Spell(String name, int baseManaCost, int minLevelRequired) {
        this.name = name;
        this.baseManaCost = baseManaCost;
        this.minLevelRequired = minLevelRequired;
    }

    public String getName() { return name; }
    public int getBaseManaCost() { return baseManaCost; }
    public int getMinLevelRequired() { return minLevelRequired; }
}