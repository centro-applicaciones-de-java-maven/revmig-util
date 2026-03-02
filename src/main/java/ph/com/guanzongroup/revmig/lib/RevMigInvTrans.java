/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ph.com.guanzongroup.revmig.lib;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.base.StringHelper;

/**
 *
 * @author Administrator
 */
public class RevMigInvTrans {
    private static GRider poDriver;
    private static String psBranchCD; 
    private static String psUserIDxx;

    private boolean pbIsWhsexx;
    private boolean pbIsReverse;
    //private boolean pbDeductxx = true; 
    private String psSourceNo = "";
    private String psSourceCD = "";
    private Date pdTranDate;
    
    private List<DetailEntry> paDetailEntry;
    private List<SerialEntry> paSerialEntry;
    
    public RevMigInvTrans(GRider foGRider){
        poDriver = foGRider;
//        psBranchCD = poDriver.getBranchCode();
//        psUserIDxx = poDriver.getUserID();
//        pbIsWhsexx = poDriver.isWarehouse();

        psBranchCD = "M001";
        psUserIDxx = "M0012500001";
        pbIsWhsexx = false;

        
        paDetailEntry = new ArrayList<>();
        paSerialEntry = new ArrayList<>();
    }

    public RevMigInvTrans(GRider foGRider, String fsBranchCD, boolean fbIsWarehouse, String fsUserIDxx){
        poDriver = foGRider;
        psBranchCD = fsBranchCD;
        psUserIDxx = fsUserIDxx;
        pbIsWhsexx = fbIsWarehouse;
        pbIsReverse = false;
        paDetailEntry = new ArrayList<>();
        paSerialEntry = new ArrayList<>();
    }

    public void initTransaction(String fsSourceCD, String fsSourceNo, Date fdTransact, boolean fbIsReverse){
        psSourceCD = fsSourceCD;
        psSourceNo = fsSourceNo;
        pdTranDate = fdTransact;
        pbIsReverse = fbIsReverse;
    }
    
