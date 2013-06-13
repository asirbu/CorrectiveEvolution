package eu.fbk.soa.evolution.sts.minimizer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.fbk.soa.evolution.sts.Action;
import eu.fbk.soa.evolution.sts.STS;
import eu.fbk.soa.evolution.sts.State;
import eu.fbk.soa.evolution.sts.Transition;
import eu.fbk.soa.util.IOUtils;


public class BisimulationMinimizer implements STSMinimizer {
	
	private STS inputSTS;
	
	public BisimulationMinimizer() {
		
	}

	public void minimizeSTS(STS inputSTS, File outputFile) {
		this.inputSTS = inputSTS;
		Partition minimizedPartition = this.minimizePaigeTarjan();
		STS resultSTS = inputSTS.getCopy();
		
		for (Block b : minimizedPartition.getBlocks()) {
			List<State> stateList = new ArrayList<State>(b.getStates());
			State repr = stateList.remove(0);
			
			for (State s : stateList) {
				resultSTS.replaceState(s, repr);
			}
		}	
		try {
			IOUtils.exportSTSToDot(resultSTS, new FileWriter(outputFile));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private Partition minimizePaigeTarjan() {
		Set<State> states = collectStatesInTransitions(inputSTS);
		
		Block initialBlock = new Block(states);
		Partition prevPartition = new Partition(initialBlock);
		
		Map<Block, Set<Block>> compoundBlocks = new HashMap<Block, Set<Block>>();
		Set<Block> initial = new HashSet<Block>();
		initial.add(initialBlock);
		compoundBlocks.put(initialBlock, initial);
		
		Partition currentPartition = new Partition(initialBlock);
		
		boolean started = false;
		while (!started || !currentPartition.equals(prevPartition)) {
//		while (!isPTStable(currentPartition)) {
			started = true;
			Block compoundBlock = compoundBlocks.keySet().iterator().next();
			Set<Block> blocks = compoundBlocks.remove(compoundBlock);
			prevPartition.removeBlock(compoundBlock);
			//System.out.println("Selected " + compoundBlock.toString() + "\n");
			
			Block b = selectRefiningBlock(blocks);
			Set<State> bstates = b.getStates();
			//System.out.println(" - Selected component " + b.toString() + "\n");
			
			blocks.remove(b);
			prevPartition.addBlock(b);
			
			Set<State> difference = this.difference(compoundBlock.getStates(), bstates);
			if (!difference.isEmpty()) {
				Block newCompoundBlock = new Block(difference);
				prevPartition.addBlock(newCompoundBlock);
				//System.out.println(" - Added difference " + newCompoundBlock.toString() + "\n");
				
				if (blocks.size() > 1) {
					compoundBlocks.put(newCompoundBlock, blocks);
				}
			}
			
			for (Action act : this.inputSTS.getActions()) {
				Set<State> preB = predecessor(b.getStates(), act);
				this.split(preB, currentPartition, prevPartition, compoundBlocks);

				Set<State> preS_B = predecessor(difference, act);
				//Set<State> preB_preS_B = this.difference(preB, preS_B);
				this.split(preS_B, currentPartition, prevPartition, compoundBlocks);
			}
		}
		return currentPartition;
	}
	
	private void split(Set<State> preBlock, Partition currentPartition, 
			Partition prevPartition, Map<Block, Set<Block>> compoundBlocks) {
		
		List<Block> snapshot = new ArrayList<Block>(currentPartition.getBlocks());
		
		for (Block d : snapshot) {
			currentPartition.removeBlock(d);
			
			Set<State> intersection = intersection(d.getStates(), preBlock);
			if (!intersection.isEmpty()) {	
				currentPartition.addBlock(new Block(intersection));
			}
			
			Set<State> diff = difference(d.getStates(), intersection);
			if (!diff.isEmpty()) {
				currentPartition.addBlock(new Block(diff));	
				
				for (Block previousBlock : prevPartition.getBlocks()) {
					if (previousBlock.contains(intersection) && previousBlock.contains(diff)) {
						Set<Block> blockSet = getComponentBlocks(previousBlock, currentPartition);
						if (blockSet.size() > 1) {	
							compoundBlocks.put(previousBlock, blockSet);
						}
					}
				}
			}
		}
	}
	
	private Set<State> collectStatesInTransitions(STS inputSTS) {
		
		Set<State> states = new HashSet<State>();
		for (Transition t : inputSTS.getTransitions()) {
			states.add(t.getSource());
			states.add(t.getTarget());
		}
		return states;
	}

	private Set<Block> getComponentBlocks(Block block, Partition partition) {
		Set<Block> components = new HashSet<Block>();
		for (Block b : partition.getBlocks()) {
			if (block.contains(b)) {
				components.add(b);
			}
		}
		return components;
	}

	private Block selectRefiningBlock(Set<Block> componentBlocks) {
		Block block = null;
		Block b2 = null;
		
		Iterator<Block> it = componentBlocks.iterator();
		if (it.hasNext()) {
			block = it.next();
			if (it.hasNext()) {
				b2 = it.next();
			}
		}
		if (b2 != null && block.size() > b2.size()) {
			block = b2;
		} 
		return block;
	}
	
	
	private Set<State> difference(Set<State> states1, Set<State> states2) {
		Set<State> difference = new HashSet<State>(states1);
		difference.removeAll(states2);
		return difference;
	}

	/*
	 * A partition P is PT stable when for any block B in P, if B' in P then 
	 *	-> either B is a subset of pre(B'), 
	 *	-> or intersection of B and pre(B') is the empty set
	 */
//	private boolean isPTStable(Partition partition) {
//		
//		for (Block b1 : partition.getBlocks()) {
//			for (Block b2 : partition.getBlocks()) {
//				Set<State> b1States = b1.getStates();
//				Set<State> preB2 = predecessor(b2.getStates());
//				if (!(isSubsetOf(b1States, preB2) 
//						|| (intersection(b1States, preB2).isEmpty()))) {
//					return false;
//				}
//			}
//		}
//		return true;
//	}
	
	private Set<State> intersection(Set<State> set1, Set<State> set2) {
		Set<State> intersection = new HashSet<State>(set1);
		for (State s : set1) {
			if (!set2.contains(s)) {
				intersection.remove(s);
			}
		}
		return intersection;
	}

	private Set<State> predecessor(Collection<State> states, Action act) {	
		Set<State> preStates = new HashSet<State>();
		for (State s : states) {
			for (Transition t : inputSTS.getTransitions()) {
				if (t.getAction().equals(act) && t.getTarget().equals(s)) {
					preStates.add(t.getSource());
				}
			}
		}
		return preStates;
	}

	@Override
	public void minimizeSTS(File inputFile, File outputFile) {
		throw new UnsupportedOperationException("Operation not implemented");
		
	}
	
	
}
