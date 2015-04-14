/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jeffreysanti.text_analy4;

import edu.stanford.nlp.pipeline.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author jeffrey
 */
public class Section {
    
    Section(String s)
    {
        this(s, -1, true);
    }
    
    Section(String s, int id)
    {
        this(s, id, true);
    }
    
    Section(String s, int id, boolean extractTags){
        txt = s;
        this.id = id;
        tags = new HashMap();
        bExtractingTags = extractTags;
        
        // now we need to extract the tags inside it
        if(bExtractingTags){
            Matcher matcher = ptrn.matcher(s);
            while (matcher.find()) {
                String tagDesc = s.substring(matcher.start()+3, matcher.end()-3);
                String tagKey = tagDesc;
                String tagVal = "";
                if(tagKey.contains(",")){
                    tagVal = tagKey.substring(tagKey.indexOf(",")+1);
                    tagKey = tagKey.substring(0, tagKey.indexOf(","));
                }
                tags.put(tagKey, tagVal);
            }
            txt = matcher.replaceAll(""); // remove all of these tags
        }
    }
    
    public boolean isExtractingTags(){
        return bExtractingTags;
    }
    
    public HashMap<String,String> getTags()
    {
        return tags;
    }
    
    public void setID(int id){
        this.id = id;
    }
    
    public String getText(){
        return txt;
    }
    public int getId(){
        return id;
    }
    public void setAnnotationDoc(Annotation d)
    {
        doc = d;
    }
    public Annotation getDoc()
    {
        return doc;
    }
    
    private static final Pattern ptrn = Pattern.compile("@@@.{1,100}@@@", Pattern.CASE_INSENSITIVE);
    
    
    private String txt;
    private int id;
    private HashMap<String, String> tags;
    private boolean bExtractingTags;
    private Annotation doc;
}
