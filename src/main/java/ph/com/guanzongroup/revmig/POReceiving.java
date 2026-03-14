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

import ph.com.guanzongroup.revmig.controller.POReceivingController;
/**
 * Notes: 1) Since this will be implemented on a certain part of PSD only,  
 *           make sure that the branch code where the items will be created be different from what PSD used.
 *        2) Set the default database to GGC_ISysDBF  
 */
public class POReceiving {
    final static String[] PSD_BRANCHES = {"GK01", "W005", "M0W1"};
    final static String INDUSTRY = "09";
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
        System.setProperty("sys.default.path.metadata", "D:/GGC_Java_Systems/metadata//");
        
        
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
            
            String lsTransNox = "";
            logwrapr.info("Processing Purchase Receiving of branches...");
            for (String lsBranchCD : PSD_BRANCHES) {
                //System.out.println("Processing Branch: " + PSD_BRANCHES[lnctr]);
                logwrapr.info("Processing Branch: " + lsBranchCD);

                String lsLastPONo = getLastPOReceiving(lsBranchCD);

                
                logwrapr.info("Extracting Branch: " + lsBranchCD);
                ResultSet loRSMaster = getMaster(lsBranchCD, lsLastPONo);

                POReceivingController poControl;
                JSONObject loJson;
                while(loRSMaster.next()){
                    logwrapr.info("Processing: " + loRSMaster.getString("sTransNox"));
                    lsTransNox = loRSMaster.getString("sTransNox");
                    ResultSet loRSDetail = getDetail(loRSMaster.getString("sTransNox"));
                    ResultSet loRSSerial = getSerial(loRSMaster.getString("sTransNox"));

                    logwrapr.info("Checking item: " + loRSMaster.getString("sTransNox"));
                    checkItem(loRSDetail);
                    logwrapr.info("Checking serial: " + loRSMaster.getString("sTransNox"));
                    checkSerial(loRSSerial);
                    
                    //create po receiving controller
                    poControl = new POReceivingController();
                    System.out.println("Calling init");
                    loJson = poControl.initTransaction(poGRider, lsBranchCD);
                    if(!"success".equals((String) loJson.get("result"))){
                        System.out.println(loJson.toJSONString());
                        System.out.println(0);
                        return;
                    }

                    //create new transaction
                    System.out.println("Calling new");
                    loJson = poControl.newTransaction();
                    if(!"success".equals((String) loJson.get("result"))){
                        System.out.println(loJson.toJSONString());
                        System.out.println(0);
                        return;
                    }

                    //assigned master
                    System.out.println("Assigning master");
                    loJson = poControl.setMaster(loRSMaster);
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
                    
                    //post po
                    
                    System.out.println("Confirming to GN_PO_Receiving - " + loRSMaster.getString("sTransNox"));
                    loJson = poControl.confirmTransaction();
                    if(!"success".equals((String) loJson.get("result"))){
                        System.out.println(loJson.toJSONString());
                        System.out.println(0);
                        return;
                    }
                }
            }

            if(!lsTransNox.isEmpty()){
                String lsSQL;
                lsSQL = "INSERT INTO GGC_ISysDBF.Demigration_Map(sTableNme, sTransNox, cLastStat)" +
                        " SELECT 'PO_Receiving_Master', pm.sTransNox, pm.cTranStat" +
                        " FROM PO_Receiving_Master pm" +
                        " WHERE pm.cTranStat IN('1', '2')" +
                        "  AND NOT EXISTS (" +
                            " SELECT 1" +
                            " FROM GGC_ISysDBF.Demigration_Map dm" +
                            " WHERE dm.sTableNme = 'PO_Receiving_Master'" +
                              " AND dm.sTransNox = pm.sTransNox)";
                poGRider.executeUpdate(lsSQL);
                
            //poGRider.beginTrans();
            //Make sure to post the transaction so that it will not be downloaded again...
                poGRider.executeUpdate("USE GCASys_DBF");
                lsSQL = "UPDATE PO_Receiving_Master" + 
                              " SET cTranStat = '2'" + 
                              " WHERE cTranStat = '1'";
                System.out.println(lsSQL);
                poGRider.executeUpdate(lsSQL);
            //poGRider.commitTrans();
            }
            
        } catch (SQLException | GuanzonException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static String getLastPOReceiving(String fsBranchCD) throws SQLException{
        String lsSQL = "SELECT sTransNox" + 
                      " FROM GN_PO_Receiving_Master" + 
                      " WHERE sTransNox LIKE " + SQLUtil.toSQL(fsBranchCD + "%") +
                      " ORDER BY sTransNox DESC" + 
                      " LIMIT 1";
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        
        String lsTransNox;
        if(loRS.next()){
            lsTransNox = loRS.getString("sTransNox");
        }
        else{
            lsTransNox = fsBranchCD + "00000000";
        }
        
        return lsTransNox;
    }
    
    private static ResultSet getMaster(String fsBranchCD, String fsLastPONo) throws SQLException{
        String lsSQL = "SELECT " +
			"  sTransNox" +	
			", dTransact" +	
			", sCompnyID" +	
			", sSupplier" +	
			", dRefernce dReferDte" +	
			", sReferNox" +	
			", sSalesInv" +	
			", nTranTotl" +	
			", sTermCode sTermIDxx" +	
			", '' sAcctCode" +	
			", nDiscount" +	
			", nAddDiscx" +	
			", cVATaxabl" +	
			", nTWithHld" +	
			", dDueDatex" +	
			", null dStatChng" +	
			", cProcessd cPaymStat" +	
			", nAmtPaidx" +	
			", sRemarksx" +	
			", '' sApproved" +	
			", nEntryNox" +	
			", cTranStat" +	
			", '' sOrderNox" +
			", '' sAddedByx"	+ 
			", null dAddedDte"	+
			", sModified"	+
			", dModified" +
                    " FROM GCASys_DBF.PO_Receiving_Master" + 
                    " WHERE sTransNox LIKE " + SQLUtil.toSQL(fsBranchCD + "%") + 
                      " AND sIndstCdx = " + SQLUtil.toSQL(INDUSTRY) + 
                      " AND cTranStat = '1'" + 
                    " ORDER BY sTransNox";

//                      " AND sTransNox > " + SQLUtil.toSQL(fsLastPONo)  + 

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


