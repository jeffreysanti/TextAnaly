/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jeffreysanti.text_analy4;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jeffrey
 */
public class Analyzer extends Thread {

    public Analyzer(int maxThreads)
    {
        JOBS = new LinkedList();
        threads = new Thread[maxThreads];
        for(int i=0; i<maxThreads; i++){
            threads[i] = null;
        }
        
        db = AnalysisCore.getDB();
        secNext = AnalysisCore.getNextSectionID();
        secMax = secNext;
    }
    
    
    public void addJob(Section s){
        JOBS.offer(s);
        secMax ++;
    }
    
    public void addJobs(SectionSplitter splitter){
        for(Section sec : splitter.getJobs()){
            addJob(sec);
        }
    }
    
    @Override
    public void run() {
        try {
            db.setAutoCommit(false);
        } catch (SQLException ex) {
            Logger.getLogger(Analyzer.class.getName()).log(Level.SEVERE, null, ex);
        }
        while(secNext < secMax){
            boolean doSleep = true;
            for(int i=0; i<threads.length; i++){
                if(threads[i] == null || !threads[i].isAlive()){ // thread dead
                    if(JOBS.size() > 0){
                        Section s = JOBS.poll();
                        s.setID(secNext);
                        SectionAnalyzer SA = new SectionAnalyzer(s, db);
                        threads[i] = new Thread(SA);
                        threads[i].setName(Integer.toString(secNext));
                        threads[i].start();
                        System.out.println("Starting Section: "+s.getId() + " / " + secMax);
                        secNext ++;
                        break;
                    }
                }
            }
            if(!doSleep)
                continue;
            try {
                this.sleep(200); // don't waste cpu time waiting for threads
                db.setAutoCommit(false);
            } catch (Exception ex) {
                Logger.getLogger(Analyzer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        while(true){
            boolean allDead = true;
            for(int i=0; i<threads.length; i++){
                if(threads[i] != null && threads[i].isAlive()){
                    allDead = false;
                }
            }
            if(allDead)
                break;
            
            try {
                db.setAutoCommit(false);
                this.sleep(200); // don't waste cpu time waiting for threads
            } catch (Exception ex) {
                Logger.getLogger(Analyzer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    //HashMap<Integer, SectionAnalyzer> completed;
    Thread [] threads;
    int secNext;
    int secMax;
    Connection db;
    
    LinkedList<Section> JOBS;
    
}
