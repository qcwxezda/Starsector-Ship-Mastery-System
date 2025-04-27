# Ship Mastery System
A Starsector mod that allows the player to gain mastery in ship hulls to unlock S-mod slots and additional bonuses.

![Revamped build-in interface](https://raw.githubusercontent.com/qcwxezda/Starsector-Ship-Mastery-System/refs/heads/master/screenshots/hullmod_screen.png)
![Mastery panel](https://raw.githubusercontent.com/qcwxezda/Starsector-Ship-Mastery-System/refs/heads/master/screenshots/mastery_screen.png)

## Ship Masteries

- Gain mastery points (MP) in a specific ship type randomly while gaining XP.
    - XP gained from combat has a chance to grant MP in combat ship types in your fleet.
    - XP gained from any other source has a chance to grant MP in civilian ship types in your fleet.
- MP can also be acquired by consuming knowledge constructs, which appear as rare loot under certain circumstances.
- Use MP to build in hullmods or advance your mastery level in a specific ship type.
    - Each ship type (a.k.a. hull spec) has its own MP pool that is shared between all ships of that type.
    - Mastery level and mastery selections (for levels that have more than one option) are also shared between all ships of
      a ship type.
- Advancing your mastery of a ship type improves its performance in a variety of ways. Some mastery levels have randomized
  bonuses, while others' are fixed.

## Revamped S-mod System

- All ships start with 0 S-mod capacity. Level up ship mastery to gain S-mod capacity.
- Each ship class has exactly 3 available S-mod capacity upgrades, available at levels 3, 6, and 8.
- Building in hullmods no longer costs story points, instead costing mastery points (MP) and credits.

## Skill Changes

- Best of the Best:
![Best of the Best](https://raw.githubusercontent.com/qcwxezda/Starsector-Ship-Mastery-System/refs/heads/master/screenshots/best_of_the_best.png)
  - Removed the additional S-mod from Best of the Best; default cap is now 3 (at max mastery) regardless of skills.
  - Added effect: +30% effectiveness of all mastery bonuses.
  - Added effect: +15% CR, +10% hull and flux capacity for capital ships with officers.


- Cybernetic Augmentation:
![Cybernetic Augmentation](https://raw.githubusercontent.com/qcwxezda/Starsector-Ship-Mastery-System/refs/heads/master/screenshots/cybernetic_augmentation.png)
  - Removed the 1% damage dealt / damage taken bonus per commander's elite skill.
  - Added an ordnance points bonus that scales based on the number of ship types the player has mastered (bonus is fixed for NPC fleets).


## NPC Masteries

- NPC fleets follow the same general rules as player fleets, and may have mastery levels in some ship classes.
- Ships in an NPC fleet that have mastery effects applied will also have a marker hullmod added, so hovering over that
  ship will show its mastery level.
- Adds a post-inflation sequence to add S-mods to ships in generic NPC fleets, if they've unlocked mastery
  levels that grant them S-mod capacity.
- NPC fleets' average mastery levels (and other mastery-related settings) can be changed in the difficulty subsection of the settings.

## Miscellaneous Features

- Adds a system for reclaiming ships lost in battle:
![Derelict Losses](https://raw.githubusercontent.com/qcwxezda/Starsector-Ship-Mastery-System/refs/heads/master/screenshots/derelict_losses.png)
  - Player ships that are lost but not recovered, either by choice or due to disengaging early from or losing a battle, spawn as derelicts near the battle site.
  - In order to spawn as a derelict, the ship must have been recoverable should the player have won (affected by reinforced bulkheads / hull restoration / etc.).
  - If the player is given a post-battle salvage option, weapons and wings from the wrecks are stripped.
  - The wrecks disappear permanently after 365 days, or if the player dismisses the fleet log entry.
  - Greatly facilitates insurance fraud if using Nexerelin's ship insurance feature.
- Adds an option to the refit screen autofit menu to include S-mods from the goal variant:
![S-mods in Autofit](https://raw.githubusercontent.com/qcwxezda/Starsector-Ship-Mastery-System/refs/heads/master/screenshots/smods_in_fit.png)
  - This will attempt to copy S-mods, automatically spending the required credits and mastery points.
- Adds the ability to view and replay recent battles:
![Recent Battles Intel](https://raw.githubusercontent.com/qcwxezda/Starsector-Ship-Mastery-System/refs/heads/master/screenshots/recent_battles_intel.png)
  - Keeps track of the 10 most recent battles you've won, displaying detailed enemy fleet and officer data.
  - Replay battles in a simulation-like environment -- quit anytime, gain nothing, and lose nothing.
  - Solo replay allows fighting against individual ships from recorded battles.
  - Pin notable battles to keep them in the intel log permanently, allowing you to replay limited boss battles at any time.
- Adds a small amount of exploration content.