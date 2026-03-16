/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ph.com.guanzongroup.revmig;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.LogWrapper;
import org.guanzon.appdriver.base.SQLUtil;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.revmig.lib.RevMigUtil;

/**
 *
 * @author User
 */
public class PRFDemigration {
    final static String INDUSTRY = "09";
    final static String TABLE_MASTER = "Payment_Request_Master";
    final static String TABLE_DETAIL = "Payment_Request_Detail";
    
    static GRider poGRider;

    public static void main(String args[]) throws SQLException{
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

        ResultSet loRSMaster = getMaster();
        JSONObject loJson = new JSONObject();

        while(loRSMaster.next()){
            Map<String, Object> loMaster = RevMigUtil.row2Map(loRSMaster, "sTableNme:cLastStat");
            String lxTransNox = RevMigUtil.convertTransNox((String)loMaster.get("sTransNox"));
            loMaster.put("sTransNox", lxTransNox);
            
            Map<String, Object> result;
            String lsSQL;

            System.out.println(loMaster);
            System.out.println("loMaster++++++++++++++++++++++++++++");
            result = RevMigUtil.createInsertSQL(loMaster, TABLE_MASTER, "");
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
            
            String lsBranchCD = lxTransNox.substring(0, 4);
            if(!lsSQL.isEmpty()){
                System.out.println(lsSQL);
                if(poGRider.executeQuery(lsSQL, TABLE_MASTER, lsBranchCD, "") <= 0){
                    poGRider.rollbackTrans();
                    System.exit(0);
                }
            }

            String lsReferNox = getDisbursement(loRSMaster.getString("sTransNox"), loRSMaster.getString("sSourceCD"));
            lsReferNox = RevMigUtil.convertTransNox(lsReferNox);
            
            ResultSet loRSDetail;
            //Is request for recurring expense
            if(loRSMaster.getString("sSourceCD").equalsIgnoreCase("REPM")){
                lsSQL = "INSERT INTO Payment_Request_Detail" + 
                       " SET sTransNox = " + SQLUtil.toSQL(loMaster.get("sTransNox")) + 
                          ", nEntryNox = 1" +
                          ", sReferNox = " + SQLUtil.toSQL(lsReferNox) +
                          ", sPRFRemxx = NULL" + 
                          ", dModified = " + SQLUtil.toSQL(loMaster.get("dModified"));
                if(poGRider.executeQuery(lsSQL, TABLE_DETAIL, lsBranchCD, "") <= 0){
                    poGRider.rollbackTrans();
                    System.exit(0);
                }

                loRSDetail = getRcExDetail(loRSMaster.getString("sTransNox"));
                if(!loRSDetail.next()){
                    poGRider.rollbackTrans();
                    System.exit(0);
                }
                
                lsSQL = "INSERT INTO Recurring_Expense_Master" +
                       " SET sTransNox = " + SQLUtil.toSQL(loMaster.get("sTransNox")) + 
                          ", sMonthxxx = ''" + 
                          ", dTransact = " + SQLUtil.toSQL(loMaster.get("dTransact")) +
                          ", sBranchCd = " + SQLUtil.toSQL(loMaster.get("sBranchCd")) +
                          ", sPayeeIDx = " + SQLUtil.toSQL(loRSDetail.getString("sPayeeIDx")) +
                          ", sPrtclrID = " + SQLUtil.toSQL(loRSDetail.getString("sPrtclrID")) +
                          ", nTranTotl = " + SQLUtil.toSQL(loMaster.get("nTranTotl")) +
                          ", sReferNox = " + SQLUtil.toSQL(loMaster.get("sTransNox")) +
                          ", cTranStat = " + SQLUtil.toSQL(loMaster.get("cTranStat")) +
                          ", sModified = " + SQLUtil.toSQL(loMaster.get("sModified")) +
                          ", dModified = " + SQLUtil.toSQL(loMaster.get("dModified"));

                do{
                    Map<String, Object> loDetail = RevMigUtil.row2Map(loRSDetail, "sPayeeIDx");
                    loDetail.put("sTransNox", lxTransNox);
                    System.out.println("loDetail++++++++++++++++++++++++++++");
                    result = RevMigUtil.createInsertSQL(loDetail, "Recurring_Expense_Detail", "");
                    System.out.println(result.get("json"));

                    System.out.println("Check if create is successfull!");
                    if(!"success".equals((String) result.get("result"))){
                        loJson.put("result", (String) result.get("result"));
                        loJson.put("message", (String) result.get("message"));
                        System.out.println(loJson);
                        poGRider.rollbackTrans();
                        System.exit(0);
                    }

                    lsSQL = (String) result.get("sql");

                    if(!lsSQL.isEmpty()){
                        System.out.println(lsSQL);
                        if(poGRider.executeQuery(lsSQL, "Recurring_Expense_Detail", lsBranchCD, "") <= 0){
                            poGRider.rollbackTrans();
                            System.exit(0);
                        }
                    }
                }while(loRSDetail.next());
            }
            else{
                loRSDetail = getDetail(loRSMaster.getString("sTransNox"));

                while(loRSDetail.next()){
                    Map<String, Object> loDetail = RevMigUtil.row2Map(loRSDetail, "");
                    loDetail.put("sTransNox", lxTransNox);
                    loDetail.put("sReferNox", lsReferNox);

                    System.out.println(loDetail);
                    System.out.println("loDetail++++++++++++++++++++++++++++");
                    result = RevMigUtil.createInsertSQL(loDetail, TABLE_DETAIL, "");
                    System.out.println(result.get("json"));

                    System.out.println("Check if create is successfull!");
                    if(!"success".equals((String) result.get("result"))){
                        loJson.put("result", (String) result.get("result"));
                        loJson.put("message", (String) result.get("message"));
                        System.out.println(loJson);
                        poGRider.rollbackTrans();
                        System.exit(0);
                    }

                    lsSQL = (String) result.get("sql");

                    if(!lsSQL.isEmpty()){
                        System.out.println(lsSQL);
                        if(poGRider.executeQuery(lsSQL, TABLE_DETAIL, lsBranchCD, "") <= 0){
                            poGRider.rollbackTrans();
                            System.exit(0);
                        }
                    }
                }
            }
            
            lsSQL = "INSERT INTO Demigration_Map" +
                   " SET sTableNme = 'Payment_Request_Master'" + 
                      ", sTransNox = " + SQLUtil.toSQL(loRSMaster.getString("sTransNox")) +
                      ", cLastStat = " + SQLUtil.toSQL(loRSMaster.getString("cTranStat"));
            poGRider.executeUpdate(lsSQL);
            
            poGRider.commitTrans();
        }
    }

