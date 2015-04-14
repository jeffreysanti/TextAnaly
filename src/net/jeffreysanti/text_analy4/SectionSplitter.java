/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.jeffreysanti.text_analy4;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jeffrey
 */
public class SectionSplitter {
    
    public enum SplitMethod{
        SPLIT_NL,
        SPLIT_EMPTYL
    }
    
    public SectionSplitter(String fl, SplitMethod m){
        J = new ArrayList();
        try {
            Scanner s = new Scanner(new File(fl)).useDelimiter("\n");
            String curSec = "";
            while(s.hasNext()){
                String line = s.next();
                curSec += line;
                if(curSec.trim().isEmpty())
                    continue; // do not add an empty section
                if(m == SplitMethod.SPLIT_NL){
                    Section sec = new Section(curSec);
                    J.add(sec);
                    curSec = "";
                }
                if(m == SplitMethod.SPLIT_EMPTYL && line.trim().equals("")){
                    Section sec = new Section(curSec);
                    J.add(sec);
                    curSec = "";
                }
            }
            if(!curSec.trim().equals("")){
                Section sec = new Section(curSec);
                J.add(sec);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SectionSplitter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    ArrayList<Section> getJobs(){
        return J;
    }
    
    private ArrayList<Section> J;
    
}
