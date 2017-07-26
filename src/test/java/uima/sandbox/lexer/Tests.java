package uima.sandbox.lexer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.mockito.Mockito;

import com.google.common.base.Charsets;

import uima.sandbox.lexer.engines.Lexer;
import uima.sandbox.lexer.resources.SegmentBankResource;

public class Tests {
	public static final Path RES = Paths.get("src", "test", "resources");
	public static final Path DOCS = RES.resolve("docs");
	public static final Path FRENCH_BANK = RES.resolve("bank").resolve("french-segment-bank.xml");
	public static final Path TERMSUITE_TYPE_SYSTEM = RES.resolve("TermSuite_TS.xml");

	public static TypeSystemDescription getTermSuiteTypeSystem() {
		return TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(TERMSUITE_TYPE_SYSTEM.toString());
	}

	public static Lexer getLexer() throws IOException, FileNotFoundException, ResourceInitializationException,
			NoSuchFieldException, IllegalAccessException {
		Lexer lexer = new Lexer();
		SegmentBankResource segmentBankResource = new SegmentBankResource();
		DataResource data = Mockito.mock(DataResource.class);
		Mockito.when(data.getInputStream()).thenReturn(new FileInputStream(Tests.FRENCH_BANK.toString()));
		segmentBankResource.load(data);
		Field typeField = Lexer.class.getDeclaredField("type");
		typeField.setAccessible(true);
		typeField.set(lexer, "fr.univnantes.termsuite.types.WordAnnotation");
		Field bankField = Lexer.class.getDeclaredField("bank");
		bankField.setAccessible(true);
		bankField.set(lexer, segmentBankResource);
		return lexer;
	}

	public static String readFile(Path path) throws RuntimeException {
		byte[] encoded;
		try {
			encoded = Files.readAllBytes(path);
			return new String(encoded, Charsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static JCas tokenize(String string) {
		Lexer lexer;
		try {
			lexer = Tests.getLexer();
			JCas cas = createCas(string);
			lexer.process(cas);
			return cas;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static JCas createCas(String string) throws UIMAException {
		JCas cas = JCasFactory.createJCas(getTermSuiteTypeSystem());
		cas.setDocumentText(string);
		return cas;
	}


}
