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
    * Attacks that attack with equal or less troops than the enemy has to defend with, unless there are no other good attacks left. Then we allow attacks on territories where we have at least 3 more total troops available than the opponent
    * Reinforcements that reinforce territories that aren't near an enemy
    * Fortifications that fortify a territory that is further away from the enemy than the fortifying territory is
    * Endphase actions that occur even though there are still good attacks or good fortifications to be done
* Additionally to further reduce the width of the MCTS-Tree we decided to group all actions with the same sourceId and targetId togheter and represent them as two actions. One with the highest value and one with half of the highest value (rounded down) among said actions.
* Regarding simulation depth, we implemented a total runs per round limit at 1400 (a number we tested and it seemed good) and then we divide that limit by the total actions in our current tree layer (so at the start of the game when there are 42 possible actions, we would divide 1400 by 42).

# Remarks
* Note that some of the above mentioned rewards are set to 0 in this version of Trisker. This is because we found the agent to work better with those options "turned of". We chose to still keep the code for these cases to showcase different scenarios we considered and keep the possiblity of changing these values in the future. They might be more useful with the right balance between the exploration factor and the other reward factors, but we did not have time to test this in depth.
* We also implemented an event-logging system and a website that can read these logs and display a map with the current game state. We kept this in the final submission because it helped us a lot during development and testing (and it took quite a lot of time to make...). You can find more information in the install.md file.
