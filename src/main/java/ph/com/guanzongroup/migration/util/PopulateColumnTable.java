/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ph.com.guanzongroup.migration.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.LogWrapper;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.json.simple.JSONObject;
import ph.com.guanzongroup.revmig.lib.RevMigUtil;

/**
 *
 * @author User
 */
public class PopulateColumnTable {
    static GRider poGRider;

    public static void main(String args[]) throws SQLException{
        LogWrapper logwrapr = new LogWrapper("Disbursement Transaction Reverse Migration", "revmig-po.log");

        //Set important path configuration for this utility
        String path;
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            path = "D:/GGC_Maven_Systems";
        }
        else{
            path = "/srv/mac/GGC_Java_Systems";
        }

        System.setProperty("sys.default.path.temp", path + "/temp");
        System.setProperty("sys.default.path.config", path);

        String lsProdctID = "gRider";

        //TODO: temporarily used my user id for testing
        String lsUserIDxx = "M001111122";
        //String lsUserIDxx = "M001250012";

        poGRider = null;

        logwrapr.info("Start of Disbursement Transaction Reverse Migration...");

        logwrapr.info("Loading application driver...");
        poGRider = new GRider(lsProdctID);

        logwrapr.info("Loading user...");
        if (!poGRider.loadUser(lsProdctID, lsUserIDxx)){
            logwrapr.severe(poGRider.getMessage() + poGRider.getMessage());
            System.exit(1);
        }

        poGRider.executeUpdate("USE GCASys_DBF");
        
        
        ResultSet loRSMaster = getTables();

        while(loRSMaster.next()){
            poGRider.beginTrans();
            ResultSet loRSColumns = getColumns(loRSMaster.getString(1));
            saveColumns(poGRider, loRSColumns, loRSMaster.getString(1));
            poGRider.commitTrans();
        }
    }

    private static ResultSet getTables() throws SQLException{
        //assume that 4 means the processing of the Disbursement was completed
        String lsSQL = "SHOW TABLES";

        System.out.println(lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        return loRS;
    }
    

    private static ResultSet getColumns(String lsTableNme) throws SQLException{
        //assume that 4 means the processing of the Disbursement was completed
        String lsSQL = "SELECT * FROM " + lsTableNme + " LIMIT 1";

        System.out.println(lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        return loRS;
    }
    
    public static boolean saveColumns(GRider foGRider, ResultSet foRS, String fsTableNme) throws SQLException {
        ResultSetMetaData meta = foRS.getMetaData();
        int lnRow = foRS.getMetaData().getColumnCount();

        String lsSQL = "SELECT" +
                            "  sColumnID" +
                            ", sTableNme" +
                            ", sColumnNm" +
                            ", sColLabel" +
                            ", sColumnDs" +
                            ", sRemarksx" +
                            ", nPosition" +
                            ", nColumnTp" +
                            ", cIsNullxx" +
                            ", nLengthxx" +
                            ", cFixedLen" +
                            ", nPrecisnx" +
                            ", nScalexxx" +
                            ", sFormatxx" +
                            ", sRegTypex" +
                            ", sValueFrm" +
                            ", sValueThr" +
                            ", sValueLst" +
                            ", cRecdStat" +
                            ", sModified" +
                            ", dModified" + 
                      " FROM xxxSysColumn";

        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++) {
            Map<String, Object> column = new LinkedHashMap<>();
            Map<String, Object> columx = new LinkedHashMap<>();

            String lsFilter = " sTableNme = " + SQLUtil.toSQL(fsTableNme) +
                    " AND sColumnNm = " + SQLUtil.toSQL(foRS.getMetaData().getColumnName(lnCtr));
            
            String sql = MiscUtil.addCondition(lsSQL, lsFilter);

            ResultSet oRS = foGRider.executeQuery(sql);
            
            if(!oRS.next()){
                sql = MiscUtil.getNextCode("xxxSysColumn", "sColumnID", false, foGRider.getConnection(), "");
                column.put("sColumnID", sql);  
            } 
            else{
                columx = RevMigUtil.row2Map(oRS, "");
                column = RevMigUtil.row2Map(oRS, "");
            }

            column.put("sTableNme", fsTableNme);
            column.put("sColumnNm", meta.getColumnName(lnCtr));
            column.put("sColLabel", meta.getColumnLabel(lnCtr));
            column.put("nPosition", lnCtr);
            column.put("nColumnTp", meta.getColumnType(lnCtr));
            column.put("cIsNullxx", String.valueOf(meta.isNullable(lnCtr)));
            column.put("nLengthxx", meta.getColumnDisplaySize(lnCtr));
            column.put("nPrecisnx", meta.getPrecision(lnCtr));
            column.put("nScalexxx", meta.getScale(lnCtr));
            column.put("cRecdStat", "1");
            column.put("sModified", "marlon");
            column.put("dModified", foGRider.getServerDate());

            Map<String, Object> loData;
            
            if(columx.isEmpty()){
                loData = RevMigUtil.createInsertSQL(column, "xxxSysColumn", "");
            }
            else
            {
                loData = RevMigUtil.createUpdateSQL(column, columx, "xxxSysColumn", lsFilter, "dModified");
            }
            
            sql = loData.get("sql").toString();
            System.out.println(sql);
            
            if(!sql.isEmpty()){
                poGRider.executeUpdate(sql);
            }
        }
        return true;
    }
}
