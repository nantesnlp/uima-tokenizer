package uima.sandbox.lexer.engines;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import uima.sandbox.lexer.models.Tree;
import uima.sandbox.lexer.models.Unit;
import uima.sandbox.lexer.resources.SegmentBank;

public class Lexer extends JCasAnnotator_ImplBase {
	
	// parameters
	public static final String PARAM_TYPE = "Type";
	@ConfigurationParameter(name = PARAM_TYPE)
	private String type;
	
	// resources
	@ExternalResource(key = SegmentBank.KEY_SEGMENT_BANK)
	private SegmentBank bank;
	
	private Type getType(JCas cas) {
		return cas.getTypeSystem().getType(this.type);
	}

	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException {
		this.tokenize(cas);
		Tree<Character> prefixes = this.bank.get("initial");
		Tree<Character> suffixes = this.bank.get("final");
		if (prefixes != null && suffixes != null) {
			this.split(cas, prefixes, suffixes);
		}
		Tree<Character> compound = this.bank.get("compound");
		if (compound != null) {
			this.merge(cas, compound);
			this.clean(cas);			
		}
	}
	
	/**
	 * remove word annotations covered by compound word ones.
	 * 
	 * @param cas the common analysis structure
	 */
	private void clean(JCas cas) {
		List<Annotation> annotations = new ArrayList<Annotation>();
		Type type = this.getType(cas);
		AnnotationIndex<Annotation> index = cas.getAnnotationIndex(type);
		FSIterator<Annotation> iterator = index.iterator();
		while (iterator.hasNext()) {
			Annotation annotation = iterator.next();
			FSIterator<Annotation> subiterator = index.subiterator(annotation);
			while (subiterator.hasNext()) {
				Annotation a = subiterator.next();
				annotations.add(a);
			}
		}
		for (Annotation a : annotations) {
			a.removeFromIndexes();
		}
	}
	
	/**
	 * create word annotations over the document text
	 * 
	 * @param cas the common analysis structure
	 */
	private void tokenize(JCas cas) {
		Type type = this.getType(cas);
		String text = cas.getDocumentText();
		int begin = 0;
		int length = text.length();
		for (int index = begin; index < length; index++) {
			if (this.hasChanged(text,index)) {
				if (!this.areSpaces(text,begin,index)) {
					AnnotationFS annotation = this.createAnnotation(cas,type,begin,index);
					cas.getCas().addFsToIndexes(annotation);
				}
				begin = index;
			} 
		}
			if (!this.areSpaces(text,begin,length)) {
				AnnotationFS annotation = this.createAnnotation(cas,type,begin,length);
				cas.getCas().addFsToIndexes(annotation);
			}
	}

	private boolean areSpaces(String text, int begin, int end) {
		for (int index = begin; index < end; index++) {
			char current = text.charAt(index);
			if(!Character.isWhitespace(current))
				return false;
		}
		return true;
	}

	protected boolean hasChanged(String text,int index) {
		if (index == 0) {
			return false;
		} else {
			char previous = text.charAt(index - 1);
			char current = text.charAt(index);
			if (Character.isWhitespace(previous) && !Character.isWhitespace(current)) {
				return true;
			} else if (!Character.isWhitespace(previous) && Character.isWhitespace(current)) {
				return true;
			} else {
				return false;
			} 
		}
	}

	private void merge(JCas cas, Tree<Character> root) {
		String text = cas.getDocumentText();
		Type type = this.getType(cas);
		Map<Tree<Character>,Unit> currents = new HashMap<Tree<Character>,Unit>();
		currents.put(root,new Unit(type,0));
		int length = text.length();
		for (int index = 0; index < length; index++) {
			char ch = Character.toLowerCase(text.charAt(index));
			Character character = new Character(ch);
			this.filter(cas,currents,character,index);
			if (currents.isEmpty()) {
				currents.put(root,new Unit(type,index + 1));
			} else {
				boolean done = false;
				for (Tree<Character> current : currents.keySet()) {
					if (current.leaf()) {
						Unit word = currents.get(current);
						word.update(index + 1);
						done = true;
					}
				}
				if (done) {
					currents.put(root,new Unit(type,index + 1));
				}
			}
		}
	}
	
	private void filter(JCas cas,Map<Tree<Character>,Unit> currents, Character character,int index) {
		Map<Tree<Character>,Unit> nexts = new HashMap<Tree<Character>,Unit>();
		for (Tree<Character> current : currents.keySet()) {
			Tree<Character> next = current.get(character);
			Unit word = currents.get(current);
			if (next == null) {
				word.fire(cas);
			} else {
				nexts.put(next,word);
			}
		}
		currents.clear();
		currents.putAll(nexts);
	}
	
