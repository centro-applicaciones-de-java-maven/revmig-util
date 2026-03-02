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
import java.util.List;
import java.util.Map;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.revmig.lib.RevMigInvConstant;
import ph.com.guanzongroup.revmig.lib.RevMigInvTrans;
import ph.com.guanzongroup.revmig.model.Model_Base;

/**
 *
 * @author kalyptus
 */
public class POReceivingController {
    private static final String psObjectName = "ph.com.guanzongroup.revmig.util.revmig.controller.POReceivingController";
    private static final String psTableMaster = "GN_PO_Receiving_Master";
    private static final String psTableDetail = "GN_PO_Receiving_Detail";
    private static final String psTableSerial = "GN_PO_Receiving_Serial";
    
    private List<Map<String, Object>> poMasterMeta;
    private List<Map<String, Object>> poDetailMeta;
    private List<Map<String, Object>> poSerialMeta;
    
    private GRider poGRider;
    private Model_Base poMaster;
    private List<Model_Base> poDetail;
    private List<Model_Base> poSerial;
    private String p_sBranchCD;
    
    private int pnEditMode;
    private boolean pbInitTrans = false;

    public JSONObject setMaster(ResultSet foRs) throws SQLException{
        JSONObject loJson = new JSONObject();

        //check if initialized was run
        if(!pbInitTrans){
            loJson.put("result", "error");
            loJson.put("message", psObjectName + " -> Object Not Initialized!");
            return loJson;
        }
        
        loJson = poMaster.setValue(foRs);
        if(!"success".equals((String) loJson.get("result"))){
            return loJson;
        }

        String lsTransNox = MiscUtil.getNextCode("GN_PO_Receiving_Master", "sTransNox", true, poGRider.getConnection(), p_sBranchCD);
        poMaster.setValue("sTransNox", lsTransNox);
        
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

        Model_Base loDetail;
        
        if(!poDetail.isEmpty()){
            //get last detail model information
            loDetail = poDetail.get(poDetail.size() - 1);

            //check if properly populated
            String lsStockIDx = (String)loDetail.getValue("sStockIDx");
            if(lsStockIDx.isEmpty()){
                loJson.put("result", "error");
                loJson.put("message", psObjectName + " -> Last Detail not properly populated!");
                return loJson;
            }
        }
        
        loDetail = new Model_Base();
        loJson = loDetail.initTransaction(poGRider, psTableDetail, poDetailMeta);
        if(!"success".equals((String) loJson.get("result"))){
            return loJson;
        }

        loJson = loDetail.newTransaction();
        if(!"success".equals((String) loJson.get("result"))){
            return loJson;
        }

        poDetail.add(loDetail);

        loJson.put("result", "success");
        loJson.put("detail", poDetail.size());
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
        if(fnKey >= poDetail.size()){
            loJson.put("result", "error");
            loJson.put("message", psObjectName + " -> Invalid index assigning detail!");
            return loJson;
        }

        //assigned value to detail
        Model_Base loDetail = poDetail.get(fnKey);
        loDetail.setValue(foRs);
        poDetail.set(fnKey, loDetail);

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

        Model_Base loDetail;
        
        if(!poSerial.isEmpty()){
            //get last detail model information
            loDetail = poSerial.get(poSerial.size() - 1);

            //check if properly populated
            String lsSerialID = (String)loDetail.getValue("sSerialID");
            if(lsSerialID.isEmpty()){
                loJson.put("result", "error");
                loJson.put("message", psObjectName + " -> Last Serial not properly populated!");
                return loJson;
            }
        }
        
        loDetail = new Model_Base();
        loJson = loDetail.initTransaction(poGRider, psTableSerial, poSerialMeta );
        if(!"success".equals((String) loJson.get("result"))){
            return loJson;
        }

        loJson = loDetail.newTransaction();
        if(!"success".equals((String) loJson.get("result"))){
            return loJson;
        }

        poSerial.add(loDetail);

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
        if(fnKey >= poSerial.size()){
            loJson.put("result", "error");
            loJson.put("message", psObjectName + " -> Invalid index assigning detail!");
            return loJson;
        }

        //assigned value to detail
        Model_Base loDetail = poSerial.get(fnKey);
        loDetail.setValue(foRs);
        poSerial.set(fnKey, loDetail);

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
        poDetailMeta = null;
        poSerialMeta = null;
         
        //Initialize detail base model
        poDetail = new ArrayList<>();

        //create meta record for detail
        Model_Base loDetail = new Model_Base();
        loJson = loDetail.initTransaction(poGRider, psTableDetail);
        if(!"success".equals((String) loJson.get("result"))){
            return loJson;
        }
        poDetailMeta = loDetail.getMeta();
        
        //Initialize serial base model
        poSerial = new ArrayList<>();
        
        //create meta record for serial
        loDetail = new Model_Base();
        loJson = loDetail.initTransaction(poGRider, psTableSerial);
        if(!"success".equals((String) loJson.get("result"))){
            return loJson;
        }
        poSerialMeta = loDetail.getMeta();
        
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
        
        loJson = poMaster.newTransaction();
         if(!"success".equals((String) loJson.get("result"))){
            return loJson;
        }

        //Initialize detail base model
        poDetail = new ArrayList<>();

        //Initialize serial base model
        poSerial = new ArrayList<>();

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

        String lsSQL;
        ResultSet loRS;
        
        //load master record
        lsSQL = "SELECT * FROM " + psTableMaster + 
               " WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox);
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
        poDetail = new ArrayList<>();
        loJson = loadDetail(fsTransNox);
        if(!"success".equals((String) loJson.get("result"))){
            return loJson;
        }

        //Initialize serial base model
        poSerial = new ArrayList<>();
        loJson = loadSerial(fsTransNox);
        if(!"success".equals((String) loJson.get("result"))){
            return loJson;
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

        //save detail model
        System.out.println("Saving detail...");
        for (Model_Base loDetail : poDetail) {
            String lsStockIDx = (String)loDetail.getValue("sStockIDx");
            if(!lsStockIDx.isEmpty()){
                System.out.println("Saving detail..." + lsStockIDx);
                loDetail.setValue("sTransNox", poMaster.getValue("sTransNox"));
                loJson = loDetail.saveTransaction();
                if(!"success".equals((String) loJson.get("result"))){
                    poGRider.rollbackTrans();
                    return loJson;
                }
            }
        }

        //save serial model
        System.out.println("Saving serial...");
        for (Model_Base loDetail : poSerial) {
            String lsSerialID = (String)loDetail.getValue("sSerialID");
            System.out.println("Saving serial:" + lsSerialID);
            if(!lsSerialID.isEmpty()){
                loDetail.setValue("sTransNox", poMaster.getValue("sTransNox"));
                loJson = loDetail.saveTransaction();
                if(!"success".equals((String) loJson.get("result"))){
                    poGRider.rollbackTrans();
                    return loJson;
                }
            }
        }
        
        poGRider.commitTrans();
        
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
        for (Model_Base loDetail : poDetail) {
            String lcHsSerial = (String)loDetail.getValue("cHsSerial");
            String lsStockIDx = (String)loDetail.getValue("sStockIDx");
            //int lnQuantity = (int)loDetail.getValue("nQuantity");
            int lEntryNox = (int)loDetail.getValue("nEntryNox");
            
            //int lnQuantity = ((BigDecimal) loDetail.getValue("nQuantity")).intValue();
            //int lEntryNox = ((BigDecimal) loDetail.getValue("nEntryNox")).intValue();
            Object value = loDetail.getValue("nQuantity");
            int lnQuantity = 0;

            if (value instanceof BigDecimal) {
                lnQuantity = ((BigDecimal) value).intValue();
            } else if (value instanceof Integer) {
                lnQuantity = (Integer) value;
            } else if (value != null) {
                lnQuantity = Integer.parseInt(value.toString());
            }

            if(lcHsSerial.equals("1")){
                for (Model_Base loSerial : poSerial) {
                    String lsSerialID = (String)loSerial.getValue("sSerialID");
                    int lnEntryNoy = (int)loSerial.getValue("nEntryNox");
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

        poMaster.setValue("cTranStat", "2");
        
        String lsSQL = "UPDATE GN_PO_Receiving_Master" + 
                      " SET cTranStat = '2'" + 
                      " WHERE sTransNox = " + SQLUtil.toSQL(poMaster.getValue("sTransNox"));
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
        if(!loRS.next()){
            int lnRecord = (int)poMaster.getValue("nEntryNox"); 
            if(lnRecord > 0){
                loJson.put("result", "error");
                loJson.put("message", "No detail record found");
                return loJson;
            }
            else{
                loJson.put("result", "success");
                return loJson;
            }
        }

        Model_Base loDetail;
        
        //prepare record detail model
        loDetail = new Model_Base();
        loJson = loDetail.initTransaction(poGRider, psTableDetail, poDetailMeta);
        if(!"success".equals((String) loJson.get("result"))){
            return loJson;
        }

        //transfer record to detail model
        String lsFilter;
        lsFilter = "sTransNox = " + SQLUtil.toSQL(fsTransNox) + 
              " AND nEntryNox = " + SQLUtil.toSQL(loRS.getInt("nEntryNox")); 
        loJson = loDetail.loadTransaction(loRS, lsFilter);
        if(!"success".equals((String) loJson.get("result"))){
            return loJson;
        }
        poDetail.add(loDetail);

        while(loRS.next()){
            loDetail = new Model_Base();
            loJson = loDetail.initTransaction(poGRider, psTableDetail, poDetailMeta);
            if(!"success".equals((String) loJson.get("result"))){
                return loJson;
            }

            loJson = loDetail.loadTransaction(loRS, lsFilter);
            if(!"success".equals((String) loJson.get("result"))){
                return loJson;
            }
            poDetail.add(loDetail);
            
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
               " ORDER BY sTransNox, nEntryNox"; 
        loRS = poGRider.executeQuery(lsSQL);
        if(!loRS.next()){
            if(!"success".equals((String) loJson.get("result"))){
                return loJson;
            }
        }

        Model_Base loDetail;
        
        //prepare serial detail model
        loDetail = new Model_Base();
        loJson = loDetail.initTransaction(poGRider, psTableSerial, poSerialMeta);
        if(!"success".equals((String) loJson.get("result"))){
            return loJson;
        }

        //transfer record to detail model
        poSerial.add(loDetail);
        String lsFilter;
        lsFilter = "sTransNox = " + SQLUtil.toSQL(fsTransNox) + 
              " AND nEntryNox = " + SQLUtil.toSQL(loRS.getInt("nEntryNox")) + 
              " AND sSerialID = " + SQLUtil.toSQL(loRS.getInt("sSerialID"));
                
        loJson = poSerial.get(0).loadTransaction(loRS, lsFilter);
        if(!"success".equals((String) loJson.get("result"))){
            return loJson;
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
        
//        Gson gson = new Gson();
//        String json = gson.toJson(loBase);
//        System.out.println(json);
        
        System.out.println("Showing detail data: +++++++++++++++++++++");
        for (Model_Base loDetail : poDetail) {
            for (Map.Entry<String, Object> entry : loDetail.getValue().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                System.out.println("Column: " + key + ", Value: " + value);
            }
        }
        
        System.out.println("Showing serial data: +++++++++++++++++++++");
        for (Model_Base loDetail : poSerial) {
            for (Map.Entry<String, Object> entry : loDetail.getValue().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                System.out.println("Column: " + key + ", Value: " + value);
            }
        }
    }
}
