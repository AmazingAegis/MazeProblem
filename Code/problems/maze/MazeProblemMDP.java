package problems.maze;

import java.util.ArrayList;
import java.util.Collection;

import learning.*;
import visualization.*;
import utils.*;

/** 
 * Implements the maze problem as a Markov Decision Process.
 */
public class MazeProblemMDP extends MDPLearningProblem  implements MazeProblem, ProblemVisualizable{
	
	/** Size of the problem. Default value is 10.*/
	protected int size = 10;
	
	/** Maze */
	protected Maze maze;
	
	/** Constructors */
	public MazeProblemMDP(){
		this.maze = new Maze(size,0);
		initialState = new MazeState(maze.posHamster.x, maze.posHamster.y);
	}
	public MazeProblemMDP(int size){
		this.size = size;
		this.maze = new Maze(size, 0);
	}
	public MazeProblemMDP(int size, int seed){
		this.size = size;
		this.maze = new Maze(size,seed);
		initialState = new MazeState(maze.posHamster.x, maze.posHamster.y);
	}
	public MazeProblemMDP(Maze maze){
		this.size = maze.size;
		this.maze = maze;
		initialState = new MazeState(maze.posHamster.x, maze.posHamster.y);
	}	
	
	/** Generates a random instance of the problem given the seed. */
	private void generateInstance(int size, int seed) {
		this.size = size;
		this.maze = new Maze(size,seed);
	    initialState = new MazeState(maze.posHamster.x, maze.posHamster.y);
	}		
	
	/** Returns a reference to the maze */
	public Maze getMaze(){
		return this.maze;
	}
	
	// Inherited from Learning Problem
	
	/** Whether the state corresponds to a final state (CAT or CHEESE).*/
	@Override
	public boolean isFinal(State state) {
		MazeState mState = (MazeState)state;
		if(mState.position.equals(maze.posCheese)){
			return true;
		}
		for (Position posCat : maze.posCats) {
			if(mState.position.equals(posCat)){
				return true;
			}
		}
		return false;
	}

	/** Returns the set of actions that can be done at each step. */
	@Override
	public ArrayList<Action> getPossibleActions(State state) {
		MazeState mState = (MazeState) state;
		ArrayList<Action> possibleActions = new ArrayList<Action>();
		Position pos = mState.position;
		if(pos.x+1<maze.size) {
			if(maze.cells[pos.x+1][pos.y]!=Maze.WALL) {
				possibleActions.add(MazeAction.RIGHT);
			}
		}
		if(pos.x-1>=0) {
			if(maze.cells[pos.x-1][pos.y]!=Maze.WALL) {
				possibleActions.add(MazeAction.LEFT);
			}
		}
		if(pos.y+1<maze.size) {
			if(maze.cells[pos.x][pos.y+1]!=Maze.WALL) {
				possibleActions.add(MazeAction.DOWN);
			}
		}
		if(pos.y-1>=0) {
			if(maze.cells[pos.x][pos.y-1]!=Maze.WALL) {
				possibleActions.add(MazeAction.UP);
			}
		}
		if(maze.cells[pos.x][pos.y]==Maze.HOLE) {
			possibleActions.add(MazeAction.DIVE);
		}
		return possibleActions;
	}	
	
	/** Returns the reward of an state. */	
	@Override
	public double getReward(State state){
		double reward=0;
		MazeState mState = (MazeState) state;
		if(maze.cells[mState.position.x][mState.position.y]==Maze.CAT)
			reward=-100;
		if(maze.cells[mState.position.x][mState.position.y]==Maze.CHEESE)
			reward=100;
		return reward;
	}	
	
	/**  In this case, the transition reward penalizes distance. */	
	@Override
	public double getTransitionReward(State fromState, Action action, State toState) {
		double reward = 0;
		reward = euclideanDistance(((MazeState)fromState).position, ((MazeState)toState).position);
		if((MazeAction)action==MazeAction.DIVE)
			reward=reward/2;
		if(maze.cells[((MazeState)fromState).position.x][((MazeState)fromState).position.y]==Maze.WATER)
			reward=reward*2;
		return -reward;
	}	
	
	public double euclideanDistance(Position start, Position finish) {
		return Math.sqrt(Math.pow(start.x-finish.x, 2) + Math.pow(start.y-finish.y, 2));
	}
	
	// From MDPLearningProblem
	
	/** Returns a collection with all possible states. */
	@Override
	public Collection<State> getAllStates() {
		ArrayList<State> allStates = new ArrayList<State>();
		for (int x=0;x<maze.size;x++)
			for(int y=0;y<maze.size;y++) 
				if (maze.cells[x][y]!=Maze.WALL)
					allStates.add(new MazeState(x, y));
		return allStates;
	}		
	
	/** Provides access to the action transition model for a pair state/action */ 
	public StateActionTransModel getTransitionModel(State state, Action action){
		return mazeTransitionModel(state, action); 
	}
	
