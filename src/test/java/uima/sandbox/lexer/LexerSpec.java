package uima.sandbox.lexer;

import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.univnantes.julestar.uima.testing.UIMATest;
import uima.sandbox.lexer.engines.Lexer;
import uima.sandbox.lexer.resources.SegmentBankResource;

public class LexerSpec {

	private static final Path RES = Paths.get("src", "test", "resources");
	private static final Path FRENCH_BANK = RES.resolve("bank").resolve("french-segment-bank.xml");
	private static final Path TYPE_SYSTEM = RES.resolve("TermSuite_TS.xml");
	
	Lexer lexer;
	TypeSystemDescription typeSystemDesc;
	
	@Before
	public void setup() throws Exception {
		lexer = new Lexer();
		SegmentBankResource segmentBankResource = new SegmentBankResource();
		DataResource data = Mockito.mock(DataResource.class);
		Mockito.when(data.getInputStream()).thenReturn(new FileInputStream(FRENCH_BANK.toString()));
		segmentBankResource.load(data);
		Field typeField = Lexer.class.getDeclaredField("type");
		typeField.setAccessible(true);
		typeField.set(lexer, "fr.univnantes.termsuite.types.WordAnnotation");
		Field bankField = Lexer.class.getDeclaredField("bank");
		bankField.setAccessible(true);
		bankField.set(lexer, segmentBankResource);
		typeSystemDesc = TypeSystemDescriptionFactory
				.createTypeSystemDescriptionFromPath(TYPE_SYSTEM.toString());
	}
	

	@Test
	public void testCompound() throws Exception {
		JCas cas = tokenize("Ce c'est-à-dire reste.");
		UIMATest.assertThat(cas)
		.containsAnnotation("WordAnnotation", 0, 2)
		.containsAnnotation("WordAnnotation", 3, 15)
		.containsAnnotation("WordAnnotation", 16, 21)
		.containsAnnotation("WordAnnotation", 21, 22)
		.hasNAnnotationOfType("WordAnnotation", 4);
		
	}

	@Test
	public void doesNotSupportAbbrs() throws Exception {
		JCas cas = tokenize("M. Dupont est là !");
		UIMATest.assertThat(cas)
		.containsAnnotation("WordAnnotation", 0, 1)
		.containsAnnotation("WordAnnotation", 1, 2)
		.containsAnnotation("WordAnnotation", 3, 9)
		.containsAnnotation("WordAnnotation", 10, 13)
		.containsAnnotation("WordAnnotation", 14, 16)
		.containsAnnotation("WordAnnotation", 17, 18)
		.hasNAnnotationOfType("WordAnnotation", 6)
		;

		
	}
	
	@Test
	public void processNormal() throws Exception {
		JCas cas = tokenize("La mère Michèle.");
		UIMATest.assertThat(cas)
			.containsAnnotation("WordAnnotation", 0, 2)
			.containsAnnotation("WordAnnotation", 3, 7)
			.containsAnnotation("WordAnnotation", 8, 15)
			.containsAnnotation("WordAnnotation", 15, 16)
			.hasNAnnotationOfType("WordAnnotation", 4)
			;
	}

	@Test
	public void processLastSingleToken() throws Exception {
		JCas cas = tokenize("La .");
		UIMATest.assertThat(cas)
			.containsAnnotation("WordAnnotation", 0, 2)
			.containsAnnotation("WordAnnotation", 3, 4)
			.hasNAnnotationOfType("WordAnnotation", 2)
			;
	}

	@Test
	public void doNotprocessLastWhitespaces() throws Exception {
		JCas cas = tokenize("La la ");
		UIMATest.assertThat(cas)
			.containsAnnotation("WordAnnotation", 0, 2)
			.containsAnnotation("WordAnnotation", 3, 5)
			.hasNAnnotationOfType("WordAnnotation", 2)
			;
	}


	public void processLastSize2Token() throws Exception {
		JCas cas = tokenize("La li");
		UIMATest.assertThat(cas)
			.containsAnnotation("WordAnnotation", 0, 2)
			.containsAnnotation("WordAnnotation", 3, 5)
			.hasNAnnotationOfType("WordAnnotation", 2)
			;
	}

	
	@Test
	public void processWithSuffix() throws Exception {
		JCas cas = tokenize("La mère Michèle a-t-elle perdu son chat?");
		UIMATest.assertThat(cas)
			.containsAnnotation("WordAnnotation", 0, 2)
			.containsAnnotation("WordAnnotation", 3, 7)
			.containsAnnotation("WordAnnotation", 8, 15)
			.containsAnnotation("WordAnnotation", 16, 17)
			.containsAnnotation("WordAnnotation", 17, 24)
			.hasNAnnotationOfType("WordAnnotation", 9)
			;
	}
	
	@Test
	public void processWithPrefix() throws Exception {
		JCas cas = tokenize("Fermé jusqu'à demain.");
		UIMATest.assertThat(cas)
			.containsAnnotation("WordAnnotation", 0, 5)
			.containsAnnotation("WordAnnotation", 6, 12)
			.containsAnnotation("WordAnnotation", 12, 13)
			.containsAnnotation("WordAnnotation", 14, 20)
			.containsAnnotation("WordAnnotation", 20, 21)
			.hasNAnnotationOfType("WordAnnotation", 5)
			;
	}
	
	
	@Test
	public void processWithPrefixAndDoubleSuffix() throws Exception {
		JCas cas = tokenize("Tout (va-t-il) bien?");
		UIMATest.assertThat(cas)
			.containsAnnotation("WordAnnotation", 0, 4)
			.containsAnnotation("WordAnnotation", 5, 6)
			.containsAnnotation("WordAnnotation", 6, 8)
			.containsAnnotation("WordAnnotation", 8, 13)
			.containsAnnotation("WordAnnotation", 13, 14)
			.containsAnnotation("WordAnnotation", 15, 19)
			.containsAnnotation("WordAnnotation", 19, 20)
			.hasNAnnotationOfType("WordAnnotation", 7)
			;
	}


	private JCas tokenize(String string) throws Exception{
		JCas cas = JCasFactory.createJCas(typeSystemDesc);
		cas.setDocumentText(string);
		lexer.process(cas);
		return cas;
	}

}
