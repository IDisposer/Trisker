# Trisker
Used for creating a Risk AI for the stragety game programming course. Made by Maximillian Gutschier and Christian Pirngruber.

# Ideas
* We deemed our original idea of having two different MCTS-Trees for the initial placing phase and the other phases as not very useful, so we did not implement it. 
* We decided to implement a reward based system, where the value given at the end of a simulation (meaning the mcts phase) is determined by the actions taken during the simulation. We give a bigger weight to actions taken earlier in the simulation, because these have a higher chance of actually happening than the later ones.

# Heuristics
* Reward for picking small continents, because those are easier to conquer and hold
* Reward for attacking with one more unit, more than one more unit, than the enemy defends with
* Reward for stationing troops near enemies and extra reward for afterwards having more units than all neighbouring enemy territories combined
* Reward for occupying continents (each newly occupied territory of a continent gives more rewards than the last)
* Reward for moving troops closer to the enemy in the fortifying phase
* Pruning of the following actions to reduce the width of the MCTS-Tree:
    * Attacks that attack with equal or less troops than the enemy has to defend with
    * Reinforcements that reinforce territories that aren't near an enemy
    * Fortifications that fortify a territory that is further away from the enemy than the fortifying territory is
    * Endphase actions that occur even though there are still good attacks or good fortifications to be done
* Additionally to further reduce the width of the MCTS-Tree we decided to group all actions with the same sourceId and targetId togheter and represent them as two actions. One with the highest value and one with half of the highest value (rounded down) among said actions.


