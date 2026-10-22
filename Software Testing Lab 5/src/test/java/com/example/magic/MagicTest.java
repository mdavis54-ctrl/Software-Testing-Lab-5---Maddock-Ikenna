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
 *                                                                   stance == IDLE
 * C6   W + Archmage(10,100)        same sequence:                    same behavioral/state
 *                                   beginChanneling(), surge 101      results/contract behavior
 * C7   W sender + W recipient      transferMana(100, 1)              sender -100; recipient +100
 * C8   W                         castSpell(Spell) vs               Spell ref: result == 40;
 *                                castSpell(Object)                 Object ref: result == 10
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


    // ============================================================
    // Challenge 5: Yo-Yo Effect Cascade
    // ============================================================
    
    @Test
    @DisplayName("Challenge 5: Archmage high-power surge preserves valid state")
    void archmage_highPowerSurge_preservesValidState() {

        Archmage archmage = new Archmage(10, 100);

        archmage.beginChanneling();

        int result = archmage.spellSurge(101);

        // Verify that the input produced the high-power path.
        assertTrue(
                result > 200,
                "Expected high-power surge to exceed 200"
        );

        /*
         * Because the high-power result causes Archmage.spellSurge()
         * to call rest(), mana should be restored to MAX_MANA.
         */
        assertEquals(
                MAX_MANA,
                archmage.getManaStore()
        );

       
        assertEquals(
                Wizard.Stance.IDLE,
                archmage.getCurrentStance()
        );
    }


    // ============================================================
    // Challenge 6: Dynamic Binding and Polymorphic Substitution
    // ============================================================

   
    @Test
    @DisplayName("Challenge 6: Wizard and Archmage remain behaviorally substitutable")
    void wizardAndArchmage_shouldBehaveConsistently() {

        Wizard wizard = new Wizard(10, 100);
        Wizard archmage = new Archmage(10, 100);

        // Give both objects the same starting state.
        wizard.beginChanneling();
        archmage.beginChanneling();

        int wizardResult = wizard.spellSurge(101);
        int archmageResult = archmage.spellSurge(101);

        assertEquals(
                151,
                wizardResult,
                "Normal Wizard should produce 151 power"
        );

        assertEquals(
                302,
                archmageResult,
                "Archmage should produce 302 power"
        );

        assertEquals(
                Wizard.Stance.CHANNELING,
                wizard.getCurrentStance()
        );

        assertEquals(
                Wizard.Stance.CHANNELING,
                archmage.getCurrentStance()
        );

        assertEquals(
                MAX_MANA - 101,
                wizard.getManaStore()
        );

        assertEquals(
                MAX_MANA,
                archmage.getManaStore(),
                "Archmage incorrectly restores mana through rest()"
        );
    }


    // ============================================================
    // Category III: Message and Parameter Passing Faults
    // ============================================================

    @Test
    @DisplayName("Challenge 7: transferMana() deducts the exact amount")
    void transferMana_deductsExactAmountFromSender() {

        Wizard sender = newWizard();
        Wizard recipient = newWizard();

        int senderInitialMana = sender.getManaStore();
        int recipientInitialMana = recipient.getManaStore();

        int amount = 100;
        int priorityTier = 1;

        sender.transferMana(
                recipient,
                amount,
                priorityTier
        );

        assertAll(
                () -> assertEquals(
                        senderInitialMana - amount,
                        sender.getManaStore(),
                        "Sender should lose exactly the transferred amount"
                ),

                () -> assertEquals(
                        recipientInitialMana + amount,
                        recipient.getManaStore(),
                        "Recipient should gain exactly the transferred amount"
                )
        );
    }


    // ============================================================
    // Challenge 8: Static Overload Resolution
    // ============================================================

    @Test
    @DisplayName("Challenge 8: compile-time type determines overloaded method")
    void castSpell_compileTimeTypeDeterminesOverload() throws Exception {

        Wizard wizard = new Wizard(10, 100);

        Spell spell = new Spell(
                "Fireball",
                20,
                5
        );

        Object spellAsObject = spell;

        int spellResult = wizard.castSpell(spell);
        int objectResult = wizard.castSpell(spellAsObject);

        assertEquals(
                40,
                spellResult,
                "Spell reference should call castSpell(Spell)"
        );

        assertEquals(
                10,
                objectResult,
                "Object reference should call castSpell(Object)"
        );
    }
}
