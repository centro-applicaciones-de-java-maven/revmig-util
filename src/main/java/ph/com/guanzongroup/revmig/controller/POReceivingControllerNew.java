/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ph.com.guanzongroup.revmig.controller;

import com.google.gson.Gson;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.revmig.lib.RevMigAPClientTrans;
import ph.com.guanzongroup.revmig.lib.RevMigInvConstant;
import ph.com.guanzongroup.revmig.lib.RevMigInvTrans;
import ph.com.guanzongroup.revmig.lib.RevMigUtil;
import ph.com.guanzongroup.revmig.model.Model_Base;

/**
 *
 * @author kalyptus
 */
public class POReceivingControllerNew {
    private static final String psObjectName = "ph.com.guanzongroup.revmig.util.revmig.controller.POReceivingController";
    private static final String psTableMaster = "GN_PO_Receiving_Master";
    private static final String psTableDetail = "GN_PO_Receiving_Detail";
    private static final String psTableSerial = "GN_PO_Receiving_Serial";
    
    private List<Map<String, Object>> poMasterMeta;
    private List<Map<String, Object>> poDetailMeta;
    private List<Map<String, Object>> poSerialMeta;

    private GRider poGRider;
    private Model_Base poMaster;
    private Model_Base poDetail;
    private Model_Base poSerial;

    private List< Map<String, Object>> poaDetail;
    private List< Map<String, Object>> poaSerial;

    private List< Map<String, Object>> poaOldDetail;
    private List< Map<String, Object>> poaOldSerial;
    
    private String p_sBranchCD;
    private String p_xTransNox;

    
    private int pnEditMode;
    private boolean pbInitTrans = false;

    public JSONObject setMaster(ResultSet foRs, String fsExclude) throws SQLException{
        JSONObject loJson = new JSONObject();

        //check if initialized was run
        if(!pbInitTrans){
            loJson.put("result", "error");
            loJson.put("message", psObjectName + " -> Object Not Initialized!");
            return loJson;
        }

        loJson = poMaster.setValue(foRs, fsExclude);
        if(!"success".equals((String) loJson.get("result"))){
            return loJson;
        }

        if(p_xTransNox.isEmpty()){
            p_xTransNox = RevMigUtil.convertTransNox((String) poMaster.getValue("sTransNox"));
        }
        
        // String lsTransNox = MiscUtil.getNextCode("GN_PO_Receiving_Master", "sTransNox", true, poGRider.getConnection(), p_sBranchCD);
        poMaster.setValue("sTransNox", p_xTransNox);
        
        loJson.put("result", "success");
        return loJson;
    }
    
    public JSONObject addDetail() throws SQLException{
        JSONObject loJson = new JSONObject();

        //check if initialized was run
        if(!pbInitTrans){
            loJson.put("result", "error");
            loJson.put("message", psObjectName + " -> Object Not Initialized!");
            return loJson;
        }

        Map<String, Object> loDetail;
        
        if(!poaDetail.isEmpty()){
            //get last detail model information
            loDetail = poaDetail.get(poaDetail.size() - 1);
            
            //check if properly populated
            String lsStockIDx = (String)loDetail.get("sStockIDx");
            if(lsStockIDx.isEmpty()){
                loJson.put("result", "error");
                loJson.put("message", psObjectName + " -> Last Detail not properly populated!");
                return loJson;
            }
        }
        
        loJson = poDetail.newTransaction();
        if(!"success".equals((String) loJson.get("result"))){
            return loJson;
        }

        poaDetail.add(poDetail.getValue());

        loJson.put("result", "success");
        loJson.put("detail", poaDetail.size());
        return loJson;
    }
    
