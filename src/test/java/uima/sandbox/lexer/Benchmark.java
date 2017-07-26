package uima.sandbox.lexer;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import uima.sandbox.lexer.engines.Lexer;

public class Benchmark {

	String text;
	
	@Before
	public void setup() {
		Path docPath = Tests.DOCS.resolve("we-fr-100k.txt");
		text = Tests.readFile(docPath);
		Logger logger = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		logger.setLevel(Level.TRACE);
	}

	@Test
	public void printAll() throws Exception {
		test(1000);
		test(2000);
		test(3000);
		test(4000);
		test(5000);
		test(6000);
		test(7000);
		test(8000);
	}

	private long test(int kiloBytes) throws Exception {
		Lexer lexer = Tests.getLexer();
		JCas cas = Tests.createCas(getTextOfSize(text, kiloBytes));
		Stopwatch sw = Stopwatch.createStarted();
		lexer.process(cas);
		sw.stop();
		long milliseconds = sw.elapsed(TimeUnit.MILLISECONDS);
		System.out.format("%8dk %10dms%n", kiloBytes, milliseconds);
		return milliseconds;
	}
	
	/*
	 * Produces a string a desired size
	 */
	private  String getTextOfSize(String text, long kiloBytes) {
		double ratio = (double)kiloBytes/100;
		int nbChars = (int)(text.length() * ratio);
		
		StringBuffer buffer = new StringBuffer();
	
		int i = 0;
		while(buffer.length() < nbChars) 
			buffer.append(text.charAt(i++ % text.length()));
		return buffer.toString();
	}
}
