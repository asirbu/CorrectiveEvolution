package eu.fbk.soa.evolution.sts.minimizer;

import java.io.File;

import eu.fbk.soa.evolution.sts.STS;

public interface STSMinimizer {

	public void minimizeSTS(STS inputSTS, File outputFile);
	
	public void minimizeSTS(File inputFile, File outputFile);
	

}