    public void addDetail(String fsStockIDx, double fnQuantity) throws SQLException, GuanzonException{
        if(psSourceNo.isEmpty()){
            //throw new GuanzonException(GuanzonException.GE_SEQUENCE_EXCEPTION, "Invalid Source No detected!");
            throw new GuanzonException(GuanzonException.GE_SEQUENCE_EXCEPTION);
        }
        
        //load sstockid  
        String lsSQL = "SELECT DISTINCT" + 
                            "  a.sWHouseID" + 
                            ", a.sStockIDx" +
                            ", a.sBranchCD" +
                            ", b.cHsSerial" +
                            ", b.sStockIDx sStockIDy" +
                       " FROM GN_Inventory b" +  
                          " LEFT JOIN GN_Inventory_Master a" +
                                " ON b.sStockIDx = a.sStockIDx" +
                               " AND a.sBranchCd = " + SQLUtil.toSQL(psBranchCD) +
                               " AND a.nQtyOnHnd >= 0.00" +
                       " WHERE b.sStockIDx = " + SQLUtil.toSQL(fsStockIDx) +
                       " ORDER  BY a.sStockIDx DESC";        

        System.out.println(lsSQL);
        ResultSet loRS = poDriver.executeQuery(lsSQL);
        
        if(!loRS.next()){
            throw new GuanzonException(GuanzonException.GE_NOTFOUND_EXCEPTION, "Stock Item does not exist!");
        }

        if(fnQuantity > 0){
            if(loRS.getString("sStockIDx") == null){
                //If item is not existing and transaction type is not accept delivery, throw an error
                String lsTransCode = RevMigInvConstant.BRANCH_TRANSFER_ACCEPTANCE + ":" + RevMigInvConstant.PURCHASE_RECEIVING;
                if(!(lsTransCode).toUpperCase().contains(psSourceCD.toUpperCase())){
                    throw new GuanzonException(GuanzonException.GE_NOTFOUND_EXCEPTION, "Please create the inventory for the branch!");
                    //throw new GuanzonException(GuanzonException.GE_HOSTNAME_EXCEPTION);
                }
            }
            
            if(loRS.getString("cHsSerial").equals("1")){
                String lsNotAllow = RevMigInvConstant.BRANCH_JOBORDER + 
                              ":" + RevMigInvConstant.BRANCH_TRANSFER + 
                              ":" + RevMigInvConstant.BRANCH_TRANSFER_ACCEPTANCE + 
                              ":" + RevMigInvConstant.GCARD_REDEMPTION + 
                              ":" + RevMigInvConstant.IMPOUND + 
                              ":" + RevMigInvConstant.IMPOUND_RELEASE + 
                              ":" + RevMigInvConstant.PURCHASE_RECEIVING + 
                              ":" + RevMigInvConstant.PURCHASE_REPLACEMENT + 
                              ":" + RevMigInvConstant.PURCHASE_RETURN + 
                              ":" + RevMigInvConstant.SALES + 
                              ":" + RevMigInvConstant.SALES_GIVEAWAY + 
                              ":" + RevMigInvConstant.SALES_GIVEAWAY_RELEASE + 
                              ":" + RevMigInvConstant.SALES_REPLACEMENT + 
                              ":" + RevMigInvConstant.SALES_RETURN + 
                              ":" + RevMigInvConstant.WARRANTY_RELEASE + 
                              ":" + RevMigInvConstant.WHOLESALE + 
                              ":" + RevMigInvConstant.WHOLESALE_REPLACEMENT + 
                              ":" + RevMigInvConstant.WHOLESALE_RETURN;

                if((lsNotAllow).toUpperCase().contains(psSourceCD.toUpperCase())){
                    //throw new GuanzonException(GuanzonException.GE_SEQUENCE_EXCEPTION, "Invalid function called! Please use addSerial if inventory has serial.");
                    //throw new GuanzonException(GuanzonException.GE_SEQUENCE_EXCEPTION);
                }
            }

        }

        //Assumed that programmer have exercise due deligence to determine that
        //transaction is valid(example: item has quantity for transactions such as sales, branch transfer) 

        //Create entry
        DetailEntry loDetail;
        if(loRS.getString("sStockIDx") == null){
            loDetail = new DetailEntry(
                      fsStockIDx
                    , "001"
                    , fnQuantity
                    , null);
        }
        else{
            loDetail = new DetailEntry(
                      fsStockIDx
                    , loRS.getString("sWHouseID")
                    , fnQuantity
                    , null);
        }
        
        //Is this the first entry
        if(paDetailEntry.isEmpty()){
            paDetailEntry.add(loDetail);
        }
        else{
            // Search and update or add
            Optional<DetailEntry> match = paDetailEntry.stream()
                .filter(p -> p.psStockIDx.equalsIgnoreCase(loDetail.psStockIDx)
                          && p.psWHouseID.equalsIgnoreCase(loDetail.psWHouseID))
                .findFirst();

            if (match.isPresent()) {
                // Update the quantity and order
                match.get().pnQuantity += loDetail.pnQuantity;
            } else {
                // Add new product
                paDetailEntry.add(loDetail);
            }
        }
        
        //perform testing of accuracy of data extracted here
        do{
            System.out.println(loRS.getString("sStockIDy") + ":" + loRS.getString("sBranchCD") + ":" + loRS.getString("sStockIDx"));
        }while(loRS.next());
        
    }

