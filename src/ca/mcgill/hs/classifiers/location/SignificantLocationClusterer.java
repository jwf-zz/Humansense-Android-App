package ca.mcgill.hs.classifiers.location;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.Ostermiller.util.CSVPrinter;

public class SignificantLocationClusterer {

	// Delta from the paper. This value represents the percentage of the points
	// in the pool that must neighbours of a point for it to be considered to be
	// part of a cluster
	// private static final float DELTA = 0.90f;

	/** The minimum number of points required to be clustered. */
	private static final int MIN_PTS = 10;

	/** A set of neighbour points. */
	private final LocationSet pool;

	/**
	 * Constructs a significant location clusterer using the specified
	 * NeighbourPointSet.
	 * 
	 * @param set
	 *            The set to use and modify.
	 */
	public SignificantLocationClusterer(final LocationSet set) {
		this.pool = set;
	}

	/**
	 * Adds all the cluster IDs of <tt>point</tt>'s neighbours to the set passed
	 * as parameter.
	 * 
	 * @param point
	 *            The point whose neighbours' clusters must be added.
	 * @param clustersToMerge
	 *            The set of clusters.
	 */
	private void addNeighbouringClusters(final Location point,
			final Collection<Integer> clustersToMerge) {
		DebugHelper.out
				.println("\tAdding neighbouring clusters into merge set.");
		final Collection<Integer> clusterIds = pool.getClusterIds(point
				.getNeighbours());
		if (!clusterIds.isEmpty()) {
			DebugHelper.out.print("\t\tClusters to merge:");
			for (final int clusterId : clusterIds) {
				DebugHelper.out.print(" " + clusterId);
			}
			DebugHelper.out.println(".");
			clustersToMerge.addAll(clusterIds);
		}
	}

	/**
	 * Adds the point's neighbours to the specified cluster.
	 * 
	 * @param point
	 *            The point whose neighbours must be moved.
	 * @param clusterID
	 *            The cluster to move them to.
	 */
	private void addNeighboursToCluster(final Location point,
			final int clusterId) {
		for (final int neighbour_id : point.getNeighbours()) {
			DebugHelper.out.print("\tAdding neighbour " + neighbour_id
					+ " to cluster " + clusterId + ": ");
			pool.addToCluster(neighbour_id, clusterId);
		}

	}

	/**
	 * Adds a neighbour point to the pool of observed points.
	 * 
	 * @param point
	 *            The point to be added.
	 */
	public int addNewLocation(final Location point) {
		pool.cacheLocation(point);

		// Add the point to the pool, compute neighbours.
		pool.add(point);
		return assignToCluster(point);
	}

