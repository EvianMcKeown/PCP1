package MonteCarloMini;

import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

// RecursiveAction if no return
// RecursiveTask if it returns something
public class MonteCarloMinimizationParallel extends RecursiveAction {
	static final boolean DEBUG = false;

	int min = Integer.MAX_VALUE;
	int local_min = Integer.MAX_VALUE;
	int finder = -1;
	Search[] searches;
	int num_searches;

	int rows, columns;
	double xmin, xmax, ymin, ymax;
	TerrainArea terrain;
	double searches_density;

	int searchfrom;
	int searchto;

	MonteCarloMinimizationParallel(int min, int local_min, int finder, Search[] searches, int rows, int columns,
			double xmin, double xmax, double ymin, double ymax, TerrainArea terrain, double searches_density,
			int searchfrom, int searchto) {
		this.min = min;
		this.local_min = local_min;
		this.finder = finder;
		this.searches = searches;
		this.num_searches = searches.length;

		this.rows = rows;
		this.columns = columns;
		this.xmin = xmin;
		this.xmax = xmax;
		this.ymin = ymin;
		this.ymax = ymax;
		this.terrain = terrain;
		this.searches_density = searches_density;

		this.searchfrom = searchfrom;
		this.searchto = searchto;
	}

	MonteCarloMinimizationParallel(int min, int local_min, int finder, Search[] searches, TerrainArea terrain,
			int searchfrom, int searchto) {
		this.min = min;
		this.local_min = local_min;
		this.finder = finder;
		this.searches = searches;
		this.num_searches = searches.length;

		this.searchfrom = searchfrom;
		this.searchto = searchto;
	}

	static long startTime = 0;
	static long endTime = 0;

	// timers - note milliseconds
	private static void tick() {
		startTime = System.currentTimeMillis();
	}

	private static void tock() {
		endTime = System.currentTimeMillis();
	}

	public static void main(String[] args) {

		int rows, columns; // grid size
		double xmin, xmax, ymin, ymax; // x and y terrain limits
		TerrainArea terrain; // object to store the heights and grid points visited by searches
		double searches_density; // Density - number of Monte Carlo searches per grid position - usually less
									// than 1!

		int num_searches; // Number of searches
		// Array of searches
		Random rand = new Random(); // the random number generator

		if (args.length != 7) {
			System.out.println("Incorrect number of command line arguments provided.");
			System.exit(0);
		}
		/* Read argument values */
		rows = Integer.parseInt(args[0]);
		columns = Integer.parseInt(args[1]);
		xmin = Double.parseDouble(args[2]);
		xmax = Double.parseDouble(args[3]);
		ymin = Double.parseDouble(args[4]);
		ymax = Double.parseDouble(args[5]);
		searches_density = Double.parseDouble(args[6]);

		if (DEBUG) {
			/* Print arguments */
			System.out.printf("Arguments, Rows: %d, Columns: %d\n", rows, columns);
			System.out.printf("Arguments, x_range: ( %f, %f ), y_range( %f, %f )\n", xmin, xmax, ymin, ymax);
			System.out.printf("Arguments, searches_density: %f\n", searches_density);
			System.out.printf("\n");
		}

		// Initialize
		terrain = new TerrainArea(rows, columns, xmin, xmax, ymin, ymax);
		num_searches = (int) (rows * columns * searches_density);
		Search[] searches_main = new Search[num_searches];
		for (int i = 0; i < num_searches; i++)
			searches_main[i] = new Search(i + 1, rand.nextInt(rows), rand.nextInt(columns), terrain);

		if (DEBUG) {
			/* Print initial values */
			System.out.printf("Number searches: %d\n", num_searches);
			// terrain.print_heights();
		}

		// ONLY PARALLELISE CODE FROM TIMER ONWARDS

		// start timer
		tick();

		// all searches
		int min = Integer.MAX_VALUE;
		int local_min = Integer.MAX_VALUE;
		int finder = -1;

		/*
		 * for (int i = 0; i < num_searches; i++) {
		 * local_min = searches[i].find_valleys();
		 * if ((!searches[i].isStopped()) && (local_min < min)) { // don't look at those
		 * who stopped because hit
		 * // exisiting path
		 * min = local_min;
		 * finder = i; // keep track of who found it
		 * }
		 * if (DEBUG)
		 * System.out.println("Search " + searches[i].getID() + " finished at  " +
		 * local_min + " in "
		 * + searches[i].getSteps());
		 * }
		 */

		MonteCarloMinimizationParallel doWork = new MonteCarloMinimizationParallel(min, local_min, finder,
				searches_main, rows, columns, xmin, xmax, ymin, ymax, terrain, searches_density, 0,
				searches_main.length);
		// WIP
		final ForkJoinPool fjPool = new ForkJoinPool();
		fjPool.invoke(doWork);

		// end timer
		tock();

		if (DEBUG) {
			/* print final state */
			doWork.terrain.print_heights();
			doWork.terrain.print_visited();
		}

		System.out.printf("Run parameters\n");
		System.out.printf("\t Rows: %d, Columns: %d\n", doWork.rows, doWork.columns);
		System.out.printf("\t x: [%f, %f], y: [%f, %f]\n", doWork.xmin, doWork.xmax, doWork.ymin, doWork.ymax);
		System.out.printf("\t Search density: %f (%d searches)\n", doWork.searches_density, doWork.num_searches);

		/* Total computation time */
		System.out.printf("Time: %d ms\n", endTime - startTime);
		int tmp = doWork.terrain.getGrid_points_visited();
		System.out.printf("Grid points visited: %d  (%2.0f%s)\n", tmp, (tmp / (doWork.rows * doWork.columns * 1.0)) * 100.0, "%");
		tmp = doWork.terrain.getGrid_points_evaluated();
		System.out.printf("Grid points evaluated: %d  (%2.0f%s)\n", tmp, (tmp / (doWork.rows * doWork.columns * 1.0)) * 100.0, "%");

		/* Results */
		System.out.printf("Global minimum: %d at x=%.1f y=%.1f\n\n", doWork.min,
				terrain.getXcoord(searches_main[doWork.finder].getPos_row()),
				terrain.getYcoord(searches_main[doWork.finder].getPos_col()));

	}

