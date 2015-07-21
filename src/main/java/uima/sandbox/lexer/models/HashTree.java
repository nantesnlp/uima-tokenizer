package uima.sandbox.lexer.models;

import java.util.HashMap;
import java.util.Map;

public class HashTree<T> implements Tree<T> {

	private HashTree<T> parent;
	
	public void setParent(HashTree<T> node) {
		this.parent = node;
	}
	
	private HashTree<T> getParent() {
		return this.parent;
	}
	
	private Map<T, HashTree<T>> children;
	
	private void setChildren() {
		this.children = new HashMap<T, HashTree<T>>();
	}
	
	private Map<T, HashTree<T>> getChildren() {
		return this.children;
	}
	
	private boolean leaf;
	
	private void leaf(boolean leaf) {
		this.leaf = leaf;
	}
	
	@Override
	public boolean leaf() {
		return this.leaf;
	}
	
	public HashTree() {
		this.setChildren();
		this.leaf(false);
	}

	public void add(T[] items,int index,int length) {
		if (index < length) {
			T item = items[index];
			HashTree<T> node = this.get(item);
			if (node == null) {
				node = new HashTree<T>();
				this.getChildren().put(item,node);
			}
			if (!node.leaf()) {
				node.leaf((index + 1) == length);
			}
			if (node.getParent() == null) {
				node.setParent(this);	
			}
			node.add(items, index + 1, length);
		}
	}

	@Override
	public HashTree<T> get(T item) {
		return this.getChildren().get(item);
	}
	
	protected int deep() {
		HashTree<T> parent = this.getParent();
		if (parent == null) {
			return 0;
		} else {
			return parent.deep() + 1;
		}
	}
	
	@Override
	public String toString() {
		String string = "";			
		for (T item : this.getChildren().keySet()) {
			HashTree<T> node = this.get(item);
			for (int index = 0; index < node.deep(); index++) {
				string += "  ";
			}
			string += item.toString();
			if (node.leaf()) {
				string += " * ";
			}
			string += "\n";
			string += node.toString();
		}
		return string;
	}
	
}
