/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package ca.mcgill.hs.classifiers.location;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * This class clusters a set of locations at which the user was stationary for a
 * brief period of time into places where the user visits regularly.
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 * 
 */
public class SignificantLocationClusterer {

	/** The minimum number of neighbours required to form a cluster */
	private static final int MIN_PTS = 10;

	/** The set of neighbour points. */
	private final LocationSet pool;

	/**
	 * Constructs a significant location clusterer using the specified set of
	 * neighbour points.
	 * 
	 * @param set
	 *            The set that contains the locations. This set will be updated
	 *            with new locations and clusters.
	 */
	public SignificantLocationClusterer(final LocationSet set) {
		this.pool = set;
	}

	/**
	 * Updates the set of clusters that need to be merged by adding all clusters
	 * that the neighbours of <tt>location</tt> belong to to the set of clusters
	 * that need to be merged.
	 * 
	 * @param location
	 *            The point whose neighbours' clusters must be added.
	 * @param clustersToMerge
	 *            The set of clusters to be merged. This set gets updated.
	 */
	private void addNeighbouringClusters(final Location location,
			final Collection<Long> clustersToMerge) {
		DebugHelper.out
				.println("\tAdding neighbouring clusters into merge set.");
		final Collection<Long> clusterIds = pool.getClusterIds(location
				.getNeighbours());
		if (!clusterIds.isEmpty()) {
			DebugHelper.out.print("\t\tClusters to merge:");
			for (final long clusterId : clusterIds) {
				DebugHelper.out.print(" " + clusterId);
			}
			DebugHelper.out.println(".");
			clustersToMerge.addAll(clusterIds);
		}
	}

	/**
	 * Adds a locations' neighbours to the specified cluster.
	 * 
	 * @param location
	 *            The point whose neighbours must be added.
	 * @param clusterId
	 *            The cluster id that the neighbours will belong to.
	 */
	private void addNeighboursToCluster(final Location location,
			final long clusterId) {
		final List<Long> neighbours = location.getNeighbours();
		for (final long neighbour_id : neighbours) {
			DebugHelper.out.println("\tAdding neighbour " + neighbour_id
					+ " to cluster " + clusterId + ": ");
			pool.addToCluster(neighbour_id, clusterId);
		}

	}

	/**
	 * Adds a new location to the pool of observed locations.
	 * 
	 * @param location
	 *            The new location to be added.
	 */
	public long addNewLocation(Location location) {
		// Add the point to the pool, compute neighbours.
		final long new_id = pool.add(location);
		if (new_id != location.getId()) {
			location = pool.getLocation(new_id);
		}
		return assignToCluster(location);
	}

	/**
	 * Does the work of adding a location to a cluster. This checks to see if
	 * the location should be added to a cluster, and also checks its neighbours
	 * to see if they should be added to a cluster. The location may already be
	 * assigned to a cluster, in which case its neighbours are also merged into
	 * that cluster if necessary.
	 * 
	 * @param location
	 *            The location to cluster. May or may not already have a cluster
	 *            id assigned to it.
	 * @return The id of the cluster, or -1 if the location is not a part of a
	 *         cluster.
	 */
	public long assignToCluster(final Location location) {
		final long location_id = location.getId();
		long cluster_id = pool.getClusterId(location_id);
		DebugHelper.out.println("Clustering location " + location_id
				+ " with timestamp " + location.getTimestamp());

		// The neighbours of the point.
		final Collection<Long> neighbour_ids = location.getNeighbours();
		final long num_neighbours = location.getNumNeighbours();
		final long num_merged = location.getNumMerged();

		// The list of cluster to merge.
		final Collection<Long> clustersToMerge = new HashSet<Long>();

		// Determine whether or not to make a new set.
		if (cluster_id < 0 && num_merged + num_neighbours >= MIN_PTS) {
			cluster_id = createNewCluster(location); // create a new cluster
			DebugHelper.out.println("\tLocation has " + num_neighbours
					+ " neighbours, and consists of " + num_merged
					+ " merged locations, so adding to a new cluster "
					+ cluster_id + ".");

			// add the cluster to the list of clusters to merge
			clustersToMerge.add(cluster_id);

			// add neighbouring clusters to list
			addNeighbouringClusters(location, clustersToMerge);

			// add neighbours to new cluster
			addNeighboursToCluster(location, cluster_id);
		} else if (cluster_id >= 0) {
			DebugHelper.out.println("\tLocation is already in a cluster.");
			clustersToMerge.add(cluster_id);
		} else {
			DebugHelper.out.println("\tLocation only has " + num_neighbours
					+ " neighbours, and consists of " + num_merged
					+ " merged locations, so not clustering.");
		}

		DebugHelper.out.println("\tChecking neighbours...");
		// Repeat process for all neighbours.
		for (final long neighbour_id : neighbour_ids) {
			// Check if neighbour is in a cluster
			if (pool.getClusterId(neighbour_id) == -1) {
				// If the neighbour is not already in a cluster, check if it now
				// has enough neighbours to form a cluster.
				final Location neighbour = pool.getLocation(neighbour_id);
				if (neighbour.getNumNeighbours() >= MIN_PTS) {
					DebugHelper.out.println("\tNeighbour " + neighbour_id
							+ " now has enough neighbours to form a cluster.");
					final long cid = createNewCluster(neighbour);
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
			for (final long id : clustersToMerge) {
				if (id < cluster_id) {
					cluster_id = id;
				}
			}
			DebugHelper.out.println("\tMinimum clusterId in set is: "
					+ cluster_id);
			// Place all the points in the set of clusters to merge in the same
			// cluster.
			for (final long id : clustersToMerge) {
				pool.changeClusterId(id, cluster_id);
			}
		}
		return cluster_id;
	}

	/**
	 * Creates a new cluster centered around the specified location. Returns the
	 * ID of the newly formed cluster.
	 * 
	 * @param location
	 *            The location representing the new cluster.
	 * @return The id of the newly formed cluster.
	 */
	private long createNewCluster(final Location location) {
		final long clusterID = pool.getNewClusterId();
		pool.addToCluster(location.getId(), clusterID);
		DebugHelper.out.println("\tAdded " + location.getId()
				+ " to new cluster " + clusterID);
		return clusterID;
	}

	@Override
	public String toString() {
		String result = "";
		final Collection<Long> locations = pool.getAllLocations();
		final Collection<Long> clusters = pool.getAllClusters();

		result += "Total points: " + locations.size() + "\n";
		int sum = 0;
		for (final long cluster_id : clusters) {
			final Collection<Long> locations_in_cluster = pool
					.getLocationsForCluster(cluster_id);
			final long num_locations_in_cluster = locations_in_cluster.size();
			if (num_locations_in_cluster > 0) {
				result += "Cluster " + cluster_id + ": "
						+ num_locations_in_cluster + "\n";
				sum += num_locations_in_cluster;
			}
		}
		result += "Noise points: " + (locations.size() - sum);
		int noise = 0;
		for (final long location_id : locations) {
			noise += (pool.getClusterId(location_id) == -1 ? 1 : 0);
		}
		if (noise == (locations.size() - sum)) {
			result += " (Validated)";
		} else {
			result += " (Invalid, Should be " + noise + " Noise Points)";
		}
		return result;
	}
}
