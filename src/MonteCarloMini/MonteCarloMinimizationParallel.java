package MonteCarloMini;

import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

// RecursiveAction if no return
// RecursiveTask if it returns something
public class MonteCarloMinimizationParallel extends RecursiveAction {
	static final boolean DEBUG = true;

	int min = Integer.MAX_VALUE;
	int local_min = Integer.MAX_VALUE;
	int finder = -1;
	Search[] searches;
	int num_searches;

	int rows, columns;
	double xmin, xmax, ymin, ymax;
	TerrainArea terrain;
	double searches_density;

	MonteCarloMinimizationParallel(int min, int local_min, int finder, Search[] searches, int rows, int columns,
			double xmin, double xmax, double ymin, double ymax, TerrainArea terrain, double searches_density) {
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
				searches_main, rows, columns, xmin, xmax, ymin, ymax, terrain, searches_density);
		ForkJoinPool pool = ForkJoinPool.commonPool();
		pool.invoke(doWork);

		// end timer
		tock();

		if (DEBUG) {
			/* print final state */
			terrain.print_heights();
			terrain.print_visited();
		}

		System.out.printf("Run parameters\n");
		System.out.printf("\t Rows: %d, Columns: %d\n", rows, columns);
		System.out.printf("\t x: [%f, %f], y: [%f, %f]\n", xmin, xmax, ymin, ymax);
		System.out.printf("\t Search density: %f (%d searches)\n", searches_density, num_searches);

		/* Total computation time */
		System.out.printf("Time: %d ms\n", endTime - startTime);
		int tmp = terrain.getGrid_points_visited();
		System.out.printf("Grid points visited: %d  (%2.0f%s)\n", tmp, (tmp / (rows * columns * 1.0)) * 100.0, "%");
		tmp = terrain.getGrid_points_evaluated();
		System.out.printf("Grid points evaluated: %d  (%2.0f%s)\n", tmp, (tmp / (rows * columns * 1.0)) * 100.0, "%");

		/* Results */
		System.out.printf("Global minimum: %d at x=%.1f y=%.1f\n\n", min,
				terrain.getXcoord(searches_main[finder].getPos_row()),
				terrain.getYcoord(searches_main[finder].getPos_col()));

	}

	@Override
	protected void compute() {
		// Assuming FJP.invoke would call compute for all the threads

		for (int i = 0; i < num_searches; i++) {
			local_min = searches[i].find_valleys();
			if ((!searches[i].isStopped()) && (local_min < min)) { // don't look at those who stopped because hit
																	// exisiting path
				min = local_min;
				finder = i; // keep track of who found it
			}
			if (DEBUG)
				System.out.println("Search " + searches[i].getID() + " finished at  " + local_min + " in "
						+ searches[i].getSteps());
		}
		// end timer
	}
}