/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jeffreysanti.text_analy4;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author jeffrey
 */
public class RInterface {
    
    
    public RInterface() throws FileNotFoundException, IOException
    {
        AnalysisCore.initalizeNoAnalysisRun("MODELS/inquirer.db");
        
        // Read the documentation list
        File f = new File("RCode/doc.lst");
        BufferedReader reader = new BufferedReader(new FileReader(f));
        String line=null;
        HashSet indexHash = new HashSet();
        while((line=reader.readLine()) != null){
            if(!line.contains(":"))
                continue;
            String nm = line.substring(0, line.indexOf(":"));
            String post = line.substring(line.indexOf(":")+1);
            String [] lst = post.split(";");
            HashSet hash = new HashSet();
            hash.add("index");
            hash.addAll(Arrays.asList(lst));
            hash.remove("");
            D.put(nm, hash);
            indexHash.add(nm);
        }
        D.put("index", indexHash);
    }
    
    public void initR()
    {
        System.out.println("\n\n\n");
        System.out.println("--- Welcome To TEXTAnalysis3 ---\n");
        System.out.println("You are inside an R environment with access to a");
        System.out.println("library of tools for discovering meaning in your");
        System.out.println("data.\n\n* To access normal R help, type: help()");
        System.out.println("* To quit the program type: q()");
        System.out.println("* For help specific to TEXTAnalysis3, type h()");
        System.out.println("* For a Gui help window, type g()\n\n");
    }
    
    public void helpMenu() throws FileNotFoundException, IOException
    {
        System.out.println("You are at page: "+helpPage+"; to change this type h(\"name\")");
        System.out.println("===================================================");
        
        File f = new File("RCode/doc/"+helpPage+".txt");
        BufferedReader reader = new BufferedReader(new FileReader(f));
        String line=null;
        while((line=reader.readLine()) != null){
            System.out.println(line);
        }
        System.out.println("-----------------Other Pages-----------------------");
        for(String s : D.get(helpPage)){
            System.out.println("* "+s);
        }
    }
    
    public void chHelp(String nm) throws FileNotFoundException, IOException
    {
        if(!D.containsKey(nm)){
            System.out.println("Page Does Not Exist!");
            return;
        }
        helpPage = nm;
        helpMenu();
    }
    
    public void showGui()
    {
        if(wnd == null)
            wnd = new HelpWnd(D);
        wnd.setVisible(true);
    }
    
    public void getDBContext(String flPath)
    {
        
    }
    
    HelpWnd wnd = null;
    private HashMap<String, HashSet<String>> D = new HashMap();
    private String helpPage = "index";
}
