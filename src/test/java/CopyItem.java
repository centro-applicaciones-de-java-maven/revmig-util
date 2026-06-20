import java.sql.ResultSet;
import java.sql.SQLException;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.SQLUtil;

public class CopyItem {
    final static String INDUSTRY = "09";
    static GRider poGRider;
    
    public static void main (String [] args) throws SQLException, GuanzonException{
        //Set important path configuration for this utility
        String path;
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            path = "D:/GGC_Maven_Systems";
        }
        else{
            path = "/srv/GGC_Maven_Systems";
        }
        
        System.setProperty("sys.default.path.temp", path + "/temp");
        System.setProperty("sys.default.path.config", path);
        System.setProperty("sys.default.path.metadata", path + "/config/metadata/");
        
        
        String lsProdctID = "gRider";
        String lsUserIDxx = "08220326";
        
        poGRider = null;
        
        poGRider = new GRider(lsProdctID);
            
        if (!poGRider.loadUser(lsProdctID, lsUserIDxx)){
            System.err.println(poGRider.getMessage() + poGRider.getMessage());
            System.exit(1);
        }
        
        String lsSQL = "SELECT sStockIDx" +
                        " FROM GCASys_DBF.Inventory" +
                        " WHERE `sBarCodex` IN ('25-GK01-002069', '25-GK01-001770', '25-GK01-002016', '25-GK01-002120')";
        
        checkItem(poGRider.executeQuery(lsSQL));
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
			", IFNULL(sModelIDx, '') sModelIDx" +	
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
}
