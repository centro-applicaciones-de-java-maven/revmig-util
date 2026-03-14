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
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.SQLUtil;
import org.json.simple.JSONObject;

/**
 * The {@code RevMigAPClientTrans} class handles Accounts Payable (AP) transactions
 * such as purchases, purchase returns, replacements, payment issues, credit memos,
 * and debit memos. It interacts with the AP Client Master and Ledger tables
 * to record and update balances and ledger entries.
 *
 * <p>Critical operations include:
 * <ul>
 *   <li>Validating client existence in {@code AP_Client_Master}</li>
 *   <li>Inserting or updating ledger entries in {@code AP_Client_Ledger}</li>
 *   <li>Updating balances in {@code AP_Client_Master}</li>
 *   <li>Handling reversal transactions safely</li>
 * </ul>
 *
 * <p>Exceptions thrown:
 * <ul>
 *   <li>{@link SQLException} if database operations fail</li>
 *   <li>{@link GuanzonException} for business rule violations</li>
 * </ul>
 *
 * @author kalyptus
 */
public class RevMigAPClientTrans {
    private final GRider instance;
    private String psClientID;
    private final String psBranchCD;
    private String psSourceCD;
    private String psSourceNo;
    private Date pdTransact;
    private double pnAmountIn;
    private double pnAmountOt;
    private boolean pbIsRevrse;
    public final static String PURCHASE_RECEIVING = "GNDA";       
    public final static String PURCHASE_RETURN = "POPr";          
    public final static String PURCHASE_REPLACEMENT = "GNPp";     
    public final static String DISBURSEMENT = "GNPy";             
    public final static String CREDIT_MEMO = "APAd";
    public final static String DEBIT_MEMO = "APAd";
    public final static String AP_ADJUSTMENT = "APAd";
            
    /**
     * Constructs an APTransaction handler.
     *
     * @param fgRider   GRiderCAS instance for DB operations
     * @param fsBranchCD Branch code
     */
    public RevMigAPClientTrans(GRider fgRider, String fsBranchCD){
        instance = fgRider;
        psBranchCD = fsBranchCD;
    }
    
    /**
     * Records a Purchase transaction.
     *
     * @param fsClientID Client ID
     * @param fsSourceNo Source Number
     * @param fdTransact Transaction Date
     * @param fnAmountxx Amount
     * @param fbIsRevrse Flag for reversal
     * @return JSON result of transaction
     * @throws SQLException if DB fails
     * @throws GuanzonException if business rules fail
     */
    public JSONObject Purchase(String fsClientID, String fsSourceNo, Date fdTransact, Double fnAmountxx, boolean fbIsRevrse) throws SQLException, GuanzonException{
        psSourceCD = PURCHASE_RECEIVING;
        psClientID = fsClientID;
        psSourceNo = fsSourceNo;
        pdTransact = fdTransact;       
        pnAmountIn = fnAmountxx;
        pnAmountOt = 0;
        pbIsRevrse = fbIsRevrse;
        return saveTransaction();
    }
    
    /**
     * Records a Purchase Return transaction.
     *
     * @param fsClientID Client ID
     * @param fsSourceNo Source Number
     * @param fdTransact Transaction Date
     * @param fnAmountxx Amount
     * @param fbIsRevrse Flag for reversal
     * @return JSON result of transaction
     * @throws SQLException if DB fails
     * @throws GuanzonException if business rules fail
     */
    public JSONObject PurchaseReturn(String fsClientID, String fsSourceNo, Date fdTransact, Double fnAmountxx, boolean fbIsRevrse) throws SQLException, GuanzonException{
        psSourceCD = PURCHASE_RETURN;
        psClientID = fsClientID;
        psSourceNo = fsSourceNo;
        pdTransact = fdTransact;       
        pnAmountIn = 0;
        pnAmountOt = fnAmountxx;
        pbIsRevrse = fbIsRevrse;
        return saveTransaction();
    }
    
