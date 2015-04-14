/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jeffreysanti.text_analy4;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jeffrey
 */
public class ReportDataIntIntVec extends ReportData {

    public static class IntIntPair{
        public IntIntPair(int x, int y){
            this.i1 = x;
            this.i2 = y;
        }
        public int i1, i2;
    }
    
    public ReportDataIntIntVec(String lbl, ViewerBase view, String t1, String t2) {
        super(lbl);
        vb = view;
        title1 = t1;
        title2 = t2;
        id = -1;
        data = new ArrayList();
    }
    
    public void setDBID(String nm){
        id = vb.getDataSetID(nm);
        loadData();
    }
    
    @Override
    public boolean isDatabaseEntry() {
        return true;
    }

    @Override
    public int getDBID() {
        return id;
    }
    
    public String getDBIDStr(){
        return vb.getDataSetName(id);
    }

    @Override
    public String getDBTable() {
        return "userdata_strintvec";
    }
    
    public static ArrayList<IntIntPair> getData(ViewerBase vb, int id){
        ArrayList<IntIntPair> ret = new ArrayList();
        try {
            String query = "SELECT val1,val2 FROM userdata_intintvec WHERE id=? ";
            PreparedStatement stmt = vb.db.prepareStatement(query);
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
    
    public ArrayList<IntIntPair> getData(){
        return data;
    }
    
    protected void loadData(){
        data = getData(vb, id);
    }
    
    protected void flushData(){
        if(id < 0)
            return;
        try {
            vb.db.setAutoCommit(false);
            
            // clear data
            PreparedStatement stmt = vb.db.prepareStatement("DELETE FROM userdata_intintvec WHERE id=?");
            stmt.setInt(1, id);
            stmt.executeUpdate();
            stmt.close();
            
            stmt = vb.db.prepareStatement("INSERT INTO userdata_intintvec (id, val1, val2) VALUES (?,?,?)");
            stmt.setInt(1, id);
            for(IntIntPair x : data){
                stmt.setInt(2, x.i1);
                stmt.setInt(3, x.i2);
                stmt.executeUpdate();
            }
            stmt.close();
            
            vb.db.setAutoCommit(true);
        } catch (SQLException ex) {
            Logger.getLogger(ViewerBase.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public String[] getDerrivativeDataNames() {
        return new String [] {};
    }
    @Override
    public ReportData getDerrivativeData(String nm) {
        return null;
    }
    @Override
    public int getElmCountX() {
        return 0;
    }
    @Override
    public int getElmCountY() {
        return 0;
    }
    @Override
    public ReportData getElm(int x, int y) {
        return null;
    }
    
    
    protected ViewerBase vb;
    public String title1, title2;
    protected int id;
    protected ArrayList<IntIntPair> data;
}
