/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ph.com.guanzongroup.revmig.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.constant.EditMode;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.revmig.lib.RevMigUtil;
import ph.com.guanzongroup.revmig.model.pojo.POJO_Base;

/**
 *
 * @author Administrator
 */
public class Model_Base {
    private String psTableNme;
    private String psFilter = "";
    private GRider poGRider;
    
    private POJO_Base poData;
    private POJO_Base poOldData;
    private List<Map<String, Object>> poMeta;
    private int pnEditMode;
    private boolean pbInitTrans = false;

    public int getEditMode(){
        return pnEditMode;
    }
    
    public void setValue(String fsKey, Object foValue){
        poData.setValue(fsKey, foValue);
    }

    public JSONObject setValue(ResultSet foRS, String fsExclude) throws SQLException {
        return poData.setValue(foRS, fsExclude);
    }
    
    public Object getValue(String fsKey){
        return poData.getValue(fsKey);
    }

    public Map<String, Object> getValue(){
        return poData.getValue();
    }
    
    public List<Map<String, Object>> getMeta(){
        return poMeta;
    }
    
    public JSONObject initTransaction(GRider foGRider, String fsTableNme){
        JSONObject loJson = new JSONObject();

        poGRider = foGRider;
        psTableNme = fsTableNme;
        poData = new POJO_Base(fsTableNme);
        poMeta = poData.getMeta();
        pnEditMode = EditMode.UNKNOWN;
        
        pbInitTrans = true;
        
        loJson.put("result", "success");
        return loJson;
    }

    public JSONObject initTransaction(GRider foGRider, String fsTableNme, List<Map<String, Object>> foMeta){
        JSONObject loJson = new JSONObject();

        poGRider = foGRider;
        psTableNme = fsTableNme;
        poData = new POJO_Base(fsTableNme, foMeta);
        poMeta = poData.getMeta();
        pnEditMode = EditMode.UNKNOWN;

        pbInitTrans = true;
        
        loJson.put("result", "success");
        return loJson;
    }

    public JSONObject newTransaction() throws SQLException{
        JSONObject loJson = new JSONObject();
        if(!pbInitTrans){
            loJson.put("result", "error");
            loJson.put("message", psTableNme + " -> Object Not Initialized!");
            return loJson;
        }
        
        //initialize the value to the current poData Object
        poData.initValue();

        //Create a mirror of the current data poData Object
        poOldData = new POJO_Base(psTableNme, poMeta);
        poOldData.initValue();
        loJson = poOldData.setValue(poData.getValue());

        pnEditMode = EditMode.ADDNEW;
        
        loJson.put("result", "success");
        return loJson;
    }    

    public JSONObject loadTransaction(ResultSet foRS, String fsFilter) throws SQLException{
        JSONObject loJson;

        //Save the value to the current poData Object
        loJson = poData.setValue(foRS, "");
        if(!"success".equals((String) loJson.get("result"))){
            return loJson;
        }
        
        //Create a mirror of the current data poData Object
        poOldData = new POJO_Base(psTableNme, poMeta);
        poOldData.initValue();
        loJson = poOldData.setValue(poData.getValue());
        if(!"success".equals((String) loJson.get("result"))){
            return loJson;
        }
        
        psFilter = fsFilter;
        pnEditMode = EditMode.READY;
        
        loJson.put("result", "success");
        return loJson;
    }
    
    public JSONObject saveTransaction() throws SQLException{
        JSONObject loJson = new JSONObject();
        if(!pbInitTrans){
            loJson.put("result", "error");
            loJson.put("message", psTableNme + " -> Object Not Initialized!");
            return loJson;
        }

        if(pnEditMode == EditMode.UNKNOWN){
            loJson.put("result", "error");
            loJson.put("message", psTableNme + " -> Object Status is unknown!");
            return loJson;
        }
        
        Map<String, Object> result;
        String lsSQL;

        System.out.println(poData.getValue().toString());
        System.out.println("poData.getValue().toString()++++++++++++++++++++++++++++");
        if(pnEditMode == EditMode.ADDNEW){
            result = RevMigUtil.createInsertSQL(poData.getValue(), psTableNme, "");
            System.out.println(result.get("json"));
        }
        else{
            result = RevMigUtil.createUpdateSQL(poData.getValue(), poOldData.getValue(), psTableNme, psFilter, "");
            System.out.println(result.get("json"));
        }

        System.out.println("Check if create is successfull!");
        if(!"success".equals((String) result.get("result"))){
            loJson.put("result", (String) result.get("result"));
            loJson.put("message", (String) result.get("message"));
            return loJson;
        }

        lsSQL = (String) result.get("sql");
        
        if(!lsSQL.isEmpty()){
            System.out.println(lsSQL);
            String lsBranchCD = poData.getValue("sTransNox").toString().substring(0, 4);
            poGRider.executeQuery(lsSQL, poData.getTableName(), lsBranchCD, "");
        }

        //set the value of the the poOldData to the value of poData
        poOldData.setValue(poData.getValue());

        pnEditMode = EditMode.READY;
        
        loJson.put("result", "success");
        return loJson;
    }
}