    /**
     * Records a Purchase Replacement transaction.
     *
     * @param fsClientID Client ID
     * @param fsSourceNo Source Number
     * @param fdTransact Transaction Date
     * @param fnAmountxx Amount
     * @param fbIsRevrse Flag for reversal
     * @return JSON result of transaction
     * @throws SQLException if DB fails
     * @throws GuanzonException if business rules fail
     */
    public JSONObject PurchaseReplacement(String fsClientID, String fsSourceNo, Date fdTransact, Double fnAmountxx, boolean fbIsRevrse) throws SQLException, GuanzonException{
        psSourceCD = PURCHASE_REPLACEMENT;
        psClientID = fsClientID;
        psSourceNo = fsSourceNo;
        pdTransact = fdTransact;       
        pnAmountIn = fnAmountxx;
        pnAmountOt = 0;
        pbIsRevrse = fbIsRevrse;
        return saveTransaction();
    }
    
    /**
     * Records a Payment Issuance transaction.
     *
     * @param fsClientID Client ID
     * @param fsSourceNo Source Number
     * @param fdTransact Transaction Date
     * @param fnAmountxx Amount
     * @param fbIsRevrse Flag for reversal
     * @return JSON result of transaction
     * @throws SQLException if DB fails
     * @throws GuanzonException if business rules fail
     */
    public JSONObject PaymentIssue(String fsClientID, String fsSourceNo, Date fdTransact, Double fnAmountxx, boolean fbIsRevrse) throws SQLException, GuanzonException{
        psSourceCD = DISBURSEMENT;
        psClientID = fsClientID;
        psSourceNo = fsSourceNo;
        pdTransact = fdTransact;       
        pnAmountIn = 0;
        pnAmountOt = fnAmountxx;
        pbIsRevrse = fbIsRevrse;
        return saveTransaction();
    }
    
    /**
     * Records a Credit Memo transaction.
     *
     * @param fsClientID Client ID
     * @param fsSourceNo Source Number
     * @param fdTransact Transaction Date
     * @param fnAmountxx Amount
     * @param fbIsRevrse Flag for reversal
     * @return JSON result of transaction
     * @throws SQLException if DB fails
     * @throws GuanzonException if business rules fail
     */
    public JSONObject CreditMemo(String fsClientID, String fsSourceNo, Date fdTransact, Double fnAmountxx, boolean fbIsRevrse) throws SQLException, GuanzonException{
        psSourceCD = CREDIT_MEMO;
        psClientID = fsClientID;
        psSourceNo = fsSourceNo;
        pdTransact = fdTransact;       
        pnAmountIn = fnAmountxx;
        pnAmountOt = 0;
        pbIsRevrse = fbIsRevrse;
        return saveTransaction();
    }
    
    /**
     * Records a Debit Memo transaction.
     *
     * @param fsClientID Client ID
     * @param fsSourceNo Source Number
     * @param fdTransact Transaction Date
     * @param fnAmountxx Amount
     * @param fbIsRevrse Flag for reversal
     * @return JSON result of transaction
     * @throws SQLException if DB fails
     * @throws GuanzonException if business rules fail
     */
    public JSONObject DebitMemo(String fsClientID, String fsSourceNo, Date fdTransact, Double fnAmountxx, boolean fbIsRevrse) throws SQLException, GuanzonException{
        psSourceCD = DEBIT_MEMO;
        psClientID = fsClientID;
        psSourceNo = fsSourceNo;
        pnAmountIn = 0;
        pnAmountOt = fnAmountxx;
        pbIsRevrse = fbIsRevrse;
        return saveTransaction();
    }
    
