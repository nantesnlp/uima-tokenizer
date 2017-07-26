package uima.sandbox.lexer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.uima.jcas.JCas;
import org.junit.Test;

import fr.univnantes.julestar.uima.testing.UIMATest;
import uima.sandbox.lexer.engines.Lexer;

public class LexerSpec {

	@Test
	public void testCompound() throws Exception {
		JCas cas = Tests.tokenize("Ce c'est-à-dire reste.");
		UIMATest.assertThat(cas)
		.containsAnnotation("WordAnnotation", 0, 2)
		.containsAnnotation("WordAnnotation", 3, 15)
		.containsAnnotation("WordAnnotation", 16, 21)
		.containsAnnotation("WordAnnotation", 21, 22)
		.hasNAnnotationOfType("WordAnnotation", 4);
		
	}

	@Test
	public void supportAbbreviations() throws Exception {
		JCas cas = Tests.tokenize("Dupont va à la C.A.F. pour");
		UIMATest.assertThat(cas)
			.containsAnnotation("WordAnnotation", 0, 6)
			.containsAnnotation("WordAnnotation", 7, 9)
			.containsAnnotation("WordAnnotation", 10, 11)
			.containsAnnotation("WordAnnotation", 12, 14)
			.containsAnnotation("WordAnnotation", 15, 21)
			.containsAnnotation("WordAnnotation", 22, 26)
			.hasNAnnotationOfType("WordAnnotation", 6)
			;
	}

	@Test
	public void supportTitle() throws Exception {
		JCas cas = Tests.tokenize("M. Dupont.");
		UIMATest.assertThat(cas)
		.containsAnnotation("WordAnnotation", 0, 2)
		.containsAnnotation("WordAnnotation", 3, 9)
		.containsAnnotation("WordAnnotation", 9, 10)
		.hasNAnnotationOfType("WordAnnotation", 3)
		;
	}
	
	@Test
	public void testIsAbbreviation() throws Exception {
		Lexer lexer = Tests.getLexer();
		assertFalse(lexer.isAbbreviation(""));
		assertFalse(lexer.isAbbreviation("M"));
		assertFalse(lexer.isAbbreviation("Mgergaegvae"));
		assertFalse(lexer.isAbbreviation("Mger.gaegvae"));
		assertFalse(lexer.isAbbreviation("CAF"));
		assertFalse(lexer.isAbbreviation("Cas.Aéd.Fs."));
		assertFalse(lexer.isAbbreviation("M."));
		assertTrue(lexer.isAbbreviation("M.M."));
		assertTrue(lexer.isAbbreviation("C.A.F."));
		assertTrue(lexer.isAbbreviation("Cas.Aed.Fs."));
	}

	@Test
	public void doNotSplitSentencesWhenSpaceMissingNormal() throws Exception {
		JCas cas = Tests.tokenize("Je vais bien.Tout va bien.");
		UIMATest.assertThat(cas)
			.containsAnnotation("WordAnnotation", 0, 2)
			.containsAnnotation("WordAnnotation", 3, 7)
			.containsAnnotation("WordAnnotation", 8, 17)
			.containsAnnotation("WordAnnotation", 18, 20)
			.containsAnnotation("WordAnnotation", 21, 25)
			.containsAnnotation("WordAnnotation", 25, 26)
			.hasNAnnotationOfType("WordAnnotation", 6)
			;
	}


	@Test
	public void processNormal() throws Exception {
		JCas cas = Tests.tokenize("La mère Michèle.");
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
		JCas cas = Tests.tokenize("La .");
		UIMATest.assertThat(cas)
			.containsAnnotation("WordAnnotation", 0, 2)
			.containsAnnotation("WordAnnotation", 3, 4)
			.hasNAnnotationOfType("WordAnnotation", 2)
			;
	}

	@Test
	public void doNotprocessLastWhitespaces() throws Exception {
		JCas cas = Tests.tokenize("La la ");
		UIMATest.assertThat(cas)
			.containsAnnotation("WordAnnotation", 0, 2)
			.containsAnnotation("WordAnnotation", 3, 5)
			.hasNAnnotationOfType("WordAnnotation", 2)
			;
	}


	public void processLastSize2Token() throws Exception {
		JCas cas = Tests.tokenize("La li");
		UIMATest.assertThat(cas)
			.containsAnnotation("WordAnnotation", 0, 2)
			.containsAnnotation("WordAnnotation", 3, 5)
			.hasNAnnotationOfType("WordAnnotation", 2)
			;
	}

	
	@Test
	public void processWithSuffix() throws Exception {
		JCas cas = Tests.tokenize("La mère Michèle a-t-elle perdu son chat?");
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
		JCas cas = Tests.tokenize("Fermé jusqu'à demain.");
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
		JCas cas = Tests.tokenize("Tout (va-t-il) bien?");
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



}
