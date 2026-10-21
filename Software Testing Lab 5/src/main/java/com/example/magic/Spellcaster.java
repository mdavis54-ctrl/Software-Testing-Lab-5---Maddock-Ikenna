package com.example.magic;

public abstract class Spellcaster {
    protected int level;
    protected int hp;
    protected int maxHp;
    protected int manaStore;

    public static final int MAX_MANA_STORE = 600;
    public static final int MAX_LEVEL = 50;
    public static final int MIN_LEVEL = 1;

    public Spellcaster(int level, int maxHp) throws HeroLevelException {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            throw new HeroLevelException("Hero level must be between 1 and 50");
        }
        this.level = level;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.manaStore = MAX_MANA_STORE;
    }

    public abstract double getPowerMultiplier();

    public void receiveDamage(int damage) {
        this.hp = Math.max(0, this.hp - damage);
    }

    public int getLevel() { return level; }
    public int getHp() { return hp; }
    public int getManaStore() { return manaStore; }
}