    private static ResultSet getMaster() throws SQLException{
        //assume that 4 means that payment for PRF is issued
        String lsSQL = "SELECT " +
			"  a.sTransNox" +	
			", a.dTransact" +	
			", a.sBranchCd" +	
			", '' sContrlNo" +	
			", a.sSeriesNo sSerialNo" +	
			", a.nTranTotl" +	
			", a.nEntryNox" +	
			", '1' cPrintedx" +	
			", a.cTranStat" +	
			", '' sApproved" +	
			", a.sSourceCd" +	
			", '' sPRFTrans" +	
			", a.sModified"	+
			", a.dModified" +
                        ", IFNULL(b.sTableNme, '') sTableNme" +
			", IFNULL(b.cLastStat, '') cLastStat" +	
                    " FROM GCASys_DBF.Payment_Request_Master a" + 
                            " LEFT JOIN Demigration_Map b" +
                                " ON a.sTransNox = b.sTransNox" +
                               " AND b.sTableNme = " + SQLUtil.toSQL(TABLE_MASTER) +
                    " WHERE a.cTranStat IN ('4')" +
                      " AND (b.sTransNox IS NULL)" +
                      " AND a.sIndstCdx = " + SQLUtil.toSQL(INDUSTRY) + 
                    " ORDER BY sTransNox";

        System.out.println(lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        return loRS;
    }
    
    private static ResultSet getDetail(String fsTransNox) throws SQLException{
        String lsSQL = "SELECT " +
			"  sTransNox" +	
			", nEntryNox" +	
			", '' sReferNox" +	
			", sPRFRemxx" +	
			", dModified" +	
                    " FROM GCASys_DBF.Payment_Request_Detail" + 
                    " WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox);
        
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        return loRS;
    }
    
    private static ResultSet getRcExDetail(String fsTransNox) throws SQLException{
        String lsSQL = "SELECT " +
			"  a.sTransNox" +	
			", a.nEntryNox" +	
			", b.sBranchCd" +	
			", c.sPrtclrID" +	
			", b.sAcctNoxx" +	
			", a.nAmountxx" +	
			", a.cVATaxabl" +	
			", a.nTWithHld" +	
			", a.nDiscount" +	
			", a.nAddDiscx" +	
			", a.dModified" +	
                        ", b.sPayeeIDx" +
                    " FROM GCASys_DBF.Payment_Request_Detail a" + 
                        " LEFT JOIN GCASys_DBF.Recurring_Expense_Schedule b ON a.sRecurrNo = b.sRecurrNo" +
                        " LEFT JOIN GCASys_DBF.Recurring_Expense c ON b.sRecurrID = c.sRecurrID" +
                    " WHERE a.sTransNox = " + SQLUtil.toSQL(fsTransNox);
        
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        return loRS;
    }
    
    private static String getDisbursement(String fsTransNox, String fsSourceCD) throws SQLException{
        String lsSourceCD = fsSourceCD;
        String lsSQL = "SELECT sTransNox" +
                      " FROM Disbursement_Detail" + 
                      " WHERE sSourceCd = " + SQLUtil.toSQL(lsSourceCD) + 
                        " AND sSourceNo = " + SQLUtil.toSQL(fsTransNox);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        
        if(loRS.next()){
            return loRS.getString("sTransNox");
        }
        else{
            return "";
        }
    }
    
    
    
}
