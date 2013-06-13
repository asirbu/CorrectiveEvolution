package eu.fbk.soa.evolution.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.fbk.soa.evolution.Correction;
import eu.fbk.soa.evolution.engine.impl.ProblemToSMV;
import eu.fbk.soa.evolution.sts.STS;
import eu.fbk.soa.process.Goal;
import eu.fbk.soa.process.ProcessModel;
import eu.fbk.soa.util.IOUtils;
import eu.fbk.soa.util.StreamGobbler;

public class RelaxedEngine extends AbstractEngine {
	
	static Logger logger = Logger.getLogger(RelaxedEngine.class);
	
	public RelaxedEngine(ProcessModel model, Goal goal, List<Correction> corrections) {
		super();
		smvTranslator = new ProblemToSMV(model, goal, corrections);
	}
	
	@Override
	public Set<ProcessModel> solveProblem() {
		logger.info("Solving relaxed corrective evolution problem");
		Set<ProcessModel> models = new HashSet<ProcessModel>();
		
		logger.info("Encoding evolution problem as a planning problem");
		String smvFilePath = outputDir + "evolution_problem.smv";
		this.translateProblemToSMV(smvFilePath);
		
		logger.info("Calling WSynth... ");
		String output = this.callWSynth(smvFilePath);
//		handleIntermediaryFile(smvFilePath);
		
		if (output.isEmpty()) {
			logger.info("No solution");
			return models;
		} 
		
		logger.info("Found solution");
		logger.debug("WSynth output:\n\n" + output);
		
		String filePath = outputDir + "wsynth_sts.dot";
		IOUtils.writeStringToFile(output, filePath);
		
		logger.info("Minimizing STS");
		STS resultSTS = this.getMinimizedNusmvSTS(filePath);	
		handleIntermediaryDotFile("wsynth_sts");
		
		logger.info("Translating STS back to a process model");
		ProcessModel corrModel = this.getCorrectedProcessModel(resultSTS);
		models.add(corrModel);
		
		this.exportResultAsPetriNet("relaxed");
		return models;
	}
	
	private String callWSynth(String problemFile) {
		Runtime runtime = Runtime.getRuntime();
		String output = "";
		
		try {		
			Process proc = runtime.exec("lib/wsynth -model_type dn -out_type dot " +
					"-algo cyclic_preferences -agaf states " +
					"-mono -dynamic -reachability_analysis " 
					+ problemFile);
			
			// any error message?
            StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERROR");            
            errorGobbler.start();
            
            // any output?
            //StreamGobbler outputGobbler = new 
            //    StreamGobbler(proc.getInputStream(), "OUTPUT", fos);
            InputStreamReader isr = new InputStreamReader(proc.getInputStream());
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			
			while ((line = br.readLine()) != null) {
				output += line + "\n";
			}
                                       
            // any error?
            int exitVal = proc.waitFor();
            logger.debug("Running wsynth - process exit value: " + exitVal); 
            
			proc.destroy();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return output;
	}

	
}
