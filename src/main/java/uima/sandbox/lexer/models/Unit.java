package uima.sandbox.lexer.models;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;

public class Unit {
	
	private int begin;
	
	private int end;
			
	private Type type;
	
	public Unit(Type type,int begin) {
		this(type,begin,0);
	}
	
	public Unit(Type type,int begin,int end) {
		this.type = type;
		this.begin = begin;
		this.end = end;
	}
	
	public void update(int end) {
		this.end = end;
	}
	
	public void fire(JCas cas) {
		if (this.begin < this.end) {
			AnnotationFS annotation = cas.getCas().createAnnotation(this.type, this.begin, this.end);
			cas.getCas().addFsToIndexes(annotation);
		}
	}
	
}
