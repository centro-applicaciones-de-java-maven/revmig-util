/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ph.com.guanzongroup.revmig.model.pojo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.revmig.lib.RevMigUtil;

/**
 *
 * @author Administrator
 */
public class POJO_Base {
    private final String psTableNme; 
    private final List<Map<String, Object>> poMeta;

    private Map<String, Object> poData;

    public POJO_Base(String fsTableNme){
        psTableNme = fsTableNme;
        poMeta = RevMigUtil.loadMeta(System.getProperty("sys.default.path.metadata") + "/" + fsTableNme);
        poData = new LinkedHashMap<>(); 
    }
    
    public POJO_Base(String fsTableNme, List<Map<String, Object>> foMeta){
        psTableNme = fsTableNme;
        poMeta = foMeta;
        
        poData = new LinkedHashMap<>(); 
    }

    public void initValue() throws SQLException{
        poData = RevMigUtil.initRowSet(poMeta);
    }

    public Object getValue(String fsKey){
        return poData.get(fsKey);
    }

    public void setValue(String fsKey, Object foValue){
        poData.put(fsKey, foValue);
    }
    
    public List<Map<String, Object>> getMeta(){
        return poMeta;
    }

    public JSONObject setValue(ResultSet foRS) throws SQLException {
        Map<String, Object> loData = RevMigUtil.row2Map(foRS);
        JSONObject loJson = new JSONObject();

        if (poData == null || loData == null || !loData.keySet().equals(poData.keySet())) {
            loJson.put("result", "error");
            loJson.put("message", "Key mismatch between expected and received data.");
            return loJson;
        }

        poData = loData;
        loJson.put("result", "success");
        return loJson;
    }

    public JSONObject setValue(Map<String, Object> foData) throws SQLException {
        JSONObject loJson = new JSONObject();

        if (poData == null || foData == null || !foData.keySet().equals(poData.keySet())) {
            loJson.put("result", "error");
            loJson.put("message", "Key mismatch between expected and received data.");
            return loJson;
        }

        // Clone the input map to avoid shared reference
        poData = new LinkedHashMap<>(foData);

        loJson.put("result", "success");
        return loJson;
    }
    
    public Map<String, Object> getValue(){
        return poData;
    }
    
    public String getTableName(){
        return psTableNme;
    }
}
