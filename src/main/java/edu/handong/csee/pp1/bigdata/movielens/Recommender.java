package edu.handong.csee.pp1.bigdata.movielens ;

import java.util.* ;
import com.google.common.collect.* ;
import org.apache.commons.configuration.* ;

public class Recommender
{
	TreeMap<Integer, Integer> countForAllItemsetsWithSize1 = new TreeMap<Integer, Integer>() ; // first item, second count
	TreeMap<FrequentItemsetSize2, Integer> countForAllItemsetsWithSize2 = new TreeMap<FrequentItemsetSize2, Integer>() ; // first item, second count
	
	// Frequent itemsets with size 1. Key is movie id and value is the number baskets (frequency) for the movie
	// all itemsets in this map stisfies the minimum support.
	TreeMap<Integer, Integer> freqItemsetsWithSize1 = new TreeMap<Integer, Integer>() ; // { 1(movie number), 4(how many times did movie '1' found) }
	
	// Frequent itemsets with size 2. Key is two movie ids (set) and value is the number baskets (frequency) for the movie
	// all itemsets in this map satisfies the minimum support.
	TreeMap<FrequentItemsetSize2, Integer> 												// { {1,2(movie number), 6(how many times did this set found) }
	freqItemsetsWithSize2 = new TreeMap<FrequentItemsetSize2, Integer>() ; 
	
	// Frequent itemsets with size 3. Key is three movie ids (set) and value is the number baskets (frequency) for the movie
	// all itemsets in this map stisfies the minimum support.
	TreeMap<FrequentItemsetSize3, Integer> 
	freqItemsetsWithSize3 = new TreeMap<FrequentItemsetSize3, Integer>() ; 

	PropertiesConfiguration config ;
	int minSupport ;
	int min_evidence_3 ;
	double confidence_threshold_rulesize_2 ;
	double confidence_threshold_rulesize_3 ;


	Recommender(PropertiesConfiguration config) {
		this.config = config ;
		this.minSupport = 
			config.getInt("training.min_supports") ;
		this.confidence_threshold_rulesize_2 = 
			config.getDouble("prediction.confidence_threshold_rulesize_2") ;
		this.confidence_threshold_rulesize_3 = 
			config.getDouble("prediction.confidence_threshold_rulesize_3") ;
		this.min_evidence_3 = 
			config.getInt("prediction.min_evidence_3") ;
	}

	public void train(MovieData data) {
		TreeMap<Integer, HashSet<Integer>> 
		baskets = data.getBaskets() ;
		/* Baskets : UserID -> Set<MovieId> */	// user id with favorite movie sets

		for (Integer user : baskets.keySet()) { // user continuously gets the user id in ascending order. ex) 720, 721, 722, ...
			HashSet<Integer> aBasket = baskets.get(user) ; // aBasket gets the 'value', which is the set of movie number according to the user id. 
														   // ex) {1, 2, 3}

			computeFreqItemsetsWithSize1(aBasket) ;	// count one element
			computeFreqItemsetsWithSize2(aBasket) ; // i.e. association rules with size 2
			computeFreqItemsetsWithSize3(aBasket) ; // i.e., association rules with size 3
			// Optional TODO: can you do this for Size K???
		}
	}

	public int predict(HashSet<Integer> anItemset, Integer q) {
		if (predictPair(anItemset, q) == 1)	// # if it is 1, it is well predicted.
			return 1 ;
		return predictTriple(anItemset, q) ;
	}

	// @param : integer set of movie numbers. ex) {1, 2, 3}
	private void computeFreqItemsetsWithSize1(HashSet<Integer> aBasket) {
		
		for (Integer item : aBasket) {
			// item = movie numbers
			Integer count = countForAllItemsetsWithSize1.get(item) ;	// countForAllItemsetsWithSize1 = {item(key), count(value)}
			if (count == null)
				count = 1 ;
			else
				count = count.intValue() + 1 ;
			
			countForAllItemsetsWithSize1.put(item, count) ; // the item is updated with new count. ex) {1(movie number), 2 times + 5 times}
		}
		
		for(Integer item:countForAllItemsetsWithSize1.keySet()) {
			// item = movie number

			if(countForAllItemsetsWithSize1.get(item) >= minSupport)
				freqItemsetsWithSize1.put(item, countForAllItemsetsWithSize1.get(item));			
			
		}
	}

