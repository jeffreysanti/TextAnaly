/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jeffreysanti.text_analy4;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

/**
 *
 * @author jeffrey
 */
public class CATag implements ComponentAnalyzer {

    public static final int TYPE_SECTION_TAG = 1;
    public static final int TYPE_SENTENCE_TAG_SPECIFIC = 2;
    public static final int TYPE_SENTENCE_TAG_GROUPED = 3;  
    
    public CATag(Connection db, Section s) throws SQLException
    {
        c = db;
        sec = s;
        Statement stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS tags (type INTEGER NOT NULL, section INTEGER, sent INTEGER, name TEXT, val TEXT)");
        stmt.close();
        tagStack = new HashMap();
        tagStackLocal = new HashMap();
    }
    
    private Connection c;
    private Section sec;
    private int sentId;
    
    @Override
    public void sentenceStart(int id) throws Exception {
        tagStackLocal.clear();
        sentId = id;
    }

    @Override
    public void pushWord(String word, String tag, String stem, int stemId) throws Exception {
    }

    @Override
    public void sentenceEnd() throws Exception {
        // push sentence local tags
        PreparedStatement stmt;
        stmt = c.prepareStatement("INSERT INTO tags (type,section,sent,name,val) VALUES (?,?,?,?,?)");
        for(String key : tagStackLocal.keySet()){
            stmt.setInt(1, TYPE_SENTENCE_TAG_SPECIFIC);
            stmt.setInt(2, sec.getId());
            stmt.setInt(3, sentId);
            stmt.setString(4, key);
            stmt.setString(5, tagStackLocal.get(key));
            stmt.execute();
        }
        for(String key : tagStack.keySet()){
            if(tagStackLocal.containsKey(key)) // already added (global is less prioritized)
                continue;
            stmt.setInt(1, TYPE_SENTENCE_TAG_GROUPED);
            stmt.setInt(2, sec.getId());
            stmt.setInt(3, sentId);
            stmt.setString(4, key);
            stmt.setString(5, tagStack.get(key));
            stmt.execute();
        }
        stmt.close();
    }

    @Override
    public void sectionEnd() throws Exception {
        PreparedStatement stmt;
        stmt = c.prepareStatement("INSERT INTO tags (type,section,name,val) VALUES (?,?,?,?)");
        for(String key : sec.getTags().keySet()){
            stmt.setInt(1, TYPE_SECTION_TAG);
            stmt.setInt(2, sec.getId());
            stmt.setString(3, key);
            stmt.setString(4, sec.getTags().get(key));
            stmt.execute();
        }
        stmt.close();
        tagStack.clear();
    }
    
    
    private HashMap<String, String> tagStack;
    private HashMap<String, String> tagStackLocal;

    @Override
    public void pushTagString(String tag)
    {
        boolean global = false;
        if(tag.contains("###")){
            tag = tag.replaceAll("###", "");
            global = true;
        }else if(tag.contains("^^^")){
            tag = tag.replaceAll("\\^\\^\\^", "");
        }else{
            System.out.println("Incorrect Tag Format: "+tag);
            return;
        }
        String tagKey = tag.toLowerCase();
        String tagVal = "";
        if(tagKey.contains(",")){
            tagVal = tagKey.substring(tagKey.indexOf(",")+1);
            tagKey = tagKey.substring(0, tagKey.indexOf(","));
        }
        if(tagKey.charAt(0) == '/' && global){
            tagStack.remove(tagKey.substring(1));
        }else{
            if(global){
                tagStack.put(tagKey, tagVal);
            }else{
                tagStackLocal.put(tagKey, tagVal);
            }
        }
    }

    @Override
    public void pushNamedEntity(String word, int stemId, String ne, int wordIndex) throws Exception {
    }
    
}
