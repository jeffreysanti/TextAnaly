/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jeffreysanti.text_analy4;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jeffreysanti.text_analy4.ReportDataIntIntVec.IntIntPair;
import net.jeffreysanti.text_analy4.ReportDataStrIntVec.StringIntPair;
import org.apache.lucene.util.fst.PairOutputs.Pair;

/**
 *
 * @author jeffrey
 */
public class ViewerBase {
    
    public Connection db=null;
    private HashMap<String,GenericReport> R;
    
    public ViewerBase(String dbFile){
        R = new HashMap();
        try {
            File f = new File(dbFile);
            if(!f.exists()){
                System.err.println("File Specified Does Not Exist!");
                return;
            }

            db = DriverManager.getConnection("jdbc:sqlite:"+dbFile);

            // Verify Database correct format
            PreparedStatement stmt = db.prepareStatement("SELECT COUNT() FROM secs");
            ResultSet r = stmt.executeQuery();
            if(!r.next())
                throw new SQLException();

            if(r.getInt(1) < 1)
                throw new SQLException();
            r.close();
            stmt.close();
            
            Statement stmt2 = db.createStatement();
            stmt2.executeUpdate("CREATE TABLE IF NOT EXISTS userdata (id INTEGER NOT NULL PRIMARY KEY, name STRING, "
                    + "deleteTime INTEGER, type STRING)");
            stmt2.close();
            stmt2 = db.createStatement();
            stmt2.executeUpdate("CREATE TABLE IF NOT EXISTS userdata_strvec (id INTEGER, val STRING)");
            stmt2.close();
            stmt2 = db.createStatement();
            stmt2.executeUpdate("CREATE TABLE IF NOT EXISTS userdata_intvec (id INTEGER, val INTEGER)");
            stmt2.close();
            stmt2 = db.createStatement();
            stmt2.executeUpdate("CREATE TABLE IF NOT EXISTS userdata_strintvec (id INTEGER, vals STRING, vali INTEGER)");
            stmt2.close();
            stmt2 = db.createStatement();
            stmt2.executeUpdate("CREATE TABLE IF NOT EXISTS userdata_intintvec (id INTEGER, val1 INTEGER, val2 INTEGER)");
            stmt2.close();
            
            
            
            Timer t = new Timer();
            t.scheduleAtFixedRate(new TimerTask() {
                    
                    public void deleteExpiredData(String table, ArrayList<Integer> toDelete) throws SQLException{
                        PreparedStatement stmt2 = db.prepareStatement("DELETE FROM "+table+" WHERE id=?");
                        for(int i : toDelete){
                            stmt2.setInt(1, i);
                            stmt2.executeUpdate();
                        }
                        stmt2.close();
                    }
                    
                    @Override
                    public void run() { // purge expired data
                        try {
                            db.setAutoCommit(false);
                            long time = System.currentTimeMillis();
                            Statement stmt = db.createStatement();
                            ResultSet res = stmt.executeQuery("SELECT * FROM userdata WHERE deleteTime != 0 AND '"+time+"' > deleteTime");
                            ArrayList<Integer> toDelete = new ArrayList();
                            while(res.next()){
                                int id = res.getInt("id");
                                toDelete.add(id);
                            }
                            res.close();
                            stmt.close();
                            
                            PreparedStatement stmt2 = db.prepareStatement("DELETE FROM userdata WHERE id=?");
                            for(int i : toDelete){
                                stmt2.setInt(1, i);
                                stmt2.executeUpdate();
                            }
                            stmt2.close();
                            deleteExpiredData("userdata_strvec", toDelete);
                            deleteExpiredData("userdata_intvec", toDelete);
                            deleteExpiredData("userdata_strintvec", toDelete);
                            deleteExpiredData("userdata_intintvec", toDelete);
                            db.setAutoCommit(true);
                        } catch (SQLException ex) {
                            Logger.getLogger(ViewerBase.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }, 0, 1000*60*15);

        } catch (SQLException ex) {
            Logger.getLogger(ViewerBase.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("SQLite Error Occured: Is DB Valid?");
            db = null;
        }
        
        addAllReports();
    }
    
    public void deleteData(int id){
        try {
            String type = getDataSetType(id);
            PreparedStatement stmt = db.prepareStatement("DELETE FROM userdata WHERE id=?");
            stmt.setInt(1, id);
            stmt.executeUpdate();
            stmt.close();
            
            stmt = db.prepareStatement("DELETE FROM userdata_"+type+" WHERE id=?");
            stmt.setInt(1, id);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(ViewerBase.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void addAllReports(){
        addReport(new ReportWordCounts(this));
        addReport(new ReportWordDist(this));
    }
    
    private void addReport(GenericReport r){
        R.put(r.getName(), r);
    }
    
    protected ArrayList<String> getReportNames(){
        ArrayList<String> ret = new ArrayList();
        for(String r : R.keySet()){
            ret.add(r);
        }
        return ret;
    }
    
    protected GenericReport getReport(String r){
        GenericReport orig = R.get(r);
        return orig.factory();
    }
    
    public static int dataCnt = 0;
    public String newUserData(String type){
        try {
            dataCnt++;
            
            Statement stmt = db.createStatement();
            long now = System.currentTimeMillis();
            String nm = "tmp-"+now+"-"+dataCnt;
            long deleteTime = now + (1000*60*60);
            stmt.executeUpdate("INSERT INTO userdata (name, deleteTime, type) VALUES ('"
                    +nm+"', '"+deleteTime+"', '"+type+"')");
            stmt.close();
            
            return nm;
        } catch (SQLException ex) {
            Logger.getLogger(ViewerBase.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }
    
    public int newUserData(String type, String nm, boolean temp){
        try {
            Statement stmt = db.createStatement();
            long now = System.currentTimeMillis();
            long deleteTime = (temp ? now + (1000*60*15) : 0);
            stmt.executeUpdate("INSERT INTO userdata (name, deleteTime, type) VALUES ('"
                    +nm+"', '"+deleteTime+"', '"+type+"')");
            stmt.close();
            
            return getDataSetID(nm);
        } catch (SQLException ex) {
            Logger.getLogger(ViewerBase.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }
    
    public ArrayList<String> getDataEntires(String typ){
        ArrayList<String> ret = new ArrayList();
        try {
            Statement stmt = db.createStatement();
            ResultSet res = stmt.executeQuery("SELECT name FROM userdata WHERE type='"+typ+"' ORDER BY -1*deleteTime DESC, name ASC");
            while(res.next()){
                ret.add(res.getString(1));
            }
            res.close();
            stmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(ViewerBase.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }
    
    
    public int getDataSetID(String nm){
        int ret = -1;
        try {
            PreparedStatement stmt = db.prepareStatement("SELECT id FROM userdata WHERE name=?");
            stmt.setString(1, nm);
            ResultSet r = stmt.executeQuery();
            if(r.next()){
                ret = r.getInt("id");
            }
            r.close();
            stmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(ViewerBase.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }
    
    public String getDataSetType(int id){
        String ret = "";
        try {
            PreparedStatement stmt = db.prepareStatement("SELECT type FROM userdata WHERE id=?");
            stmt.setInt(1, id);
            ResultSet r = stmt.executeQuery();
            if(r.next()){
                ret = r.getString(1);
            }
            r.close();
            stmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(ViewerBase.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }
    
    public long getDataSetExpiration(int id){
        long ret = 0;
        try {
            PreparedStatement stmt = db.prepareStatement("SELECT deleteTime FROM userdata WHERE id=?");
            stmt.setInt(1, id);
            ResultSet r = stmt.executeQuery();
            if(r.next()){
                ret = r.getLong(1);
            }
            r.close();
            stmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(ViewerBase.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }
    
    public String getDataSetName(int id){
        String ret = "";
        try {
            PreparedStatement stmt = db.prepareStatement("SELECT name FROM userdata WHERE id=?");
            stmt.setInt(1, id);
            ResultSet r = stmt.executeQuery();
            if(r.next()){
                ret = r.getString(1);
            }
            r.close();
            stmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(ViewerBase.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }
    
    public boolean tryDataSetName(int id, String nm){
        if(nm.trim().isEmpty()){
            return false;
        }
        try {
            PreparedStatement stmt = db.prepareStatement("SELECT id FROM userdata WHERE name=?");
            stmt.setString(1, nm);
            ResultSet r = stmt.executeQuery();
            if(r.next()){
                r.close();
                stmt.close();
                return false; // name already taken
            }
            r.close();
            stmt.close();
            
            stmt = db.prepareStatement("UPDATE userdata SET name=? WHERE id=?");
            stmt.setString(1, nm);
            stmt.setInt(2, id);
            stmt.executeUpdate();
            stmt.close();
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(ViewerBase.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
    public void dataSetExpr(int id, long expr){
        try {
            PreparedStatement stmt = db.prepareStatement("UPDATE userdata SET deleteTime=? WHERE id=?");
            stmt.setLong(1, expr);
            stmt.setInt(2, id);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(ViewerBase.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void filterStrIntVecRemoveGT(int id, int gt){
        try {
            PreparedStatement stmt = db.prepareStatement("DELETE FROM userdata_strintvec WHERE id=? AND vali > ?");
            stmt.setLong(1, id);
            stmt.setInt(2, gt);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(ViewerBase.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void filterStrIntVecRemoveLT(int id, int lt){
        try {
            PreparedStatement stmt = db.prepareStatement("DELETE FROM userdata_strintvec WHERE id=? AND vali < ?");
            stmt.setLong(1, id);
            stmt.setInt(2, lt);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(ViewerBase.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    public ArrayList<IntIntPair> getIntIntVec(int id, boolean reorder, boolean sort1, boolean asc){
        ArrayList<IntIntPair> ret = new ArrayList();
        try {
            String query = "SELECT val1,val2 FROM userdata_intintvec WHERE id=? ";
            if(reorder){
                query += " ORDER BY ";
                query += sort1 ? "val1" : "val2";
                query += asc ? " ASC" : " DESC";
            }
            PreparedStatement stmt = db.prepareStatement(query);
            stmt.setInt(1, id);
            ResultSet r = stmt.executeQuery();
            while(r.next()){
                ret.add(new IntIntPair(r.getInt(1), r.getInt(2)));
            }
            r.close();
            stmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(ViewerBase.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }
    
    public void setIntIntVec(int id, ArrayList<IntIntPair> dta){
        try {
            db.setAutoCommit(false);
            
            // clear data
            PreparedStatement stmt = db.prepareStatement("DELETE FROM userdata_intintvec WHERE id=?");
            stmt.setInt(1, id);
            stmt.executeUpdate();
            stmt.close();
            
            stmt = db.prepareStatement("INSERT INTO userdata_intintvec (id, val1, val2) VALUES (?,?,?)");
            stmt.setInt(1, id);
            for(IntIntPair x : dta){
                stmt.setInt(2, x.i1);
                stmt.setInt(3, x.i2);
                stmt.executeUpdate();
            }
            stmt.close();
            
            db.setAutoCommit(true);
        } catch (SQLException ex) {
            Logger.getLogger(ViewerBase.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void filterIntIntVecRemoveGT(int id, int gt, int n){
        try {
            PreparedStatement stmt = db.prepareStatement("DELETE FROM userdata_intintvec WHERE id=? AND val"+n+" > ?");
            stmt.setLong(1, id);
            stmt.setInt(2, gt);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(ViewerBase.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void filterIntIntVecRemoveLT(int id, int lt, int n){
        try {
            PreparedStatement stmt = db.prepareStatement("DELETE FROM userdata_intintvec WHERE id=? AND val"+n+" < ?");
            stmt.setLong(1, id);
            stmt.setInt(2, lt);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(ViewerBase.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