    //public void addSerial(String fsIndstCdx, String fsSerialID, boolean fbWithOrdr, String fcSoldStat, String fcLocation, double fnUnitPrice, String fsWHouseID) throws SQLException, GuanzonException{
    public void addSerial(String fsSerialID) throws SQLException, GuanzonException{
        String lsSQL;
        lsSQL = "SELECT" + 
                    "  IFNULL(a.sWHouseID, '001') sWHouseID" + 
                    ", b.sStockIDx" +
                    ", b.cHsSerial" +
                    ", c.sSerialID" +
                    ", a.sBranchCd" +
                    ", c.cSoldStat" +
                    ", c.cLocation" +
               " FROM GN_Inventory b" +  
                  " LEFT JOIN GN_Inventory_Master a" +
                        " ON b.sStockIDx = a.sStockIDx" +
                       " AND a.sBranchCd = " + SQLUtil.toSQL(psBranchCD) +
                  " LEFT JOIN GN_Inventory_Serial c" +
                        " ON b.sStockIDx = c.sStockIDx" +
               " WHERE c.sSerialID = " + SQLUtil.toSQL(fsSerialID);

        System.out.println(lsSQL);
        ResultSet loRS = poDriver.executeQuery(lsSQL);
        
        if(!loRS.next()){
            throw new GuanzonException(GuanzonException.GE_NOTFOUND_EXCEPTION, "Serial ID does not exist!");
            //throw new GuanzonException(GuanzonException.GE_HOSTNAME_EXCEPTION);
        }

        String lcSoldStat = loRS.getString("cSoldStat");
        String lcLocation = loRS.getString("cLocation");
        switch(psSourceCD){
            case RevMigInvConstant.IMPOUND:
                //Warehouse/Customer
                if(pbIsReverse){
                    lcLocation = "3";
                }
                else{
                    lcLocation = pbIsWhsexx ? "0" : "1";
                }
                break;
            case RevMigInvConstant.IMPOUND_RELEASE:
                //Customer
                if(pbIsReverse){
                    lcLocation = pbIsWhsexx ? "0" : "1";
                }
                else{
                    lcLocation = "3";
                }
                break;
            case RevMigInvConstant.SALES:
            case RevMigInvConstant.WHOLESALE:
            case RevMigInvConstant.SALES_REPLACEMENT:
            case RevMigInvConstant.WHOLESALE_REPLACEMENT:    
                //Customer
                if(pbIsReverse){
                    lcLocation = pbIsWhsexx ? "0" : "1";
                }
                else{
                    lcLocation = "3";
                }
                lcSoldStat = "1";
                break;
            case RevMigInvConstant.SALES_RETURN:
            case RevMigInvConstant.WHOLESALE_RETURN:
                //Warehouse/Branch
                if(pbIsReverse){
                    lcLocation = "3";
                }
                else{
                    lcLocation = pbIsWhsexx ? "0" : "1";
                }
                
                break;
            case RevMigInvConstant.BRANCH_TRANSFER:
                //On-Transit
                if(pbIsReverse){
                    lcLocation = pbIsWhsexx ? "0" : "1";
                }
                else{
                    lcLocation = "4";
                }
                
                break;
            case RevMigInvConstant.BRANCH_TRANSFER_ACCEPTANCE:
                //Warehouse/Branch
                lcLocation = pbIsWhsexx ? "0" : "1";
                break;
            case RevMigInvConstant.PURCHASE_RECEIVING:    
                //Warehouse/Branch
                if(pbIsReverse){
                    lcLocation = "2";
                }
                else{
                    lcLocation = pbIsWhsexx ? "0" : "1";
                }
                break;
            case RevMigInvConstant.PURCHASE_REPLACEMENT:    
                //Warehouse/Branch
                if(pbIsReverse){
                    lcLocation = "2";
                }
                else{
                    lcLocation = pbIsWhsexx ? "0" : "1";
                }
                break;
            case RevMigInvConstant.PURCHASE_RETURN:    
                //Supplier
                if(pbIsReverse){
                    lcLocation = pbIsWhsexx ? "0" : "1";
                }
                else{
                    lcLocation = "2";
                }
                break;
        }
        
        //add entry for the serialif(
        SerialEntry loSerial = 
                new SerialEntry(
                          loRS.getString("sStockIDx")
                        , loRS.getString("sWHouseID")
                        , fsSerialID
                        , lcSoldStat
                        , lcLocation);
        paSerialEntry.add(loSerial);

        DetailEntry loDetail; 
        loDetail = new DetailEntry(
                  loRS.getString("sStockIDx")
                , loRS.getString("sWHouseID")
                , 1
                , null);

        if(paDetailEntry.isEmpty()){
            paDetailEntry.add(loDetail);
        }
        else{
            // Search and update or add
            Optional<DetailEntry> match = paDetailEntry.stream()
                .filter(p -> p.psStockIDx.equalsIgnoreCase(loDetail.psStockIDx)
                          && p.psWHouseID.equalsIgnoreCase(loDetail.psWHouseID))
                .findFirst();

            if (match.isPresent()) {
                // Update the fields
                match.get().pnQuantity += loDetail.pnQuantity;
            } else {
                // Add new product
                paDetailEntry.add(loDetail);
            }
        }

        //perform testing of accuracy of data extracted here
        do{
            System.out.println(loRS.getString("sSerialID") + ":" + loRS.getString("sBranchCD") + ":" + loRS.getString("sStockIDx"));
        }while(loRS.next());
    }
    
