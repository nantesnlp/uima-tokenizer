package uima.sandbox.lexer.models;

public interface Tree<T> {

	public boolean leaf();
	
	public Tree<T> get(T item);
	
}
