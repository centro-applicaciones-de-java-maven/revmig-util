/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ph.com.guanzongroup.revmig;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.LogWrapper;
import org.guanzon.appdriver.base.SQLUtil;
import org.json.simple.JSONObject;

import ph.com.guanzongroup.revmig.controller.POReceivingControllerNew;
import ph.com.guanzongroup.revmig.lib.RevMigUtil;
/**
 * Notes: 1) Since this will be implemented on a certain part of PSD only,  
 *           make sure that the branch code where the items will be created be different from what PSD used.
 *        2) Set the default database to GGC_ISysDBF  
 */
public class POReceivingNew {
    final static String INDUSTRY = "09";
    final static String TABLE_NAME = "PO_Receiving_Master";
    
    static GRider poGRider;
    
    public static void main(String args[]){
        LogWrapper logwrapr = new LogWrapper("POReceving Reverse Migration", "revmig-poreceiving.log");       

        //Set important path configuration for this utility
        String path;
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            path = "D:/GGC_Maven_Systems";
        }
        else{
            path = "/srv/mac/GGC_Maven_Systems";
        }
        
        System.setProperty("sys.default.path.temp", path + "/temp");
        System.setProperty("sys.default.path.config", path);
        System.setProperty("sys.default.path.metadata", "D:/GGC_Java_Systems/metadata/");
        
        
        String lsProdctID = "gRider";

        //TODO: temporarily used my user id for testing
        String lsUserIDxx = "M001111122";
        //String lsUserIDxx = "M001250012";
        
        poGRider = null;

        logwrapr.info("Start of PO Receiving Reverse Migration...");
        