	public int assignToCluster(final Location location) {
		// if (location.getId() > 4) {
		// System.exit(0);
		// }
		int cluster_id = -1;
		DebugHelper.out.println("Considering new location " + location.getId()
				+ " with timestamp " + location.getTimestamp());

		// The neighbours of the point.
		final Collection<Integer> neighbour_ids = location.getNeighbours();

		// The list of cluster to merge.
		final Collection<Integer> clustersToMerge = new HashSet<Integer>();

		// Determine whether or not to make a new set.
		if (neighbour_ids.size() >= MIN_PTS) {
			cluster_id = createNewCluster(location); // create a new cluster
			DebugHelper.out.println("\tLocation has " + neighbour_ids.size()
					+ " neighbours, so adding to a new cluster " + cluster_id
					+ ".");
			// add the cluster to the list of clusters to merge
			clustersToMerge.add(cluster_id);

			// add neighbouring clusters to list
			addNeighbouringClusters(location, clustersToMerge);

			// add neighbours to new cluster
			addNeighboursToCluster(location, cluster_id);
		} else {
			DebugHelper.out.println("\tLocation only has "
					+ neighbour_ids.size() + " neighbours, so not clustering.");
		}

		DebugHelper.out.println("\tChecking neighbours...");
		// Repeat process for all neighbours.
		for (final int neighbour_id : neighbour_ids) {
			// Check if neighbour is in a cluster
			if (pool.getClusterId(neighbour_id) == -1) {
				// If the neighbour is not already in a cluster, check if it now
				// has enough neighbours to form a cluster.
				final Location neighbour = pool.getLocation(neighbour_id);
				if (neighbour.getNeighbours().size() >= MIN_PTS) {
					DebugHelper.out.println("\tNeighbour " + neighbour_id
							+ " now has enough neighbours to form a cluster.");
					final int cid = createNewCluster(neighbour);
					clustersToMerge.add(cid);
					addNeighbouringClusters(neighbour, clustersToMerge);
					addNeighboursToCluster(neighbour, cid);
				}
			}
		}

		// Only merge if there is >1 cluster in set.
		if (clustersToMerge.size() > 1) {
			// The minimum ID of the clusters to merge will be used.
			cluster_id = Integer.MAX_VALUE;
			for (final int id : clustersToMerge) {
				if (id < cluster_id) {
					cluster_id = id;
				}
			}
			DebugHelper.out.println("\tMinimum clusterId in set is: "
					+ cluster_id);
			// Place all the points in the set of clusters to merge in the same
			// cluster.
			for (final int id : clustersToMerge) {
				pool.changeClusterId(id, cluster_id);
			}
		}
		return cluster_id;
	}

	/**
	 * Creates a new cluster centered around the specified point. Returns the ID
	 * of the newly formed cluster.
	 * 
	 * @param point
	 *            The point centering the new cluster.
	 * @return The ID of the newly formed cluster.
	 */
	private int createNewCluster(final Location point) {
		final int clusterID = pool.getNewClusterId();
		pool.addToCluster(point.getId(), clusterID);
		DebugHelper.out.println("\tAdded " + point.getId() + " to new cluster "
				+ clusterID);
		return clusterID;
	}

	public void dumpToFile(final String filename) {
		try {
			final Map<Integer, Integer> clusterIds = new HashMap<Integer, Integer>();
			int counter = 1;

			final CSVPrinter printer = new CSVPrinter(new FileOutputStream(
					filename));
			final Collection<Integer> locations = pool.getAllLocations();
			for (final int location_id : locations) {
				final Location location = pool.getLocation(location_id);
				final long timestamp = (long) (1000L * location.getTimestamp());
				printer.print("" + timestamp);
				final int cluster_id = pool.getClusterId(location_id);
				if (cluster_id == -1) {
					printer.print("0");
				} else {
					Integer remappedId = clusterIds.get(cluster_id);
					if (remappedId == null) {
						clusterIds.put(cluster_id, counter);
						remappedId = counter;
						counter += 1;
					}

					printer.print(remappedId.toString());
				}
				printer.println();
			}
			printer.close();
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public String toString() {
		String result = "";
		final Collection<Integer> locations = pool.getAllLocations();
		final Collection<Integer> clusters = pool.getAllClusters();

		result += "Total points: " + locations.size() + "\n";
		int sum = 0;
		for (final int cluster_id : clusters) {
			final Collection<Integer> locations_in_cluster = pool
					.getLocationsForCluster(cluster_id);
			final int num_locations_in_cluster = locations_in_cluster.size();
			if (num_locations_in_cluster > 0) {
				result += "Cluster " + cluster_id + ": "
						+ num_locations_in_cluster + "\n";
				sum += num_locations_in_cluster;
			}
		}
		result += "Noise points: " + (locations.size() - sum);
		int noise = 0;
		for (final int location_id : locations) {
			noise += (pool.getClusterId(location_id) == -1 ? 1 : 0);
		}
		if (noise == (locations.size() - sum)) {
			result += " (Validated)";
		} else {
			result += " (Invalid, Actually " + noise + " Noise Points)";
		}
		return result;
	}
}
