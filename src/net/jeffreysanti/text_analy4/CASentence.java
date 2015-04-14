/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jeffreysanti.text_analy4;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;

/**
 *
 * @author jeffrey
 */
public class CASentence implements ComponentAnalyzer {
    
    public CASentence(Connection db, Section s) throws SQLException
    {
        c = db;
        sec = s;
        Statement stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS slst (sec INTEGER, sent INTEGER, wcnt INTEGER, "
                + "lextokans INTEGER, unqWords INTEGER, complexwrds INTEGER, syl INTEGER, txt TEXT, rootlist TEXT"
                + Inquirer.getSqlCreateAddition()+" )");
        stmt.close();
        inqSent = new Inquirer();
    }
    
    private Connection c;
    private Section sec;
    
    private Inquirer inqSent;
    private int wordCount = 0;
    private int lexCount = 0;
    private int sylCount = 0;
    private int complexCount = 0;
    private String txt="";
    private ArrayList<Integer> roots = new ArrayList();
    int curSentNo;

    @Override
    public void sentenceStart(int id) {
        curSentNo = id;
        inqSent = new Inquirer();
        wordCount = 0;
        lexCount = 0;
        sylCount = 0;
        complexCount = 0;
        txt = "";
        roots.clear();
    }

    @Override
    public void pushWord(String word, String tag, String stem, int stemId) {
        
        txt += word + " ";
        
        if(CAWord.doSkipInCount(tag))
            return;
        
        wordCount ++;
        roots.add(stemId);
        
        if(CAWord.isLexicalWord(tag))
            lexCount ++;
        
        int syls = CAWord.getEnglishSyllableCount(word);
        if(syls >= 3)
            complexCount ++;
        sylCount += syls;
        
        inqSent.loadWord(word, stem);
    }

    @Override
    public void sentenceEnd() throws SQLException{
        PreparedStatement stmt;
        stmt = c.prepareStatement("INSERT INTO slst (sec,sent,wcnt,lextokans,unqWords,complexwrds,syl,txt,rootlist"+
                inqSent.getSqlInsertNames()+") VALUES (?,?,?,?,?,?,?,?,?"+inqSent.getSqlInsertValues()+")");
        stmt.setInt(1, sec.getId());
        stmt.setInt(2, curSentNo);
        stmt.setInt(3, wordCount);
        stmt.setInt(4, lexCount);
        stmt.setInt(5, roots.size());
        stmt.setInt(6, complexCount);
        stmt.setInt(7, sylCount);
        stmt.setString(8, txt);
        String rootlist = "";
        for(int rid : roots){
            rootlist += "|"+rid+";";
        }
        stmt.setString(9, rootlist);
        stmt.execute();
        stmt.close();
    }

    @Override
    public void sectionEnd() {
        //throw new UnsupportedOperationException("Not supported yet.");
        roots.clear();
    }

    @Override
    public void pushTagString(String tag) {
    }

    @Override
    public void pushNamedEntity(String word, int stemId, String ne, int wordIndex) throws Exception {
    }
    
}
