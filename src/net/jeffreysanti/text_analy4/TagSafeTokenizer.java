/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jeffreysanti.text_analy4;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.LexedTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import java.io.Reader;
import java.util.Iterator;

/**
 *
 * @author jeffrey
 */
public class TagSafeTokenizer extends PTBTokenizer<HasWord> {
    
    public TagSafeTokenizer(final Reader r,
                      final LexedTokenFactory<HasWord> tokenFactory,
                      final String options) 
    {
        super(r, tokenFactory, options);
    }
    
}
