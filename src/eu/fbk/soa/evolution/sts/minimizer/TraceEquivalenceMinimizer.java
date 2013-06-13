package eu.fbk.soa.evolution.sts.minimizer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;

import eu.fbk.soa.evolution.sts.STS;
import eu.fbk.soa.util.ConfigUtils;


public class TraceEquivalenceMinimizer implements STSMinimizer {

	static Logger logger = Logger.getLogger(TraceEquivalenceMinimizer.class);
		
	@Override
	public void minimizeSTS(STS inputSTS, File outputFile) {
		String input2Dot = inputSTS.toDot();
		
		try {
			File tempFile = File.createTempFile("temp", "dot");
//			tempFile = new File("temp.dot");
			FileWriter fout = new FileWriter(tempFile);
	        fout.write(input2Dot);
	        fout.flush();
	        fout.close();
	        this.minimizeSTS(tempFile, outputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	@Override
	public void minimizeSTS(File inputFile, File outputFile) {
		String mcrl2Path;
		try {
			mcrl2Path = ConfigUtils.getProperty("mcrl2Path");
		
			String[] cmdarray = {mcrl2Path + "ltsconvert.exe", "-etrace", 
					inputFile.getPath(), outputFile.getPath()};
			ProcessBuilder builder = new ProcessBuilder(cmdarray);
			builder.redirectErrorStream(true);
			
			Process proc = new ProcessBuilder(cmdarray).start();
	        proc.waitFor();
	        
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
}
