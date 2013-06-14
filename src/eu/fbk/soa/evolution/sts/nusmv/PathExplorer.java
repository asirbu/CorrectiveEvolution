package eu.fbk.soa.evolution.sts.nusmv;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import eu.fbk.soa.evolution.sts.State;
import eu.fbk.soa.evolution.sts.impl.NusmvSTS;
import eu.fbk.soa.util.ConfigUtils;



public class PathExplorer {
	
	static Logger logger = Logger.getLogger(PathExplorer.class);
	
	private String nusmvPath;

	private String outFile;

	private NuSMVConsole console;

	private ConsoleGobbler input;

	private StateTree pathsToExplore;
	
	private NusmvSTS sts;
	
	private int exploredPaths;
	
	public PathExplorer(String outputDir){
		outFile = outputDir + "nusmvout.txt";
		try {
			nusmvPath = ConfigUtils.getProperty("nusmvPath");
		} catch (IOException e) {
			logger.error("Could not load configuration file.");
			e.printStackTrace();
		}
	}
	
	private void init(String nusmvFile) throws IOException {
		sts = new NusmvSTS();
		pathsToExplore = new StateTree();
		exploredPaths = 0;
	}
	
	private void startNuSMVConsole(String nusmvFile) throws IOException {
		console = new NuSMVConsole(nusmvFile, nusmvPath, outFile, sts);
		console.loadConsole();
		
		input = new ConsoleGobbler(console);
		input.start();	
		
		console.waitForCommandPrompt();
	}

	public NusmvSTS explore(String nusmvFile) throws IOException {
		logger.info("Using NuSMV to retrieve STS from SMV file");		
		init(nusmvFile);

		int iteration = 0;

		while (!this.pathsToExplore.isVisited()) {
			iteration++;
			logger.info("Starting up..");
			startNuSMVConsole(nusmvFile);	

			//		sendCommandToConsole("set shown_states 400");
			sendCommandToConsole("go");
			sendCommandToConsole("build_boolean_model");
			sendCommandToConsole("bmc_setup");

			if (iteration == 1) {
				List<State> newStates = new ArrayList<State>();
				String consolePrint = this.sendCommandToConsole("print_reachable_states -v");
				newStates = NuSMVOutputHandler.addStatesToSTS(sts, consolePrint, "-------");
				logger.info("Nr of reachable states: " + newStates.size());

				if (newStates.isEmpty()) {
					logger.error("Incorrect specification: the generated NuSMV model has no reachable states");
				}
			}

			int i = 0;
			while (!this.pathsToExplore.isVisited() && i < 70) {
				i++;
				sendCommandToConsole("set nusmv_stdout " + outFile);
				String consolePrint = console.pickInitialState(pathsToExplore);

				logger.trace(consolePrint);
				sendCommandToConsole("set nusmv_stdout " + outFile);
				consolePrint = console.simulateInteractively();
				logger.trace(consolePrint + "\n");

				exploredPaths++;
				logger.info("Explored path " + this.exploredPaths);

			}
			console.execCommand("quit");
			console.closeConsole();
			input.setRun(false);
		}
		if (!sts.getStates().isEmpty()) {
			logger.debug("Obtained STS:\n" + sts.toDot());
		}
		
		if (!Boolean.getBoolean(ConfigUtils.getProperty("intermediaryFiles"))) {
			File nusmvOutput = new File(outFile);
			nusmvOutput.deleteOnExit();
		}
		return sts;
	}


	private String sendCommandToConsole(String command) throws IOException {
		console.execCommand(command);
		String consolePrint = console.waitForCommandPrompt();
		return consolePrint;
	}

}


class ConsoleGobbler extends Thread {
	private NuSMVConsole console;
	boolean run;

	ConsoleGobbler(NuSMVConsole console) {
		this.console = console;
		run = true;
	}

	void setRun(boolean run) {
		this.run = run;
	}

	@Override
	public void run() {
		Thread.currentThread().setName("inputConsole");
		while(run) {
			console.readChars();
		}
	}
}

