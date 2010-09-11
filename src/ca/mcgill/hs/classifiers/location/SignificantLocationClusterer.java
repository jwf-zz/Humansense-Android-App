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
	//private static final float DELTA = 0.90f;
	
	/** The minimum number of points required to be clustered. */
	private static final int MIN_PTS = 50;
	
	/** A set of neighbour points. */
	private LocationSet pool;
	
	/**
	 * Constructs a significant location clusterer using the specified NeighbourPointSet.
	 * @param set The set to use and modify.
	 */
	public SignificantLocationClusterer(LocationSet set) {
		this.pool = set;
	}
	
	/**
	 * Adds a neighbour point to the pool of observed points.
	 * @param point The point to be added.
	 */
	public void addNewLocation(Location point) {
		pool.cacheLocation(point);

		// Add the point to the pool, compute neighbours.
		pool.add(point);
		assignToCluster(point);
	}
	
	public void assignToCluster(Location location) {
//		if (location.getId() > 4) {
//			System.exit(0);
//		}
		DebugHelper.out.println("Considering new location " + location.getId() + " with timestamp " + location.getTimestamp());
		
		// The neighbours of the point.
		Collection<Integer> neighbour_ids = location.getNeighbours();
		
		// The list of cluster to merge.
		Collection<Integer> clustersToMerge = new HashSet<Integer>();
		
		// Determine whether or not to make a new set.
		if(neighbour_ids.size() >= MIN_PTS) {
			int clusterId = createNewCluster(location);       // create a new cluster
			DebugHelper.out.println("\tLocation has " + neighbour_ids.size() + " neighbours, so adding to a new cluster " + clusterId + ".");
			clustersToMerge.add(clusterId);                // add the cluster to the list of clusters to merge
			addNeighbouringClusters(location,clustersToMerge); // add neighbouring clusters to list
			addNeighboursToCluster(location,clusterId);        // add neighbours to new cluster
		}
		else {
			DebugHelper.out.println("\tLocation only has " + neighbour_ids.size() + " neighbours, so not clustering.");
		}

		DebugHelper.out.println("\tChecking neighbours...");
		// Repeat process for all neighbours.
		for(int neighbour_id : neighbour_ids) {      // repeat for neighbours...
			// Check if neighbour is in a cluster
			int cluster_id = pool.getClusterId(neighbour_id);
			
			if (cluster_id == -1) { 
				// If the neighbour is not already in a cluster, check if it now has enough neighbours.
				Location neighbour = pool.getLocation(neighbour_id);
				if (neighbour.getNeighbours().size() >= MIN_PTS) {
					DebugHelper.out.println("\tNeighbour " + neighbour_id + " now has enough neighbours to form a cluster.");
					int cid = createNewCluster(neighbour);  // repeat same process as above
					clustersToMerge.add(cid);
					addNeighbouringClusters(neighbour,clustersToMerge);
					addNeighboursToCluster(neighbour,cid);
				}
			}
		}
		
		// Only merge if there is >1 cluster in set.
		if(clustersToMerge.size() > 1) {
			// The minimum ID of the clusters to merge will be used.
			int clusterID = Integer.MAX_VALUE;
			for(int id : clustersToMerge) if(id < clusterID) clusterID = id;
			DebugHelper.out.println("\tMinimum clusterId in set is: " + clusterID);
			// Place all the points in the set of clusters to merge in the same cluster.
			for(int id : clustersToMerge) {
				pool.changeClusterId(id,clusterID);
			}
		}
	}
	
	/**
	 * Creates a new cluster centered around the specified point. Returns the 
	 * ID of the newly formed cluster.
	 * @param point The point centering the new cluster.
	 * @return The ID of the newly formed cluster.
	 */
	private int createNewCluster(Location point) {
		int clusterID = pool.getNewClusterId();
		pool.addToCluster(point.getId(), clusterID);
		DebugHelper.out.println("\tAdded " + point.getId() + " to new cluster " + clusterID);
		return clusterID;
	}
	
	/**
	 * Adds all the cluster IDs of <tt>point</tt>'s neighbours to the set passed 
	 * as parameter.
	 * @param point The point whose neighbours' clusters must be added.
	 * @param clustersToMerge The set of clusters.
	 */
	private void addNeighbouringClusters(Location point, Collection<Integer> clustersToMerge) {
		DebugHelper.out.println("\tAdding neighbouring clusters into merge set.");
		Collection<Integer> clusterIds = pool.getClusterIds(point.getNeighbours());
		if (!clusterIds.isEmpty()) {
			DebugHelper.out.print("\t\tClusters to merge:");
			for (int clusterId : clusterIds) {
				DebugHelper.out.print(" " + clusterId);
			}
			DebugHelper.out.println(".");
			clustersToMerge.addAll(clusterIds);
		}
	}
	
	/**
	 * Adds the point's neighbours to the specified cluster.
	 * @param point The point whose neighbours must be moved.
	 * @param clusterID The cluster to move them to.
	 */
	private void addNeighboursToCluster(Location point, int clusterId) {
		for(int neighbour_id : point.getNeighbours()) {
			DebugHelper.out.print("\tAdding neighbour " + neighbour_id + " to cluster " + clusterId + ": ");
			pool.addToCluster(neighbour_id, clusterId);
		}
		
	}
	
	public void dumpToFile(String filename) {
		try {
			Map<Integer,Integer> clusterIds = new HashMap<Integer, Integer>();
			int counter = 1;
			
			//DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			CSVPrinter printer = new CSVPrinter(new FileOutputStream(filename));
			Collection<Integer> locations = pool.getAllLocations();
			for (int location_id : locations) {
				Location location = pool.getLocation(location_id);
				long timestamp = (long)(1000L*location.getTimestamp());
				printer.print("" + timestamp);
				int cluster_id = pool.getClusterId(location_id);
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
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	@Override
	public String toString() {
		String result = "";
		Collection<Integer> locations = pool.getAllLocations();
		Collection<Integer> clusters = pool.getAllClusters();
		
		result += "Total points: " + locations.size() + "\n";
		int sum = 0;
		for(int cluster_id : clusters) {
			Collection<Integer> locations_in_cluster = pool.getLocationsForCluster(cluster_id);
			int num_locations_in_cluster = locations_in_cluster.size();
			if(num_locations_in_cluster > 0) {
				result += "Cluster " + cluster_id + ": " + num_locations_in_cluster + "\n";
				sum += num_locations_in_cluster;
			}
		}
		result += "Noise points: " + (locations.size()-sum);
		int noise = 0;
		for(int location_id : locations) {
			noise += (pool.getClusterId(location_id) == -1 ? 1 : 0);
		}
		if(noise == (locations.size()-sum)) {
			result += " (Validated)";
		} else {
			result += " (Invalid, Actually " + noise + " Noise Points)";
		}
		return result;
	}
}
