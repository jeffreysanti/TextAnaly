/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jeffreysanti.text_analy4;

/**
 *
 * @author jeffrey
 */
public interface ComponentAnalyzer {
    
    public void sentenceStart(int id) throws Exception;
    
    public void pushWord(String word, String tag, String stem, int stemId) throws Exception;
    public void pushNamedEntity(String word, int stemId, String ne, int wordIndex) throws Exception;
    public void sentenceEnd() throws Exception;
    public void sectionEnd() throws Exception;
    
    public void pushTagString(String tag);
}
