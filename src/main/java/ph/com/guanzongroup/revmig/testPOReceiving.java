/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ph.com.guanzongroup.revmig;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.SQLUtil;
import ph.com.guanzongroup.revmig.controller.POReceivingController;
import com.ibm.icu.text.RuleBasedNumberFormat;
import java.util.Locale;

/**
 *
 * @author kalyptus
 */
public class testPOReceiving {
    public static void main(String args[]){
        String path = "D:/GGC_Maven_Systems";
        System.setProperty("sys.default.path.config", path);
        System.setProperty("sys.default.path.metadata", "D:/GGC_Java_Systems/metadata//");
        
        GRider instance = new GRider("gRider");

        try {
            ResultSet loRSMaster = getMaster(instance, "GK0125000507");
            ResultSet loRSDetail = getDetail(instance, "GK0125000507");
            ResultSet loRSSerial = getSerial(instance, "GK0125000507");
            
            POReceivingController poController = new POReceivingController();
            System.out.println(poController.initTransaction(instance, "GK01"));
            System.out.println(poController.newTransaction());
            
            if(loRSMaster.next()){
                System.out.println(poController.setMaster(loRSMaster));
            }
            
            int lnctr = 0;
            while(loRSDetail.next()){
                System.out.println(poController.addDetail());
                System.out.println(poController.setDetail(lnctr, loRSDetail));
                lnctr++;
            }
            
            lnctr = 0;
            while(loRSSerial.next()){
                System.out.println(poController.addSerial());
                System.out.println(poController.setSerial(lnctr, loRSSerial));
                lnctr++;
            }
            
            poController.showTransaction();
            
            poController.confirmTransaction();
        } catch (SQLException ex) {
            Logger.getLogger(testPOReceiving.class.getName()).log(Level.SEVERE, null, ex);
        } catch (GuanzonException ex) {
            Logger.getLogger(testPOReceiving.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static ResultSet getMaster(GRider instance, String fsTransNox){
        String lsSQL = "SELECT *" + 
                      " FROM GN_PO_Receiving_Master" + 
                      " WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox);
        return instance.executeQuery(lsSQL);
    } 
    
    private static ResultSet getDetail(GRider instance, String fsTransNox){
        String lsSQL = "SELECT *" + 
                      " FROM GN_PO_Receiving_Detail" + 
                      " WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox) + 
                      " ORDER BY sTransNox, nEntryNox";
        return instance.executeQuery(lsSQL);
    } 

    private static ResultSet getSerial(GRider instance, String fsTransNox){
        String lsSQL = "SELECT *" + 
                      " FROM GN_PO_Receiving_Serial" + 
                      " WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox) + 
                      " ORDER BY sTransNox, nEntryNox, sSerialID";
        return instance.executeQuery(lsSQL);
    } 
    
}
