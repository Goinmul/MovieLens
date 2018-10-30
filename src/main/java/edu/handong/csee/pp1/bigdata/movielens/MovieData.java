package edu.handong.csee.pp1.bigdata.movielens ;

import java.io.* ;
import java.util.* ;

import org.apache.commons.csv.* ;
import org.apache.commons.configuration.* ;

public class MovieData 
{
	//1. HashSet : basically hashmap(instance from hashmap). Fast, since it doesn't provide
	// 			   sorting. It only contains unique elements(no redundancy).
	
	// ex) {1, 2, 5, 3, 6, 9, 11}
	
	//2. TreeMap : basically a hashmap, but it is arranged in a natural order.
	//			   Hashmap allows one null key, but treemap doesn't take a null key.
	//			   Provide speed, less memory need, but the tree balance should be balanced(hard to implement)
	
	// ex) {720(user_id), {1, 2, 5}}
	//	   {721, {4, 1, 9}}
	//	   ...
	TreeMap<Integer, HashSet<Integer>>
	Baskets = new TreeMap<Integer, HashSet<Integer>>() ;	// Baskets = { USER_ID, {movie1, movie2, ...} }

	TreeMap<Integer, Integer>
	numRatingsOfMovies = new TreeMap<Integer, Integer>() ;	// { MOVIE_NUMBER, 4.0 }

	TreeMap<Integer, Double>
	accumulatedRatingsOfMovies = new TreeMap<Integer, Double>() ;	// { MOVIE_NUMBER, 3.75 }

	PropertiesConfiguration config ;
	double like_threshold ;
	int outlier_threshold ;

	public MovieData (PropertiesConfiguration config) {
		this.config = config ;
		this.like_threshold = config.getDouble("data.like_threshold") ;
		this.outlier_threshold = config.getInt("data.outlier_threshold") ;
	}

	public void load (FileReader f) throws IOException {
		
		// Load each instance from a csv file.
		for (CSVRecord r : CSVFormat.newFormat(',').withFirstRecordAsHeader().parse(f)) {
			Integer user   = Integer.parseInt(r.get(0)) ;
			Integer movie  = Integer.parseInt(r.get(1)) ;
			Double  rating = Double.parseDouble(r.get(2)) ;

			// 1) count number of ratings 
			// 2) accumulate rating scores by a user
			if (numRatingsOfMovies.containsKey(movie) == false) {	// the very first introduction of a movie, it doesn't contain a key. So it is false.
				numRatingsOfMovies.put(movie, 1) ;
				accumulatedRatingsOfMovies.put(movie, rating) ;
			}
			else {
				numRatingsOfMovies.put(movie, numRatingsOfMovies.get(movie) + 1) ;	// if the movie had been put before: add up the number of ratings(how many)
				accumulatedRatingsOfMovies.put(movie, accumulatedRatingsOfMovies.get(movie) + rating) ;
			}

			// Deal with a good rating based on the like_threshold.
			// We consider that a user likes the movie when he/she rates the movie more than this score.
			// If a user likes this movie, put it in the Basket for getting association rules
			if (rating >= like_threshold) {
				// Get(take, I'd say.) the basket of this user.
				HashSet<Integer> basket = Baskets.get(user) ; // this means that pick up the value according to the key.
															  // At first, of course, Baskets.get(user) returns null.
															  // in Baskets = { 720(key), {harry_porter_1}(value) }
				
				// basket = { harry_porter_1, bourne_legacy, ... } it contains movie numbers(as integer form), and these movies' ratings are more than 4.0(threshold).
				// Baskets = { 720, {harry_porter_1} } it contains one user_id and one movie set.
				
				if (basket == null) { // no Basket for the user? Create one(form of Baskets).
									  // data looks like : 170, 47, 5.0
					basket = new HashSet<Integer>() ;
					Baskets.put(user, basket) ;	// put a new element of TreeMap, something like { 840 , { harry_porter_1, bourne_identity, ... } }
				}
				
				// put the movie for the user.
				basket.add(movie) ;
			}
		}
	}

	public void removeOutliers() {
		HashSet<Integer> outliers = new HashSet<Integer>() ;
		for (Integer userId : Baskets.keySet()) {	// literally, set of keys(user_id). {720, 721, 722, ...}
			HashSet<Integer> basket = Baskets.get(userId) ;
			if (basket.size() > outlier_threshold) // if basket size is bigger than 100(outlier_threshold)
				outliers.add(userId) ;	// make a blacklist to delete them(at below code)
		}
		System.out.print("Outlier removed: ");
		int i=0;
		for (Integer userId : outliers) {
			Baskets.remove(userId) ;
			i++;
		}
		System.out.println(i + " users who have a big basket (size > " + outlier_threshold + ") were removed");
	}

	public TreeMap<Integer, HashSet<Integer>>
	getBaskets() {
		return Baskets ;
	}

	public void show() {
		ChartGenerator chartGenerator = new ChartGenerator(Baskets, numRatingsOfMovies, accumulatedRatingsOfMovies);
		chartGenerator.showMovieStat() ;
		chartGenerator.showUserStat() ;
		chartGenerator.showRatingStat() ;
	}
}