    public JSONObject setDetail(int fnKey, ResultSet foRs) throws SQLException{
        JSONObject loJson = new JSONObject();

        //check if initialized was run
        if(!pbInitTrans){
            loJson.put("result", "error");
            loJson.put("message", psObjectName + " -> Object Not Initialized!");
            return loJson;
        }
        
        //check if size of detail 
        if(fnKey >= poaDetail.size()){
            loJson.put("result", "error");
            loJson.put("message", psObjectName + " -> Invalid index assigning detail!");
            return loJson;
        }

        poDetail.newTransaction();
        
        //assigned value to detail
        poDetail.setValue(foRs, "");
        
       if(p_xTransNox.isEmpty()){
            p_xTransNox = RevMigUtil.convertTransNox((String) poDetail.getValue("sTransNox"));
        }
        
        poDetail.setValue("sTransNox", p_xTransNox);
        poaDetail.set(fnKey, poDetail.getValue());

        loJson.put("result", "success");
        loJson.put("data", fnKey);
        return loJson;
    }

    public JSONObject addSerial() throws SQLException{
        JSONObject loJson = new JSONObject();

        //check if initialized was run
        if(!pbInitTrans){
            loJson.put("result", "error");
            loJson.put("message", psObjectName + " -> Object Not Initialized!");
            return loJson;
        }

        Map<String, Object> loDetail;
        
        if(!poaSerial.isEmpty()){
            //get last detail model information
            loDetail = poaSerial.get(poaSerial.size() - 1);
            
            //check if properly populated
            String lsSerialID = (String)loDetail.get("sSerialID");
            if(lsSerialID.isEmpty()){
                loJson.put("result", "error");
                loJson.put("message", psObjectName + " -> Last Serial not properly populated!");
                return loJson;
            }
        }
        
        loJson = poSerial.newTransaction();
        if(!"success".equals((String) loJson.get("result"))){
            return loJson;
        }

        poaSerial.add(poSerial.getValue());

        loJson.put("result", "success");
        return loJson;
    }

    public JSONObject setSerial(int fnKey, ResultSet foRs) throws SQLException{
        JSONObject loJson = new JSONObject();

        //check if initialized was run
        if(!pbInitTrans){
            loJson.put("result", "error");
            loJson.put("message", psObjectName + " -> Object Not Initialized!");
            return loJson;
        }
        
        //check if size of detail 
        if(fnKey >= poaSerial.size()){
            loJson.put("result", "error");
            loJson.put("message", psObjectName + " -> Invalid index assigning detail!");
            return loJson;
        }

        poSerial.newTransaction();
        
        //assigned value to detail
        poSerial.setValue(foRs, "");

       if(p_xTransNox.isEmpty()){
            p_xTransNox = RevMigUtil.convertTransNox((String) poSerial.getValue("sTransNox"));
        }

        poSerial.setValue("sTransNox", p_xTransNox);
        poaSerial.set(fnKey, poSerial.getValue());

        loJson.put("result", "success");
        return loJson;
    }

    public JSONObject initTransaction(GRider foGRider, String fsBranchCD){
        JSONObject loJson;

        poGRider = foGRider;
        p_sBranchCD = fsBranchCD;
        
        //Initialize master base model
        poMaster = new Model_Base();
        loJson = poMaster.initTransaction(foGRider, psTableMaster);
         if(!"success".equals((String) loJson.get("result"))){
            return loJson;
        }

        poMasterMeta = poMaster.getMeta();
         
        //Initialize detail base model
        poaDetail = new ArrayList<>();
        //create meta record for serial
        poDetail = new Model_Base();
        loJson = poDetail.initTransaction(poGRider, psTableDetail);
        if(!"success".equals((String) loJson.get("result"))){
            return loJson;
        }
        
        poDetailMeta = poDetail.getMeta();
        
        //Initialize serial base model
        poaSerial = new ArrayList<>();
        //create meta record for serial
        poSerial = new Model_Base();
        loJson = poSerial.initTransaction(poGRider, psTableSerial);
        if(!"success".equals((String) loJson.get("result"))){
            return loJson;
        }
        poSerialMeta = poSerial.getMeta();
        
        pnEditMode = EditMode.UNKNOWN;
        pbInitTrans = true;
        
        loJson.put("result", "success");
        return loJson;
    }
    
