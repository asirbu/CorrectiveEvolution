package eu.fbk.soa.xml;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.transform.stream.StreamSource;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

import eu.fbk.soa.process.GoalWithPriorities;
import eu.fbk.soa.process.domain.DomainObject;
import eu.fbk.soa.util.CustomPrefixMapper;

public class XMLAdapter {

	public static DomainObject unmarshalDomainObject(File domainObjFile) {
		DomainObject obj = (DomainObject) unmarshalAnyObject("eu.fbk.soa.process.domain.DomainObject", domainObjFile);
		obj.updateEventReferences();

		return obj;
	}

	public static GoalWithPriorities unmarshalGoal(File goalFile) {
		return (GoalWithPriorities) unmarshalAnyObject("eu.fbk.soa.process.GoalWithPriorities", goalFile);
	}


	private static Object unmarshalAnyObject(String className, File file) {
		JAXBContext jc;
		Object obj = null;
		try {
			Class<?> myClass = Class.forName(className);
			//System.out.println(myClass);
			jc = JAXBContext.newInstance(myClass);

			Unmarshaller u = jc.createUnmarshaller();
			JAXBElement<?> root = u.unmarshal(
				new StreamSource(file), myClass);
			obj = root.getValue();

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return obj;
	}


	public static String marshal(Object obj) {
		JAXBContext jc;
		String xml = "";

		try {
			jc = JAXBContext.newInstance(obj.getClass());
			Marshaller marshaller = jc.createMarshaller();

			NamespacePrefixMapper prefixMapper = new CustomPrefixMapper();
			marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", prefixMapper);
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

			OutputStream output = new ByteArrayOutputStream();
			marshaller.marshal(obj, output);
			xml = output.toString();

		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (FactoryConfigurationError e) {
			e.printStackTrace();
		}
		return xml;
	}

	public static XMLProcessModel unmarshalModel(File file) {
		XMLProcessModel im = (XMLProcessModel) unmarshalAnyObject("eu.fbk.soa.xml.XMLProcessModel", file);
		return im;
	}

	public static XMLActivitySet unmarshalActivitySet(File file) {
		XMLActivitySet actSet = (XMLActivitySet) unmarshalAnyObject("eu.fbk.soa.xml.XMLActivitySet", file);
		return actSet;
	}

}
