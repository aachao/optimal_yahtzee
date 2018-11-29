# optimal_yahtzee

### Yahtzee

In Yahtzee, players attempt to score the highest number of points by rolling 5 dice through 13 rounds. There are 13 categories, each with different rules for scoring, and one must be filled at the end of each round. Each round allows 3 dice rolls. After each roll, the player may choose a subset of the dice to hold or not roll for the subsequent roll.

### Approach

In this dynamic programming approach, we envision the progression of the game as a search space, in which nodes represent states of the game and edges represent user actions or random possibilities from rolling the dice. Once all states of the game can be computed, along with all possible actions and consequences, we use the information from our final states (when only one category is unfilled) to work backwards to our initial state (when all categories are unfilled).

We will keep track of the maximum score of the following state and weigh it according to the probability of reaching the state from a preceding state, allowing us to compute an expected value for each state.

To optimize efficiency, we preprocess the expected vales for all base cases (powerset of unfilled categories). We serialize this data locally, so it does not need to be recomputed for every run.

### Usage

First, edit the 2 file paths in Strategy (bestHold and bestCategory) to indicate where to locally serialize base case events.
To compute and serialize base case events, run saveTables().
To load base case events, run loadTables().
To receive optimal strategy, run input().