    public JSONObject newTransaction() throws SQLException{
        JSONObject loJson = new JSONObject();

        if(!pbInitTrans){
            loJson.put("result", "error");
            loJson.put("message", psObjectName + " -> Object Not Initialized!");
            return loJson;
        }

        p_xTransNox = "";
        
        loJson = poMaster.newTransaction();
         if(!"success".equals((String) loJson.get("result"))){
            return loJson;
        }

        //Initialize detail base model
        poaDetail = new ArrayList<>();

        //Initialize serial base model
        poaSerial = new ArrayList<>();

        pnEditMode = EditMode.ADDNEW;
        
        loJson.put("result", "success");
        return loJson;
    }
    
    public JSONObject openTransaction(String fsTransNox) throws SQLException{
        JSONObject loJson = new JSONObject();

        if(!pbInitTrans){
            loJson.put("result", "error");
            loJson.put("message", psObjectName + " -> Object Not Initialized!");
            return loJson;
        }

        p_xTransNox = fsTransNox;
        
        String lsSQL;
        ResultSet loRS;
        
        //load master record
        lsSQL = "SELECT * FROM " + psTableMaster + 
               " WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox);
        System.out.println(lsSQL);
        loRS = poGRider.executeQuery(lsSQL);
        if(!loRS.next()){
            loJson.put("result", "error");
            loJson.put("message", "No record found");
            return loJson;
        }

        loJson = poMaster.loadTransaction(loRS, "sTransNox = " + SQLUtil.toSQL(fsTransNox));
        if(!"success".equals((String) loJson.get("result"))){
            return loJson;
        }

        //Initialize detail base model
        poaDetail = new ArrayList<>();
        loJson = loadDetail(fsTransNox);
        if(!"success".equals((String) loJson.get("result"))){
            return loJson;
        }

        //Initialize serial base model
        poaSerial = new ArrayList<>();
        loJson = loadSerial(fsTransNox);
        if(!"success".equals((String) loJson.get("result"))){
            return loJson;
        }

        // Clone poaDetail into poaOldDetail
        poaOldDetail = new ArrayList<>();
        for (Map<String, Object> item : poaDetail) {
            poaOldDetail.add(new HashMap<>(item)); // deep copy each map
        }

        // Clone poaSerial into poaOldSerial
        poaOldSerial = new ArrayList<>();
        for (Map<String, Object> item : poaSerial) {
            poaOldSerial.add(new HashMap<>(item)); // deep copy each map
        }
        
        pnEditMode = EditMode.READY;
        
        loJson.put("result", "success");
        return loJson;
    }

    public JSONObject saveTransaction() throws SQLException{
        JSONObject loJson = new JSONObject();

        if(!pbInitTrans){
            loJson.put("result", "error");
            loJson.put("message", psObjectName + " -> Object Not Initialized!");
            return loJson;
        }

        if(pnEditMode == EditMode.UNKNOWN){
            loJson.put("result", "error");
            loJson.put("message", psObjectName + " -> Object Status is unknown!");
            return loJson;
        }
        
        poGRider.beginTrans();

        //save master model
        System.out.println("Saving master...");
        loJson = poMaster.saveTransaction();
        if(!"success".equals((String) loJson.get("result"))){
            poGRider.rollbackTrans();
            return loJson;
        }

        String result;        
        
        System.out.println("Saving detail...");
        loJson = saveDetail();
        result = (String) loJson.get("result");
        if (!"success".equalsIgnoreCase(result)) {
            poGRider.rollbackTrans();
            return loJson;
        }
        
        System.out.println("Saving serial...");
        loJson = saveSerial();
        result = (String) loJson.get("result");
        if (!"success".equalsIgnoreCase(result)) {
            poGRider.rollbackTrans();
            return loJson;
        }
        
        poGRider.commitTrans();
        
        loJson.put("result", "success");
        return loJson;
    }
    
