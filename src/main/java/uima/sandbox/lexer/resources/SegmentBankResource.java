package uima.sandbox.lexer.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import uima.sandbox.lexer.models.HashTree;
import uima.sandbox.lexer.models.Segment;
import uima.sandbox.lexer.models.SegmentFactory;
import uima.sandbox.lexer.models.Segments;

public class SegmentBankResource implements SegmentBank {

	private Map<String, HashTree<Character>> trees;
		
	@Override
	public HashTree<Character> get(String id) {
		return this.trees.get(id);
	}
	
	private void compile() {
		this.trees = new HashMap<String, HashTree<Character>>();
		for (Segment segment : this.model.getSegment()) {
			String type = segment.getType();
			Boolean reverse = segment.isReverse();
			String value = segment.getValue();
			HashTree<Character> tree = this.trees.get(type);
			if (tree == null) {
				tree = new HashTree<Character>();
				this.trees.put(type, tree);
			}
			if (reverse == null) {
                char[] cs = value.toCharArray();
                Character[] characters = new Character[cs.length];
                for (int index = 0; index < cs.length; index++) {
                        characters[index] = new Character(cs[index]);
                }
				tree.add(characters, 0, characters.length);
			} else if (reverse.booleanValue()) {
				String eulav = new StringBuffer(value).reverse().toString();
                char[] cs = eulav.toCharArray();
                Character[] characters = new Character[cs.length];
                for (int index = 0; index < cs.length; index++) {
                        characters[index] = new Character(cs[index]);
                }
				tree.add(characters, 0, characters.length);
			} else {
                char[] cs = value.toCharArray();
                Character[] characters = new Character[cs.length];
                for (int index = 0; index < cs.length; index++) {
                        characters[index] = new Character(cs[index]);
                }
				tree.add(characters, 0, characters.length);
			} 			
		}
	}
	
	@Override
	public void load(DataResource data) throws ResourceInitializationException {
		try {
			this.load(data.getInputStream());
		} catch (Exception e) {
			UIMAFramework.getLogger().log(Level.INFO, "Error load SegmentBankResource: " + data.getUri());
			throw new ResourceInitializationException(e);
		}
	}
	
	private Segments model;
	
	@Override
	public void load(InputStream inputStream) throws IOException {
		try {
			JAXBContext context = JAXBContext.newInstance(Segments.class);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			StreamSource source = new StreamSource(inputStream);
			JAXBElement<Segments> root = unmarshaller.unmarshal(source, Segments.class);
			this.model = root.getValue();
			this.compile();
		} catch (JAXBException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void store(OutputStream outputStream) throws IOException {
		if (this.model != null) {
			try {
				SegmentFactory factory = new SegmentFactory();
				JAXBContext context = JAXBContext.newInstance(Segments.class);
				JAXBElement<Segments> element = factory.createSegments(this.model);
				Marshaller marshaller = context.createMarshaller();
				marshaller.setProperty("jaxb.formatted.output",Boolean.TRUE);
				marshaller.marshal(element, outputStream);
			} catch (JAXBException e) {
				throw new IOException(e);
			}
		}
	}

}
