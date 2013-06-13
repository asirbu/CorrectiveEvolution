package eu.fbk.soa.evolution.sts.minimizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Partition {

	private List<Block> blocks;
	
	public Partition(Collection<Block> blockCollection) {
		blocks = new ArrayList<Block>(blockCollection);
	}
	
	public Partition() {
		this.blocks = new ArrayList<Block>();
	}

	public Partition(Block initialBlock) {
		this();
		this.blocks.add(initialBlock);
	}

	public void addBlock(Block newBlock) {
		boolean isIncluded = false;
		for (Block b : blocks) {
			if (b.equals(newBlock)) {
				isIncluded = true;
				break;
			}
		}
		if (!isIncluded) {
			blocks.add(newBlock);
		}
	}

	public List<Block> getBlocks() {
		return blocks;
	}

	public void removeBlock(Block block) {
		this.blocks.remove(block);
		
	}
	
	public boolean equals(Object obj) {
		if (! (obj instanceof Partition)) {
			return false;
		}
		
		Partition partition = (Partition) obj;
		if (!(partition.includes(this) && this.includes(partition))) {
				return false;
		}	
		return true;
	}
	
	public boolean includes(Partition partition) {
		for (Block b : partition.getBlocks()) {
			if (!this.containsBlock(b)) {
				return false;
			}
		}
		return true;
	}
	
	private boolean containsBlock(Block b) {
		for (Block block : this.blocks) {
			if (block.equals(b)) {
				return true;
			}
		}
		return false;
	}

	public int hashCode() {
		int hashcode = 0;
		for (Block b : this.blocks) {
			hashcode += b.hashCode();
		}
		return hashcode;
	}
	
}