    private JSONObject saveDetail(){
        JSONObject loJson = new JSONObject();
        Map<String, Object> result;
        String lsSQL;

        if(pnEditMode == EditMode.ADDNEW){
            System.out.println("Saving detail for add new...");
            for (Map<String, Object> loDetail : poaDetail) {
                result = RevMigUtil.createInsertSQL(loDetail, psTableDetail, "");
                System.out.println(result.get("json"));

                System.out.println("Check if creating insert is successfull!");
                if(!"success".equals((String) result.get("result"))){
                    loJson.put("result", (String) result.get("result"));
                    loJson.put("message", (String) result.get("message"));
                    return loJson;
                }

                lsSQL = (String) result.get("sql");

                if(!lsSQL.isEmpty()){
                    System.out.println(lsSQL);
                    String lsBranchCD = loDetail.get("sTransNox").toString().substring(0, 4);
                    poGRider.executeQuery(lsSQL, psTableDetail, lsBranchCD, "");
                }
            } 
        }
        else{
            //save detail model
            System.out.println("Saving detail for update...");
            for (int i = 0; i < Math.min(poaDetail.size(), poaOldDetail.size()); i++) {
                Map<String, Object> newItem = poaDetail.get(i);
                Map<String, Object> oldItem = poaOldDetail.get(i);

                // Ensure nEntryNox is treated as integer
                int entryNo = (newItem.get("nEntryNox") instanceof Number)
                                ? ((Number) newItem.get("nEntryNox")).intValue()
                                : Integer.parseInt(newItem.get("nEntryNox").toString());

                // Build filter correctly: sTransNox quoted, nEntryNox numeric
                String lsFilter = "sTransNox = " + SQLUtil.toSQL((String)newItem.get("sTransNox")) +
                                  " AND nEntryNox = " + entryNo;

                result = RevMigUtil.createUpdateSQL(newItem, oldItem, psTableDetail, lsFilter, "");
                System.out.println(result.get("json"));
                
                System.out.println("Check if create is successfull!");
                if(!"success".equals((String) result.get("result"))){
                    loJson.put("result", (String) result.get("result"));
                    loJson.put("message", (String) result.get("message"));
                    return loJson;
                }

                lsSQL = (String) result.get("sql");

                if(!lsSQL.isEmpty()){
                    System.out.println(lsSQL);
                    String lsBranchCD = newItem.get("sTransNox").toString().substring(0, 4);
                    poGRider.executeQuery(lsSQL, psTableDetail, lsBranchCD, "");
                }
            }
            
            // INSERT for extra items in poaDetail
            System.out.println("Saving insert for new detail after update...");
            for (int i = poaOldDetail.size(); i < poaDetail.size(); i++) {
                Map<String, Object> loDetail = poaDetail.get(i);
                
                if(((String)loDetail.get("sStockIDx")).isEmpty()){
                    continue;
                }
                
                result = RevMigUtil.createInsertSQL(loDetail, psTableDetail, "");
                System.out.println(result.get("json"));

                System.out.println("Check if create is successfull!");
                if(!"success".equals((String) result.get("result"))){
                    loJson.put("result", (String) result.get("result"));
                    loJson.put("message", (String) result.get("message"));
                    return loJson;
                }

                lsSQL = (String) result.get("sql");

                if(!lsSQL.isEmpty()){
                    System.out.println(lsSQL);
                    String lsBranchCD = loDetail.get("sTransNox").toString().substring(0, 4);
                    poGRider.executeQuery(lsSQL, psTableDetail, lsBranchCD, "");
                }
            }

            // DELETE for extra items in poaOldDetail
            System.out.println("Deleting old detail after update...");
            for (int i = poaDetail.size(); i < poaOldDetail.size(); i++) {
                Map<String, Object> oldItem = poaOldDetail.get(i);

                // Ensure nEntryNox is treated as integer
                int entryNo = (oldItem.get("nEntryNox") instanceof Number)
                                ? ((Number) oldItem.get("nEntryNox")).intValue()
                                : Integer.parseInt(oldItem.get("nEntryNox").toString());

                // Build filter correctly: sTransNox quoted, nEntryNox numeric
                String lsFilter = "sTransNox = " + SQLUtil.toSQL((String)oldItem.get("sTransNox")) +
                                  " AND nEntryNox = " + entryNo;
                
                
                String deleteSQL = "DELETE FROM " + psTableDetail + 
                                  " WHERE " + lsFilter;
                System.out.println(deleteSQL);
            }
        }

        loJson.put("result", "success");
        return loJson;
    }

