/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ph.com.guanzongroup.revmig.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.constant.EditMode;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.revmig.model.pojo.POJO_Base;

/**
 *
 * @author User
 */
public class List_Model_Base {
    private String psTableNme;
    private GRider poGRider;
    
    private List<POJO_Base> poaData;
    private List<POJO_Base> poaOldData;
    private List<Map<String, Object>> poMeta;
    private int pnEditMode;
    private boolean pbInitTrans = false;
    private String[] pasPrimeKey;

    public JSONObject initTransaction(GRider foGRider, String fsTableNme, String[] fasKeysx) {
        JSONObject loJson = new JSONObject();

        // 1. Validation: Ensure required objects are provided
        if (foGRider == null || fsTableNme == null || fsTableNme.isEmpty()) {
            loJson.put("result", "error");
            loJson.put("message", "Invalid GRider instance or Table Name.");
            return loJson;
        }

        try {
            // 2. Assign Global/Field Variables
            this.poGRider = foGRider;
            this.psTableNme = fsTableNme;
            this.pasPrimeKey = fasKeysx;

            // 3. Initialize Data Model & Metadata
            POJO_Base loData = new POJO_Base(fsTableNme);
            this.poMeta = loData.getMeta();

            if (this.poMeta == null) {
                throw new Exception("Failed to retrieve table metadata.");
            }

            // 4. Reset State
            this.poaData = new ArrayList<>();
            this.poaOldData = new ArrayList<>();
            this.pnEditMode = EditMode.UNKNOWN;
            this.pbInitTrans = true;

            // 5. Success Response
            loJson.put("result", "success");

        } catch (Exception e) {
            // 6. Error Handling
            this.pbInitTrans = false;
            loJson.put("result", "error");
            loJson.put("message", e.getMessage());
        }

        return loJson;
    }
    
    public JSONObject initTransaction(
        GRider foGRider, 
        String fsTableNme, 
        List<Map<String, Object>> foMeta, 
        String[] fasKeysx) {
        
        JSONObject loJson = new JSONObject();

        // 1. Critical Validation
        if (foGRider == null || fsTableNme == null || foMeta == null) {
            loJson.put("result", "error");
            loJson.put("message", "One or more required parameters (GRider, Table Name, or Metadata) are null.");
            return loJson;
        }

        // 2. State Assignment
        this.poGRider = foGRider;
        this.psTableNme = fsTableNme;
        this.pasPrimeKey = (fasKeysx != null) ? fasKeysx : new String[0]; // Avoid null for primary keys
        this.poMeta = foMeta;

        // 3. Reset Data Containers
        // It's safer to clear the list if it exists, or create a new one to avoid reference issues
        this.poaData = new ArrayList<>();
        this.poaOldData = new ArrayList<>();

        // 4. Mode Synchronization
        this.pnEditMode = EditMode.ADDNEW;
        this.pbInitTrans = true;

        // 5. Success Result
        loJson.put("result", "success");
        return loJson;
    }

    public JSONObject addDetail(ResultSet foRS) throws SQLException, CloneNotSupportedException{
        JSONObject loJson = new JSONObject();
        POJO_Base loData;

        if(!poaData.isEmpty()){
            loJson.put("result", "error");
            loJson.put("message", "Row is not empty!");
        }
        
        foRS.beforeFirst();
        
        while(foRS.next()){
            loData = new POJO_Base(psTableNme, poMeta);

            //Save the value to the current poData Object
            loJson = loData.setValue(foRS, "");
            if(!"success".equals((String) loJson.get("result"))){
                return loJson;
            }
            
            poaData.add(loData);
            poaOldData.add(loData.clone());
        }
        
        this.pnEditMode = EditMode.READY;
        
        loJson.put("result", "success");
        return loJson;
    }
    
    public JSONObject addDetail() throws SQLException{
        JSONObject loJson = new JSONObject();
        POJO_Base loData;
        loData = new POJO_Base(psTableNme, poMeta);
        //Save the value to the current poData Object
        loData.initValue();

        if(poaData.isEmpty()){
            poaData.add(loData);
        }
        else{
            String lsData = (String) poaData.get(poaData.size() - 1).getValue(pasPrimeKey[pasPrimeKey.length -1]);
            if(lsData.isEmpty()){
                loJson.put("result", "error");
                loJson.put("message", "Last row is empty!");
            }
            poaData.add(loData);
        }

        loJson.put("result", "success");
        return loJson;
    }
    
}
