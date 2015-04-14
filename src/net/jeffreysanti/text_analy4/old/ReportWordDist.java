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
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jeffrey
 */
public class ReportWordDist extends GenericReport {

    public ReportWordDist(ViewerBase base) {
        super(base);
    }

    @Override
    public String getName() {
        return "Word Distribution Report";
    }

    @Override
    public void runReport() {
        boolean aggregate = false;
        ReportDataStringVec inputList = null;
        for(ReportData d : I){
            if(d.label.equals("Words")){
                inputList = ((ReportDataStringVec)d);
            }
            if(d.label.equals("Result Type")){
                aggregate = ((ReportDataDropDown)d).selected.equals("Aggregate");
            }
        }
        
        String outputDataName = vb.newUserData("seccountvec");
        int outDataSetID = vb.getDataSetID(outputDataName);        
        
        String query = "INSERT INTO userdata_intintvec (id, val1, val2) "
                + " SELECT ?, "
                + "        sec, "
                + "        (SELECT coalesce(SUM(cnt), 0) FROM worddist WHERE "
                + "          worddist.section=secs.sec AND "
                + "          worddist.word IN "
                + "            (SELECT root FROM altwords"
                + "              WHERE word IN (SELECT val FROM userdata_strvec WHERE id=?)))"
                + "     FROM secs ORDER BY sec";
        
        PreparedStatement stmt;
        try {
            stmt = vb.db.prepareStatement("DELETE FROM userdata_intintvec WHERE id=?");
            stmt.setInt(1, outDataSetID);
            stmt.executeUpdate();
            stmt.close();
            
            stmt = vb.db.prepareStatement(query);
            stmt.setInt(1, outDataSetID);
            stmt.setInt(2, inputList.getDBID());
            stmt.executeUpdate();
            stmt.close();
            
            if(aggregate){
                vb.db.setAutoCommit(false);
                stmt = vb.db.prepareStatement("SELECT * FROM userdata_intintvec WHERE id=? ORDER BY val1");
                stmt.setInt(1, outDataSetID);
                ResultSet r = stmt.executeQuery();
                HashMap<Integer,Integer> vals = new HashMap();
                int tot = 0;
                while(r.next()){
                    tot += r.getInt("val2");
                    vals.put(r.getInt("val1"), tot);
                }
                r.close();
                stmt.close();
                stmt = vb.db.prepareStatement("UPDATE userdata_intintvec SET val2=? WHERE id=? AND val1=?");
                for(int key : vals.keySet()){
                    stmt.setInt(1, vals.get(key));
                    stmt.setInt(2, outDataSetID);
                    stmt.setInt(3, key);
                    stmt.executeUpdate();
                }
                stmt.close();
                vb.db.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            Logger.getLogger(ReportWordCounts.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        ReportDataSecCountVec res = new ReportDataSecCountVec("Results", vb);
        res.setDBID(outputDataName);
        O.add(res);
    }

    @Override
    public GenericReport factory() {
        
        ReportWordDist report = new ReportWordDist(vb);
        
        report.I.add(new ReportDataStringVec("Words", vb));
        report.I.add(new ReportDataDropDown("Result Type", new String []{"By-Section", "Aggregate"}, "By-Section"));
        
        return report;
    }
    
}
