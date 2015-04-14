/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jeffreysanti.text_analy4;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jeffreysanti.text_analy4.SectionSplitter.SplitMethod;
import net.jeffreysanti.text_analy4.tomcatview.TomcatView;

/**
 *
 * @author jeffrey
 */
public class Main {
    
    public static void analyze(String dir, int count, int threads)
    {
        AnalysisCore.initialize("MODELS/english-left3words-distsim.tagger",
                                "MODELS/englishPCFG.ser.gz",
                                "MODELS/english.all.3class.distsim.crf.ser.gz",
                                "MODELS/inquirer.db",
                                "TESTS/"+dir+".db");
        Analyzer a = new Analyzer(threads);
        for(int i=1; i<=10; i++){
            try {
                a.addJob(new Section(new Scanner(new File("TESTS/"+dir+"/"+i+".txt")).useDelimiter("\\Z").next()));
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        a.run();
        AnalysisCore.endAnalysis();
    }
    
    public static void view(String dir)
    {
        AnalysisCore.initalizeNoAnalysisRun("MODELS/inquirer.db");
        
    }
    
    public static void main(String[] args) throws FileNotFoundException, SQLException
    {   
        //analyze("EXA", 22, 4);
        //view("EXA");
        
        
        
        
    }
}
