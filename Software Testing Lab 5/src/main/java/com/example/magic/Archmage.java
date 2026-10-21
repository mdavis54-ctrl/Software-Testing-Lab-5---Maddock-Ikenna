package com.example.magic;

public class Archmage extends Wizard {
    private boolean barrierActive = false;

    public Archmage(int level, int maxHp) throws HeroLevelException {
        super(level, maxHp);
    }

    @Override
    public double getPowerMultiplier() {
        return 2.0;
    }

    // LSP Violation & Hidden Inheritance Coupling
    @Override
    public void receiveDamage(int damage) {
        if (barrierActive) {
            // FAULT: Consumes manaStore without checking if manaStore >= damage,
            // allowing manaStore to drop below 0 (violating superclass invariant: manaStore >= 0)
            this.manaStore -= damage;
        } else {
            super.receiveDamage(damage);
        }
    }

    public void toggleBarrier() {
        this.barrierActive = !this.barrierActive;
    }

    // Yo-Yo Effect & Side-effect Overriding Fault
    @Override
    public int spellSurge(int manaInvested) throws WizardException {
        int result = super.spellSurge(manaInvested);
        // FAULT: Arbitrarily mutates state after calling super, breaking stance synchronization
        if (result > 200) {
            this.rest(); // Calls rest(), which triggers the unfixed rest() state bug
        }
        return result;
    }
}