	@Override
	protected void compute() {
		// return the min ?

		/*
		 * for (int i = 0; i < num_searches; i++) {
		 * local_min = searches[i].find_valleys();
		 * if ((!searches[i].isStopped()) && (local_min < min)) { // don't look at those
		 * who stopped because hit
		 * // exisiting path
		 * min = local_min;
		 * finder = i; // keep track of who found it
		 * }
		 * if (DEBUG)
		 * System.out.println("Search " + searches[i].getID() + " finished at  " +
		 * local_min + " in "
		 * + searches[i].getSteps());
		 * }
		 */

		// must call constructor to call recursive computes
		// additionally define 'base case'
		int sLength = searchto - searchfrom;
		if (sLength <= 10) {
			for (int i = searchfrom; i < searchfrom + sLength; i++) {
				local_min = searches[i].find_valleys();
				if ((!searches[i].isStopped()) && (local_min < min)) {
					this.min = local_min;
					this.finder = i;
					// ISSUE finder ID is of the shortened searches not the original
					
				}
				if (DEBUG)
					System.out.println("Search " + searches[i].getID() + " finished at  " + local_min + " in "
							+ searches[i].getSteps());
			}
		} else {
			// calculate new bounds or shorten and pass searches array? (probably slower)
			int leftFrom = searchfrom;
			int rightFrom = (int) ((searchfrom + searchto) / 2);
			int leftTo = (int) ((searchfrom + searchto) / 2);
			int rightTo = (int) ((searchto));

			/*
			 * Search[] leftSearches = new Search[Math.round(searches.length / 2)];
			 * Search[] rightSearches = new Search[searches.length -
			 * Math.round(searches.length / 2)];
			 * 
			 * for (int i = 0; i < leftSearches.length; i++) {
			 * leftSearches[i] = searches[i];
			 * }
			 * for (int i = leftSearches.length; i < rightSearches.length +
			 * leftSearches.length - 1; i++) {
			 * rightSearches[i - leftSearches.length] = searches[i];
			 * }
			 */

			// MonteCarloMinimizationParallel left = new MonteCarloMinimizationParallel(min,
			// local_min, finder, searches,
			// terrain, leftFrom, leftTo);
			MonteCarloMinimizationParallel left = new MonteCarloMinimizationParallel(min, local_min, finder, searches,
					rows, columns, xmin, xmax, ymin, ymax, terrain, searches_density, leftFrom, leftTo);
			//MonteCarloMinimizationParallel right = new MonteCarloMinimizationParallel(min, local_min, finder, searches,
			//		terrain, rightFrom, rightTo);
			MonteCarloMinimizationParallel right = new MonteCarloMinimizationParallel(min, local_min, finder, searches,
					rows, columns, xmin, xmax, ymin, ymax, terrain, searches_density, rightFrom, rightTo);
			// TODO set bounds above
			left.fork();
			right.compute();
			left.join();
			// unsure about the min thing
			this.min = Math.min(left.min, right.min);
			if (left.min == this.min) {
				this.finder = left.finder;
			} else if (right.min == this.min) {
				this.finder = right.finder;
			} 
		}

		// end timer

		// System.out.println(this.min);
		// System.out.println(min);
		// System.out.println(local_min);
		// return this.min;

		// or localmin?
	}
}