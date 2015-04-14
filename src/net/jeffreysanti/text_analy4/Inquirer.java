package net.jeffreysanti.text_analy4;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author jeffrey
 */
public class Inquirer {
    
    static public void initialize(String dbLoc)
    {
        // Positivity
        reconTraits.add("Pos");
        reconTraits.add("Neg");
        
        // Strength
        reconTraits.add("Strng");
        reconTraits.add("Weak");
        
        // Feeling
        reconTraits.add("Pleasure");
        reconTraits.add("Pain");
        reconTraits.add("Arousal");
        reconTraits.add("Feel");
        reconTraits.add("Virtue");
        reconTraits.add("Vice");
        
        reconTraits.add("Ovrst");
        reconTraits.add("Undrst");
        
        reconTraits.add("MALE");
        reconTraits.add("Female");
        
        
        
        try {
            c = DriverManager.getConnection("jdbc:sqlite:"+dbLoc);
            
            // test connection
            PreparedStatement pst = c.prepareStatement("SELECT * FROM inquirer");
            ResultSet r = pst.executeQuery();
            if(!r.next()){
                System.err.println("INQUIRER DB HAS NO WORDS!!!");
            }
        } catch (SQLException ex) {
            Logger.getLogger(Inquirer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public Inquirer(){
        traits = new HashMap();
        for(String key : reconTraits){
            traits.put(key, 0.0);
        }
    }
    
        
    public void loadWord(String word, String alt)
    {
        try {
            PreparedStatement pst = c.prepareStatement("SELECT * FROM inquirer WHERE word = ?");
            pst.setString(1, word);
            ResultSet r = pst.executeQuery();
            if(r.next()){
                HashMap<String, Double> ts = RSToTraitSet(r);
                for(String key : ts.keySet()){
                    traits.put(key, traits.get(key)+ts.get(key));
                }
            }else{
                pst.setString(1, alt);
                r = pst.executeQuery();
                if(r.next()){
                    HashMap<String, Double> ts = RSToTraitSet(r);
                    for(String key : ts.keySet()){
                        traits.put(key, traits.get(key)+ts.get(key));
                    }
                }
            }
            pst.close();
                     
        } catch (SQLException ex) {
            Logger.getLogger(Inquirer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /*static public void createInqDb(String loc) throws SQLException, FileNotFoundException, IOException
    {
     
        // import inquirer data
        File f = new File("inqdict.txt");
        BufferedReader reader = new BufferedReader(new FileReader(f));
        String line=null;


        
        Statement stmt = c.createStatement();
        String qstr = "CREATE TABLE IF NOT EXISTS inquirer (word STRING";
        for(String s : reconTraits){
            qstr += ", " + s+" REAL";
        }
        stmt.executeUpdate(qstr + ")");
        stmt.close();
        stmt = c.createStatement();
        stmt.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS wrd ON inquirer (word)");
        stmt.close();

        c.setAutoCommit(false);
        while((line=reader.readLine()) != null){
            // strip off word name
            String word = line.substring(0, line.indexOf(" ")).toLowerCase();
            if(word.contains("#"))
                word = word.substring(0, word.indexOf("#"));

            line = line.substring(line.indexOf(" ")+1);

            // Now assemeble all the attributes
            if(!line.contains("|"))
                continue; // not part of db
            String mod = line.substring(line.indexOf("|")+1);
            line = line.substring(0, line.indexOf("|"));
            double mult = 1;
            if(mod.contains("%")){
                mult = Integer.parseInt(mod.substring(1, mod.indexOf("%")));
                mult /= 100;
            }
            
            HashMap<String, Double> attrVals = new HashMap();
            String []attrs = line.split(" ");
            for(String a : attrs){
                if(!reconTraits.contains(a)){
                    unkTraits.add(a);
                    continue;
                }
                attrVals.put(a, mult);
            }
            
            if(attrVals.size() <= 0)
                continue;
            
            for(String key : reconTraits){
                if(!attrVals.containsKey(key))
                    attrVals.put(key, 0.0);
            }
            
            // find out if word already in db
            PreparedStatement pst = c.prepareStatement("SELECT * FROM inquirer WHERE word = ?");
            pst.setString(1, word);
            ResultSet r = pst.executeQuery();
            if(r.next()){
                HashMap<String, Double> old = RSToTraitSet(r);
                qstr = "UPDATE inquirer SET ";
                boolean first=true;
                for(String key : attrVals.keySet()){
                    if(!first)
                        qstr += ", ";
                    old.put(key, old.get(key) + attrVals.get(key));
                    qstr += key+"="+old.get(key);
                    first = false;
                }
                qstr += " WHERE word=?";
                PreparedStatement pst2 = c.prepareStatement(qstr);
                pst2.setString(1, word);
                pst2.executeUpdate();
                pst2.close();
            }else{
                qstr = "INSERT INTO inquirer (word"; // WHERE wrd='"+word+"' SET ";
                for(String key : attrVals.keySet()){
                    qstr += ", ";
                    qstr += key;
                }
                qstr += ") VALUES (?";
                for(String key : attrVals.keySet()){
                    qstr += ", ";
                    qstr += attrVals.get(key);
                }
                qstr += ")";
                PreparedStatement pst2 = c.prepareStatement(qstr);
                pst2.setString(1, word);
                pst2.executeUpdate();
                pst2.close();
            }
            
        }
        c.setAutoCommit(true);
        reader.close();
    }*/
    
    private static HashMap<String, Double> RSToTraitSet(ResultSet r)
    {
        HashMap<String, Double> ret = new HashMap();
        
        for(String s : reconTraits){
            try {
                ret.put(s, r.getDouble(s));
            } catch (SQLException ex) {
                Logger.getLogger(Inquirer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return ret;
    }
    
    public static String getSqlCreateAddition(){
        String qstr = "";
        for(String s : reconTraits){
            qstr += ", " + s+" REAL";
        }
        return qstr;
    }
    public String getSqlInsertNames(){
        String qstr = "";
        for(String s : reconTraits){
            qstr += ", " + s;
        }
        return qstr;
    }
    public String getSqlInsertValues(){
        String qstr = "";
        for(String s : reconTraits){
            qstr += ", " + traits.get(s);
        }
        return qstr;
    }
    
    public void add(Inquirer inq)
    {
        for(String key : inq.traits.keySet()){
            if(!traits.containsKey(key))
                traits.put(key, inq.traits.get(key));
            else
                traits.put(key, traits.get(key) + inq.traits.get(key));
        }
    }
    
    public static String[] getAttrNames()
    {
        return reconTraits.toArray(new String[0]);
    }
    
    private HashMap<String, Double> traits;
    private static HashSet<String> reconTraits = new HashSet();
    
    private static HashSet<String> unkTraits = new HashSet();
    private static Connection c;
}