	private void computeFreqItemsetsWithSize2(HashSet<Integer> aBasket) {	// bounded to the first, one-element sets
		
		HashSet<Integer> allItemsetsWithSize1ThatSatisfyMinSupportInTheBasket = new HashSet<Integer>() ;
		for (Integer item : aBasket) {
			// We only need to consider items in the frequent itemsets with Size 1 at first. => Using monotonicity to improve this algorithm(a-priority algorithm)
			if (freqItemsetsWithSize1.containsKey(item))
				allItemsetsWithSize1ThatSatisfyMinSupportInTheBasket.add(item) ; // only consider items that satisfy minimum support value. (a-priority algorithm)
		}
		
		aBasket = allItemsetsWithSize1ThatSatisfyMinSupportInTheBasket;
		
		// it is obvious that aBasket must have at least two items for computing its frequency. (size less than 2 is omitted.)
		if (aBasket.size() >= 2) {
			
			// Sets.combinations is a public method from a google's common collection package.
			// this method returns all combinations of elements of a specific size (the second parameter).
			// for example, when we have the first parameter value {1,2,3} and the value of the second parameter '2',
			// it returns all subsets from the combinations with the given size {{1,2},{1,3},{2,3}}.
			// Note that the order of items mat not be ordered by item ids but the order is sorted when ItemsetWithSize2 is instantiated.
			for (Set<Integer> aSubsetWithTwoItems : Sets.combinations(aBasket, 2)) {
				Integer count = countForAllItemsetsWithSize2.get(new FrequentItemsetSize2(aSubsetWithTwoItems)) ;
				if (count == null) 
					count = 1 ;
				else
					count = count.intValue() + 1 ;
				countForAllItemsetsWithSize2.put(new FrequentItemsetSize2(aSubsetWithTwoItems), count) ;
			}
			
			for(FrequentItemsetSize2 itemset:countForAllItemsetsWithSize2.keySet()) {
				
				if(countForAllItemsetsWithSize2.get(itemset)>=minSupport) // omit again.
					freqItemsetsWithSize2.put(itemset, countForAllItemsetsWithSize2.get(itemset)); // # finally, we got the size 2 item sets.
			}
		}
	}

	private void computeFreqItemsetsWithSize3(HashSet<Integer> aBasket) {
		
		// Naively using monotonicity to improve efficiency of this algorithm
		// Based on slides, we could apply monotonicity for itemsets with Size 2 but we did this with Itemsets with Size 1 for simplicity.
		HashSet<Integer> allItemsThatSatisfyMinSupportInTheBasket = new HashSet<Integer>() ;
		for (Integer item : aBasket) {
			// We do not need to count size 1 items whose frequency is less than minimum support. => Using monotonicity to improve this algorithm
			if (freqItemsetsWithSize1.containsKey(item))
				allItemsThatSatisfyMinSupportInTheBasket.add(item) ;
		}
		
		aBasket = allItemsThatSatisfyMinSupportInTheBasket ;

		// it is obvious that aBasket must have at least three items for computing its frequency.
		if (aBasket.size() >= 3) {
			// Sets.combinations is a public method from a google's common collection package.
			// this method returns all combinations of elements of a specific size (the second parameter).
			// for example, when we have the first parameter value {1,2,3,4} and the value of the second parameter '2',
			// it returns all subsets from the combinations with the given size {{1,2,3},{1,2,4},{1,3,4},{2,3,4}}.
			// Note that the order of items mat not be ordered by item ids but the order is sorted when ItemsetWithSize3 is instantiated.
			for (Set<Integer> aSubsetWithThreeItems : Sets.combinations(aBasket, 3)) {
				Integer count = freqItemsetsWithSize3.get(new FrequentItemsetSize3(aSubsetWithThreeItems));
				if (count == null) 
					count = 1 ;
				else
					count = count.intValue() + 1 ;
				
				freqItemsetsWithSize3.put(new FrequentItemsetSize3(aSubsetWithThreeItems), count) ;
			}
		}
	}