    private JSONObject saveSerial(){
        JSONObject loJson = new JSONObject();
        Map<String, Object> result;
        String lsSQL;

        if(pnEditMode == EditMode.ADDNEW){
            System.out.println("Saving serial for add new...");
            for (Map<String, Object> loDetail : poaSerial) {
                result = RevMigUtil.createInsertSQL(loDetail, psTableSerial, "");
                System.out.println(result.get("json"));

                System.out.println("Check if create is successfull!");
                if(!"success".equals((String) result.get("result"))){
                    loJson.put("result", (String) result.get("result"));
                    loJson.put("message", (String) result.get("message"));
                    return loJson;
                }

                lsSQL = (String) result.get("sql");

                if(!lsSQL.isEmpty()){
                    System.out.println(lsSQL);
                    String lsBranchCD = loDetail.get("sTransNox").toString().substring(0, 4);
                    poGRider.executeQuery(lsSQL, psTableSerial, lsBranchCD, "");
                }
            } 
        }
        else{
            //save detail model
            System.out.println("Saving serial for update...");
            for (int i = 0; i < Math.min(poaSerial.size(), poaOldSerial.size()); i++) {
                Map<String, Object> newItem = poaSerial.get(i);
                Map<String, Object> oldItem = poaOldSerial.get(i);

                // Ensure nEntryNox is treated as integer
                int entryNo = (newItem.get("nEntryNox") instanceof Number)
                                ? ((Number) newItem.get("nEntryNox")).intValue()
                                : Integer.parseInt(newItem.get("nEntryNox").toString());

                // Build filter correctly: sTransNox quoted, nEntryNox numeric
                String lsFilter = "sTransNox = " + SQLUtil.toSQL((String)newItem.get("sTransNox")) +
                             " AND nEntryNox = " + entryNo +
                             " AND sSerialID = " + SQLUtil.toSQL((String)newItem.get("sSerialID"));

                result = RevMigUtil.createUpdateSQL(newItem, oldItem, psTableSerial, lsFilter, "");
                System.out.println(result.get("json"));
                
                System.out.println("Check if create is successfull!");
                if(!"success".equals((String) result.get("result"))){
                    loJson.put("result", (String) result.get("result"));
                    loJson.put("message", (String) result.get("message"));
                    return loJson;
                }

                lsSQL = (String) result.get("sql");

                if(!lsSQL.isEmpty()){
                    System.out.println(lsSQL);
                    String lsBranchCD = newItem.get("sTransNox").toString().substring(0, 4);
                    poGRider.executeQuery(lsSQL, psTableDetail, lsBranchCD, "");
                }
            }
            
            // INSERT for extra items in poaDetail
            System.out.println("Saving serial for new serial after update...");
            for (int i = poaOldSerial.size(); i < poaSerial.size(); i++) {
                Map<String, Object> loDetail = poaSerial.get(i);

                result = RevMigUtil.createInsertSQL(loDetail, psTableSerial, "");
                System.out.println(result.get("json"));

                System.out.println("Check if create is successfull!");
                if(!"success".equals((String) result.get("result"))){
                    loJson.put("result", (String) result.get("result"));
                    loJson.put("message", (String) result.get("message"));
                    return loJson;
                }

                lsSQL = (String) result.get("sql");

                if(!lsSQL.isEmpty()){
                    System.out.println(lsSQL);
                    String lsBranchCD = loDetail.get("sTransNox").toString().substring(0, 4);
                    poGRider.executeQuery(lsSQL, psTableSerial, lsBranchCD, "");
                }
            }

            // DELETE for extra items in poaOldSerial
            System.out.println("Deleting old serial after update...");
            for (int i = poaSerial.size(); i < poaOldSerial.size(); i++) {
                Map<String, Object> oldItem = poaOldSerial.get(i);

                // Ensure nEntryNox is treated as integer
                int entryNo = (oldItem.get("nEntryNox") instanceof Number)
                                ? ((Number) oldItem.get("nEntryNox")).intValue()
                                : Integer.parseInt(oldItem.get("nEntryNox").toString());

                // Build filter correctly: sTransNox quoted, nEntryNox numeric
                String lsFilter = "sTransNox = " + SQLUtil.toSQL((String)oldItem.get("sTransNox")) +
                             " AND nEntryNox = " + entryNo +
                             " AND sSerialID = " + SQLUtil.toSQL((String)oldItem.get("sSerialID"));
                
                String deleteSQL = "DELETE FROM " + psTableSerial + 
                                  " WHERE " + lsFilter;
                System.out.println(deleteSQL);
            }
        }

        loJson.put("result", "success");
        return loJson;
    }
    