	private void split(JCas cas, Tree<Character> prefixes, Tree<Character> suffixes) {
		List<AnnotationFS> splittedAnnotations = new ArrayList<AnnotationFS>();
		List<AnnotationFS> deletedAnnotations = new ArrayList<AnnotationFS>();
		Type type = this.getType(cas);
		AnnotationIndex<Annotation> index = cas.getAnnotationIndex(type);
		FSIterator<Annotation> iterator = index.iterator();
		while (iterator.hasNext()) {
			Annotation annotation = iterator.next();
			AnnotationFS resultingAnnotation = this.splitPrefix(cas, type, annotation, prefixes, splittedAnnotations, deletedAnnotations);
			this.splitSuffix(cas, type, resultingAnnotation, suffixes, splittedAnnotations, deletedAnnotations);
		}
		for (AnnotationFS annotation :splittedAnnotations) 
			cas.getCas().addFsToIndexes(annotation);
		for (AnnotationFS annotation:deletedAnnotations) 
			cas.getCas().removeFsFromIndexes(annotation);
	}

	private AnnotationFS splitPrefix(JCas cas, Type type, AnnotationFS annotation, Tree<Character> prefixes, List<AnnotationFS> splittedAnnotations, List<AnnotationFS> deletedAnnotations) {
		AnnotationFS coveringAnnotation = annotation;
		AnnotationFS prefix = this.findPrefix(cas,type,annotation.getBegin(),annotation.getEnd(),annotation.getBegin(),prefixes);
		if (prefix != null) {
			if ((coveringAnnotation = this.fill(cas, type, coveringAnnotation, prefix, splittedAnnotations, deletedAnnotations)) != null) {
				splittedAnnotations.add(prefix);
				return this.splitPrefix(cas, type, coveringAnnotation, prefixes, splittedAnnotations, deletedAnnotations);
			} 
		}
		return coveringAnnotation;
	}

	private AnnotationFS splitSuffix(JCas cas, Type type, AnnotationFS annotation, Tree<Character> suffixes, List<AnnotationFS> splittedAnnotations, List<AnnotationFS> deletedAnnotations) {
		AnnotationFS coveringAnnotation = annotation;
		AnnotationFS suffix = this.findSuffix(cas,type,annotation.getBegin(),annotation.getEnd(),annotation.getEnd(),suffixes);
		if (suffix != null) {
			if ((coveringAnnotation = this.fill(cas, type, coveringAnnotation, suffix, splittedAnnotations, deletedAnnotations)) != null) {
				splittedAnnotations.add(suffix);
				return this.splitSuffix(cas, type, coveringAnnotation, suffixes, splittedAnnotations, deletedAnnotations);
			}
		}
		return coveringAnnotation;
	}

	private AnnotationFS findPrefix(JCas cas,Type type,int begin,int end,int index,Tree<Character> current) {
		if (index < end) {
			char ch = cas.getDocumentText().charAt(index);
			Character c = Character.toLowerCase(ch);
			Tree<Character> tree = current.get(c);
			if (tree == null) {
				if (current.leaf()) { 
					return this.createAnnotation(cas,type,begin,index);
				} else {
					return null;
				}
			} else {
				return this.findPrefix(cas,type,begin,end,index + 1,tree);
			}
		} else {
			return null;
		}
	}
	
	private AnnotationFS findSuffix(JCas cas,Type type,int begin,int end,int index,Tree<Character> current) {
		if (index > begin) {
			char ch = cas.getDocumentText().charAt(index - 1);
			Character c = Character.toLowerCase(ch);
			Tree<Character> tree = current.get(c);
			if (tree == null) {
				if (current.leaf()) {
					return this.createAnnotation(cas,type,index,end);
				} else {
					return null;
				}
			} else {
				return this.findSuffix(cas,type,begin,end,index - 1,tree);
			}
		} else {
			return null;
		}
	}
	
	private AnnotationFS fill(JCas cas,Type type,AnnotationFS coveringAnnotation,AnnotationFS coveredAnnotation,List<AnnotationFS> splittedAnnotations,List<AnnotationFS> deletedAnnotations) {
		if (coveringAnnotation.getBegin() < coveredAnnotation.getBegin()) {
			deletedAnnotations.add(coveringAnnotation);
			AnnotationFS newCoveringAnnotation = this.createAnnotation(cas, type, coveringAnnotation.getBegin(), coveredAnnotation.getBegin());
			splittedAnnotations.add(newCoveringAnnotation);
			return newCoveringAnnotation;
		} else if (coveredAnnotation.getEnd() < coveringAnnotation.getEnd()) {
			deletedAnnotations.add(coveringAnnotation);
			AnnotationFS newCoveringAnnotation = this.createAnnotation(cas, type, coveredAnnotation.getEnd(), coveringAnnotation.getEnd());
			splittedAnnotations.add(newCoveringAnnotation);
			return newCoveringAnnotation;
		} else {
			return null;
		}
	}
	
	protected AnnotationFS createAnnotation(JCas cas,Type type,int begin,int end) {
		return cas.getCas().createAnnotation(type, begin, end);
	}
	
}