    /**
     * Saves the transaction into AP_Client_Master and AP_Client_Ledger.
     *
     * <p>Critical logic:
     * <ul>
     *   <li>Validates client existence in AP_Client_Master</li>
     *   <li>Checks if ledger entry exists (important for reversals)</li>
     *   <li>Handles reversal vs new entry logic</li>
     *   <li>Updates balances accordingly</li>
     * </ul>
     *
     * @return JSON result object with "success" or "failed"
     * @throws SQLException if DB fails
     * @throws GuanzonException if business rules fail
     */
    private JSONObject saveTransaction() throws SQLException, GuanzonException{
        JSONObject loRes = new JSONObject();
        
        String lsSQL;
        
        lsSQL = "SELECT" 
                + "  sClientID"
                + ", nABalance"
                + ", nLedgerNo"
              + " FROM AP_Client_Master"
              + " WHERE sClientID = " + SQLUtil.toSQL(psClientID); 

            ResultSet loRSM = instance.executeQuery(lsSQL);
            System.out.println(lsSQL);

            if (!loRSM.next()) {
                loRes.put("result", "failed");
                loRes.put("message", "Client " + psClientID + " does not exist!");
                return loRes;
            }        

            long lnEntryNox = Long.parseLong(loRSM.getString("nLedgerNo")) ;
            
            lsSQL = "SELECT *" 
                 + " FROM AP_Client_Ledger"  
                 + " WHERE sClientID = " + SQLUtil.toSQL(psClientID) 
                   + " AND sSourceCD = " + SQLUtil.toSQL(psSourceCD)
                   + " AND sSourceNo = " + SQLUtil.toSQL(psSourceNo);  
            ResultSet loRS = instance.executeQuery(lsSQL);
            
            if(!loRS.next()){
                if(pbIsRevrse){
                    loRes.put("result", "failed");
                    loRes.put("message", "Ledger does not exist!");
                    return loRes;
                }
            }

            String lsSQLx;
            if(pbIsRevrse){
                lsSQL = "DELETE FROM AP_Client_Ledger"   
                 + " WHERE sClientID = " + SQLUtil.toSQL(psClientID) 
                   + " AND sSourceCD = " + SQLUtil.toSQL(psSourceCD)
                   + " AND sSourceNo = " + SQLUtil.toSQL(psSourceNo);  

                lsSQLx = "UPDATE AP_Client_Master" + 
                        " SET nABalance = nABalance - " + SQLUtil.toSQL(pnAmountIn - pnAmountOt) 
                      + " WHERE sClientID = " + SQLUtil.toSQL(psClientID); 
                        
            }
            else{
                
                String lsEntryNox = String.format("%06d", lnEntryNox + 1);
                
                lsSQL = "INSERT INTO AP_Client_Ledger" 
                    +  " SET sClientID = " + SQLUtil.toSQL(psClientID) 
                        + ", nLedgerNo = " + SQLUtil.toSQL(lsEntryNox)
                        + ", dTransact = " + SQLUtil.toSQL(pdTransact) 
                        + ", sSourceCd = "+ SQLUtil.toSQL(psSourceCD)
                        + ", sSourceNo = " + SQLUtil.toSQL(psSourceNo)
                        + ", nAmountIn = " + SQLUtil.toSQL(pnAmountIn) 
                        + ", nAmountOt = " + SQLUtil.toSQL(pnAmountOt) 
                        + ", dModified = " + SQLUtil.toSQL(instance.getServerDate());

                lsSQLx = "UPDATE AP_Client_Master"   
                      + " SET nABalance = nABalance + " + SQLUtil.toSQL(pnAmountIn - pnAmountOt) 
                         + ", nLedgerNo = " + SQLUtil.toSQL(lsEntryNox)
                      + " WHERE sClientID = " + SQLUtil.toSQL(psClientID); 
            }

            instance.executeQuery(lsSQL, "AP_Client_Ledger", psBranchCD, "");
            instance.executeQuery(lsSQLx, "AP_Client_Master", psBranchCD, "");

            loRes.put("result", "success");
            return loRes;
    }
}