    public JSONObject confirmTransaction() throws SQLException, GuanzonException{
        JSONObject loJson = new JSONObject();

        if(!pbInitTrans){
            loJson.put("result", "error");
            loJson.put("message", psObjectName + " -> Object Not Initialized!");
            return loJson;
        }

        if(pnEditMode == EditMode.UNKNOWN){
            loJson.put("result", "error");
            loJson.put("message", psObjectName + " -> Object Status is unknown!");
            return loJson;
        }
        
        poGRider.beginTrans();

        String lsBranchCD = ((String) poMaster.getValue("sTransNox")).substring(0, 4);
        //RevMigInvTrans poInvTrans = new RevMigInvTrans(poGRider);
        RevMigInvTrans poInvTrans = new RevMigInvTrans(poGRider, lsBranchCD, false, "M0012500001");

        System.out.println("poMaster.getValue('dTransact')");
        System.out.println(poMaster.getValue("dTransact"));
        
        poInvTrans.initTransaction(
                RevMigInvConstant.PURCHASE_RECEIVING, 
                (String) poMaster.getValue("sTransNox"), 
                //creating purchase receiving 
                SQLUtil.toDate((String)poMaster.getValue("dTransact"), SQLUtil.FORMAT_SHORT_DATE),  
                false);
        
        //save detail model
        for (Map<String, Object> loDetail : poaDetail) {
            String lcHsSerial = (String)loDetail.get("cHsSerial");
            String lsStockIDx = (String)loDetail.get("sStockIDx");
            //int lnQuantity = (int)loDetail.getValue("nQuantity");
            int lEntryNox = (int)loDetail.get("nEntryNox");
            
            //int lnQuantity = ((BigDecimal) loDetail.getValue("nQuantity")).intValue();
            //int lEntryNox = ((BigDecimal) loDetail.getValue("nEntryNox")).intValue();
            Object value = loDetail.get("nQuantity");
            int lnQuantity = 0;

            if (value instanceof BigDecimal) {
                lnQuantity = ((BigDecimal) value).intValue();
            } else if (value instanceof Integer) {
                lnQuantity = (Integer) value;
            } else if (value != null) {
                lnQuantity = Integer.parseInt(value.toString());
            }

            if(lcHsSerial.equals("1")){
                for (Map<String, Object> loSerial : poaSerial) {
                    String lsSerialID = (String)loSerial.get("sSerialID");
                    int lnEntryNoy = (int)loSerial.get("nEntryNox");
                    if(lnEntryNoy == lEntryNox){
                        poInvTrans.addSerial(lsSerialID);
                    }
                }
            }
            else{
                poInvTrans.addDetail(lsStockIDx, lnQuantity);
            }
        }

        //post transaction
        poInvTrans.saveTransaction();

        poMaster.setValue("cTranStat", "1");
        
        String lsSQL = "UPDATE GN_PO_Receiving_Master" + 
                      " SET cTranStat = '1'" + 
                      " WHERE sTransNox = " + SQLUtil.toSQL(poMaster.getValue("sTransNox"));
        System.out.println(p_sBranchCD);
        poGRider.executeQuery(lsSQL, "GN_PO_Receiving_Master", p_sBranchCD, "");
        
        poGRider.commitTrans();
        
        loJson.put("result", "success");
        return loJson;
    }
    
