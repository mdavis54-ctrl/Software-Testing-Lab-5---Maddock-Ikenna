# Test Cases

| Test Case | TCI | Inputs / Steps | Expected Result |
|---|---|---|---|
| **C1** | C1 | Create `Wizard(5, 100)`. Call `beginChanneling()` and `spellSurge(10)` four times. Verify stance is `EXHAUSTED`. Call `rest()`. | Mana is restored to `600` and stance changes from `EXHAUSTED` to `IDLE`. |
| **C2** | C2 | Create `Wizard(5, 100)`. Call `spellSurge(10)` while `IDLE`. Record mana before and after. Then perform three valid channel/surge operations followed by a fourth. | First surge throws `InvalidStateException` and mana remains `600`. After three surges wizard is not exhausted; the fourth surge changes stance to `EXHAUSTED`. |
| **C3a** | C3a | Create `Wizard(5, 100)`. Begin channeling and repeatedly call `spellSurge(600)` while possible. | Mana never becomes less than `0` or greater than `600`. Invalid surge attempts throw an exception without creating an invalid mana value. |
| **C3b** | C3b | Create `Wizard(5, 100)`. Repeatedly call `castSpell(new Spell("Fireball", 20, 1))` until the mana limit is reached. | Mana remains within `0 <= mana <= 600` after every operation. |
| **C3c** | C3c | Create `Wizard(5, 100)`. Alternate `spellSurge(600)` and `castSpell(new Spell("Fireball", 20, 1))` while possible. | Mana remains within `0 <= mana <= 600` after every operation. |
| **C4a** | C4a | Create `Archmage(10, 100)`. Activate the barrier with `toggleBarrier()`. Call `receiveDamage(50)`. | Mana remains non-negative (`mana >= 0`). |
| **C4b** | C4b | Create `Archmage(10, 100)`. Activate the barrier. Call `receiveDamage(600)`. | Mana becomes `0` and never becomes negative. |
| **C4c** | C4c | Create `Archmage(10, 100)`. Activate the barrier. Call `receiveDamage(601)`. | Mana remains non-negative (`mana >= 0`). |
| **C5** | C5 | Create `Archmage(10, 100)`. Call `beginChanneling()`, then `spellSurge(101)`. | Surge produces a result greater than `200`; after the surge, mana is `600` and stance is `IDLE`. |
| **C6** | C6 | Create a `Wizard(10, 100)` and an `Archmage(10, 100)`. For both, call `beginChanneling()` and `spellSurge(101)`. Compare their results, mana, and stance. | Wizard retains `499` mana and `CHANNELING`; Archmage incorrectly restores mana to `600` and changes to `IDLE`. |
| **C7** | C7 | Create sender and recipient `Wizard` objects. Record both mana values. Call `sender.transferMana(recipient, 100, 1)`. | Sender loses exactly `100` mana and recipient gains exactly `100` mana. |
| **C8** | C8 | Create `Spell spell = new Spell("Fireball", 20, 1)`. Call `castSpell(spell)`. Then assign it to `Object objectSpell = spell` and call `castSpell(objectSpell)`. | `Spell` reference uses `castSpell(Spell)` and returns `40`. `Object` reference uses `castSpell(Object)` and returns `10`. |


# Test Case Inputs (TCIs)

**W = `new Wizard(5, 100)`, mana starts at 600**

| ID | Setup | Input | Expected |
|---|---|---|---|
| **C1** | W | 4 × (`beginChanneling()`, `spellSurge(10)`), then `rest()` | Stance is `EXHAUSTED` before `rest()`; mana == 600 and stance == `IDLE` after `rest()` |
| **C2** | W (IDLE) | `spellSurge(10)` without channeling; then 3 channeled surges, then 4th surge | `InvalidStateException`; mana remains 600; after 3 surges not `EXHAUSTED`; 4th surge → `EXHAUSTED` |
| **C3a** | W | `spellSurge(600)`, repeated | `0 <= mana <= 600` after every call |
| **C3b** | W | `castSpell(Fireball, cost 20)`, repeated | `0 <= mana <= 600` after every call |
| **C3c** | W | Alternate `spellSurge(600)` and `castSpell(Fireball, cost 20)` | `0 <= mana <= 600` after every call |
| **C4a** | `Archmage(10, 100)`, barrier active | `receiveDamage(50)` | Mana >= 0 |
| **C4b** | `Archmage(10, 100)`, barrier active | `receiveDamage(600)` | Mana >= 0; mana reaches 0 |
| **C4c** | `Archmage(10, 100)`, barrier active | `receiveDamage(601)` | Mana >= 0 |
| **C5** | `Archmage(10, 100)` | `beginChanneling()`, `spellSurge(101)` | Result > 200; mana == 600; stance == `IDLE` |
| **C6** | W + `Archmage(10, 100)` | Same sequence: `beginChanneling()`, `spellSurge(101)` | Compare behavior; Wizard: mana 499, `CHANNELING`; Archmage: mana 600, `IDLE` |
| **C7** | W sender + W recipient | `transferMana(100, 1)` | Sender -100 mana; recipient +100 mana |
| **C8** | W | `castSpell(Spell)` vs. `castSpell(Object)` | Spell reference → result 40; Object reference → result 10 |