        try {

            logwrapr.info("Loading application driver...");
            poGRider = new GRider(lsProdctID);
            
            logwrapr.info("Loading user...");
            if (!poGRider.loadUser(lsProdctID, lsUserIDxx)){
                logwrapr.severe(poGRider.getMessage() + poGRider.getMessage());
                System.exit(1);
            }
            
            logwrapr.info("Processing Purchase Receiving of branches...");
            ResultSet loRSMaster = getMaster();

            POReceivingControllerNew poControl;
            JSONObject loJson;
            while(loRSMaster.next()){
                String lsTransNox = loRSMaster.getString("sTransNox");
                String lxTransNox = RevMigUtil.convertTransNox(lsTransNox);
                String lsBranchCD = lsTransNox.substring(0, 4);

                logwrapr.info("Processing: " + lsTransNox);
                ResultSet loRSDetail = getDetail(lsTransNox);
                ResultSet loRSSerial = getSerial(lsTransNox);

                logwrapr.info("Checking item: " + lsTransNox);
                checkItem(loRSDetail);
                logwrapr.info("Checking serial: " + lsTransNox);
                checkSerial(loRSSerial);

                //create po receiving controller
                poControl = new POReceivingControllerNew();
                System.out.println("Calling init");
                loJson = poControl.initTransaction(poGRider, lsBranchCD);
                if(!"success".equals((String) loJson.get("result"))){
                    System.out.println(loJson.toJSONString());
                    System.out.println(0);
                    return;
                }
                
                System.out.println("Tablle: " + loRSMaster.getString("sTableNme"));
                if(loRSMaster.getString("sTableNme").isEmpty()){
                    //create new transaction
                    System.out.println("Calling new");
                    loJson = poControl.newTransaction();
                    if(!"success".equals((String) loJson.get("result"))){
                        System.out.println(loJson.toJSONString());
                        System.out.println(0);
                        return;
                    }
                }
                else{
                    System.out.println("Opening transaction: " + lxTransNox);
                    loJson = poControl.openTransaction(lxTransNox);
                    if(!"success".equals((String) loJson.get("result"))){
                        System.out.println(loJson.toJSONString());
                        System.out.println(0);
                        continue;
                    }
                }

                //assigned master
                System.out.println("Assigning master...");
                loJson = poControl.setMaster(loRSMaster, "sTableNme:cLastStat");
                if(!"success".equals((String) loJson.get("result"))){
                    System.out.println(loJson.toJSONString());
                    System.out.println(0);
                    return;
                }

                //assigned detail
                int lnctr = 0;
                loRSDetail.beforeFirst();
                while(loRSDetail.next()){
                    loJson = poControl.addDetail();
                    if(!"success".equals((String) loJson.get("result"))){
                        System.out.println(loJson.toJSONString());
                        return;

                        //continue;
                    }

                    System.out.println("Assigning detail:" + loRSDetail.getString("sStockIDx"));
                    loJson = poControl.setDetail(lnctr, loRSDetail);
                    if(!"success".equals((String) loJson.get("result"))){
                        System.out.println(loJson.toJSONString());
                        return;
                        //continue;
                    }

                    System.out.println(loJson.toJSONString());
                    lnctr++;
                }

                //assigned serial
                lnctr = 0;
                loRSSerial.beforeFirst();
                while(loRSSerial.next()){
                    loJson = poControl.addSerial();
                    if(!"success".equals((String) loJson.get("result"))){
                        System.out.println(loJson.toJSONString());
                        return;
                        //continue;
                    }

                    System.out.println("Assigning serial");
                    loJson = poControl.setSerial(lnctr, loRSSerial);
                    if(!"success".equals((String) loJson.get("result"))){
                        System.out.println(loJson.toJSONString());
                        return;
                        //continue;
                    }
                    lnctr++;
                }

                //save po
                //poControl.showTransaction();
                System.out.println("Saving to GN_PO_Receiving - " + loRSMaster.getString("sTransNox"));
                loJson = poControl.saveTransaction();
                if(!"success".equals((String) loJson.get("result"))){
                    System.out.println(loJson.toJSONString());
                    System.out.println(0);
                    return;
                }

                String lsSQL;
                lsSQL = "INSERT INTO Demigration_Map" +
                       " SET sTableNme = 'PO_Receiving_Master'" + 
                          ", sTransNox = " + SQLUtil.toSQL(loRSMaster.getString("sTransNox")) +
                          ", cLastStat = " + SQLUtil.toSQL(loRSMaster.getString("cTranStat"));
                poGRider.executeUpdate(lsSQL);
                
                if(loRSMaster.getString("cLastStat").isEmpty()){
                    System.out.println("Confirming to GN_PO_Receiving - " + loRSMaster.getString("sTransNox"));
                    loJson = poControl.confirmTransaction();
                    if(!"success".equals((String) loJson.get("result"))){
                        System.out.println(loJson.toJSONString());
                        System.out.println(0);
                        return;
                    }
                }
                else if(loRSMaster.getString("cLastStat").equalsIgnoreCase("1")){
                    if(loRSMaster.getString("cTranStat").equalsIgnoreCase("2")){
                        
                    }
                }
                
            }
        } catch (SQLException | GuanzonException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static ResultSet getMaster() throws SQLException{
        String lsSQL = "SELECT " +
			"  a.sTransNox" +	
			", a.dTransact" +	
			", a.sCompnyID" +	
			", a.sSupplier" +	
			", a.dRefernce dReferDte" +	
			", a.sReferNox" +	
			", a.sSalesInv" +	
			", a.nTranTotl" +	
			", a.sTermCode sTermIDxx" +	
			", '' sAcctCode" +	
			", a.nDiscount" +	
			", a.nAddDiscx" +	
			", a.cVATaxabl" +	
			", a.nTWithHld" +	
			", a.dDueDatex" +	
			", null dStatChng" +	
			", a.cProcessd cPaymStat" +	
			", a.nAmtPaidx" +	
			", a.sRemarksx" +	
			", '' sApproved" +	
			", a.nEntryNox" +	
			", a.cTranStat" +	
			", '' sOrderNox" +
			", '' sAddedByx"	+ 
			", null dAddedDte"	+
			", a.sModified"	+
			", a.dModified" +
                        ", IFNULL(b.sTableNme, '') sTableNme" +
			", IFNULL(b.cLastStat, '') cLastStat" +	
                    " FROM GCASys_DBF.PO_Receiving_Master a" + 
                            " LEFT JOIN Demigration_Map b" +
                                " ON a.sTransNox = b.sTransNox" +
                               " AND b.sTableNme = " + SQLUtil.toSQL(TABLE_NAME) +
                    " WHERE a.cTranStat IN ('1', '2')" +
                      " AND (b.sTransNox IS NULL OR a.cTranStat > b.cLastStat)" +
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
			", cSerialze cHsSerial" +	
			", cUnitType" +	
			", dModified" +	
                    " FROM GCASys_DBF.PO_Receiving_Detail" + 
                    " WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox);
        
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        return loRS;
    }

    private static ResultSet getSerial(String fsTransNox) throws SQLException{
        String lsSQL = "SELECT " +
			"  sTransNox" +	
			", nEntryNox" +	
			", sSerialID" +	
			", '' sOldSerlx" +	
			", dModified" +	
                    " FROM GCASys_DBF.PO_Receiving_Serial" + 
                    " WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox);
        
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        return loRS;
    }
    
    private static void checkItem(ResultSet foRS) throws SQLException, GuanzonException{
        System.out.println("Checkitem beforefirst");
        foRS.beforeFirst();
        while(foRS.next()){
            String lsSQL = "SELECT sStockIDx, sModelIDx " + 
                          " FROM GN_Inventory" +  
                          " WHERE sStockIDx = " + SQLUtil.toSQL(foRS.getString("sStockIDx")); 
            System.out.println(lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            
            if(!loRS.next()){
                System.out.println("Copying item: " + foRS.getString("sStockIDx"));
                copyItem(foRS.getString("sStockIDx"));
            }
        }
    }
    
    private static void copyItem(String fsStockIDx) throws SQLException, GuanzonException{
        String lsSQL = "SELECT" +
			"  sStockIDx" +	
			", sBarCodex sBarrcode" +	
			", sDescript" +	
			", sBrandIDx" +	
			", sModelIDx" +	
			", '' sMadeIDxx" +	
			", sColorIDx" +	
			", '' sSizeIDxx" +	
			", sCategCd1 sCategID1" +	
			", sCategCd2 sCategID2" +	
			", sCategCd3 sCategID3" +	
			", sCategCd4 sCategID4" +	
			", '' sCategID5" +	
			", sAltBarCd sPartNoxx" +	
			", cSerialze cHsSerial" +	
			", nUnitPrce nPurchase" +	
			", nSelPrice" +	
			", 0 nSelPrce2" +	
			", 0 nSelPrce3" +	
			", 0 nSelPrce4" +	
			", 0 nLastPrce" +	
			", nDiscLev1 nMaxDisc1" +	
			", nDiscLev2 nMaxDisc2" +	
			", nDiscLev3 nMaxDisc3" +	
			", 0 dPrceAsOf" +	
			", sInvTypCd" +	
			", cUnitType cInvTypex" +	
			", cRecdStat" +	
			", CAST(AES_DECRYPT(UNHEX(sModified), '08220326') AS CHAR) sModified" +	
			", dModified" +	
                    " FROM GCASys_DBF.Inventory" +  
                    " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx) +
                      " AND sIndstCdx = " + SQLUtil.toSQL(INDUSTRY); 
        System.out.println(lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        
        loRS.next();
        
        lsSQL = "INSERT INTO GN_Inventory SET " + 
                    "  sStockIDx = " + SQLUtil.toSQL(loRS.getObject("sStockIDx")) +
                    ", sBarrcode = " + SQLUtil.toSQL(loRS.getObject("sBarrcode")) +
                    ", sDescript = " + SQLUtil.toSQL(loRS.getObject("sDescript")) +
                    ", sBrandIDx = " + SQLUtil.toSQL(loRS.getObject("sBrandIDx")) +
                    ", sModelIDx = " + SQLUtil.toSQL(loRS.getObject("sModelIDx")) +
                    ", sMadeIDxx = " + SQLUtil.toSQL(loRS.getObject("sMadeIDxx")) +
                    ", sColorIDx = " + SQLUtil.toSQL(loRS.getObject("sColorIDx")) +
                    ", sSizeIDxx = " + SQLUtil.toSQL(loRS.getObject("sSizeIDxx")) +
                    ", sCategID1 = " + SQLUtil.toSQL(loRS.getObject("sCategID1")) +
                    ", sCategID2 = " + SQLUtil.toSQL(loRS.getObject("sCategID2")) +
                    ", sCategID3 = " + SQLUtil.toSQL(loRS.getObject("sCategID3")) +
                    ", sCategID4 = " + SQLUtil.toSQL(loRS.getObject("sCategID4")) +
                    ", sCategID5 = " + SQLUtil.toSQL(loRS.getObject("sCategID5")) +
                    ", sPartNoxx = " + SQLUtil.toSQL(loRS.getObject("sPartNoxx")) +
                    ", cHsSerial = " + SQLUtil.toSQL(loRS.getObject("cHsSerial")) +
                    ", nPurchase = " + SQLUtil.toSQL(loRS.getObject("nPurchase")) +
                    ", nSelPrice = " + SQLUtil.toSQL(loRS.getObject("nSelPrice")) +
                    ", nSelPrce2 = " + SQLUtil.toSQL(loRS.getObject("nSelPrce2")) +
                    ", nSelPrce3 = " + SQLUtil.toSQL(loRS.getObject("nSelPrce3")) +
                    ", nSelPrce4 = " + SQLUtil.toSQL(loRS.getObject("nSelPrce4")) +
                    ", nLastPrce = " + SQLUtil.toSQL(loRS.getObject("nLastPrce")) +
                    ", nMaxDisc1 = " + SQLUtil.toSQL(loRS.getObject("nMaxDisc1")) +
                    ", nMaxDisc2 = " + SQLUtil.toSQL(loRS.getObject("nMaxDisc2")) +
                    ", nMaxDisc3 = " + SQLUtil.toSQL(loRS.getObject("nMaxDisc3")) +
                    ", dPrceAsOf = " + SQLUtil.toSQL(loRS.getObject("dPrceAsOf")) +
                    ", sInvTypCd = " + SQLUtil.toSQL(loRS.getObject("sInvTypCd")) +
                    ", cInvTypex = " + SQLUtil.toSQL(loRS.getObject("cInvTypex")) +
                    ", cRecdStat = " + SQLUtil.toSQL(loRS.getObject("cRecdStat")) +
                    ", sModified = " + SQLUtil.toSQL(loRS.getObject("sModified")) +
                    ", dModified = " + SQLUtil.toSQL(loRS.getObject("dModified"));        
        poGRider.executeQuery(lsSQL, "GN_Inventory", "", "");
    }

    private static void checkSerial(ResultSet foRS) throws SQLException, GuanzonException{
        foRS.beforeFirst();
        while(foRS.next()){
            String lsSQL = "SELECT sSerialID" + 
                          " FROM GN_Inventory_Serial" +  
                          " WHERE sSerialID = " + SQLUtil.toSQL(foRS.getString("sSerialID")); 
            ResultSet loRS = poGRider.executeQuery(lsSQL);

            if(!loRS.next()){
                copySerial(foRS.getString("sSerialID"));
            }
        }
    }

    private static void copySerial(String fsSerialID) throws SQLException, GuanzonException{
        String lsSQL = "SELECT" +
			"  sSerialID" +	
			", sBranchCd" +	
			", sSerial01 sSerialNo" +	
			", '' sSupplier" +	
			", sStockIDx" +	
			", cLocation" +	
			", cSoldStat" +	
			", cUnitType" +	
			",  '' cUnitClas" +	
			",  '' sModified" +	
			", dModified" +	
                    " FROM GCASys_DBF.Inv_Serial" +  
                    " WHERE sSerialID = " + SQLUtil.toSQL(fsSerialID); 
        System.out.println(lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        
        loRS.next();
        
        lsSQL = "INSERT INTO GN_Inventory_Serial SET " + 
                    "  sSerialID = " + SQLUtil.toSQL(loRS.getObject("sSerialID")) +
                    ", sBranchCd = " + SQLUtil.toSQL(loRS.getObject("sBranchCd")) +
                    ", sSerialNo = " + SQLUtil.toSQL(loRS.getObject("sSerialNo")) +
                    ", sSupplier = " + SQLUtil.toSQL(loRS.getObject("sSupplier")) +
                    ", sStockIDx = " + SQLUtil.toSQL(loRS.getObject("sStockIDx")) +
                    ", cLocation = " + SQLUtil.toSQL(loRS.getObject("cLocation")) +
                    ", cSoldStat = " + SQLUtil.toSQL(loRS.getObject("cSoldStat")) +
                    ", cUnitType = " + SQLUtil.toSQL(loRS.getObject("cUnitType")) +
                    ", dModified = " + SQLUtil.toSQL(loRS.getObject("dModified"));
        poGRider.executeQuery(lsSQL, "GN_Inventory_Serial", "", "");
    }
}


