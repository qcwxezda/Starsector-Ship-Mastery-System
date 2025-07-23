# Ship Mastery System
A Starsector mod that allows the player to gain mastery in ship hulls to unlock S-mod slots and additional bonuses.

## Mastering Ship Classes

![Mastery panel](https://raw.githubusercontent.com/qcwxezda/Starsector-Ship-Mastery-System/refs/heads/master/screenshots/mastery_screen.png)

- Ship classes (a.k.a. hull specs) in your fleet gain mastery XP and can use that XP to level up.
    - Player XP gained from combat grants mastery XP to the ship classes in your fleet that participated.
    - Player XP gained from any other source grants mastery XP to civilian ship classes.
      - For this purpose, civilian ship classes are hull specs that have the CIVILIAN hint, but do not have the CARRIER hint.
- Mastery XP can also be acquired by consuming knowledge constructs, which appear as rare loot under certain circumstances.
- Mastery XP can be used to level up a ship class's mastery, upon which you may select a perk to unlock.
    - Each ship class has its own mastery XP pool and level-up choices that are shared between all ships of that type.
- Advancing your mastery of a ship type improves its performance in a variety of ways. Some mastery levels have randomized bonuses, while others' are fixed.

## Revamped S-mod System

![Revamped build-in interface](https://raw.githubusercontent.com/qcwxezda/Starsector-Ship-Mastery-System/refs/heads/master/screenshots/hullmod_screen.png)

- All ships start with 0 S-mod capacity. Level up ship mastery to gain S-mod capacity.
- Each ship class has exactly 2 available S-mod capacity upgrades, available at levels 2 and 5.
- Building in hullmods costs credits instead of story points.
  - You may still use story points to reduce the credit cost of building in hullmods.

## Mastery Unlocks

- As you progress your mastery level in a ship class, gain additional features that can be accessed via the refit screen.
  - **At level 3**: Unlock the Selective Restoration ability, allowing you to choose which d-mods to keep and which to repair.
  - **At level 4**: Unlock the Knowledge Forge ability, reducing mastery XP gained but generating items that grant mastery XP to any ship class of your choosing.
  - **At level 5**: Unlock the Hull Reversion ability, allowing you to choose which S-mods to keep and which to revert.
  - **At level 7**: Unlock the Refresh Masteries ability, allowing you to reroll randomized mastery perks.
  - **At level 7**: Unlock the AI Core Interface ability, allowing you to install an AI core into a ship to grant an additional, modest bonus.


## NPC Masteries

- NPC fleets follow the same general rules as player fleets, and may have mastery levels in some ship classes.
- Ships in an NPC fleet that have mastery effects applied will also have a marker hullmod added, so hovering over that
  ship will show its mastery level.
- Adds a post-inflation sequence to add S-mods to ships in generic NPC fleets if they've unlocked mastery
  levels that grant them S-mod capacity.
- NPC fleets' average mastery levels can be changed in the difficulty subsection of the settings. Other difficulty options can be accessed via data/shipmastery/faction_difficulty.csv.

## Miscellaneous Features

- Adds a system for reclaiming ships lost in battle:
![Derelict Losses](https://raw.githubusercontent.com/qcwxezda/Starsector-Ship-Mastery-System/refs/heads/master/screenshots/derelict_losses.png)
  - Player ships that are lost but not recovered, either by choice or due to disengaging early from or losing a battle, spawn as derelicts near the battle site.
  - To spawn as a derelict, the ship must have been recoverable should the player have won (affected by reinforced bulkheads / hull restoration / etc.).
  - If the player is given a post-battle salvage option, weapons and wings from the wrecks are stripped.
  - The wrecks disappear permanently after 365 days, or if the player dismisses the fleet log entry.
  - Greatly facilitates insurance fraud if using Nexerelin's ship insurance feature.
- Adds an option to the refit screen autofit menu to include S-mods from the goal variant:
![S-mods in Autofit](https://raw.githubusercontent.com/qcwxezda/Starsector-Ship-Mastery-System/refs/heads/master/screenshots/smods_in_fit.png)
  - This will attempt to copy S-mods, automatically spending the required credits.
- Adds the ability to view and replay recent battles:
![Recent Battles Intel](https://raw.githubusercontent.com/qcwxezda/Starsector-Ship-Mastery-System/refs/heads/master/screenshots/recent_battles_intel.png)
  - Keeps track of the 10 most recent battles you've won, displaying detailed enemy fleet and officer data.
  - Replay battles in a simulation-like environment -- quit anytime, gain nothing, and lose nothing.
  - Solo replay allows fighting against individual ships from recorded battles.
  - Pin notable battles to keep them in the intel log permanently, allowing you to replay limited boss battles at any time.
- Adds some exploration content.

## Mod Integration and Advanced Config

### Modifying and Adding Mastery Effects

- All possible mastery effects are read from data/shipmastery/mastery_list.csv.
  - The ModifyStatsMult and ModifyStatsFlat effects take an additional parameter read from data/shipmastery/stats_list.csv.
- Mastery effects are assigned to ship classes via data/shipmastery/mastery_presets.json and data/shipmastery/mastery_assignments.json.
  - Presets affect groups of ship classes, while assignments apply to single ship classes.
  - Both assignments and presets can define a parent preset. 
  - The \_DEFAULT\_ preset is a fallback preset that acts as the parent of every ship class without an explicitly defined preset.
    - Modifying this preset will generally affect all ship classes. You can add or remove mastery levels by modifying "maxLevel" and adding or removing the corresponding level JSON objects. See mastery_list.csv for mastery effect initialization syntax.
- To add specific, fixed mastery effects for a ship class, add an entry for that ship class in mastery_assignments.json. You can use an existing effect, or create an entirely new effect for this.
  - All mastery effects should extend shipmastery.mastery.BaseMasteryEffect.
  - The interface shipmastery.mastery.MasteryEffect contains additional information in its documentation.

### Mastery Aliasing

- By default, all hull specs have their own mastery pool, including skins of other hull specs.
- It's possible to force a hull spec to share a mastery pool with another by modifying mastery_aliases.json.

### Custom AI Core Interfaces

- By default, only vanilla AI cores (4 in total) and all Pseudocores (7 in total) can be installed into ships to grant additional bonuses.
- You can add interfaces for custom AI cores by adding an entry in data/shipmastery/ai_core_interface_list.csv. 
  - The plugin field should point to your custom plugin, which should implement shipmastery.aicoreinterface.AICoreInterfacePlugin.
  - The methods in AICoreInterfacePlugin act the same as hullmods, with the additional feature that they are applied after all other hullmod effects have been applied.