    public JSONObject postTransaction() throws SQLException, GuanzonException{
        JSONObject loJson = new JSONObject();

        if(!pbInitTrans){
            loJson.put("result", "error");
            loJson.put("message", psObjectName + " -> Object Not Initialized!");
            return loJson;
        }

        if(pnEditMode == EditMode.UNKNOWN){
            loJson.put("result", "error");
            loJson.put("message", psObjectName + " -> Object Status is unknown!");
            return loJson;
        }
        
        poGRider.beginTrans();

        String lsBranchCD = ((String) poMaster.getValue("sTransNox")).substring(0, 4);
        //RevMigInvTrans poInvTrans = new RevMigInvTrans(poGRider);
        RevMigAPClientTrans poClientTrans = new RevMigAPClientTrans(poGRider, lsBranchCD);

        System.out.println("poMaster.getValue('dTransact')");
        System.out.println(poMaster.getValue("dTransact"));
        
        poClientTrans.Purchase(
                (String)poMaster.getValue("sClientID"),
                (String) poMaster.getValue("sTransNox"), 
                SQLUtil.toDate((String)poMaster.getValue("dTransact"), SQLUtil.FORMAT_SHORT_DATE), 
                (Double) poMaster.getValue("nTranTotl") - (Double) poMaster.getValue("nDiscount") , 
                false);
        
        poMaster.setValue("cTranStat", "2");
        
        String lsSQL = "UPDATE GN_PO_Receiving_Master" + 
                      " SET cTranStat = '2'" + 
                      " WHERE sTransNox = " + SQLUtil.toSQL(poMaster.getValue("sTransNox"));
        System.out.println(p_sBranchCD);
        poGRider.executeQuery(lsSQL, "GN_PO_Receiving_Master", p_sBranchCD, "");
        
        poGRider.commitTrans();
        
        loJson.put("result", "success");
        return loJson;
    }
    
    private JSONObject loadDetail(String fsTransNox) throws SQLException{
        JSONObject loJson = new JSONObject();
        
        String lsSQL;
        ResultSet loRS;


        //load detail record
        lsSQL = "SELECT * FROM " + psTableDetail + 
               " WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox) + 
               " ORDER BY sTransNox, nEntryNox"; 
        
        loRS = poGRider.executeQuery(lsSQL);

        while(loRS.next()){
            loJson = poDetail.newTransaction();
            if(!"success".equals((String) loJson.get("result"))){
                return loJson;
            }
            
            poDetail.newTransaction();
            poDetail.setValue(loRS, "");
            
            poaDetail.add(poDetail.getValue());
        }
        
        loJson.put("result", "success");
        return loJson;
    }
    
    private JSONObject loadSerial(String fsTransNox) throws SQLException{
        JSONObject loJson = new JSONObject();
        
        String lsSQL;
        ResultSet loRS;
        
        //load serial record
        lsSQL = "SELECT * FROM " + psTableSerial + 
               " WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox) + 
               " ORDER BY sTransNox, nEntryNox, sSerialID"; 
        loRS = poGRider.executeQuery(lsSQL);

        while(loRS.next()){
            loJson = poSerial.newTransaction();
            if(!"success".equals((String) loJson.get("result"))){
                return loJson;
            }
            
            poSerial.newTransaction();
            poSerial.setValue(loRS, "");
            
            poaSerial.add(poSerial.getValue());
        }

        loJson.put("result", "success");
        return loJson;
    }
    
    public void showTransaction(){
        System.out.println("Showing master data: +++++++++++++++++++++");
        Map<String, Object> loBase = poMaster.getValue();
        for (Map.Entry<String, Object> entry : loBase.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            System.out.println("Column: " + key + ", Value: " + value);
        }
        
        System.out.println("Showing detail data: +++++++++++++++++++++");
        for (Map<String, Object> detail : poaDetail) {
            System.out.println("Entry:");
            for (Map.Entry<String, Object> entry : detail.entrySet()) {
                System.out.println("   Key: " + entry.getKey() + ", Value: " + entry.getValue());
            }
        }

        System.out.println("Showing serial data: +++++++++++++++++++++");
        for (Map<String, Object> detail : poaSerial) {
            System.out.println("Entry:");
            for (Map.Entry<String, Object> entry : detail.entrySet()) {
                System.out.println("   Key: " + entry.getKey() + ", Value: " + entry.getValue());
            }
        }
    }
}
