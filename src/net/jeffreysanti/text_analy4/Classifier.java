/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.jeffreysanti.text_analy4;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jblas.DoubleMatrix;
import org.jblas.ranges.IntervalRange;
import org.jblas.ranges.Range;

/**
 *
 * @author jeffrey
 */
public class Classifier {
    
    public Classifier(Connection d){
        try {
            db = d;
            
            PreparedStatement stmt = db.prepareStatement("SELECT COUNT() FROM secs");
            ResultSet r = stmt.executeQuery();
            if(!r.next())
                return; // error ??
            totalDocCount = r.getInt(1);
            r.close();
            stmt.close();
            
            stmt = db.prepareStatement("SELECT COUNT() FROM slst");
            r = stmt.executeQuery();
            if(!r.next())
                return; // error ??
            totalSentCount = r.getInt(1);
            r.close();
            stmt.close();
            
            // generate root list
            roots = new ArrayList();
            roots_IDF = new ArrayList();
            stmt = db.prepareStatement("SELECT rid FROM roots ORDER BY root");
            r = stmt.executeQuery();
            while(r.next()){
                int root = r.getInt(1);
                roots.add(root);
                roots_IDF.add(IDFreqency(root));
            }
            r.close();
            stmt.close();
            
        } catch (SQLException ex) {
            Logger.getLogger(Classifier.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private double calculateError(double[] ent, double[] cent){
        double sum = 0;
        for(int i=0; i<roots.size(); i++){
            sum += Math.pow(ent[i] - cent[i], 2.0);
        }
        return sum;
    }
    
    private void updateCentroid(double[] cent, ArrayList<Integer> list, double[][] v){
        for(int n=0; n<roots.size(); n++){ // for each word
            cent[n] = 0;
            for(int sec : list){
                cent[n] += v[sec][n];
            }
            cent[n] = cent[n] / list.size();
        }
    }
    
    private double[] getSentenceVector(int secNo, int sentNo){
        /*double totalWords = WordCount_Section(secNo);
        arr[roots.size()] = secNo; // so we can always backtrace classification
        for(int i=0; i<roots.size(); i++){
            arr[i] = TermCount_Section(secNo, roots.get(i)) / totalWords * roots_IDF.get(i);
        }
        return arr;
        */
        
        double [] arr = new double[roots.size()+2];
        for(int i=0; i<roots.size(); i++){
            arr[i] = 0.0;
        }
        arr[roots.size()] = secNo; // so we can always backtrace classification
        arr[roots.size()+1] = sentNo; // so we can always backtrace classification
        
        try {
            // fetch senetence
            PreparedStatement stmt = db.prepareStatement("SELECT rootlist, wcnt FROM slst WHERE sec=? AND sent=?");
            stmt.setInt(1, secNo);
            stmt.setInt(2, sentNo);
            ResultSet r = stmt.executeQuery();
            if(!r.next()){
                return arr;
            }
            
            // now extract roots
            String hash = r.getString(1);
            hash = hash.replaceAll("\\|", "");
            String[] countroots = hash.split(";");
            
            int totalWCount = r.getInt(2);
            
            r.close();
            stmt.close();
            
            for(int i=0; i<roots.size(); i++){
                int cnt = 0;
                for(String root : countroots){
                    if(root.equals(Integer.toString(roots.get(i)))){
                        cnt ++;
                    }
                }
                if(cnt > 0)
                    arr[i] = (double)cnt/(double)totalWCount * roots_IDF.get(i);
            }
            stmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(Classifier.class.getName()).log(Level.SEVERE, null, ex);
        }
        return arr;
    }
    
    private double IDFreqency(int rid){
        try {
            PreparedStatement stmt = db.prepareStatement("SELECT COUNT() FROM worddist WHERE word=?");
            stmt.setInt(1, rid);
            ResultSet r = stmt.executeQuery();
            if(!r.next())
                return 0;
            double containingDocs = r.getInt(1);
            r.close();
            stmt.close();
            
            return Math.log((double)totalDocCount/containingDocs);
            
        } catch (SQLException ex) {
            Logger.getLogger(Classifier.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 0;
    }
    private double WordCount_Section(int section){
        int instances = 0;
        try {
            PreparedStatement stmt = db.prepareStatement("SELECT wcnt FROM secs WHERE sec=?");
            stmt.setInt(1, section);
            ResultSet r = stmt.executeQuery();
            if(r.next())
                instances = r.getInt(1);
            r.close();
            stmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(Classifier.class.getName()).log(Level.SEVERE, null, ex);
        }
        return instances;
    }
    private double TermCount_Section(int section, int rid){
        int instances = 0;
        try {
            PreparedStatement stmt = db.prepareStatement("SELECT cnt FROM worddist WHERE section=? AND word=?");
            stmt.setInt(1, section);
            stmt.setInt(2, rid);
            ResultSet r = stmt.executeQuery();
            if(r.next())
                instances = r.getInt(1);
            r.close();
            stmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(Classifier.class.getName()).log(Level.SEVERE, null, ex);
        }
        return instances;
    }
    
    private double[] getDocumentVector(int secNo){
        double [] arr = new double[roots.size()+1];
        for(int i=0; i<roots.size(); i++){
            arr[i] = 0.0;
        }
        double totalWords = WordCount_Section(secNo);
        arr[roots.size()] = secNo; // so we can always backtrace classification
        for(int i=0; i<roots.size(); i++){
            arr[i] = TermCount_Section(secNo, roots.get(i)) / totalWords * roots_IDF.get(i);
        }
        return arr;
    }
    
    int []indicies(int min, int max){
        int[] ret = new int[max-min+1];
        for(int i=0; i<max-min+1; i++){
            ret[i] = min + i;
        }
        return ret;
    }
    
    private double kMeansAlgo(int k, double centroids[][], double docs[][]){
        ArrayList<Integer>[] divisor = new ArrayList[k];
        for(int i=0; i<k; i++){
            divisor[i] = new ArrayList();
        }
        
        DoubleMatrix D = new DoubleMatrix(docs);
        D.put(indicies(0, D.rows-1),indicies(roots.size(), D.columns-1), 0);
        //D.print();
        
        DoubleMatrix C = new DoubleMatrix(centroids);
        C.put(indicies(0, C.rows-1),indicies(roots.size(), C.columns-1), 0);
        

        // now begin the algorithm
        double lastTotalError = 0;
        int iters = 0;
        while(true){
            double totalError = 0;
            
            DoubleMatrix CNew = DoubleMatrix.zeros(C.rows, C.columns);
            DoubleMatrix CContainCount = DoubleMatrix.zeros(C.rows, 1);

            // assign documents to cluster
            for(int i=0; i<docs.length; i++){
                // check each cluster for min value
                
                DoubleMatrix Drow = D.getRow(i);
                Drow = C.subRowVector(Drow);
                Drow = Drow.mul(Drow); // square
                Drow = Drow.rowSums(); // get each distance
                int centroidIndex = Drow.argmin();
                totalError += Drow.min();
                
                CNew.putRow(centroidIndex, CNew.getRow(centroidIndex).add(D.getRow(i)));
                CContainCount.put(centroidIndex, CContainCount.get(centroidIndex)+1);
                
                /*double min = -1;
                int minc = -1;
                for(int c=0; c<k; c++){
                    double thisMin = D.getRow(i).distance2(C.getRow(c)); //calculateError(docs[i], centroids[c]);
                    if(thisMin < min || min == -1){
                        min = thisMin;
                        minc = c;
                    }
                }*/
                docs[i][roots.size()] = centroidIndex; // update document to point to centroid
                //divisor[minc].add(i); // add document to centroid's list
                //totalError += min;
            }
            if(lastTotalError == totalError || iters > 500){ // no more change
                break;
            }
            lastTotalError = totalError;

            // now update centroids
            /*for(int c=0; c<k; c++){
                updateCentroid(centroids[c], divisor[c], docs);
            }*/
            C = CNew.divColumnVector(CContainCount);
            iters ++;
        }
        return lastTotalError;
    }
    
    
    public void kMeansSections(int k){
        try {
            System.out.println("KMeans k="+k);            
            double centroids[][] = new double[k][];
            
            // choose k initial documents at random & generate centroids
            PreparedStatement stmt = db.prepareStatement("SELECT sec FROM secs ORDER BY RANDOM() LIMIT ?");
            stmt.setInt(1, k);
            ResultSet r = stmt.executeQuery();
            int cnt = 0;
            while(r.next()){
                centroids[cnt] = getDocumentVector(r.getInt(1));
                cnt ++;
            }
            if(cnt != k){
                System.out.println("Insufficent Articles!");
                return;
            }
            r.close();
            stmt.close();
            
            // now fetch each document
            stmt = db.prepareStatement("SELECT sec FROM secs");
            r = stmt.executeQuery();
            ArrayList<double[]> tmp = new ArrayList();
            while(r.next()){
                tmp.add(getDocumentVector(r.getInt(1)));
            }
            r.close();
            stmt.close();
            
            double docs[][] = new double[tmp.size()][];
            docs = tmp.toArray(docs);
            
            System.out.println(kMeansAlgo(k, centroids, docs));
            
            // now print results
            for(int i=0; i<k; i++){
                System.out.print("Cluster "+i+": ");
                for(int x=0; x<docs.length; x++){
                    if(docs[x][roots.size()] == i)
                        System.out.print((x+1)+", ");
                }
                System.out.print("\n");
            }
            
        } catch (SQLException ex) {
            Logger.getLogger(Classifier.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public double kMeansSentences(int k){
        try {           
            double centroids[][] = new double[k][];
            
            // choose k initial sentences at random & generate centroids
            PreparedStatement stmt = db.prepareStatement("SELECT sec,sent FROM slst ORDER BY RANDOM() LIMIT ?");
            stmt.setInt(1, k);
            ResultSet r = stmt.executeQuery();
            int cnt = 0;
            while(r.next()){
                centroids[cnt] = getSentenceVector(r.getInt(1), r.getInt(2));
                cnt ++;
            }
            if(cnt != k){
                System.out.println("Insufficent Sentences!");
                return -1;
            }
            r.close();
            stmt.close();
            
            // now fetch each document
            stmt = db.prepareStatement("SELECT sec,sent FROM slst");
            r = stmt.executeQuery();
            ArrayList<double[]> tmp = new ArrayList();
            while(r.next()){
                tmp.add(getSentenceVector(r.getInt(1), r.getInt(2)));
            }
            r.close();
            stmt.close();
            
            double docs[][] = new double[tmp.size()][];
            docs = tmp.toArray(docs);
            
            double err = kMeansAlgo(k, centroids, docs);
            
            // now print results
            for(int i=0; i<k; i++){
                System.out.print("Cluster "+i+": ");
                for(int x=0; x<docs.length; x++){
                    if(docs[x][roots.size()] == i)
                        System.out.print((x+1)+", ");
                }
                System.out.print("\n");
            }
            
            return err;
            
        } catch (SQLException ex) {
            Logger.getLogger(Classifier.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }
    
    
    private Connection db;
    private ArrayList<Integer> roots;
    private ArrayList<Double> roots_IDF;
    private int totalDocCount;
    private int totalSentCount;
    
}
