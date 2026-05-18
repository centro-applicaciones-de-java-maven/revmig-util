/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ph.com.guanzongroup.revmig;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.LogWrapper;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import static ph.com.guanzongroup.revmig.POReceiving_old.poGRider;

/**
 *
 * @author kalyptus
 */
public class BankTrans {
    final static String[] PSD_BRANCHES = {"GK01", "W005", "M0W1"};
    final static String INDUSTRY = "09";
    static GRider poGRider;
    
    public static void main(String args[]){
        LogWrapper logwrapr = new LogWrapper("Bank Transaction Reverse Migration", "revmig-banktrans.log");

        //Set important path configuration for this utility
        String path;
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            path = "D:/GGC_Maven_Systems";
        }
        else{
            path = "/srv/mac/GGC_Java_Systems";
        }

        System.setProperty("sys.default.path.temp", path + "/temp");
        System.setProperty("sys.default.path.config", path);

        String lsProdctID = "gRider";

        //TODO: temporarily used my user id for testing
        String lsUserIDxx = "M001111122";
        //String lsUserIDxx = "M001250012";

        poGRider = null;

        logwrapr.info("Start of Bank Transaction Reverse Migration...");

        logwrapr.info("Loading application driver...");
        poGRider = new GRider(lsProdctID);

        logwrapr.info("Loading user...");
        if (!poGRider.loadUser(lsProdctID, lsUserIDxx)){
            logwrapr.severe(poGRider.getMessage() + poGRider.getMessage());
            System.exit(1);
        }

        try {
            
            String lsSQL;

            lsSQL = "SELECT distinct sBnkActID" +
                    " FROM GCASys_DBF.Bank_Account_Ledger" +
                    " WHERE cTranStat = '0'";
            
            ResultSet loRSDistinct = poGRider.executeQuery(lsSQL);

            poGRider.executeUpdate("USE GGCISys_DBF");
            poGRider.beginTrans();
            
            while(loRSDistinct.next()){
                lsSQL = "SELECT b.nLedgerNo, a.*" + 
                       " FROM Bank_Account a" + 
                        " LEFT JOIN Bank_Account_Ledger b ON a.sBnkActID = b.sBnkActID" +
                       " WHERE a.sBnkActID = " + SQLUtil.toSQL(loRSDistinct.getString("sBnkActID")) + 
                       " ORDER BY b.nLedgerNo DESC" + 
                       " LIMIT 1"; 
                ResultSet loRSMaster = poGRider.executeQuery(lsSQL);

                if(loRSMaster.next()){
                    double lnOBalance = loRSMaster.getDouble("nOBalance");
                    double lnABalance = loRSMaster.getDouble("nABalance");
                    Date ldTransact = null;                    
                    int lnLedgerNo = Optional.ofNullable(loRSMaster.getString("nLedgerNo"))
                                             .filter(s -> !s.isEmpty())
                                             .map(Integer::parseInt)
                                             .orElse(0); 

                    lsSQL = "SELECT *" +
                           " FROM GCASys_DBF.Bank_Account_Ledger" +
                           " WHERE sBnkActID = " + SQLUtil.toSQL(loRSMaster.getString("sBnkActID")) +
                             " AND cTranStat = '0'" + 
                           " ORDER BY dTransact, nLedgerNo" ;                   
                    ResultSet loRSLedger = poGRider.executeQuery(lsSQL);

                    while(loRSLedger.next()){
                        lnLedgerNo++;
                        double lnTranAmnt = loRSLedger.getDouble("nAmountIn") - loRSLedger.getDouble("nAmountOt");
                        lnOBalance += lnTranAmnt;
                        lnABalance += lnTranAmnt;
                        ldTransact = loRSLedger.getDate("dTransact");
                        
                        lsSQL = "INSERT INTO Bank_Account_Ledger SET " + 
                       			"  sBnkActID = " + SQLUtil.toSQL(loRSLedger.getString("sBnkActID")) +   
                              		", sBranchCd = " + SQLUtil.toSQL(loRSLedger.getString("sBranchCd")) +
                              		", nLedgerNo = " + SQLUtil.toSQL(String.format("%06d", lnLedgerNo)) +
                              		", dTransact = " + SQLUtil.toSQL(loRSLedger.getString("dTransact")) +
                              		", sSourceCd = " + SQLUtil.toSQL(loRSLedger.getString("sSourceCd")) +
                              		", sSourceNo = " + SQLUtil.toSQL(loRSLedger.getString("sSourceNo")) +
                              		", nAmountIn = " + SQLUtil.toSQL(loRSLedger.getString("nAmountIn")) +
                              		", nAmountOt = " + SQLUtil.toSQL(loRSLedger.getString("nAmountOt")) +
                              		", nOBalance = " + SQLUtil.toSQL(lnOBalance) +
                              		", nABalance = " + SQLUtil.toSQL(lnABalance) +
                              		", dPostedxx = " + SQLUtil.toSQL(loRSLedger.getString("dPostedxx")) +
                              		", dModified = " + SQLUtil.toSQL(loRSLedger.getString("dModified"));
                        System.out.println(lsSQL);
                        poGRider.executeQuery(lsSQL, "Bank_Account_Ledger", loRSLedger.getString("sBranchCd"), "");
                    }

                    if(ldTransact != null){
                        lsSQL = "UPDATE Bank_Account SET" +
                                    "  nOBalance = " + SQLUtil.toSQL(lnOBalance) +
                                    ", nABalance = " + SQLUtil.toSQL(lnABalance) +
                                    ", dLastTran = " + SQLUtil.toSQL(ldTransact) + 
                               " WHERE sBnkActID = " + SQLUtil.toSQL(loRSMaster.getString("sBnkActID"));
                        System.out.println(lsSQL);
                        poGRider.executeQuery(lsSQL, "Bank_Account", loRSMaster.getString("sBranchCd"), "");
                    }
                }
            }

            poGRider.rollbackTrans();

            poGRider.beginTrans();
            poGRider.executeUpdate("USE GCASys_DBF");
            lsSQL = "UPDATE Bank_Account_Ledger" +
                   " SET cTranStat = '2'" + 
                   " WHERE cTranStat = '1'";
            System.out.println(lsSQL);
            poGRider.executeUpdate(lsSQL);
            poGRider.rollbackTrans();

        } catch (SQLException ex) {
            Logger.getLogger(BankTrans.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    
}
