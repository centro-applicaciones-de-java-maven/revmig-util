/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ph.com.guanzongroup.revmig;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.LogWrapper;
import org.guanzon.appdriver.base.SQLUtil;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.revmig.lib.RevMigAPClientTrans;
import ph.com.guanzongroup.revmig.lib.RevMigBankAccountTrans;
import ph.com.guanzongroup.revmig.lib.RevMigUtil;

/**
 *
 * @author User
 */
public class DisbursementDemigration {
    final static String INDUSTRY = "09";
    final static String TABLE_MASTER = "AP_Payment_Master";
    final static String TABLE_DETAIL = "AP_Payment_Detail";
    final static String DISBURSEMENT_TABLE = "Check_Disbursement";
    
    static GRider poGRider;

    public static void main(String args[]) throws SQLException, GuanzonException{
        LogWrapper logwrapr = new LogWrapper("Disbursement Transaction Reverse Migration", "revmig-po.log");

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

        logwrapr.info("Start of Disbursement Transaction Reverse Migration...");

        logwrapr.info("Loading application driver...");
        poGRider = new GRider(lsProdctID);

        logwrapr.info("Loading user...");
        if (!poGRider.loadUser(lsProdctID, lsUserIDxx)){
            logwrapr.severe(poGRider.getMessage() + poGRider.getMessage());
            System.exit(1);
        }

        ResultSet loRSMaster = getMaster();
        JSONObject loJson = new JSONObject();

        while(loRSMaster.next()){

            //Convert the ResultSet into Map
            Map<String, Object> loDisbursement = RevMigUtil.row2Map(loRSMaster, "nNetTotal:nTranTotl:nEntryNox:sTableNme:cLastStat");
            //Convert the transaction number into the agreed format to avoid duplicate in GGC_ISysDBF.Check_Disbursement
            String lxTransNox = RevMigUtil.convertTransNox((String)loDisbursement.get("sTransNox"));
            loDisbursement.put("sTransNox", lxTransNox);

            //Extract additional information needed to demigrate disbursement successfully
            ResultSet loRSDisbOthers = getDisbOthers(loRSMaster.getString("sTransNox"));
            if(!loRSDisbOthers.next()){
                String lsMessage = "Cannot find additional information for " + loRSMaster.getString("sTransNox");
                System.out.println(lsMessage);
                break;
            }
            //Update the fields in the Check_Disbursement to complete the required field
            loDisbursement.put("sPrtclrID", loRSDisbOthers.getString("sPrtclrID"));
            loDisbursement.put("sSourceCd", loRSDisbOthers.getString("sSourceCd"));
            loDisbursement.put("sSourceNo",  RevMigUtil.convertTransNox(loRSDisbOthers.getString("sSourceNo")));
            loDisbursement.put("sAcctCode", loRSDisbOthers.getString("sAcctCode"));
            
            Map<String, Object> result;
            String lsSQL;

            //Create the insert statement for Check_Disbursement
            System.out.println(loDisbursement);
            System.out.println("loDisbursement++++++++++++++++++++++++++++");
            result = RevMigUtil.createInsertSQL(loDisbursement, DISBURSEMENT_TABLE, "");
            System.out.println(result.get("json"));

            System.out.println("Check if create is successfull!");
            if(!"success".equals((String) result.get("result"))){
                loJson.put("result", (String) result.get("result"));
                loJson.put("message", (String) result.get("message"));
                System.out.println(loJson);
                break;
            }

            lsSQL = (String) result.get("sql");

            poGRider.beginTrans();
            //String lsBranchCD = lxTransNox.substring(0, 4);
            String lsBranchCD = loRSMaster.getString("sBranchCD");
            
            //Write the insert statement in Check_Disbursement
            if(!lsSQL.isEmpty()){
                System.out.println(lsSQL);
                if(poGRider.executeQuery(lsSQL, DISBURSEMENT_TABLE, lsBranchCD, "") <= 0){
                    poGRider.rollbackTrans();
                    System.exit(0);
                }
            }

            String lsSourceCD = loRSDisbOthers.getString("sSourceCD");
            String lsSourceNo = loRSDisbOthers.getString("sSourceNo");
            String lsClientID = "";
            double lnCredtTot = 0;
            //Check if from SOA(SOAt) or from Purchase Delivery(PODA) and create a AP Payment transaction if it does
            if("SOAt:PODA".contains(lsSourceCD)){
                //Process the record for AP_Payment_Detail
                //AP_Payment_Detail is deliberately process first since i need the value of lnCredtTot for AP_Payment_Master
                lnCredtTot = 0;
                do{
                    double amount = loRSDisbOthers.getDouble("nAmountxx");
                    double lnDebitAmt = amount > 0 ? amount : 0;
                    double lnCredtAmt = amount < 0 ? -amount : 0;
                    lnCredtTot += lnCredtAmt; 
                    
                    lsSQL = "INSERT INTO AP_Payment_Detail" + 
                           " SET sTransNox = " + SQLUtil.toSQL(lxTransNox) +
                              ", nEntryNox = " + SQLUtil.toSQL(loRSDisbOthers.getInt("nEntryNox")) +
                              ", sReferNox = " + SQLUtil.toSQL(RevMigUtil.convertTransNox(loRSDisbOthers.getString("sSourceNo"))) +
                              ", sSourceCD = " + SQLUtil.toSQL(loRSDisbOthers.getString("sSourceCD")) +
                              ", nDebitAmt = " + SQLUtil.toSQL(lnDebitAmt) +
                              ", nCredtAmt = " + SQLUtil.toSQL(lnCredtAmt) +
                              ", nAppliedx = " + SQLUtil.toSQL(Math.abs(loRSDisbOthers.getDouble("nAmountxx"))) +
                              ", dModified = " + SQLUtil.toSQL(loRSMaster.getDate("dModified"));
                    System.out.println(lsSQL);
                    if(poGRider.executeQuery(lsSQL, TABLE_DETAIL, lsBranchCD, "") <= 0){
                        poGRider.rollbackTrans();
                        System.exit(0);
                    }
                }while(loRSDisbOthers.next());    
                
                //get Client ID of the source transaction
                lsClientID = getClientString(lsSourceNo, lsSourceCD);
                
                //create the AP Payment transaction from Disbursement_Master
                lsSQL = "INSERT INTO AP_Payment_Master" + 
                       " SET sTransNox = " + SQLUtil.toSQL(lxTransNox) +
                          ", sClientID = " + SQLUtil.toSQL(lsClientID) +
                          ", dTransact = " + SQLUtil.toSQL(loRSMaster.getDate("dTransact")) +
                          ", sRemarksx = " + SQLUtil.toSQL(loRSMaster.getString("sRemarksx")) +
                          ", nTranTotl = " + SQLUtil.toSQL(loRSMaster.getDouble("nTranTotl") + lnCredtTot) +
                          ", nCashAmtx = 0.00" + 
                          ", nCheckAmt = " + SQLUtil.toSQL(loRSMaster.getDouble("nTranTotl")) +
                          ", nGiftChck = 0.00" + 
                          ", nCredtAmt = " + SQLUtil.toSQL(lnCredtTot) +  
                          ", nEntryNox = " + SQLUtil.toSQL(loRSMaster.getInt("nEntryNox")) +
                          ", cTranStat = " + SQLUtil.toSQL(loRSMaster.getString("cTranStat")) +
                          ", sModified = " + SQLUtil.toSQL(loRSMaster.getString("sModified")) +
                          ", dModified = " + SQLUtil.toSQL(loRSMaster.getDate("dModified"));

                //Write the insert statement of AP_Payment_Master
                System.out.println(lsSQL);
                if(poGRider.executeQuery(lsSQL, TABLE_MASTER, lsBranchCD, "") <= 0){
                    poGRider.rollbackTrans();
                    System.exit(0);
                }

                //TODO:Perform the posting of 
                postDisbursement(loRSMaster, lsBranchCD, lxTransNox, lsClientID, lnCredtTot);
            }

            //Record that this record was demigrated to the old system
            lsSQL = "INSERT INTO Demigration_Map" +
                   " SET sTableNme = 'Disbursement_Master'" + 
                      ", sTransNox = " + SQLUtil.toSQL(loRSMaster.getString("sTransNox")) +
                      ", cLastStat = " + SQLUtil.toSQL(loRSMaster.getString("cTranStat"));
            poGRider.executeUpdate(lsSQL);
            
            poGRider.commitTrans();
        }
    }

