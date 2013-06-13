package eu.fbk.soa.util;

import java.util.ArrayList;
import java.util.List;

public class ArrayUtils<E> {

	 // print N! permutation of the elements of array a (not in order)
    public List<List<E>> computePermutations(List<E> nodeList) {
    	List<List<E>> permutations = new ArrayList<List<E>>();
    	
    	List<E> nodeListCopy = new ArrayList<E>(nodeList);
    	permutation(nodeListCopy, nodeList.size(), permutations);
    	return permutations;
    }

    private void permutation(List<E> nodeList, int n, List<List<E>> permutations) {
    	if (n == 1) {
    		permutations.add(nodeList);
    	}
    	
        for (int i = 0; i < n; i++) {
            List<E> newList = swap(nodeList, i, n - 1);
//            permutations.add(newList);
            permutation(newList, n-1, permutations);
        }
    }  

    // swap the objects at indices i and j
    private List<E> swap(List<E> objList, int i, int j) {
       List<E> copy = new ArrayList<E>(objList);

       E elem = copy.get(i); 
       copy.set(i, copy.get(j));
       copy.set(j, elem);
       return copy;
    }
	
}
