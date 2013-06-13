package eu.fbk.soa.util;


import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import eu.fbk.soa.evolution.sts.Action;
import eu.fbk.soa.evolution.sts.STS;
import eu.fbk.soa.evolution.sts.State;
import eu.fbk.soa.evolution.sts.Transition;
import eu.fbk.soa.evolution.sts.impl.DefaultAction;
import eu.fbk.soa.evolution.sts.impl.DefaultSTS;
import eu.fbk.soa.evolution.sts.impl.DefaultState;
import eu.fbk.soa.evolution.sts.impl.NusmvSTS;
import eu.fbk.soa.evolution.sts.impl.NusmvTransition;
import eu.fbk.soa.process.ProcessModel;


public class IOUtils {
	
	static Logger logger = Logger.getLogger(IOUtils.class);
	
	public static String readFileAsString(File file){
		String str = "";
		BufferedInputStream f = null;
	    try {
	        f = new BufferedInputStream(new FileInputStream(file));
	        int available = -1;
	        if ((available = f.available()) > 0) {
	        	byte[] buffer = new byte[available];
	        	f.read(buffer);
	        	str = new String(buffer);
	        }
	    } catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
	        if (f != null) {
	        	try { 
	        		f.close(); 
	        	} catch (IOException e) { 
	        		e.printStackTrace();}
	        	}
	    }
	    return str;
	}
	
	public static NusmvSTS readNusmvSTSFromFile(File file) {
		String str = IOUtils.readFileAsString(file);	
		NusmvSTS newSTS = new NusmvSTS();
	
		for (String line : str.split("\n")) {
			String[] split = line.split("->");
			if (split.length == 2) {
				String from = split[0];
				Map<String, String> varMap = new HashMap<String, String>();
				varMap.put("name.state", from);
				State fromState = newSTS.addState(varMap);

				String[] split2 = split[1].split("\\[label=\"");
				if (split2.length == 2) {
					String to = split2[0];
					varMap.put("name.state", to);
					State toState = newSTS.addState(varMap);

					String[] acts = split2[1].split("\\\\n");
					for (String act : acts) {
						String[] split3 = act.split(" = ");

						if (split3.length == 2) {
							String var = split3[0];
							String value = split3[1].substring(0, split3[1].length()-3);
							if (value.endsWith("\"")) {
								value = value.substring(0, value.length()-1);
							}
							varMap.clear();
							varMap.put(var, value);

							Action action = newSTS.addAction(varMap);
							newSTS.addTransition(new NusmvTransition(fromState, action, toState));

						}
					}
				}
			}
		}

		return newSTS;
	}
	
	public static STS readSTSFromFile(File file) {
		String str = IOUtils.readFileAsString(file);	
		STS newSTS = new DefaultSTS();
	
		for (String line : str.split("\n")) {
			String[] split = line.split("->");
			if (split.length == 2) {
				String from = split[0];
				State fromState = newSTS.getState(from);
				if (fromState == null) {
//					System.out.println("Creating state " + from);
					fromState = new DefaultState(from);
					newSTS.addState(fromState);
				}

				String[] split2 = split[1].split("\\[label=\"");
				if (split2.length == 2) {
					String to = split2[0];

					State toState = newSTS.getState(to);
					if (toState == null) {
						toState = new DefaultState(to);
						newSTS.addState(toState);
					}
					
					String actName = split2[1].substring(0, split2[1].length() - 4);
					if (actName.endsWith("\"")) {
						actName = actName.substring(0, actName.length() - 1);
					}
					Action action = new DefaultAction(actName, true);
					newSTS.addAction(action);
					newSTS.addTransition(new NusmvTransition(fromState, action, toState));
				}
			}
		}
		
		return newSTS;
	}
	
	public static void writeStringToFile(String str, String filePath) {
		File file = new File(filePath);
		
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(file, false));
			out.write(str);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void exportSTSToDot(STS sts, String filePath) {
		File file = new File(filePath);
		try {
			IOUtils.exportSTSToDot(sts, new FileWriter(file));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void exportSTSToDot(STS sts, Writer writer) {
		PrintWriter out = new PrintWriter(writer);
		out.println(sts.toDot());
		out.flush();
		out.close();
	}
	
	public static void createImage(String dotFilePath, String outputFilePath) {
		File dotFile = new File(dotFilePath);
		File img = new File(outputFilePath);
		
		try {
			String graphvizPath = ConfigUtils.getProperty("graphvizPath");
			
			String[] cmdarray = {graphvizPath + "dot.exe", "-Tpng", 
					dotFile.getAbsolutePath(), "-o", img.getAbsolutePath()};
			Runtime rt = Runtime.getRuntime();
			Process p;
			
			p = rt.exec(cmdarray);
			p.waitFor();

		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
	}

	
	public static void exportProcessModelToDot(ProcessModel model, String dotFilePath) {
			OrderedNodesDOTExporter dotExporter = new OrderedNodesDOTExporter();
			File file = new File(dotFilePath);
			FileWriter writer;
			try {
				writer = new FileWriter(file);
				dotExporter.export(writer, model);	
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	public static void exportSTSAsPNML(STS sts, String pnmlFilePath) {
//		StringBuffer buffer = new StringBuffer();
		String pnml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				"<pnml><net id=\"result\" " +
				"type=\"http://www.pnml.org/version-2009/grammar/pnmlcoremodel\">\n";
		
		for (State state : sts.getStates()) {
			pnml += "<place id=\"" + state.getName() + "\" />\n";
		}
		
		int arcIndex = 1;
		int actionIndex = 1;
	//	int newTransIndex = 1;
		
		for (Transition trans : sts.getTransitions()) {
			String actName = trans.getActionName();
			if (actName.isEmpty()) {
				actName = "Trans"; //+ newTransIndex;
				//newTransIndex++;
			}
			String pnmlTrans = actName + actionIndex;
			String type = IOUtils.getEventType(actName);
			String name = IOUtils.getEventName(actName);
			String logEventStr = "";
			if (!type.isEmpty()) {
				logEventStr = "\n <toolspecific tool=\"ProM\" version=\"5.2\">"
						+ "\n\t<logevent><name>" + name + "</name><type>" + type 
						+ "</type></logevent>\n </toolspecific>";
			}
			
			pnml += "\n<transition id=\"" + pnmlTrans  + "\">\n " 
					+ " <name><text>" + actName + "</text></name>" + logEventStr 
					+ "\n <graphics><position x=\"17.5\" y=\"15.0\"/>" 
					+ "<dimension x=\"25.0\" y=\"20.0\"/><fill color=\"#FFFFFF\"/>" 
					+ "</graphics>" + "\n</transition>\n";
			actionIndex++;
			
			pnml += "\n<arc id=\"a"+ arcIndex + "\" source=\"" + trans.getSource().getName() 
					+ "\" target=\"" + pnmlTrans + "\">" 
					+ "<arctype><text>normal</text></arctype></arc>\n"; 
			arcIndex++;
			
			pnml += "\n<arc id=\"a"+ arcIndex + "\" source=\"" + pnmlTrans 
					+ "\" target=\"" + trans.getTarget().getName() + "\">" 
					+ "<arctype><text>normal</text></arctype></arc>\n"; 
			arcIndex++;
		}
		
		pnml += "\n</net></pnml>";
		IOUtils.writeStringToFile(pnml, pnmlFilePath);
	}
	
	
	private static String getEventName(String actName) {
		String name = actName;
		if (actName.endsWith("_COMPLETE") || actName.endsWith("_SCHEDULE")) {
			name = actName.substring(0, actName.length() - 9);
		}
		if (actName.endsWith("_START")) {
			name = actName.substring(0, actName.length() - 6);
		}
		if (name.startsWith("W")) {
			name = name.replaceAll("_", " ");
			name = name.replaceFirst(" ", "_");
		}
		return name;
	}

	private static String getEventType(String actName) {
		String type = "";
		if (actName.contains("COMPLETE")) {
			type = "complete";
		} 
		if (actName.contains("SCHEDULE")) {
			type = "schedule";
		}
		if (actName.contains("START")) {
			type = "start";
		}
		return type;
	}
	
	
	
}