	private int predictPair(HashSet<Integer> anItemset, Integer j) {
		/* TODO: implement this method */
		// dealing with items like {movie 1} -> {movie 2}.
		if (anItemset.size() == 0)
			return 0;
		
		// Compute support, confidence, or lift. Based on their threshold, decide how to predict. Return 1 when metrics are satisfied by threshold, otherwise 0.
		int evidence = 0; // evidence is the number of a specific item's occurance.
		
		
		
		
		return 0 ;
	}

	private int predictTriple(HashSet<Integer> anItemset, Integer j) { // association rule anItemset (I) -> j
		
		// only consider the case whose itemset size is >=2 since this method deals with {movie 1, movie 2} -> {movie 3} rules
		if (anItemset.size() < 2)
			return 0 ;

		// Compute support, confidence, or lift. Based on their threshold, decide how to predict. Return 1 when metrics are satisfied by thresholds, otherwise 0.
		// In the current implementation, we considered only confidence.
		int evidence = 0 ;
		for (Set<Integer> p : Sets.combinations(anItemset, 2)) {
			
			// the number baskets for I (I -> j)
			Integer numBasketsForI = freqItemsetsWithSize2.get(new FrequentItemsetSize2(p)) ;
			
			if (numBasketsForI == null)
				continue ;
			
			// the number of baskets for I U {j}
			TreeSet<Integer> assocRule = new TreeSet<Integer>(p) ;
			assocRule.add(j) ;
			FrequentItemsetSize3 item = new FrequentItemsetSize3(assocRule) ;
			Integer numBasketsForIUnionj = freqItemsetsWithSize3.get(item) ; // All itemsets in freqItemsetsWithSize3 satisfy minimum support when the are computed.
			if (numBasketsForIUnionj == null)	// if not here, just skip
				continue ;
												// else,
			// compute confidence: The confidence of the rule I -> j is the ratio of the number of baskets for I U {j} and the number of baskets for I.
			double confidence = (double) numBasketsForIUnionj / numBasketsForI;
		
			if (confidence >= confidence_threshold_rulesize_3) 
				evidence++ ;
		}

		if (evidence >= min_evidence_3) 
			return 1 ;

		return 0 ;
	}	
}

@SuppressWarnings("rawtypes")
class FrequentItemsetSize2 implements Comparable 
{

	int first ;
	int second ;

	public FrequentItemsetSize2(int first, int second) {
		if (first <= second) {
			this.first = first ;
			this.second = second ;
		}
		else {
			this.first = second ;
			this.second = first ;
		}
	}

	public FrequentItemsetSize2(Set<Integer> s) {
		
		Integer [] elem = s.toArray(new Integer[2]) ;
		// order item ids!
		if (elem[0] < elem[1]) {
			this.first = elem[0] ;
			this.second = elem[1] ;
		}
		else {
			this.first = elem[1] ;
			this.second = elem[0] ;
		}
	}

	@Override
	public int compareTo(Object obj) { // this method is used for sorting when using TreeMap
		FrequentItemsetSize2 p = (FrequentItemsetSize2) obj ;

		if (this.first < p.first) 
			return -1 ;
		if (this.first > p.first)
			return 1 ;

		return (this.second - p.second) ;
	}
}

// there will be more conditions, since there will be 2 and 3 item sets.
@SuppressWarnings("rawtypes")
class FrequentItemsetSize3 implements Comparable 
{
	int [] items ;

	FrequentItemsetSize3(Set<Integer> s) {
		/* TODO: implement this method */
		Integer [] elem = s.toArray(new Integer[3]);
		
		// simple bubble sort for this array. from index 0 to 2, numbers are arranged in a smaller manner. (1, 2, 3)
		int temp = 0;
		for(int i = 0; i < 2 ; i++)
		{
			for (int j = i+1; j<3 ; j++)
			{
				if (elem[j] < elem[i])
				{
					temp = elem[i];
					elem[i] = elem[j];
					elem[j] = temp;
				}
			}
		}
		
		// elem[] is sorted, so just put them in items[].
		for (int k = 0; k<3; k++)
		{
			items[k] = elem[k];
		}
		// values in s must be sorted and saved into items array
	}

	@Override
	public int compareTo(Object obj) {  // this method is used for sorting when using TreeMap
		/* TODO: implement this method */
		FrequentItemsetSize3 p = (FrequentItemsetSize3) obj ;

		
		return 0;
	}
}
