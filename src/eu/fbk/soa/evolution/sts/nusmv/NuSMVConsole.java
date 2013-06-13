package eu.fbk.soa.evolution.sts.nusmv;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import eu.fbk.soa.evolution.sts.impl.NusmvSTS;
import eu.fbk.soa.evolution.sts.impl.NusmvState;
import eu.fbk.soa.util.IOUtils;


class NuSMVConsole extends Thread {
	
	static int simulationSteps = 60;
	
	static Logger logger = Logger.getLogger(NuSMVConsole.class);
	
	private static String cmd_prompt = "NuSMV > ";
	
	private String nusmvFile;
	
	private String nusmvPath;
	
//	private String outFilePath;
	
	private File outFile;

	private NusmvSTS sts;
	
	private Process proc;
	
	private BufferedReader brInput;
	
	private BufferedWriter bwOutput;

	private StringBuffer sbTotal;
	
	private String consolePrint;
    
	private String strTotal = "";
	
	private Node initialNode;
	
	private Node currentNode;
	
//	private sts.State currentState;	

	NuSMVConsole(String nusmvFile, String nusmvPath, String outFilePath, NusmvSTS sts) {
		this.nusmvFile = nusmvFile;		
		this.sbTotal = new StringBuffer();		
		this.nusmvPath = nusmvPath;
		this.outFile = new File(outFilePath);
		this.sts = sts;
	}

	void readChars() {
		try {
			if (brInput.ready()) {
				char[] cbuf = new char[2048];
				int nrOfChars = brInput.read(cbuf, 0, 2048);
				if (nrOfChars > 0) {
					int i = 0;
					while (i < nrOfChars) {
						sbTotal.append(cbuf[i]);
						strTotal += cbuf[i];
						i++;
					}
				}
			} 
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private synchronized String readStr() {
		return strTotal;
	}

	private synchronized String printConsole() {
		consolePrint = sbTotal.toString();
		sbTotal = new StringBuffer();
		strTotal = "";
		return consolePrint;
	}

	void closeConsole() {
		proc.destroy();
	}

	String waitForCommandPrompt() throws IOException {
		while(! readStr().endsWith(cmd_prompt)) {
			try {
				Thread.sleep(50);				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		synchronized(this) {
			return printConsole();
		}
	}

	
	
	String pickInitialState(StateTree pathsToExplore) throws IOException {
		
		execCommand("bmc_pick_state -v -i");
		try {
			Thread.sleep(80);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String fileContents = IOUtils.readFileAsString(outFile);
		List<eu.fbk.soa.evolution.sts.State> initialStates = 
			NuSMVOutputHandler.addInitialStatesToSTS(fileContents, sts);
		
		while (initialStates.isEmpty()) {
			try {
				logger.info("Read file to soon, waiting..");
				Thread.sleep(200);
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
			fileContents = IOUtils.readFileAsString(outFile);
			initialStates = NuSMVOutputHandler.addInitialStatesToSTS(fileContents, sts);
		}
		
		
		List<Node> nodes = pathsToExplore.getRoot().addChildren(initialStates);
		if (nodes.isEmpty()) {
			nodes = pathsToExplore.getRoot().getChildren();
		}
		
		int index = 0;
		if (nodes.size() > 1) {		
			for (int i = 0; i < nodes.size(); i++) {
				if (!nodes.get(i).subtreeIsVisited()) {
					index = i;
					break;
				}
			}
		}
		
		initialNode = nodes.get(index);
		initialNode.visit();

		execCommand(String.valueOf(index));
		waitForCommandPrompt();

		synchronized(this) {
			return printConsole();
		}		
	}
	

	String simulateInteractively() throws IOException {	
		execCommand("bmc_inc_simulate -v -i -k " + simulationSteps);
		logger.info("Executing NuSMV simulation");
		
		currentNode = initialNode;
//		currentState = initialNode.getState();; 		
		int runNr = 1;
		String fileContents = "";
		
		while(!strTotal.endsWith(cmd_prompt) && runNr <= NuSMVConsole.simulationSteps) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			boolean finished = false;
			boolean noUpdate = true;
			
			while (noUpdate && !finished) {
				fileContents = IOUtils.readFileAsString(outFile);
				if (strTotal.endsWith(cmd_prompt) || 
						fileContents.contains("Trace Description")) {
					finished = true;
					break;
				}
				
				String[] runs = fileContents.split("AVAILABLE STATES");
				if (runs.length - 1 == runNr) {
					noUpdate = false;
					break;
				} 
				// nothing new was added, wait a while
				try {
					Thread.sleep(100);
//					logger.debug("Nothing new was added, waiting..");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			if (!finished) {
				pickNextState(fileContents);
			}
			runNr++;
		}
		
		synchronized(this) {
			return printConsole();
		}
	}
	
	
	private void pickNextState(String fileContents) throws IOException {
		
		List<NusmvState> states = 
			new ArrayList<NusmvState>();
		NusmvState currentState = (NusmvState) this.currentNode.getState();
		
		try {
			states = NuSMVOutputHandler.addTransitionsFromState(
					fileContents, currentState, sts);
		} catch (IllegalArgumentException e) {
			// read the file too soon, one action was not complete 
			try {
				logger.info("Read file to soon, waiting..");
				Thread.sleep(200);
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
			fileContents = IOUtils.readFileAsString(outFile);
			states = NuSMVOutputHandler.addTransitionsFromState(
					fileContents, currentState, sts);
		}
		
		if (!states.isEmpty()) {
			List<Node> nodes = currentNode.addChildren(states);
			int index = 0;
			if (nodes.size() > 1) {		
				for (int i = 0; i < nodes.size(); i++) {
					if (!nodes.get(i).subtreeIsVisited()) {
						index = i;
						break;
					}
				}
			}
			if (nodes.size() >= 1) {
				currentNode = nodes.get(index);
//				currentState = states.get(index);
				currentNode.visit();
				execCommand(String.valueOf(index));
			}				
		}
	}
	
	
	void loadConsole() throws IOException {
		File file = new File(nusmvFile);
		if (!file.exists()) {
			// TODO Throw an exception
			logger.error("Cannot find file " + nusmvFile);
		}
		
		String[] cmdarray = {nusmvPath + "NuSMV.exe", "-int", nusmvFile};
		ProcessBuilder builder = new ProcessBuilder(cmdarray);
		builder.redirectErrorStream(true);		
		proc = new ProcessBuilder(cmdarray).start();

		InputStream inputStream = proc.getInputStream();
		OutputStream outputStream = proc.getOutputStream();
		brInput = new BufferedReader(new InputStreamReader(inputStream));
		bwOutput = new BufferedWriter(new OutputStreamWriter(outputStream));
	}

	synchronized void execCommand(String command) throws IOException {
		logger.trace("Executing command " + command);
		bwOutput.write(command);
		bwOutput.newLine();
		bwOutput.flush();
	}
	

}