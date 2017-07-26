package uima.sandbox.lexer.engines;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uima.sandbox.lexer.models.Tree;
import uima.sandbox.lexer.models.Unit;
import uima.sandbox.lexer.resources.SegmentBank;

public class Lexer extends JCasAnnotator_ImplBase {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Lexer.class);
	
	private static final Pattern ABBREVIATION = Pattern.compile("^[A-Z][a-z]*\\.([A-Z][a-z]*\\.)+$");
	

	private AtomicLong totalTimeInMillis = new AtomicLong(0);

	
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
		long start = System.currentTimeMillis();
		if(LOGGER.isTraceEnabled()) 
			LOGGER.trace("tokenizing");
		List<AnnotationFS> tokens = this.tokenize(cas);
		
		Tree<Character> prefixes = this.bank.get("initial");
		Tree<Character> suffixes = this.bank.get("final");
		if (prefixes != null && suffixes != null) {
			if(LOGGER.isTraceEnabled()) 
				LOGGER.trace("Splitting");
			this.split(cas, tokens, prefixes, suffixes);
		}
		Tree<Character> compound = this.bank.get("compound");
		if (compound != null) {
			if(LOGGER.isTraceEnabled()) 
				LOGGER.trace("merging");
			this.merge(cas, compound);
			if(LOGGER.isTraceEnabled()) 
				LOGGER.trace("cleaning");
			this.clean(cas);			
		}
		
		long duration = System.currentTimeMillis() - start;
		
		totalTimeInMillis.addAndGet(duration);
		LOGGER.debug("Tokenized document in {}ms [Cumulated: {}ms]", 
				duration, 
				totalTimeInMillis.get());

	}
	
	/**
	 * remove word annotations covered by compound word ones.
	 * 
	 * @param cas the common analysis structure
	 */
	private void clean(JCas cas) {
		List<Annotation> remAnnotations = new ArrayList<Annotation>();
		Type type = this.getType(cas);
		AnnotationIndex<Annotation> index = cas.getAnnotationIndex(type);
		FSIterator<Annotation> iterator = index.iterator();
		
		Queue<Annotation> lastAnnotationBuffer = new LinkedList<>();
		int lastEnd = -1;
		
		Annotation annotation;
		while (iterator.hasNext()) {
			annotation = iterator.next();
			if(annotation.getBegin()>lastEnd) { 
				/*
				 * Fast, easy and most frequent case: the annotation does 
				 * not overlap with the ones in the buffer
				 */
				lastAnnotationBuffer.clear();
			} else {
				/*
				 * Does not mean that is is contained in another annotation.
				 */
				for(Annotation candidateContainer:lastAnnotationBuffer)
					if(candidateContainer.getBegin() <= annotation.getBegin()
					  && candidateContainer.getEnd() >= annotation.getEnd())
						// annotation is contained in candidateContainer
						remAnnotations.add(annotation);
			}
			lastEnd = Integer.max(lastEnd, annotation.getEnd());
			lastAnnotationBuffer.add(annotation);
		}
		for (Annotation a : remAnnotations) {
			a.removeFromIndexes();
		}
	}
	
	/**
	 * create word annotations over the document text
	 * 
	 * @param cas the common analysis structure
	 */
	private List<AnnotationFS> tokenize(JCas cas) {
		List<AnnotationFS> tokens = new ArrayList<>();
		String text = cas.getDocumentText();
		int begin = 0;
		int length = text.length();
		int cnt = 0;
		for (int index = begin; index < length; index++) {
			if (this.hasChanged(text,index)) {
				if (!this.areSpaces(text,begin,index)) {
					AnnotationFS annotation = this.createAnnotation(cas,begin,index);
					tokens.add(annotation);
//					cas.getCas().addFsToIndexes(annotation);
					cnt++;
				}
				begin = index;
			} 
		}
		if (!this.areSpaces(text,begin,length)) {
			AnnotationFS annotation = this.createAnnotation(cas,begin,length);
//			cas.getCas().addFsToIndexes(annotation);
			tokens.add(annotation);
			cnt++;
		}
		if(LOGGER.isTraceEnabled()) 
			LOGGER.trace("Number of annotations created and indexed: {}", cnt);
		return tokens;
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
	
	private void split(JCas cas, List<AnnotationFS> tokens, Tree<Character> prefixes, Tree<Character> suffixes) {
		List<AnnotationFS> splittedAnnotations = new ArrayList<AnnotationFS>();
		List<AnnotationFS> deletedAnnotations = new ArrayList<AnnotationFS>();
//		Type type = this.getType(cas);
//		AnnotationIndex<Annotation> index = cas.getAnnotationIndex(type);
//		FSIterator<Annotation> iterator = index.iterator();
		if(LOGGER.isTraceEnabled()) 
			LOGGER.trace("Iterating over all annotations");
//		while (iterator.hasNext()) {
//			Annotation annotation = iterator.next();
		for(AnnotationFS annotation:tokens) {
			
			/*
			 * Split prefixes
			 */
			AnnotationFS resultingAnnotation = this.splitPrefix(cas, annotation, prefixes, splittedAnnotations, deletedAnnotations);
			
			/*
			 * Split suffixes
			 */
			this.splitSuffix(cas, resultingAnnotation, suffixes, splittedAnnotations, deletedAnnotations);
		}
		
		if(LOGGER.isTraceEnabled()) 
			LOGGER.trace("Adding {} splitted annotations", splittedAnnotations.size());
		for (AnnotationFS annotation :splittedAnnotations) {
			tokens.add(annotation);
//			cas.getCas().addFsToIndexes(annotation);
		}
		if(LOGGER.isTraceEnabled()) 
			LOGGER.trace("Removing {} annotations", deletedAnnotations.size());
		Set<AnnotationFS> deletedAnnotationsSet = new HashSet<>();
		deletedAnnotationsSet.addAll(deletedAnnotations);
		for (AnnotationFS annotation:deletedAnnotations) 
			cas.getCas().removeFsFromIndexes(annotation);
		tokens.removeAll(deletedAnnotationsSet);
		if(LOGGER.isTraceEnabled()) 
			LOGGER.trace("Adding {} tokens to indexes", tokens.size());
		for(AnnotationFS fs:tokens) {
			
			cas.addFsToIndexes(fs);
		}
		if(LOGGER.isTraceEnabled()) 
			LOGGER.trace("Tokens indexed in CAS");		
	}

	private AnnotationFS splitPrefix(JCas cas, AnnotationFS annotation, Tree<Character> prefixes, List<AnnotationFS> splittedAnnotations, List<AnnotationFS> deletedAnnotations) {
		AnnotationFS coveringAnnotation = annotation;
		if(isAbbreviation(coveringAnnotation))
			// do not split abbreviations
			return coveringAnnotation;
		AnnotationFS prefix = this.findPrefix(cas,annotation.getBegin(),annotation.getEnd(),annotation.getBegin(),prefixes);
		if (prefix != null) {
			if ((coveringAnnotation = this.fill(cas, coveringAnnotation, prefix, splittedAnnotations, deletedAnnotations)) != null) {
				splittedAnnotations.add(prefix);
				return this.splitPrefix(cas, coveringAnnotation, prefixes, splittedAnnotations, deletedAnnotations);
			} 
		}
		return coveringAnnotation;
	}

	private boolean isAbbreviation(AnnotationFS coveringAnnotation) {
		String string = coveringAnnotation.getCoveredText();
		return isAbbreviation(string);
	}

	public boolean isAbbreviation(String string) {
		return ABBREVIATION.matcher(string).find();
	}

	private AnnotationFS splitSuffix(JCas cas, AnnotationFS annotation, Tree<Character> suffixes, List<AnnotationFS> splittedAnnotations, List<AnnotationFS> deletedAnnotations) {
		AnnotationFS coveringAnnotation = annotation;
		if(isAbbreviation(coveringAnnotation))
			// do not split abbreviations
			return coveringAnnotation;
		AnnotationFS suffix = this.findSuffix(cas,annotation.getBegin(),annotation.getEnd(),annotation.getEnd(),suffixes);
		if (suffix != null) {
			if ((coveringAnnotation = this.fill(cas, coveringAnnotation, suffix, splittedAnnotations, deletedAnnotations)) != null) {
				splittedAnnotations.add(suffix);
				return this.splitSuffix(cas, coveringAnnotation, suffixes, splittedAnnotations, deletedAnnotations);
			}
		}
		return coveringAnnotation;
	}

	private AnnotationFS findPrefix(JCas cas,int begin,int end,int index,Tree<Character> current) {
		if (index < end) {
			char ch = cas.getDocumentText().charAt(index);
			Character c = Character.toLowerCase(ch);
			Tree<Character> tree = current.get(c);
			if (tree == null) {
				if (current.leaf()) { 
					return this.createAnnotation(cas,begin,index);
				} else {
					return null;
				}
			} else {
				return this.findPrefix(cas,begin,end,index + 1,tree);
			}
		} else {
			return null;
		}
	}
	
	private AnnotationFS findSuffix(JCas cas,int begin,int end,int index,Tree<Character> current) {
		if (index > begin) {
			char ch = cas.getDocumentText().charAt(index - 1);
			Character c = Character.toLowerCase(ch);
			Tree<Character> tree = current.get(c);
			if (tree == null) {
				if (current.leaf()) {
					return this.createAnnotation(cas,index,end);
				} else {
					return null;
				}
			} else {
				return this.findSuffix(cas,begin,end,index - 1,tree);
			}
		} else {
			return null;
		}
	}
	
	private AnnotationFS fill(JCas cas,AnnotationFS coveringAnnotation,AnnotationFS coveredAnnotation,List<AnnotationFS> splittedAnnotations,List<AnnotationFS> deletedAnnotations) {
		if (coveringAnnotation.getBegin() < coveredAnnotation.getBegin()) {
			deletedAnnotations.add(coveringAnnotation);
			AnnotationFS newCoveringAnnotation = this.createAnnotation(cas, coveringAnnotation.getBegin(), coveredAnnotation.getBegin());
			splittedAnnotations.add(newCoveringAnnotation);
			return newCoveringAnnotation;
		} else if (coveredAnnotation.getEnd() < coveringAnnotation.getEnd()) {
			deletedAnnotations.add(coveringAnnotation);
			AnnotationFS newCoveringAnnotation = this.createAnnotation(cas, coveredAnnotation.getEnd(), coveringAnnotation.getEnd());
			splittedAnnotations.add(newCoveringAnnotation);
			return newCoveringAnnotation;
		} else {
			return null;
		}
	}
	
	protected AnnotationFS createAnnotation(JCas cas,int begin,int end) {
		return cas.getCas().createAnnotation(getType(cas), begin, end);
	}
	
}