    private static ResultSet getMaster() throws SQLException{
        //assume that 4 means the processing of the Disbursement was completed
        String lsSQL = "SELECT" +
                        "  a.sTransNox" +
                        ", a.dTransact" +
                        ", a.sBranchCD" +
                        ", a.sVouchrNo" +
                        ", c.sBnkActID" +
                        ", c.sCheckNox" +
                        ", c.dCheckDte" +
                        ", c.sPayeeIDx" +
                        ", c.nAmountxx" +
                        ", '' sPrtclrID" +
                        ", a.sRemarksx" +
                        ", '' sAcctCode" +
                        ", a.sApproved" +
                        ", '0' cLgrPostx" +
                        ", a.cPrintxxx cVchrPrnt" +
                        ", c.cPrintxxx cChckPrnt" +
                        ", c.dPrintxxx dChckPrnt" +
                        ", '0' cRemitnce" +
                        ", a.cTranStat" +
                        ", '' sSourceCd" +
                        ", '' sSourceNo" +
                        ", '1' cUseCltNm" +
                        ", c.cReleased cChckRlsd" +
                        ", NULL dChckRlsd" +
                        ", NULL cULBDOCDx" +
                        ", c.cLocation" +
                        ", '' sModified" +
                        ", a.dModified" +
                        ", a.nEntryNox" +
                        ", a.nTranTotl" +
                        ", a.nNetTotal" +
                        ", IFNULL(b.sTableNme, '') sTableNme" +
                        ", IFNULL(b.cLastStat, '') cLastStat" +
                       " FROM GCASys_DBF.Disbursement_Master a" +
                            " LEFT JOIN GCASys_DBF.Check_Payments c ON c.sSourceNo = a.sTransNox AND c.sSourceCD = 'DISb'" +
                            " LEFT JOIN Demigration_Map b" +
                                " ON a.sTransNox = b.sTransNox" +
                               " AND b.sTableNme = 'Disbursement_Master'" +
                    " WHERE a.cTranStat IN ('4')" +
                      " AND (b.sTransNox IS NULL)" +
                      " AND a.sIndstCdx = " + SQLUtil.toSQL(INDUSTRY) + 
                    " ORDER BY sTransNox";

        System.out.println(lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        return loRS;
    }
    
    private static ResultSet getDisbOthers(String fsTransNox) throws SQLException{
        String lsSQL = "SELECT a.sPrtclrID, a.sSourceCd, a.sSourceNo, IFNULL(b.sAcctCode, '') sAcctCode, a.nEntryNox, c.dModified, a.nAmountxx, a.nAmtAppld" +
                      " FROM GCASys_DBF.Disbursement_Detail a" +
                           " LEFT JOIN GCASys_DBF.Particular b ON a.sPrtclrID = b.sPrtclrID" +
                           " LEFT JOIN GCASys_DBF.Disbursement_Master c on a.sTransNox = c.sTransNox" +
                      " WHERE a.sTransNox = " + SQLUtil.toSQL(fsTransNox);
        
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        return loRS;
    }    
    
    private static String getClientString(String fsTransNox, String fsSourceCD) throws SQLException{
        String lsSQL;

        if(fsSourceCD.equalsIgnoreCase("SOAt")){
            lsSQL = "SELECT sClientID" +
                   " FROM GCASys_DBF.AP_Payment_Master" +
                    " WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox);
        }
        else{
            lsSQL = "SELECT sSupplier sClientID" +
                   " FROM GCASys_DBF.PO_Receiving_Master" +
                    " WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox);
        }

        ResultSet loRS = poGRider.executeQuery(lsSQL);
        if(loRS.next()){
           return loRS.getString("sClientID");
        }

        return "";
    }    
    
    private static boolean postDisbursement(ResultSet foRS, String fsBranchCD, String fsTransNox, String fsClientID, double fnCredtTot) throws SQLException, GuanzonException{
        RevMigAPClientTrans loClient = new RevMigAPClientTrans(poGRider, fsBranchCD);
        JSONObject loJson = loClient.PaymentIssue(fsClientID, fsTransNox, foRS.getDate("dTransact"), foRS.getDouble("nTranTotl") + fnCredtTot, false);

        if(!((String)loJson.get("result")).equalsIgnoreCase("success")){
            System.out.println(loJson.toJSONString());
            return false;
        }
        
        RevMigBankAccountTrans loBank = new RevMigBankAccountTrans(poGRider);
        loJson = loBank.InitTransaction();
        if(!((String)loJson.get("result")).equalsIgnoreCase("success")){
            System.out.println(loJson.toJSONString());
            return false;
        }

        loJson = loBank.CheckDisbursement(foRS.getString("sBnkActID"), fsTransNox, foRS.getDate("dTransact"), foRS.getDouble("nAmountxx"), foRS.getString("sCheckNox"), "", false);
        if(!((String)loJson.get("result")).equalsIgnoreCase("success")){
            System.out.println(loJson.toJSONString());
            return false;
        }

        return true;
    }
}
