/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jeffreysanti.text_analy4;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Dictionaries.MentionType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jeffrey
 */
public class EntitySystem {
    
    public class EntityRefLoc{
        int sec;
        int sent;
        int wFirst;
        int wLast;
        Entity e;
        
        @Override
        public boolean equals(Object wp){
            if(! (wp instanceof EntitySystem.EntityRefLoc) )
                return false;
            EntitySystem.EntityRefLoc w = (EntitySystem.EntityRefLoc) wp;
            if(w.sec == sec && w.sent == sent && w.wFirst == wFirst && w.wLast == wLast && w.e.eid == e.eid)
                return true;
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 17 * hash + this.sec;
            hash = 17 * hash + this.sent;
            hash = 17 * hash + this.wFirst;
            hash = 17 * hash + this.wLast;
            hash = 17 * hash + this.e.eid;
            return hash;
        }        
    }
    
    public class Entity{
        int eid;
        String primaryName;
    }
    
    private ConcurrentHashMap<String, Entity> E = new ConcurrentHashMap();
    private Connection c;
    
    public EntitySystem(Connection c) throws SQLException{
        this.c = c;
        
        Statement stmt;
        stmt = c.createStatement();
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ents (eid INTEGER NOT NULL PRIMARY KEY, name STRING NOT NULL)");
        stmt.close();
        
        // read old data
        importOldEntities();
        
        // now add self entity
        Entity e = getOrNewEntity("[self]");
    }
    
    private void importOldEntities() throws SQLException{
        Statement stmt = c.createStatement();
        ResultSet r = stmt.executeQuery("SELECT * FROM ents");
        while(r.next()){
            Entity e = new Entity();
            e.primaryName = r.getString("name");
            e.eid = r.getInt("eid");
            E.put(e.primaryName, e);
        }
        stmt.close();
    }
    
    
    private synchronized Entity getOrNewEntity(String nm){
        Entity e;
        if(E.containsKey(nm)){
            e = E.get(nm);
        }else{
            e = new Entity();
            e.primaryName = nm;
            e.eid = E.size() + 1;
            E.put(e.primaryName, e);
            writeEntityToDB(e);
        }
        return e;
    }
    
    private synchronized void writeEntityToDB(Entity e){
        try {
            PreparedStatement stmt = c.prepareStatement("SELECT * FROM ents WHERE eid=?");
            stmt.setInt(1, e.eid);
            ResultSet r = stmt.executeQuery();
            if(r.next()){
                
            }else{
                stmt = c.prepareStatement("INSERT INTO ents (eid,name) VALUES (?,?)");
                stmt.setInt(1, e.eid);
                stmt.setString(2, e.primaryName);
                stmt.execute();
            }
        } catch (SQLException ex) {
            Logger.getLogger(EntitySystem.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    private synchronized void addMetionIfApplicable(CorefMention m, Section s, HashSet<EntityRefLoc> S, Entity e){
        EntityRefLoc loc;
        if(m.mentionType == MentionType.PRONOMINAL || m.mentionType == MentionType.PROPER){
            loc = new EntityRefLoc();
            loc.sent = m.sentNum-1;
            loc.sec = s.getId();
            loc.wFirst = m.headIndex - 1;
            loc.wLast = m.endIndex - 2;
            loc.e = e;
            S.add(loc);
        }
    }
    
    public synchronized void extractCorefChain(CorefChain chain, Section s, HashSet<EntityRefLoc> S){
        // Is this branch a part of self?
        boolean sawI=false;
        String sawNE="";
        if(chain.getRepresentativeMention().mentionSpan.equalsIgnoreCase("i"))
            sawI = true;
        if(chain.getRepresentativeMention().mentionType.equals(MentionType.PROPER))
            sawNE = chain.getRepresentativeMention().mentionSpan;
        
        for(CorefMention m : chain.getMentionsInTextualOrder()){
            if(m.mentionSpan.equalsIgnoreCase("i"))
                sawI = true;
            if(m.mentionType.equals(MentionType.PROPER))
                sawNE = chain.getRepresentativeMention().mentionSpan;
        }

        String baseEntity;
        if(sawNE.equals("") && sawI)
            baseEntity = "[self]";
        else if(sawI){
            if(sawNE.equalsIgnoreCase("i"))
                sawNE = "[self]";
            baseEntity = sawNE;
        }
        else
            baseEntity = chain.getRepresentativeMention().mentionSpan;
        
        baseEntity = baseEntity.toLowerCase();
        Entity e = getOrNewEntity(baseEntity);
        
        // register base mentions
        addMetionIfApplicable(chain.getRepresentativeMention(), s, S, e);
        
        // now for each member in the chain
        for(CorefMention m : chain.getMentionsInTextualOrder()){
            addMetionIfApplicable(m, s, S, e);
        }
    }
    
    public synchronized void dumpData(){
        /*for(Entity e : E.values()){
            System.out.println("E: "+e.primaryName);
            for(String corefs : e.corefs.keySet()){
                System.out.println("  * "+corefs+" ["+e.corefs.get(corefs)+"]");
            }
            for(EntityRefLoc loc : e.R){
                System.out.println("  ->  "+loc.refID+" ["+loc.sent+"]");
            }
        }*/
    }
    
    public synchronized EntityRefLoc findEntityLoc(Section s, int snum, int wordNo, String lemma, HashSet<EntityRefLoc> S){
        for(EntityRefLoc loc : S){
            if(loc.sec == s.getId() && loc.sent == snum && loc.wLast >= wordNo && loc.wFirst <= wordNo){
                return loc;
            }
        }
        
        if(lemma.equalsIgnoreCase("i")){
            EntityRefLoc loc = new EntityRefLoc();
            loc.e = getOrNewEntity("[self]");
            loc.sec = s.getId();
            loc.sent = snum;
            loc.wFirst = wordNo;
            loc.wLast = wordNo;
            S.add(loc);
            return loc;
        }
        
        // Entity not found -> we must create one :)
        Entity e = getOrNewEntity(lemma);
        
        EntityRefLoc loc = new EntityRefLoc();
        loc.e = e;
        loc.sec = s.getId();
        loc.sent = snum;
        loc.wFirst = wordNo;
        loc.wLast = wordNo;
        S.add(loc);
        
        return loc;
    }
    
    
    
}
