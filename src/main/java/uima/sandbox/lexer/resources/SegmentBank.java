package uima.sandbox.lexer.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.uima.resource.SharedResourceObject;

import uima.sandbox.lexer.models.Tree;

public interface SegmentBank extends SharedResourceObject {
	
	public static final String KEY_SEGMENT_BANK = "Bank";

	public Tree<Character> get(String id);
	
	public void load(InputStream inputStream) throws IOException;
	
	public void store(OutputStream outputStream) throws IOException;
	
}
