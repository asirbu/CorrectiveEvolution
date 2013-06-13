package eu.fbk.soa.util;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

public class CustomPrefixMapper extends NamespacePrefixMapper {

	@Override
    public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
		
		if (namespaceUri.equals("http://soa.fbk.eu/Object"))
			return "obj";
		if (namespaceUri.equals("http://soa.fbk.eu/Process"))
			return "process";
		if (namespaceUri.equals("http://docs.oasis-open.org/wsbpel/2.0/process/executable")) {
			return "bpel";
		}
		return "";
		
    }
	
	
}
