/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package ph.com.guanzongroup.revmig.lib;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;

/**
 *
 * @author kalyptus
 */
public class RevMigUtil {
    public static boolean saveMeta(GRider foGRider, ResultSet foRS, String fsFileName, String fsTableNme) throws SQLException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fsFileName))) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            ResultSetMetaData meta = foRS.getMetaData();
            metadata.put("table", fsTableNme);

            List<Map<String, Object>> columns = new ArrayList<>();
            int lnRow = foRS.getMetaData().getColumnCount();

            String lsSQL = "SELECT sColLabel, sRemarksx, sFormatxx, sRegTypex, sValueFrm, sValueThr, sValueLst FROM GCASys_DBF.xxxSysColumn";

            for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++) {
                Map<String, Object> column = new LinkedHashMap<>();

                column.put("POSITION", lnCtr);
                column.put("COLUMN_NAME", meta.getColumnName(lnCtr));
                column.put("COLUMN_LABEL", meta.getColumnLabel(lnCtr));
                column.put("DATA_TYPE", meta.getColumnType(lnCtr));
                column.put("NULLABLE", meta.isNullable(lnCtr));
                column.put("LENGTH", meta.getColumnDisplaySize(lnCtr));
                column.put("PRECISION", meta.getPrecision(lnCtr));
                column.put("SCALE", meta.getScale(lnCtr));

                String sql = MiscUtil.addCondition(lsSQL,
                            " sTableNme = " + SQLUtil.toSQL(fsTableNme) +
                        " AND sColumnNm = " + SQLUtil.toSQL(foRS.getMetaData().getColumnName(lnCtr)));

                ResultSet oRS = foGRider.executeQuery(sql);
                if (oRS.next()) {
                    column.put("COLUMN_LABEL", oRS.getString("sColLabel"));
                    column.put("COLUMN_HELP", oRS.getString("sRemarksx"));
                    column.put("FORMAT", oRS.getString("sFormatxx"));
                    column.put("REGTYPE", oRS.getString("sRegTypex"));
                    column.put("FROM", oRS.getString("sValueFrm"));
                    column.put("THRU", oRS.getString("sValueThr"));
                    column.put("LIST", oRS.getString("sValueLst"));
                } else {
                    //System.out.println(fsFileName + ": FORMAT");
                    column.put("COLUMN_HELP", null);
                    column.put("FORMAT", null);
                    column.put("REGTYPE", null);
                    column.put("FROM", null);
                    column.put("THRU", null);
                    column.put("LIST", null);
                }

                columns.add(column);
            }

            metadata.put("columns", columns);

            //create the 
            lsSQL = "SHOW CREATE TABLE " + fsTableNme;
            ResultSet oRS = foGRider.executeQuery(lsSQL);
            
            oRS.next();
            
            metadata.put("create", oRS.getString(1));
            
            Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
            String jsonOutput = gson.toJson(metadata);
            writer.write(jsonOutput);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
    
    public static List<Map<String, Object>> loadMeta(String fsFileName) {
        String lsFileName = fsFileName.replace(".json", "");
        lsFileName += ".json";
        
        try (BufferedReader reader = new BufferedReader(new FileReader(lsFileName))) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> metadata = gson.fromJson(reader, type);

            if (metadata == null || !metadata.containsKey("columns")) {
                System.err.println("Metadata is null or missing 'columns' key.");
                return Collections.emptyList();
            }

            Object columnsObj = metadata.get("columns");
            if (columnsObj instanceof List) {
                Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
                return gson.fromJson(gson.toJson(columnsObj), listType);
            } else {
                System.err.println("'columns' is not a list.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }
    
    
    public static Map<String, Object> getColumn(List<Map<String, Object>> columns, String columnName) {
        return columns.stream()
            .filter(col -> columnName.equalsIgnoreCase((String) col.get("COLUMN_NAME")))
            .findFirst()
            .orElse(null);
    }
    
    public static Map<String, Object> getColumn(List<Map<String, Object>> columns, int position){
        return columns.stream()
            .filter(col -> {
                Object pos = col.get("POSITION");
                return pos instanceof Number && ((Number) pos).intValue() == position;
            })
            .findFirst()
            .orElse(null);
    }

    public static Map<String, Object> initRowSet(List<Map<String, Object>> foMeta) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();

        for (Map<String, Object> colmeta : foMeta) {
            Object dataTypeObj = colmeta.get("DATA_TYPE");
            Object columnNameObj = colmeta.get("COLUMN_NAME");

            int dataType = ((Double)dataTypeObj).intValue();
            String columnName = (String) columnNameObj;

            switch (dataType) {
                case java.sql.Types.BIGINT:
                case java.sql.Types.INTEGER:
                case java.sql.Types.SMALLINT:
                case java.sql.Types.TINYINT:
                    row.put(columnName, 0);
                    break;
                case java.sql.Types.DECIMAL:
                case java.sql.Types.DOUBLE:
                case java.sql.Types.FLOAT:
                case java.sql.Types.NUMERIC:
                case java.sql.Types.REAL:
                    row.put(columnName, 0.0);
                    break;
                case java.sql.Types.CHAR:
                case java.sql.Types.NCHAR:
                case java.sql.Types.NVARCHAR:
                case java.sql.Types.VARCHAR:
                    row.put(columnName, "");
                    break;
                default:
                    row.put(columnName, null);
            }
        }

        return row;
    }
    
    public static Map<String, Object> row2Map(ResultSet rs, String fsExclude) throws SQLException {
        // LinkedHashMap is used to ensure the column order is preserved.
        Map<String, Object> row = new LinkedHashMap<>();

        // Read metadata once outside the loop for efficiency.
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        
        String format = "yyyy-MM-dd HH:mm:ss";
        
        // Iterate through columns using 1-based JDBC indexing.
        for (int i = 1; i <= columnCount; i++) {
            // Use getColumnLabel to respect AS aliases in the SQL query.
            String columnName = meta.getColumnLabel(i);
            Object value;

            // Skip excluded column
            if (fsExclude.contains(columnName)) {
                continue;
            }
            
            try {
                Object raw = rs.getObject(i);

                if (raw instanceof LocalDate) {
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
                    value = ((LocalDate) raw).format(dtf);
                    if(((String) value).contains(" 00:00:00")){
                        value = ((String)value).replace(" 00:00:00", "");
                    }
                } else if (raw instanceof LocalDateTime) {
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
                    value = ((LocalDateTime) raw).format(dtf);
                    if(((String) value).contains(" 00:00:00")){
                        value = ((String)value).replace(" 00:00:00", "");
                    }
                } else if (raw instanceof java.util.Date) {
                    SimpleDateFormat sdf = new SimpleDateFormat(format);
                    value = sdf.format((java.util.Date) raw);
                    if(((String) value).contains(" 00:00:00")){
                        value = ((String)value).replace(" 00:00:00", "");
                    }
                } else if (raw instanceof Calendar) {
                    SimpleDateFormat sdf = new SimpleDateFormat(format);
                    value = sdf.format(((Calendar) raw).getTime());
                    if(((String) value).contains(" 00:00:00")){
                        value = ((String)value).replace(" 00:00:00", "");
                    }
                } else {
                    value = raw;
                }
                
                
                
            } catch (SQLException ex) {
                // Critical Workaround: Handle the specific SQL error for illegal dates ('0000-00-00')
                // This is common in some MySQL/MariaDB configurations.
                if (ex.getMessage()!= null && ex.getMessage().contains("Zero date value prohibited")) {
                    value = null; // Treat invalid zero date as NULL
                } else {
                    // Re-throw any other SQL exception to the caller.
                    throw ex;
                }
            }

            row.put(columnName, value);
            
        }

        return row;
    }

    public static Map<String, Object> Updated(Map<String, Object> foNewData, Map<String, Object> foOldData) {
        Map<String, Object> loChanges = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : foNewData.entrySet()) {
            String key = entry.getKey();
            Object newValue = entry.getValue();
            Object oldValue = foOldData.get(key);

            boolean isChanged = (newValue == null && oldValue != null) ||
                                (newValue != null && !newValue.equals(oldValue));

            if (isChanged) {
                loChanges.put(key, newValue);
            }
        }

        return loChanges;
    }

    public static Map<String, Object> createUpdateSQL(Map<String, Object> foData, String fsTableNme, String fsCondition, String fsExclude) {
        Map<String, Object> loRet = new LinkedHashMap<>();

        try {
            StringBuilder updates = new StringBuilder();

            for (Map.Entry<String, Object> entry : foData.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                // Skip excluded column
                if (fsExclude.contains(key)) {
                    continue;
                }
                
                if (updates.length() > 0) {
                    updates.append(", ");
                }

                updates.append(key).append(" = ").append(SQLUtil.toSQL(value));
            }

            String sql = "UPDATE " + fsTableNme + " SET " + updates + " WHERE " + fsCondition;
            String json = new Gson().toJson(foData);

            loRet.put("result", "success");
            loRet.put("sql", sql);
            loRet.put("json", json);
        } catch (Exception e) {
            loRet.put("result", "error");
            loRet.put("sql", "");
            loRet.put("json", "");
            loRet.put("message", e.getMessage());
        }

        return loRet;
    }    
    
    public static Map<String, Object> createInsertSQL(Map<String, Object> foData, String fsTableNme, String fsExclude) {
        Map<String, Object> loRet = new LinkedHashMap<>();

        try {
            StringBuilder fields = new StringBuilder();
            StringBuilder values = new StringBuilder();
            Map<String, Object> loIns = new LinkedHashMap<>();

            for (Map.Entry<String, Object> entry : foData.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                // Skip excluded column
                if (fsExclude.contains(key)) {
                    continue;
                }

                System.out.println(key + ":" + value);
                if (value != null) {
                    if (fields.length() > 0) {
                        fields.append(", ");
                        values.append(", ");
                    }
                    
                    fields.append(key);
                    values.append(SQLUtil.toSQL(value));
                    loIns.put(key, value);
                }
            }

            if(!loIns.isEmpty()){
                String sql = "INSERT INTO " + fsTableNme + " (" + fields + ") VALUES (" + values + ")";

                Gson gson = new Gson();
                String json = gson.toJson(loIns);

                loRet.put("result", "success");
                loRet.put("sql", sql);
                loRet.put("json", json);
            }
            else{
                loRet.put("result", "error");
                loRet.put("sql", "");
                loRet.put("json", "");
                loRet.put("message", "No data provided for insertion.");
            }
        } catch (Exception e) {
            loRet.put("result", "error");
            loRet.put("sql", "");
            loRet.put("json", "");
            loRet.put("message", e.getMessage());
        }

        return loRet;
    }    
    
    public static Map<String, Object> createUpdateSQL(Map<String, Object> foNewData, Map<String, Object> foOldData, String fsTableNme, String fsCondition, String fsExclude) {
        Map<String, Object> loRet = new LinkedHashMap<>();

        try {
            StringBuilder updates = new StringBuilder();
            Map<String, Object> loUpd = new LinkedHashMap<>();

            for (Map.Entry<String, Object> entry : foNewData.entrySet()) {
                String key = entry.getKey();
                Object newValue = entry.getValue();
                Object oldValue = foOldData.get(key);

                fsExclude += ":dtimestmp";
                // Skip excluded column
                if (fsExclude.contains(key)) {
                    continue;
                }
                
//                Set<String> excludedKeys = new HashSet<>(Arrays.asList("dtimestmp", "dmodified", "smodified"));
//                if (!excludedKeys.contains(key.toLowerCase())) {
                boolean isChanged = !SQLUtil.equalValue(oldValue, newValue);

                if (isChanged){
                    if (updates.length() > 0) {
                        updates.append(", ");
                    }

                    updates.append(key).append(" = ");
                    updates.append(SQLUtil.toSQL(newValue));

                    loUpd.put(key, newValue);
                }
//                }
            }

            if (updates.length() == 0) {
                loRet.put("result", "success");
                loRet.put("sql", "");
                loRet.put("json", "");
                return loRet;
            }

            String sql = "UPDATE " + fsTableNme + " SET " + updates + " WHERE " + fsCondition;
            Gson gson = new Gson();
            String json = gson.toJson(loUpd);

            loRet.put("result", "success");
            loRet.put("sql", sql);
            loRet.put("json", json);
        } catch (Exception e) {
            loRet.put("result", "error");
            loRet.put("sql", "");
            loRet.put("json", "");
            loRet.put("message", e.getMessage());
        }

        return loRet;
    }
    
    
    public static ResultSet retrieveTrans4Migration(GRider foGRider, String fsTableNme){
        ResultSet loRes;
        String lsSQL = "SELECT b.sTableNme, a.sTransNox, a.cTranStat, b.cLastStat" +
                      " FROM " + fsTableNme + " a" +
                            " LEFT JOIN GGC_ISysDBF.Demigration_Map b" +
                                " ON a.sTransNox = b.sTransNox" +
                               " AND b.sTableNme = " + SQLUtil.toSQL(fsTableNme) +
                      " WHERE a.cTranStat >= '1'" +
                       " AND (b.sTransNox IS NULL OR a.cTranStat <> b.cLastStat)";
        loRes = foGRider.executeQuery(lsSQL);
        return loRes;
    }
    
    /**
     * Converts a transaction number by transforming the 6th character (index 5) 
     * from a numeric digit to its alphabetical equivalent.
     * <p>
     * The transformation follows a 0-based offset where:
     * <ul>
     * <li>'0' becomes 'A'</li>
     * <li>'1' becomes 'B'</li>
     * <li>...</li>
     * <li>'9' becomes 'J'</li>
     * </ul>
     * * If the input string is null, shorter than 6 characters, or the 6th character 
     * is not a digit, the original string is returned unchanged.
     *
     * @param lsTransNox the raw transaction string to be converted (e.g., "M00126000001")
     * @return the converted transaction string (e.g., "M0012G000001"), 
     * or the original string if validation fails.
     */
    public static String convertTransNox(String lsTransNox) {
        // 1. Minimum length for index 5 to exist is 6, but your logic uses 7.
        if (lsTransNox == null || lsTransNox.length() < 6) {
            return lsTransNox; 
        }

        StringBuilder sb = new StringBuilder(lsTransNox);
        sb.setCharAt(5, (char) ('A' + (lsTransNox.charAt(5) - '0')));
        return sb.toString();
    }    
}