    public void saveTransaction() throws SQLException, GuanzonException{
        for (DetailEntry loDetail : paDetailEntry) {
            String lsSQL = "SELECT *" + 
                          " FROM GN_Inventory_Master" +
                          " WHERE sBranchCd = " + SQLUtil.toSQL(psBranchCD) +
                            " AND sStockIDx = " + SQLUtil.toSQL(loDetail.psStockIDx) ; 
            System.out.println(lsSQL);
            ResultSet loRS = poDriver.executeQuery(lsSQL);
            
            int lnLedgerNo;
            
            //Create the record if it does not exist
            if(!loRS.next()){
                lsSQL = "INSERT INTO GN_Inventory_Master" + 
                       " SET sBranchCd = " + SQLUtil.toSQL(psBranchCD) +
                          ", sStockIDx = " + SQLUtil.toSQL(loDetail.psStockIDx) + 
                          ", sWHouseID = " + SQLUtil.toSQL("") +  
                          ", sSectnIDx = " + SQLUtil.toSQL("") +  
                          ", sLevelIDx = " + SQLUtil.toSQL("") +  
                          ", nReorderx = " + SQLUtil.toSQL(0) +  
                          ", nBackOrdr = " + SQLUtil.toSQL(0) +  
                          ", nResvOrdr = " + SQLUtil.toSQL(0) +  
                          ", nDmoQtyxx = " + SQLUtil.toSQL(0) +  
                          ", nBegQtyxx = " + SQLUtil.toSQL(0) +  
                          ", nQtyOnHnd = " + SQLUtil.toSQL(0) +  
                          ", nLedgerNo = " + SQLUtil.toSQL(0) +  
                          ", nMinLevel = " + SQLUtil.toSQL(0) +  
                          ", nMaxLevel = " + SQLUtil.toSQL(0) +  
                          ", nAveMonSl = " + SQLUtil.toSQL(0) +  
                          ", cClassify = " + SQLUtil.toSQL("F") +  
                          ", nFloatQty = " + SQLUtil.toSQL(0) +  
                          ", cRecdStat = " + SQLUtil.toSQL("1") +
                          ", sModified = " + SQLUtil.toSQL(psUserIDxx) +
                          ", dModified = " + SQLUtil.toSQL(poDriver.getServerDate());
                        
                System.out.println(lsSQL);
                poDriver.executeQuery(lsSQL, "GN_Inventory_Master", psBranchCD, "");

                lnLedgerNo = 1;
            }
            else{
                lnLedgerNo = loRS.getInt("nLedgerNo") + 1;
            }
                
            //initialize variable to use in determining the type of changes in the stock
            double lnQtyInxxx = 0;
            double lnQtyOutxx = 0;

            if(pbIsReverse){
                //identify the type of changes in the stock(quantity/order)
                if(RevMigInvConstant.getDebitTrans().toUpperCase().contains(psSourceCD.toUpperCase())){
                    //lnQtyInxxx += loDetail.pnQuantity;
                    lnQtyOutxx += loDetail.pnQuantity;
                } 

                if(RevMigInvConstant.getCreditTrans().toUpperCase().contains(psSourceCD.toUpperCase())){
                    //lnQtyOutxx += loDetail.pnQuantity;
                    lnQtyInxxx += loDetail.pnQuantity;
                }

            }
            else{
                //identify the type of changes in the stock(quantity/order)
                if(RevMigInvConstant.getDebitTrans().toUpperCase().contains(psSourceCD.toUpperCase())){
                    lnQtyInxxx += loDetail.pnQuantity;
                } 

                if(RevMigInvConstant.getCreditTrans().toUpperCase().contains(psSourceCD.toUpperCase())){
                    lnQtyOutxx += loDetail.pnQuantity;
                }
            }

            //Update GN_Inventory_Master
            lsSQL = "UPDATE GN_Inventory_Master" + 
                   " SET nQtyOnHnd = nQtyOnHnd + " + SQLUtil.toSQL(lnQtyInxxx - lnQtyOutxx) +
                      ", nLedgerNo = " + SQLUtil.toSQL(StringHelper.prepad(String.valueOf(lnLedgerNo), 6, '0')) +
                      ", sModified = " + SQLUtil.toSQL(psUserIDxx) +
                      ", dModified = " + SQLUtil.toSQL(poDriver.getServerDate()) +
                   " WHERE sBranchCd = " + SQLUtil.toSQL(psBranchCD) +
                     " AND sStockIDx = " + SQLUtil.toSQL(loDetail.psStockIDx); 
            System.out.println(lsSQL);
            poDriver.executeQuery(lsSQL, "GN_Inventory_Master", psBranchCD, "");

            lsSQL = "INSERT INTO GN_Inventory_Ledger" +
                   " SET sBranchCd = " + SQLUtil.toSQL(psBranchCD) +
                      ", sStockIDx = " + SQLUtil.toSQL(loDetail.psStockIDx) + 
                      ", nLedgerNo = " + SQLUtil.toSQL(StringHelper.prepad(String.valueOf(lnLedgerNo), 6, '0')) +
                      ", dTransact = " + SQLUtil.toSQL(pdTranDate) +
                      ", sSourceCD = " + SQLUtil.toSQL(psSourceCD) +
                      ", sSourceNo = " + SQLUtil.toSQL(psSourceNo) +
                      ", nQtyInxxx = " + SQLUtil.toSQL(lnQtyInxxx) +
                      ", nQtyOutxx = " + SQLUtil.toSQL(lnQtyOutxx) +
                      ", nQtyOrder = " + SQLUtil.toSQL(0) +
                      ", nQtyIssue = " + SQLUtil.toSQL(0) +
                      ", cUnitType = " + SQLUtil.toSQL("1") +
                      ", nUnitPrce = " + SQLUtil.toSQL(0) +
                      ", nAvgCostx = " + SQLUtil.toSQL(0) +
                      ", dModified = " + SQLUtil.toSQL(poDriver.getServerDate());
            System.out.println(lsSQL);
            poDriver.executeQuery(lsSQL, "GN_Inventory_Ledger", psBranchCD, "");
        }
        
        for (SerialEntry loSerial : paSerialEntry) {
            String lsSQL = "SELECT *" +
                          " FROM GN_Inventory_Serial" +
                          " WHERE sSerialID = " + SQLUtil.toSQL(loSerial.psSerialID);
            ResultSet loRS = poDriver.executeQuery(lsSQL);
            System.out.println(lsSQL);
            if(!loRS.next()){
                throw new GuanzonException(GuanzonException.GE_NOTFOUND_EXCEPTION, "Inventory serial does not exist!");
                //throw new GuanzonException(GuanzonException.GE_HOSTNAME_EXCEPTION);
            }

            lsSQL = "UPDATE GN_Inventory_Serial" +
                   " SET sBranchCD = " + SQLUtil.toSQL(psBranchCD) +
                      ", cLocation = " + SQLUtil.toSQL(loSerial.pcLocation) +
                      ", cSoldStat = " + SQLUtil.toSQL(loSerial.pcSoldStat) +
                      ", dModified = " + SQLUtil.toSQL(poDriver.getServerDate()) +
                   " WHERE sSerialID = " + SQLUtil.toSQL(loSerial.psSerialID);
            System.out.println(lsSQL);
            poDriver.executeQuery(lsSQL, "GN_Inventory_Serial", psBranchCD, "");
            
            lsSQL = "INSERT INTO GN_Inventory_Serial_Ledger" +
                   " SET sSerialID = " + SQLUtil.toSQL(loSerial.psSerialID) +
                      ", sBranchCD = " + SQLUtil.toSQL(psBranchCD) +
                      ", dTransact = " + SQLUtil.toSQL(pdTranDate) +
                      ", sSourceCd = " + SQLUtil.toSQL(psSourceCD) +
                      ", sSourceNo = " + SQLUtil.toSQL(psSourceNo) +
                      ", dModified = " + SQLUtil.toSQL(poDriver.getServerDate());
            System.out.println(lsSQL);
            poDriver.executeQuery(lsSQL, "GN_Inventory_Serial_Ledger", psBranchCD, "");
        }
    }
    
    private class DetailEntry{
        public String psStockIDx;
        public String psWHouseID;
        public double pnQuantity;
        public Date pdExpiryxx;
        
        public DetailEntry(String fsStockIDx, String fsWHouseID, double fnQuantity, Date fdExpiryxx){
            this.psStockIDx = fsStockIDx;
            this.psWHouseID = fsWHouseID;
            this.pnQuantity = fnQuantity;
            this.pdExpiryxx = fdExpiryxx;
        }
    }
    
    private class SerialEntry{
        public String psStockIDx;
        public String psWHouseID;
        public String psSerialID;
        public String pcLocation;
        public String pcSoldStat;

        public SerialEntry(String fsStockIDx, String fsWHouseID, String fsSerialID, String fcSoldStat, String fcLocation){
            this.psStockIDx = fsStockIDx;
            this.psWHouseID = fsWHouseID;
            this.psSerialID = fsSerialID;
            this.pcLocation = fcLocation;
            this.pcSoldStat = fcSoldStat;
        }
    }
}
