package com.example.magic;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.magic.Wizard.Stance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Lab 5: Object Oriented Testing (Challenges 1-4). Black-box: only public members are
 * used; the private surge counter is judged by its effects (when the wizard exhausts).
 *
 * TEST CASE INPUTS (TCIs)   [W = new Wizard(5, 100), mana starts at 600]
 * ------------------------------------------------------------------------------------
 * ID   Setup                     Input                              Expected
 * C1   W                         4 x (beginChanneling, surge 10)    stance EXHAUSTED (precondition)
 *                                then rest()                        mana == 600, stance == IDLE
 * C2   W (IDLE)                  spellSurge(10), no channeling      InvalidStateException; mana == 600;
 *                                                                   then 3 channeled surges -> not
 *                                                                   EXHAUSTED, 4th -> EXHAUSTED
 * C3a  W                         surge 600, repeated                0 <= mana <= 600 after every call
 * C3b  W                         castSpell(Fireball cost 20) rep.   0 <= mana <= 600 after every call
 * C3c  W                         (surge 600, cast cost 20) repeated 0 <= mana <= 600 after every call
 * C4a  Archmage(10,100), barrier damage 50                          mana >= 0   (below mana: valid)
 * C4b  Archmage(10,100), barrier damage 600                         mana >= 0   (boundary: all mana)
 * C4c  Archmage(10,100), barrier damage 601                         mana >= 0   (just above mana)
 */
class SpellcastingContractTest {

    private static final int MAX_MANA = Spellcaster.MAX_MANA_STORE;
    private static final int SAFE_INVESTMENT = 10;   // within safe-cast range for level 5
    private static final int SURGES_TO_EXHAUST = 4;

    // ---------------------------------------------------------------------
    // Category I: State control faults
    // ---------------------------------------------------------------------

    /** TCI C1. */
    @Test
    @DisplayName("Challenge 1: rest() restores full mana and leaves EXHAUSTED for IDLE")
    void rest_whenExhausted_resetsStanceToIdle() {
        Wizard wizard = newWizard();
        surge(wizard, SURGES_TO_EXHAUST);
        assertEquals(Stance.EXHAUSTED, wizard.getCurrentStance()); // precondition

        wizard.rest();

        assertAll(
                () -> assertEquals(MAX_MANA, wizard.getManaStore()),
                () -> assertEquals(Stance.IDLE, wizard.getCurrentStance()));
    }

    /**
     * TCI C2. The surge counter is private, so "not mutated" is checked by its effect:
     * if the rejected call had counted as a surge, the wizard would exhaust one surge early.
     */
    @Test
    @DisplayName("Challenge 2: spellSurge() without channeling throws and leaves no trace")
    void spellSurge_withoutChanneling_throwsAndLeavesStateUnchanged() {
        Wizard wizard = newWizard();

        assertThrows(InvalidStateException.class, () -> wizard.spellSurge(SAFE_INVESTMENT));
        assertEquals(MAX_MANA, wizard.getManaStore());

        surge(wizard, SURGES_TO_EXHAUST - 1);
        assertNotEquals(Stance.EXHAUSTED, wizard.getCurrentStance());
        surge(wizard, 1);
        assertEquals(Stance.EXHAUSTED, wizard.getCurrentStance());
    }

    /**
     * TCI C3a-c. S = channel + surge(600), C = cast a cost-20 spell; the sequence is repeated.
     * Surging 600 uses all mana (a valid boundary) and later surges must be rejected.
     */
    @ParameterizedTest(name = "drain sequence \"{0}\" never breaks 0 <= mana <= 600")
    @ValueSource(strings = {"S", "C", "SC"})
    @DisplayName("Challenge 3: mana invariant holds under repeated drain")
    void manaInvariant_holdsUnderRepeatedDrain(String sequence) {
        Wizard wizard = newWizard();
        Spell fireball = new Spell("Fireball", 20, 1);

        for (char op : sequence.repeat(30).toCharArray()) {
            try {
                if (op == 'S') {
                    wizard.beginChanneling();
                    wizard.spellSurge(MAX_MANA);
                } else {
                    wizard.castSpell(fireball);
                }
            } catch (WizardException expected) {
                // Rejecting the operation is the correct way to protect the invariant.
            }
            int mana = wizard.getManaStore();
            assertTrue(mana >= 0 && mana <= MAX_MANA, "mana out of range: " + mana);
        }
    }

    // ---------------------------------------------------------------------
    // Category II: Inheritance and polymorphism faults
    // ---------------------------------------------------------------------

    /** TCI C4a-c: damage below, equal to, and just above the Archmage's starting mana of 600. */
    @ParameterizedTest(name = "barrier absorbing {0} damage keeps mana >= 0")
    @ValueSource(ints = {50, 600, 601})
    @DisplayName("Challenge 4: Archmage barrier must not violate the Spellcaster lower bound")
    void archmageBarrier_neverDrivesManaNegative(int damage) {
        Archmage archmage = new Archmage(10, 100);
        archmage.toggleBarrier();
        Spellcaster wizard = archmage; // polymorphic reference

        wizard.receiveDamage(damage);

        assertTrue(wizard.getManaStore() >= 0, "mana went negative: " + wizard.getManaStore());
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static Wizard newWizard() {
        return new Wizard(5, 100);
    }

    /** Performs 'count' consecutive channeled surges through the public interface. */
    private static void surge(Wizard w, int count) {
        for (int i = 0; i < count; i++) {
            w.beginChanneling();
            w.spellSurge(SAFE_INVESTMENT);
        }
    }
}