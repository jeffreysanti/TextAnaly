/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.jeffreysanti.text_analy4.tomcatview;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.jeffreysanti.text_analy4.ReportDataIntIntVec.IntIntPair;
import net.jeffreysanti.text_analy4.ReportDataStrIntVec;
import net.jeffreysanti.text_analy4.ReportDataStrIntVec.StringIntPair;
import net.jeffreysanti.text_analy4.ReportDataStringVec;
import net.jeffreysanti.text_analy4.ViewerBase;

/**
 *
 * @author jeffrey
 */
public class TomcatDataView {
    
   /* private ViewerBase vb;
    
    public TomcatDataView(ViewerBase view){
        vb = view;
    }
    
    public void service(HttpServletRequest req, HttpServletResponse resp)
    {
        if(req.getParameterMap().containsKey("dataid")){
            int i = Str2IntOr(req.getParameter("dataid"), -1);
            serviceDataPage(req, resp, i);
            return;
        }
        try {
            String urlBase = req.getRequestURL() + "?dataid=";
            
            resp.setContentType("text/html;charset=UTF-8");
            resp.getWriter().println("<h1>Data Manager</h1>\n");
            
            resp.getWriter().println("<h2>String-Value Data: </h2>");
            for(String nm : vb.getDataEntires("strintvec")){
                int id = vb.getDataSetID(nm);
                resp.getWriter().println(" * <a href=\""+urlBase+id+"\">"+nm+"</a><br />");
            }
            
            resp.getWriter().println("<h2>Value-Value Data: </h2>");
            for(String nm : vb.getDataEntires("intintvec")){
                int id = vb.getDataSetID(nm);
                resp.getWriter().println(" * <a href=\""+urlBase+id+"\">"+nm+"</a><br />");
            }
            
            resp.getWriter().println("<h2>String List Data: </h2>");
            for(String nm : vb.getDataEntires("strvec")){
                int id = vb.getDataSetID(nm);
                resp.getWriter().println(" * <a href=\""+urlBase+id+"\">"+nm+"</a><br />");
            }
            
        } catch (IOException ex) {
            Logger.getLogger(TomcatDataView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    public void serviceDataPage(HttpServletRequest req, HttpServletResponse resp, int id)
    {
        String nm = vb.getDataSetName(id);
        String typ = vb.getDataSetType(id);
        long expir = vb.getDataSetExpiration(id);
        String urlFormOut = req.getRequestURL() + "?dataid="+id+"&";
        
        try {
            resp.setContentType("text/html;charset=UTF-8");
            
            if(nm.isEmpty() || typ.isEmpty()){
                resp.getWriter().println("<h1>No Such Dataset</h1>\n");
                return;
            }
            
            // handle any updates
            boolean submit = false;
            if(req.getParameterMap().containsKey("submit")){
                submit = true;
                
                // process data
                if(req.getParameterMap().containsKey("nm") && !req.getParameter("nm").equals(nm)){
                    vb.tryDataSetName(id, req.getParameter("nm"));
                }
                if(req.getParameterMap().containsKey("perm")){
                    vb.dataSetExpr(id, 0); // make permenant data
                }
                if(req.getParameterMap().containsKey("del")){
                    vb.deleteData(id);
                    resp.getWriter().println("<h1>Data Deleted</h1>\n");
                    return;
                }
                
                nm = vb.getDataSetName(id);
                expir = vb.getDataSetExpiration(id);
            }
            
            resp.getWriter().println("<h1>"+nm+"</h1>");
            
            resp.getWriter().println("<form method=\"post\" action=\""+urlFormOut+"\">\n");
            resp.getWriter().println("<table><tr><td></td><td></td></tr>\n");
            
            // Change of name
            resp.getWriter().println("<tr><td>Data Set Name</td><td><input name=\"nm\" type=\"input\" value=\""
                    + nm + "\" /></td></tr>");
            
            // permenancy
            if(expir != 0){
                expir = (expir - System.currentTimeMillis())/1000/60;
                resp.getWriter().println("<tr><td>Data Will Expire In</td><td>"+expir+" Minutes</td></tr>");
                resp.getWriter().println("<tr><td>Make Permenant</td><td><input name=\"perm\" type=\"checkbox\" "
                        + "value=\"yes\" /></td></tr>");
            }
            
            // deletion
            resp.getWriter().println("<tr><td>Delete</td><td><input name=\"del\" type=\"checkbox\" "
                        + "value=\"yes\" /></td></tr>");
            
            
            // now type specific
            if(typ.equals("strvec")){
                if(submit){
                    if(req.getParameterMap().containsKey("add")){
                        vb.appendStrVec(id, ReportDataStringVec.textListToHashSet(req.getParameter("add")));
                    }
                    if(req.getParameterMap().containsKey("addfrom")){
                        vb.appendStrVecFrom(id, req.getParameter("addfrom"));
                    }
                    if(req.getParameterMap().containsKey("rem")){
                        vb.removeFromStrVec(id, ReportDataStringVec.textListToHashSet(req.getParameter("rem")));
                    }
                    if(req.getParameterMap().containsKey("remfrom")){
                        vb.removeFromStrVecFrom(id, req.getParameter("remfrom"));
                    }
                    if(req.getParameterMap().containsKey("reorder")){
                        boolean asc = !req.getParameterMap().containsKey("desc");
                        vb.setStrVec(id, vb.getStrVec(id, true, asc));
                    }
                }
                resp.getWriter().println("<tr><td>Reorder Data</td><td><input name=\"reorder\" type=\"checkbox\" "
                        + "value=\"yes\" /></td></tr>");
                resp.getWriter().println("<tr><td>Descending Order</td><td><input name=\"desc\" type=\"checkbox\" "
                        + "value=\"yes\" /></td></tr>");
                
                resp.getWriter().println("<tr><td>Add Items</td><td><textarea name=\"add\"></textarea></td></tr>");
                resp.getWriter().println("<tr><td>Remove Items</td><td><textarea name=\"rem\"></textarea></td></tr>");
                
                resp.getWriter().println("<tr><td>Add Items From DS</td><td><input name=\"addfrom\" type=\"text\" "
                        + "value=\"\" /></td></tr>");
                resp.getWriter().println("<tr><td>Remove Items From DS</td><td><input name=\"remfrom\" type=\"text\" "
                        + "value=\"\" /></td></tr>");
                
                resp.getWriter().println("<tr><td>---</td><td>---</td></tr>");
                for(String x : vb.getStrVec(id, false, true)){
                    resp.getWriter().println("<tr><td>"+x+"</td><td></td></tr>");
                }
            }else if(typ.equals("strintvec")){
                if(submit){
                    if(req.getParameterMap().containsKey("rgt")){
                        vb.filterStrIntVecRemoveGT(id, Str2IntOr(req.getParameter("rgt"), Integer.MAX_VALUE));
                    }
                    if(req.getParameterMap().containsKey("rlt")){
                        vb.filterStrIntVecRemoveLT(id, Str2IntOr(req.getParameter("rlt"), Integer.MIN_VALUE));
                    }
                    if(req.getParameterMap().containsKey("reorder")){
                        boolean asc = !req.getParameterMap().containsKey("desc");
                        boolean ssort = !req.getParameterMap().containsKey("oval");
                        vb.setStrIntVec(id, vb.getStrIntVec(id, true, ssort, asc));
                    }
                }
                resp.getWriter().println("<tr><td>Reorder Data</td><td><input name=\"reorder\" type=\"checkbox\" "
                        + "value=\"yes\" /></td></tr>");
                resp.getWriter().println("<tr><td>Descending Order</td><td><input name=\"desc\" type=\"checkbox\" "
                        + "value=\"yes\" /></td></tr>");
                resp.getWriter().println("<tr><td>Order By Value</td><td><input name=\"oval\" type=\"checkbox\" "
                        + "value=\"yes\" /></td></tr>");
                
                resp.getWriter().println("<tr><td>Remove Greater Than</td><td><input name=\"rgt\" type=\"text\" "
                        + "value=\"\" /></td></tr>");
                resp.getWriter().println("<tr><td>Remove Less Than</td><td><input name=\"rlt\" type=\"text\" "
                        + "value=\"\" /></td></tr>");
                
                
                resp.getWriter().println("<tr><td>---</td><td>---</td></tr>");
                for(StringIntPair x : vb.getStrIntVec(id, false, true, true)){
                    resp.getWriter().println("<tr><td>"+x.s+"</td><td>"+x.i+"</td></tr>");
                }
            }else if(typ.equals("intintvec")){
                if(submit){
                    if(req.getParameterMap().containsKey("r2gt")){
                        vb.filterIntIntVecRemoveGT(id, Str2IntOr(req.getParameter("r2gt"), Integer.MAX_VALUE), 2);
                    }
                    if(req.getParameterMap().containsKey("r2lt")){
                        vb.filterIntIntVecRemoveLT(id, Str2IntOr(req.getParameter("r2lt"), Integer.MIN_VALUE), 2);
                    }
                    if(req.getParameterMap().containsKey("r1gt")){
                        vb.filterIntIntVecRemoveGT(id, Str2IntOr(req.getParameter("r1gt"), Integer.MAX_VALUE), 1);
                    }
                    if(req.getParameterMap().containsKey("r1lt")){
                        vb.filterIntIntVecRemoveLT(id, Str2IntOr(req.getParameter("r1lt"), Integer.MIN_VALUE), 1);
                    }
                    if(req.getParameterMap().containsKey("reorder")){
                        boolean asc = !req.getParameterMap().containsKey("desc");
                        boolean sort2 = !req.getParameterMap().containsKey("2val");
                        vb.setIntIntVec(id, vb.getIntIntVec(id, true, !sort2, asc));
                    }
                }
                resp.getWriter().println("<tr><td>Reorder Data</td><td><input name=\"reorder\" type=\"checkbox\" "
                        + "value=\"yes\" /></td></tr>");
                resp.getWriter().println("<tr><td>Descending Order</td><td><input name=\"desc\" type=\"checkbox\" "
                        + "value=\"yes\" /></td></tr>");
                resp.getWriter().println("<tr><td>Order By Second Value</td><td><input name=\"2val\" type=\"checkbox\" "
                        + "value=\"yes\" /></td></tr>");
                
                resp.getWriter().println("<tr><td>Remove Where Column 2 Greater Than</td><td><input name=\"r2gt\" type=\"text\" "
                        + "value=\"\" /></td></tr>");
                resp.getWriter().println("<tr><td>Remove Where Column 2 Less Than</td><td><input name=\"r2lt\" type=\"text\" "
                        + "value=\"\" /></td></tr>");
                
                resp.getWriter().println("<tr><td>Remove Where Column 1 Greater Than</td><td><input name=\"r1gt\" type=\"text\" "
                        + "value=\"\" /></td></tr>");
                resp.getWriter().println("<tr><td>Remove Where Column 1 Less Than</td><td><input name=\"r1lt\" type=\"text\" "
                        + "value=\"\" /></td></tr>");
                
                resp.getWriter().println("<tr><td>---</td><td>---</td></tr>");
                for(IntIntPair x : vb.getIntIntVec(id, false, true, true)){
                    resp.getWriter().println("<tr><td>"+x.i1+"</td><td>"+x.i2+"</td></tr>");
                }
            }else{
                resp.getWriter().println("???");
                System.err.println("Unknown Data Type: "+typ);
            }
            
            
            
            
            
            
            
            
            resp.getWriter().println("</table>\n");
            resp.getWriter().println("<input type=\"submit\" name=\"submit\" value=\"Go\" />\n");
            resp.getWriter().println("</form>\n");
            
            
            
        } catch (IOException ex) {
            Logger.getLogger(TomcatDataView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }*/
    
}
