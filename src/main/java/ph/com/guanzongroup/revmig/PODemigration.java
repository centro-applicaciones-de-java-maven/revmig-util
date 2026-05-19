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
import org.guanzon.appdriver.constant.EditMode;
import ph.com.guanzongroup.revmig.lib.RevMigUtil;
import org.json.simple.JSONObject;

/**
 *
 * @author User
 */
public class PODemigration {
    final static String INDUSTRY = "09";
    final static String TABLE_MASTER = "GN_PO_Master";
    final static String TABLE_DETAIL = "GN_PO_Detail";
    
    static GRider poGRider;

    public static void main(String args[]) throws SQLException{
        LogWrapper logwrapr = new LogWrapper("PO Transaction Reverse Migration", "revmig-po.log");

        //Set important path configuration for this utility
        String path;
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            path = "D:/GGC_Maven_Systems";
        }
        else{
            path = "/srv/GGC_Maven_Systems";
        }

        System.setProperty("sys.default.path.temp", path + "/temp");
        System.setProperty("sys.default.path.config", path);

        String lsProdctID = "gRider";

        //TODO: temporarily used my user id for testing
        String lsUserIDxx = "08220326";
        //String lsUserIDxx = "M001250012";

        poGRider = null;

        logwrapr.info("Start of PO Transaction Reverse Migration...");

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
            
            if(!lsSQL.isEmpty()){
                System.out.println(lsSQL);
                String lsBranchCD = lxTransNox.substring(0, 4);
                if(poGRider.executeQuery(lsSQL, TABLE_MASTER, lsBranchCD, "") <= 0){
                    poGRider.rollbackTrans();
                    System.exit(0);
                }
            }
            
            ResultSet loRSDetail = getDetail(loRSMaster.getString("sTransNox"));

            while(loRSDetail.next()){
                Map<String, Object> loDetail = RevMigUtil.row2Map(loRSDetail, "sTableNme:cLastStat");
                loDetail.put("sTransNox", lxTransNox);
                
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
                    String lsBranchCD = lxTransNox.substring(0, 4);
                    if(poGRider.executeQuery(lsSQL, TABLE_DETAIL, lsBranchCD, "") <= 0){
                        poGRider.rollbackTrans();
                        System.exit(0);
                    }
                }
            }

            lsSQL = "INSERT INTO Demigration_Map" +
                   " SET sTableNme = 'PO_Master'" + 
                      ", sTransNox = " + SQLUtil.toSQL(loRSMaster.getString("sTransNox")) +
                      ", cLastStat = " + SQLUtil.toSQL(loRSMaster.getString("cTranStat"));
            poGRider.executeUpdate(lsSQL);
            
            poGRider.commitTrans();
        }
    }

    private static ResultSet getMaster() throws SQLException{
        //assume that five means approved and advance payment was paid
        String lsSQL = "SELECT " +
			"  a.sTransNox" +	
			", a.dTransact" +	
			", a.sCompnyID" +	
			", a.sSupplier" +	
			", '' sBrandIdx" +	
			", a.sReferNox" +	
			", a.sBranchCd" +	
			", a.sRemarksx" +	
			", a.nTranTotl" +	
			", a.nDiscount" +	
			", a.nAddDiscx" +	
			", '1' cVATaxabl" +	
			", 0.00 nTWithHld" +	
			", a.nAmtPaidx" +	
			", a.sTermCode sTermIDxx" +	
			", a.dExpected dDelivery" +	
			", null dDueDatex" +	
			", '' sApproved" +	
			", a.nEntryNox" +	
			", a.cEmailSnt" +	
			", a.cTranStat" +	
			", '' sAddedByx" +	
			", NULL dAddedDte" +	
			", '' sModified"	+
			", a.dModified" +
                        ", IFNULL(b.sTableNme, '') sTableNme" +
			", IFNULL(b.cLastStat, '') cLastStat" +	
                    " FROM GCASys_DBF.PO_Master a" + 
                            " LEFT JOIN Demigration_Map b" +
                                " ON a.sTransNox = b.sTransNox" +
                               " AND b.sTableNme = " + SQLUtil.toSQL("PO_Master") +
                    " WHERE a.cTranStat IN ('5')" +
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
			", sStockIDx" +	
			", nQuantity" +	
			", nUnitPrce" +	
			", nRecOrder" +	
			", nReceived" +	
			", dModified" +	
                    " FROM GCASys_DBF.PO_Detail" + 
                    " WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox);
        
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        return loRS;
    }
}