	/** Generates the transition model for a certain state in this particular problem. 
	  * Assumes that the  action can be applied. This method is PRIVATE.*/
	private StateActionTransModel mazeTransitionModel(State state, Action action){
		// Structures contained by the transition model.
		State[] reachable;
		double[] probs;	
		
		// Coordinates of the current state
		int fromX,fromY;
		fromX = ((MazeState) state).X();
		fromY = ((MazeState) state).Y();	
		
		/* First considers diving. */
		if (action==MazeAction.DIVE){
			// It must be a hole. Gets the outputs.
			Position inputHolePos = new Position(fromX,fromY);
			// It considers all holes but one
			reachable = new State[maze.numHoles-1];
			probs = new double[maze.numHoles-1];
			int holeNum=0;
			for (Position holePos: maze.holeList){
				if (holePos.equals(inputHolePos))
					continue;
				reachable[holeNum]=new MazeState(holePos);
				probs[holeNum++] = 1.0/(maze.numHoles-1);
			}
			// Returns 
			return new StateActionTransModel(reachable, probs);
		}
		
		
		/* Otherwise it is a simple movement.*/
		
		// Considers first it must count all reachable positions.
		int numReachablePos = 0;
		if ((fromY>0) && (maze.cells[fromX][fromY-1]!=Maze.WALL)) numReachablePos++;	           //UP
		if ((fromY<maze.size-1) && (maze.cells[fromX][fromY+1]!=Maze.WALL)) numReachablePos++;  //DOWN	
		if ((fromX>0) && (maze.cells[fromX-1][fromY]!=Maze.WALL)) numReachablePos++;            //LEFT
		if ((fromX<maze.size-1) && (maze.cells[fromX+1][fromY]!=Maze.WALL)) numReachablePos++;  //RIGHT
		
		// Creates the transition model.
		reachable = new State[numReachablePos];
		probs = new double[numReachablePos];
		
		// Probability of error 0.1 times each position.
		double probError = 0.1;
		double probSuccess = 1.0 - probError*(numReachablePos-1);
		
		int ind=0;
		if ((fromY>0) && (maze.cells[fromX][fromY-1]!=Maze.WALL)) { // UP
			reachable[ind] = new MazeState(fromX,fromY-1);
			if (action==MazeAction.UP)
				probs[ind]=probSuccess;
			else
				probs[ind]=probError;
			ind++;
		}
		
		if ((fromY<maze.size-1) && (maze.cells[fromX][fromY+1]!=Maze.WALL)) { // DOWN
			reachable[ind] = new MazeState(fromX,fromY+1);
			if (action==MazeAction.DOWN)
				probs[ind]=probSuccess;
			else
				probs[ind]=probError;
			ind++;
		}
		
		if ((fromX>0) && (maze.cells[fromX-1][fromY]!=Maze.WALL)) { // LEFT
			reachable[ind] = new MazeState(fromX-1,fromY);
			if (action==MazeAction.LEFT)
				probs[ind]=probSuccess;
			else
				probs[ind]=probError;
			ind++;
		}
		
		if ((fromX<maze.size-1) && (maze.cells[fromX+1][fromY]!=Maze.WALL)) { // RIGHT
			reachable[ind] = new MazeState(fromX+1,fromY);
			if (action==MazeAction.RIGHT)
				probs[ind]=probSuccess;
			else
				probs[ind]=probError;
			ind++;
		}
		
		// Returns 
		return new StateActionTransModel(reachable, probs);
	}	
	
	// Utilities
	
	/** Returns a random state. */
	@Override
	public State getRandomState() {
		// Returns only positions corresponding to empty cells.
		int posX, posY;
		boolean validCell = false;
		do{
			posX = Utils.random.nextInt(size);
			posY = Utils.random.nextInt(size);
			// Walls are not valid states. 
			if (maze.cells[posX][posY]==Maze.WALL)
				continue;
			// Sometimes (not very often) there are empty cells surrounded
			// by walls or by the limit of the maze.  Test that there is at least
			// an adjacent position to move.
			if (posX>0  && maze.cells[posX-1][posY]==Maze.EMPTY) validCell = true;
			if (posX<maze.size-1  && maze.cells[posX+1][posY]==Maze.EMPTY) validCell = true;
			if (posY>0  && maze.cells[posX][posY-1]==Maze.EMPTY) validCell = true;
			if (posY>maze.size-1  && maze.cells[posX][posY+1]==Maze.EMPTY) validCell = true;
		} while (!validCell);
		return new MazeState(posX,posY);
	}
	

	/** Generates and instance of the problem.*/
	@Override
	public void setParams(String[] params) {
		try {
			if (params.length==1) generateInstance(Integer.parseInt(params[0]), 0);
			else generateInstance(Integer.parseInt(params[0]), Integer.parseInt(params[1]));		
		}
		catch (Exception E) {
			System.out.println("There has been an error while generating the na new instance of MazeProblem.");
		}
	}	
	
	/* Visualization */
	
	/** Returns a panel with the view of the problem. */
	public ProblemView getView(int sizePx){
		MazeView mazeView = new MazeView(this, sizePx);
		return mazeView;
	}
	
	/* Test */
	public static void main(String[] args) {
		MazeProblemMF mazeProblem = new MazeProblemMF(15, 5);
//		ProblemVisualization mainWindow = new ProblemVisualization(mazeProblem,600);		// Main window
		
		State currentState = mazeProblem.initialState();
		System.out.println("Current state: "+currentState);
		System.out.println("Reward: "+mazeProblem.getReward(currentState));
		System.out.println("Is final: "+mazeProblem.isFinal(currentState));
		
		System.out.println("Possible actions: ");
		ArrayList<Action> actions = mazeProblem.getPossibleActions(currentState);
		for (Action action: actions)
			System.out.println("\t"+action);
		
		System.out.println("Apply action UP.\n");
		State newState = mazeProblem.applyAction(currentState, MazeAction.UP);
		System.out.println("New state: "+newState);
		System.out.println("Transition reward:"+ mazeProblem.getTransitionReward(currentState, MazeAction.UP, newState));
	}
}
