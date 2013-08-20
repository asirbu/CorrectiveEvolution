package eu.fbk.soa.evolution.engine.impl.test;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.PropertyConfigurator;

import eu.fbk.soa.evolution.Correction;
import eu.fbk.soa.evolution.engine.impl.ProblemToSMV;
import eu.fbk.soa.evolution.test.InputConstructor;
import eu.fbk.soa.process.ProcessModel;

public class ProblemToSMVFuncTest {

	public void translateProblemToSMV() {
		
		ProcessModel pm = InputConstructor.constructSimpleLogisticsProcessModel();
		
		List<Correction> corrections = new ArrayList<Correction>();
		Correction corr = InputConstructor.constructSimpleLogisticsCorrection(pm);
		corrections.add(corr);
		
		ProblemToSMV pb2SMV = new ProblemToSMV(pm, corrections);
		String translation = pb2SMV.translateProblemToSMV();
		System.out.println("Translation to SMV:\n\n" + translation);
	}
	
	public static void main(String[] args) throws Exception {
		PropertyConfigurator.configure("log4j.properties");
		ProblemToSMVFuncTest test = new ProblemToSMVFuncTest();
		
		test.translateProblemToSMV();
	}

}
