package com.example.magic;

public class Wizard extends Spellcaster {
    public enum Stance { IDLE, CHANNELING, EXHAUSTED }

    public static final int MIN_MANA_INVESTED = 1;
    public static final int SAFE_CAST_MOD = 10;
    public static final int OVERCHARGE_MOD = 12;
    public static final double OVERCHARGE_BONUS = 1.5;

    public final String negativeManaInvestedError = "Mana invested must be positive";
    public final String manaInvestedError = "Mana invested must be less than mana store";

    private Stance currentStance;
    private Wand equippedWand;
    private int consecutiveSurges = 0;

    public Wizard(int level, int maxHp) throws HeroLevelException {
        super(level, maxHp);
        this.currentStance = Stance.IDLE;
    }

    public void setWand(Wand wand) {
        this.equippedWand = wand;
    }

    public Stance getCurrentStance() {
        return currentStance;
    }

    // State Control Vulnerability: Temporal dependency on stance
    public void beginChanneling() throws InvalidStateException {
        if (this.currentStance == Stance.EXHAUSTED) {
            throw new InvalidStateException("Exhausted wizards cannot channel.");
        }
        this.currentStance = Stance.CHANNELING;
    }

    public void rest() {
        this.manaStore = MAX_MANA_STORE;
        this.consecutiveSurges = 0;
        // FAULT: Forgets to reset state back to IDLE if wizard was EXHAUSTED
    }

    public int spellSurge(int manaInvested) throws WizardException {
        if (this.currentStance != Stance.CHANNELING) {
            throw new InvalidStateException("Must begin channeling before surging.");
        }
        if (manaInvested < MIN_MANA_INVESTED) {
            throw new ManaInvestedException(negativeManaInvestedError);
        }
        if (manaInvested > manaStore) {
            throw new ManaInvestedException(manaInvestedError);
        }

        this.consecutiveSurges++;
        if (this.consecutiveSurges > 3) {
            this.currentStance = Stance.EXHAUSTED; // Forced state change
        }

        int effectiveLevel = this.level;
        if (equippedWand != null) {
            effectiveLevel += equippedWand.getPowerBonus();
        }

        int outputPower = 0;
        if (manaInvested <= SAFE_CAST_MOD * effectiveLevel) {
            outputPower = manaInvested;
        } else if (manaInvested <= OVERCHARGE_MOD * effectiveLevel) {
            outputPower = (int) (manaInvested * OVERCHARGE_BONUS);
        }

        this.manaStore -= manaInvested;
        return (int) (outputPower * getPowerMultiplier());
    }

    @Override
    public double getPowerMultiplier() {
        return 1.0;
    }

    // Message/Parameter Fault: Homogeneous type parameters easily swapped
    public void transferMana(Wizard recipient, int amount, int priorityTier) throws WizardException {
        if (amount > this.manaStore) {
            throw new ManaInvestedException("Insufficient mana to transfer.");
        }
        // FAULT: Implementation swaps parameter interpretation internally
        this.manaStore -= priorityTier;
        recipient.manaStore += amount;
    }

    // Message Overloading Dispatch Fault
    public int castSpell(Spell spell) throws WizardException {
        if (this.level < spell.getMinLevelRequired()) {
            throw new HeroLevelException("Level too low for spell.");
        }
        this.manaStore -= spell.getBaseManaCost();
        return spell.getBaseManaCost() * 2;
    }

    public int castSpell(Object target) {
        // Fallback overload returning flat damage

        return 10;
    }
}