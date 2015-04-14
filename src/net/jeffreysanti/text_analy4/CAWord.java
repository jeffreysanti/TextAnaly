/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jeffreysanti.text_analy4;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author jeffrey
 */
public class CAWord implements ComponentAnalyzer {
    
    private static final List<String> skipTags = Arrays.asList(
        ".",
        ",",
        ":",
        "?",
        ";",
        "-LRB-",
        "-RRB-",
        "''",
        "\"\"",
        "``",
        "POS");
    
    private static final List<String> lexicalTags = Arrays.asList(
        "CD",
        "FW",
        "JJ",
        "JJR",
        "JJS",
        "NN",
        "NNP",
        "NNPS",
        "NNS",
        "RB",
        "RBR",
        "RBS",
        "VB",
        "VBD",
        "VBG",
        "VBN",
        "VBP",
        "VBZ");
    
    private static final List<String> refTags = Arrays.asList(
        "NN",
        "NNP",
        "NNPS",
        "NNS");
    
    
    public CAWord(Connection db, Section s) throws SQLException
    {
        c = db;
        sec = s;
        Statement stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS worddist (word INTEGER NOT NULL, section INTEGER, cnt INTEGER)");
        stmt.close();
        stmt = c.createStatement();
        stmt.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS sec_word ON worddist (section,word)");
        stmt.close();
        
        rootCount = new HashMap();
    }
    
    private Connection c;
    private Section sec;
    private HashMap<Integer, Integer> rootCount = new HashMap(); // keeps track of root usage in section

    @Override
    public void sentenceStart(int id) {
    }

    @Override
    public void pushWord(String word, String tag, String stem, int stemId) {
        if(rootCount.containsKey(stemId))
            rootCount.put(stemId, rootCount.get(stemId)+1);
        else
            rootCount.put(stemId, 1);
    }
    
    public static boolean doSkipInCount(String pos)
    {
        return(skipTags.contains(pos));
    }
    
    public static boolean isLexicalWord(String pos)
    {
        return(lexicalTags.contains(pos));
    }
    
    public static boolean shouldConsiderReference(String pos)
    {
        return(refTags.contains(pos));
    }
    
    public static int getEnglishSyllableCount(String word)
    {
        return EnglishSyllableCounter.countSyllables(word);
    }

    @Override
    public void sentenceEnd() {
    }

    @Override
    public void sectionEnd() throws Exception {
        PreparedStatement stmt;
        stmt = c.prepareStatement("INSERT INTO worddist (word,section,cnt) VALUES (?,?,?)");
        for(int rid : rootCount.keySet()){
            stmt.setInt(1, rid);
            stmt.setInt(2, sec.getId());
            stmt.setInt(3, rootCount.get(rid));
            stmt.execute();
        }
        rootCount.clear();
        stmt.close();
    }

    @Override
    public void pushTagString(String tag) {
    }

    @Override
    public void pushNamedEntity(String word, int stemId, String ne, int wordIndex) throws Exception {
    }
    
}
