package ph.com.guanzongroup.revmig.lib;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.guanzon.appdriver.base.CommonUtils;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.EditMode;
import org.json.simple.JSONObject;

/**
 * RevMigBankAccountTrans
 * <p>
 * This class encapsulates the logic for handling bank account transactions such as
 * deposits (cash and check), withdrawals, disbursements, debit/credit memos, and
 * clearing of checks. Each transaction is validated, recorded in the ledger, and
 * reflected in the master account balances to ensure consistency and auditability.
 * </p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Supports multiple transaction types: cash deposit, check deposit, cash withdrawal,
 *       check disbursement, debit memo, credit memo, and clearing checks.</li>
 *   <li>Ensures proper validation of initialization and update modes before processing.</li>
 *   <li>Updates both ledger and master tables to maintain traceability and balance integrity.</li>
 *   <li>Provides rollback capability via {@link #DeleteTransaction()}.</li>
 *   <li>Allows updating of reference numbers (serial and check numbers) for audit purposes.</li>
 * </ul>
 *
 * <h2>Usage Notes</h2>
 * <ul>
 *   <li>Transactions must be initialized via {@link #InitTransaction()} before use.</li>
 *   <li>Only {@link EditMode#ADDNEW} and {@link EditMode#DELETE} are supported for most operations.</li>
 *   <li>Balances are updated atomically in both master and ledger tables.</li>
 * </ul>
 *
 * <h2>Database Tables</h2>
 * <ul>
 *   <li><b>Bank_Account_Master</b>: Stores overall account balances and metadata.</li>
 *   <li><b>Bank_Account_Ledger</b>: Stores detailed transaction records for audit trail.</li>
 * </ul>
 *
 * @author Michael "xurpas" Cuison 
 * @version 1.0
 * @since 2026-01-21
 */
public class RevMigBankAccountTrans {
    public static final String CHECK_DEPOSIT = "CkDp";
    public static final String CHECK_PAYMENT = "CkCy";
    public static final String CASH_DEPOSIT = "CsDp";
    public static final String CASH_WITHDRAWAL = "CsWd";
    public static final String CHECK_DISBURSEMENT = "CkDs";
    public static final String WIRED_DISBURSEMENT = "WrDs"; 
    public static final String EPAY_DISBURSEMENT = "EPDs";
    public static final String CREDIT_MEMO = "BACm";
    public static final String DEBIT_MEMO = "BADm";
    public static final String BANK_INTEREST = "BInt";

    private final String MASTER_TABLE = "Bank_Account";
    private final String DETAIL_TABLE = "Bank_Account_Ledger";
    
    private final GRider poGRider;
    private ResultSet poMaster;
    
    // Identifiers and references
    private String psBnkActID; 
    private String psBranchCd; 
    private String psSourceCd; 
    private String psSourceNo;  
    private String psCheckNox; 
    private String psSerialNo; 
    private String psReferNox;
    private Boolean pbReversex;
    
    // Transaction dates
    private Date pdTransact; 
          
    // Transaction amounts
    private double pnAmountIn; 
    private double pnAmountOt; 
    private double pnATranAmt; 
    private double pnOTranAmt; 

    private Boolean pbInitTran; 
    private JSONObject poJSON;
    
    /**
     * Constructor requires a GRiderCAS driver to execute queries.
     * @param grider application driver
     */
    public RevMigBankAccountTrans(GRider grider){
        poGRider = grider;
    }
    
    /**
     * Initializes the transaction object. Must be called before any transaction.
     * @return JSON result indicating success or error
     */
    public JSONObject InitTransaction(){
        poJSON = new JSONObject();
        
        if (poGRider == null){
            poJSON.put("result", "error");
            poJSON.put("message", "Applicaton driver is not set.");
            return poJSON;
        }
                
        if (psBranchCd == null || psBranchCd.isEmpty()) psBranchCd = poGRider.getBranchCode();
        
        pbInitTran = true;
        poJSON.put("result", "success");
        return poJSON;
    }
    
    /**
     * Initializes references.
     */
    private void initReferences() {
        psReferNox = "";
        psCheckNox = "";
        psSerialNo = "";
    }

    /**
    * Records a cash deposit transaction for a given bank account.
    *
    * @param bankAccountId   the unique identifier of the bank account where the deposit is made.
    *                        This corresponds to the primary key in the master account table.
    * @param sourceNo        the source reference number for this transaction (e.g., receipt number).
    *                        Used to uniquely identify the transaction in the ledger.
    * @param transactionDate the date when the cash deposit occurred. This is used for posting
    *                        and updating the account’s last transaction date.
    * @param amount          the deposit amount to be credited into the account. This value will
    *                        increase both the available and outstanding balances.
     * @param reverse
    *
    * @return a {@link JSONObject} containing the result of the operation:
    *         <ul>
    *           <li>"success" if the transaction was recorded and balances updated correctly</li>
    *           <li>"error" with a descriptive message if validation fails or database update fails</li>
    *         </ul>
    *
    * @throws SQLException       if a database access error occurs during validation,
    *                            ledger insertion, or master account update.
    * @throws GuanzonException   if application-specific errors occur during transaction processing.
    *
    * @see #saveTransaction()
    * @see #validateInitAndMode(int)
    */
    public JSONObject CashDeposit(String bankAccountId, String sourceNo, Date transactionDate, double amount, boolean reverse)
            throws SQLException, GuanzonException {

        initReferences();

        psSourceCd = CASH_DEPOSIT;
        psBnkActID = bankAccountId;
        psSourceNo = sourceNo;
        pdTransact = transactionDate;
        pnAmountIn = amount;
        pnAmountOt = 0.00;
        pbReversex = reverse;

        return saveTransaction();
    }

    /**
     * Records a bank interest transaction in the system.
     * <p>
     * This method initializes transaction references, sets the source code to
     * {@code BANK_INTEREST}, and prepares the transaction details such as
     * bank account ID, source number, transaction date, amount, and reversal flag.
     * It then delegates to {@link #saveTransaction()} to persist the transaction.
     * </p>
     *
     * @param bankAccountId   the unique identifier of the bank account where the interest is applied
     * @param sourceNo        the source reference number associated with the transaction
     * @param transactionDate the date of the transaction
     * @param amount          the interest amount to be recorded; positive values indicate debit,
     *                        while negative values indicate credit
     * @param reverse         {@code true} if the transaction should be reversed; {@code false} otherwise
     *
     * @return a {@link JSONObject} containing the result of the transaction save operation,
     *         including status and any relevant metadata
     *
     * @throws SQLException       if a database access error occurs during transaction processing
     * @throws GuanzonException   if a business rule or application-specific validation fails
     */
    public JSONObject BankInterest(String bankAccountId, String sourceNo, Date transactionDate, double amount, boolean reverse)
            throws SQLException, GuanzonException {

        initReferences();

        psSourceCd = BANK_INTEREST;
        psBnkActID = bankAccountId;
        psSourceNo = sourceNo;
        pdTransact = transactionDate;
        pnAmountIn = amount;
        pnAmountOt = 0.00;
        pbReversex = reverse;

        return saveTransaction();
    }
    
    
    /**
    * Records a check deposit transaction for a given bank account.
    * <p>
    * Unlike cash deposits, check deposits initially increase only the outstanding
    * balance until the check is cleared. The available balance is updated later
    * when the check is successfully cleared via {@link #ClearCheckDeposited}.
    * </p>
    *
    * @param bankAccountId   the unique identifier of the bank account where the check is deposited.
    *                        This corresponds to the primary key in the master account table.
    * @param sourceNo        the source reference number for this transaction (e.g., deposit slip number).
    *                        Used to uniquely identify the transaction in the ledger.
    * @param transactionDate the date when the check deposit occurred. This is used for posting
    *                        and updating the account’s last transaction date.
    * @param amount          the deposit amount to be credited into the account. This value will
    *                        increase the outstanding balance but not the available balance until cleared.
     * @param reverse
    *
    * @return a {@link JSONObject} containing the result of the operation:
    *         <ul>
    *           <li>"success" if the transaction was recorded and balances updated correctly</li>
    *           <li>"error" with a descriptive message if validation fails or database update fails</li>
    *         </ul>
    *
    * @throws SQLException       if a database access error occurs during validation,
    *                            ledger insertion, or master account update.
    * @throws GuanzonException   if application-specific errors occur during transaction processing.
    *
    * @see #saveTransaction()
    * @see #validateInitAndMode(int)
    * @see #ClearCheckDeposited(String, String, Date, double, int)
    */
    public JSONObject CheckDeposit(String bankAccountId, String sourceNo, Date transactionDate, double amount, boolean reverse)
            throws SQLException, GuanzonException {

        initReferences();

        psSourceCd = CHECK_DEPOSIT;
        psBnkActID = bankAccountId;
        psSourceNo = sourceNo;
        pdTransact = transactionDate;
        pnAmountIn = amount;
        pnAmountOt = 0.00;
        pbReversex = reverse;

        return saveTransaction();
    }

    /**
    * Records a cash withdrawal transaction for a given bank account.
    * <p>
    * Cash withdrawals decrease both the available balance and the outstanding balance
    * immediately, since funds are physically withdrawn from the account.
    * </p>
    *
    * @param bankAccountId   the unique identifier of the bank account from which the withdrawal is made.
    *                        This corresponds to the primary key in the master account table.
    * @param sourceNo        the source reference number for this transaction (e.g., withdrawal slip number).
    *                        Used to uniquely identify the transaction in the ledger.
    * @param transactionDate the date when the cash withdrawal occurred. This is used for posting
    *                        and updating the account’s last transaction date.
    * @param amount          the withdrawal amount to be debited from the account. This value will
    *                        decrease both the available and outstanding balances.
     * @param reverse
    *
    * @return a {@link JSONObject} containing the result of the operation:
    *         <ul>
    *           <li>"success" if the transaction was recorded and balances updated correctly</li>
    *           <li>"error" with a descriptive message if validation fails or database update fails</li>
    *         </ul>
    *
    * @throws SQLException       if a database access error occurs during validation,
    *                            ledger insertion, or master account update.
    * @throws GuanzonException   if application-specific errors occur during transaction processing.
    *
    * @see #saveTransaction()
    * @see #validateInitAndMode(int)
    */
    public JSONObject CashWithdrawal(String bankAccountId, String sourceNo, Date transactionDate, double amount, boolean reverse)
            throws SQLException, GuanzonException {

        initReferences();

        psSourceCd = CASH_WITHDRAWAL;
        psBnkActID = bankAccountId;
        psSourceNo = sourceNo;
        pdTransact = transactionDate;
        pnAmountIn = 0.00;
        pnAmountOt = amount;
        pbReversex = reverse;

        return saveTransaction();
    }

    /**
    * Records a check disbursement transaction for a given bank account.
    * <p>
    * Check disbursements represent funds leaving the account via issued checks.
    * The outstanding balance is reduced once the check is cleared, while the
    * available balance is reduced immediately if the check is dated for today
    * or earlier. Future-dated checks remain pending until their transaction
    * date is reached.
    * </p>
    *
    * @param bankAccountId   the unique identifier of the bank account from which the check is issued.
    *                        This corresponds to the primary key in the master account table.
    * @param sourceNo        the source reference number for this transaction (e.g., voucher number).
    *                        Used to uniquely identify the transaction in the ledger.
    * @param transactionDate the date when the check disbursement occurred. This is used for posting
    *                        and updating the account’s last transaction date.
    * @param amount          the disbursement amount to be debited from the account. This value will
    *                        decrease the available balance immediately if the check is current or past-dated,
    *                        and will remain pending if the check is future-dated.
    * @param checkNo         the check number associated with this disbursement. Stored in the master
    *                        account record for reference and audit purposes.
    * @param serialNo        the serial number associated with this disbursement. Typically used for
    *                        branch-level tracking of issued checks.
     * @param reverse
    *
    * @return a {@link JSONObject} containing the result of the operation:
    *         <ul>
    *           <li>"success" if the transaction was recorded and balances updated correctly</li>
    *           <li>"error" with a descriptive message if validation fails or database update fails</li>
    *         </ul>
    *
    * @throws SQLException       if a database access error occurs during validation,
    *                            ledger insertion, or master account update.
    * @throws GuanzonException   if application-specific errors occur during transaction processing.
    *
    * @see #saveTransaction()
    * @see #validateInitAndMode(int)
    * @see #clearChecks()
    */
    public JSONObject CheckDisbursement(String bankAccountId, String sourceNo, Date transactionDate, double amount, String checkNo, String serialNo, boolean reverse)
            throws SQLException, GuanzonException {

        initReferences();

        psSourceCd = CHECK_DISBURSEMENT;
        psBnkActID = bankAccountId;
        psSourceNo = sourceNo;
        pdTransact = transactionDate;
        psCheckNox = checkNo;
        psSerialNo = serialNo;
        pnAmountIn = 0.00;
        pnAmountOt = amount;
        pbReversex = reverse;

        return saveTransaction();
    }
    
    /**
    * Records a wired disbursement transaction for a given bank account.
    * <p>
    * Wired disbursements represent funds transferred electronically out of the account
    * (e.g., bank-to-bank transfers, online payments). This method validates the transaction
    * state and update mode, sets the appropriate transaction details (source code, account ID,
    * source number, transaction date, reference number, and disbursement amount), and then
    * calls {@link #saveTransaction()} to persist the transaction in the ledger and update
    * the master account balances.
    * </p>
    * <p>
    * Since wired disbursements are immediate transfers, the available balance is reduced
    * at the time of posting. No check or serial numbers are associated with this type of
    * transaction, but a reference number is stored for traceability.
    * </p>
    *
    * @param bankAccountId   the unique identifier of the bank account from which the wired disbursement is made.
    *                        This corresponds to the primary key in the master account table.
    * @param sourceNo        the source reference number for this transaction (e.g., voucher or transaction ID).
    *                        Used to uniquely identify the transaction in the ledger.
    * @param transactionDate the date when the wired disbursement occurred. This is used for posting
    *                        and updating the account’s last transaction date.
    * @param amount          the disbursement amount to be debited from the account. This value will
    *                        decrease the available balance immediately.
    * @param referNo         the reference number associated with this wired disbursement. Typically used
    *                        for tracking electronic transfers and ensuring auditability.
     * @param reverse
    *
    * @return a {@link JSONObject} containing the result of the operation:
    *         <ul>
    *           <li>"success" if the transaction was recorded and balances updated correctly</li>
    *           <li>"error" with a descriptive message if validation fails or database update fails</li>
    *         </ul>
    *
    * @throws SQLException       if a database access error occurs during validation,
    *                            ledger insertion, or master account update.
    * @throws GuanzonException   if application-specific errors occur during transaction processing.
    *
    * @see #saveTransaction()
    * @see #validateInitAndMode(int)
    */
    public JSONObject WiredDisbursement(String bankAccountId, String sourceNo, Date transactionDate, double amount, String referNo, boolean reverse)
            throws SQLException, GuanzonException {

        initReferences();

        psSourceCd = WIRED_DISBURSEMENT;
        psBnkActID = bankAccountId;
        psSourceNo = sourceNo;
        pdTransact = transactionDate;
        psReferNox = referNo;
        psCheckNox = "";
        psSerialNo = "";
        pnAmountIn = 0.00;
        pnAmountOt = amount;
        pbReversex = reverse;

        return saveTransaction();
    }
    
    /**
    * Records an electronic payment (e-payment) disbursement transaction for a given bank account.
    * <p>
    * Electronic payment disbursements represent funds transferred digitally out of the account
    * (e.g., online bill payments, mobile banking transfers, or automated electronic debits).
    * This method validates the transaction state and update mode, sets the appropriate transaction
    * details (source code, account ID, source number, transaction date, reference number, and
    * disbursement amount), and then calls {@link #saveTransaction()} to persist the transaction
    * in the ledger and update the master account balances.
    * </p>
    * <p>
    * Since e-payments are immediate transfers, the available balance is reduced at the time of posting.
    * No check or serial numbers are associated with this type of transaction, but a reference number
    * is stored for traceability and audit purposes.
    * </p>
    *
    * @param bankAccountId   the unique identifier of the bank account from which the e-payment disbursement is made.
    *                        This corresponds to the primary key in the master account table.
    * @param sourceNo        the source reference number for this transaction (e.g., voucher number or transaction ID).
    *                        Used to uniquely identify the transaction in the ledger.
    * @param transactionDate the date when the e-payment disbursement occurred. This is used for posting
    *                        and updating the account’s last transaction date.
    * @param amount          the disbursement amount to be debited from the account. This value will
    *                        decrease the available balance immediately.
    * @param referNo         the reference number associated with this e-payment disbursement. Typically used
    *                        for tracking electronic transfers and ensuring auditability.
     * @param reverse
    *
    * @return a {@link JSONObject} containing the result of the operation:
    *         <ul>
    *           <li>"success" if the transaction was recorded and balances updated correctly</li>
    *           <li>"error" with a descriptive message if validation fails or database update fails</li>
    *         </ul>
    *
    * @throws SQLException       if a database access error occurs during validation,
    *                            ledger insertion, or master account update.
    * @throws GuanzonException   if application-specific errors occur during transaction processing.
    *
    * @see #saveTransaction()
    * @see #validateInitAndMode(int)
    */
    public JSONObject EPaymentDisbursement(String bankAccountId, String sourceNo, Date transactionDate, double amount, String referNo, boolean reverse)
            throws SQLException, GuanzonException {

        initReferences();

        psSourceCd = EPAY_DISBURSEMENT;
        psBnkActID = bankAccountId;
        psSourceNo = sourceNo;
        pdTransact = transactionDate;
        psReferNox = referNo;
        psCheckNox = "";
        psSerialNo = "";
        pnAmountIn = 0.00;
        pnAmountOt = amount;
        pbReversex = reverse;

        return saveTransaction();
    }

    /**
    * Records a debit memo transaction for a given bank account.
    * <p>
    * Debit memos represent adjustments or charges applied to the account by the bank
    * (e.g., service fees, penalties). They increase both the available and outstanding
    * balances when treated as incoming credits, depending on business rules.
    * </p>
    *
    * @param bankAccountId   the unique identifier of the bank account where the debit memo is applied.
    *                        This corresponds to the primary key in the master account table.
    * @param sourceNo        the source reference number for this transaction (e.g., memo number).
    *                        Used to uniquely identify the transaction in the ledger.
    * @param transactionDate the date when the debit memo was issued. This is used for posting
    *                        and updating the account’s last transaction date.
    * @param amount          the debit memo amount to be credited into the account. This value will
    *                        increase both the available and outstanding balances.
     * @param reverse
    *
    * @return a {@link JSONObject} containing the result of the operation:
    *         <ul>
    *           <li>"success" if the transaction was recorded and balances updated correctly</li>
    *           <li>"error" with a descriptive message if validation fails or database update fails</li>
    *         </ul>
    *
    * @throws SQLException       if a database access error occurs during validation,
    *                            ledger insertion, or master account update.
    * @throws GuanzonException   if application-specific errors occur during transaction processing.
    *
    * @see #saveTransaction()
    * @see #validateInitAndMode(int)
    */
    public JSONObject DebitMemo(String bankAccountId, String sourceNo, Date transactionDate, double amount, boolean reverse)
            throws SQLException, GuanzonException {

        initReferences();

        psSourceCd = DEBIT_MEMO;
        psBnkActID = bankAccountId;
        psSourceNo = sourceNo;
        pdTransact = transactionDate;
        pnAmountIn = amount;
        pnAmountOt = 0.00;
        pbReversex = reverse;

        return saveTransaction();
    }

    /**
    * Records a credit memo transaction for a given bank account.
    * <p>
    * Credit memos represent adjustments or deductions applied to the account by the bank
    * (e.g., service charges, corrections). They decrease both the available and outstanding
    * balances immediately, since funds are effectively withdrawn from the account.
    * </p>
    *
    * @param bankAccountId   the unique identifier of the bank account where the credit memo is applied.
    *                        This corresponds to the primary key in the master account table.
    * @param sourceNo        the source reference number for this transaction (e.g., memo number).
    *                        Used to uniquely identify the transaction in the ledger.
    * @param transactionDate the date when the credit memo was issued. This is used for posting
    *                        and updating the account’s last transaction date.
    * @param amount          the credit memo amount to be debited from the account. This value will
    *                        decrease both the available and outstanding balances.
     * @param reverse
    *
    * @return a {@link JSONObject} containing the result of the operation:
    *         <ul>
    *           <li>"success" if the transaction was recorded and balances updated correctly</li>
    *           <li>"error" with a descriptive message if validation fails or database update fails</li>
    *         </ul>
    *
    * @throws SQLException       if a database access error occurs during validation,
    *                            ledger insertion, or master account update.
    * @throws GuanzonException   if application-specific errors occur during transaction processing.
    *
    * @see #saveTransaction()
    * @see #validateInitAndMode(int)
    */
    public JSONObject CreditMemo(String bankAccountId, String sourceNo, Date transactionDate, double amount, boolean reverse)
            throws SQLException, GuanzonException {

        initReferences();

        psSourceCd = CREDIT_MEMO;
        psBnkActID = bankAccountId;
        psSourceNo = sourceNo;
        pdTransact = transactionDate;
        pnAmountIn = 0.00;
        pnAmountOt = amount;
        pbReversex = reverse;

        return saveTransaction();
    }

    private JSONObject saveTransaction() throws SQLException, GuanzonException{
        poJSON = new JSONObject();
                
        //load the transaction
        if (!loadTransaction()){
            poJSON.put("result", "error");
            poJSON.put("message", "Unable to load transaction!");
            return poJSON;
        }
        
        //process detail
        poJSON = processDetail();
        if ("error".equals((String) poJSON.get("result"))) return poJSON;
        
        //for delete
        if (pbReversex){
            poJSON = deleteTransaction();
            return poJSON;
        }
        
        //save detail
        poJSON = saveDetail();
        if ("error".equals((String) poJSON.get("result"))) return poJSON;
        
        String lsSQL;
        
        if (pnATranAmt + pnOTranAmt != 0.00){
            lsSQL = "UPDATE " + MASTER_TABLE + " SET" +
                        "  nABalance = nABalance + " + SQLUtil.toSQL(pnATranAmt) +
                        ", nOBalance = nOBalance + " + SQLUtil.toSQL(pnOTranAmt);
            
            if (poMaster.getDate("dLastTran") == null){
                lsSQL += ", dLastTran = " + SQLUtil.toSQL(pdTransact);
            } else {
                if (CommonUtils.dateDiff(CommonUtils.toLocalDate(poMaster.getDate("dLastTran")), 
                                            CommonUtils.toLocalDate(pdTransact), ChronoUnit.DAYS) > 0){
                    lsSQL += ", dLastTran = " + SQLUtil.toSQL(pdTransact);
                }
            }
            
            if (pnATranAmt != 0.00){
                if (poMaster.getDate("dLastPost") == null){
                    lsSQL += ", dLastPost = " + SQLUtil.toSQL(pdTransact);
                } else {
                    if (CommonUtils.dateDiff(CommonUtils.toLocalDate(poMaster.getDate("dLastPost")), 
                                            CommonUtils.toLocalDate(pdTransact), ChronoUnit.DAYS) > 0){
                    lsSQL += ", dLastPost = " + SQLUtil.toSQL(pdTransact);
                }
                }
            }
            
            if (psCheckNox != null && !psCheckNox.isEmpty()){
                lsSQL += ", sCheckNox = " + SQLUtil.toSQL(psCheckNox);
            }

            if (psSerialNo != null && !psSerialNo.isEmpty()){
                lsSQL += ", sSerialNo = " + SQLUtil.toSQL(psSerialNo);
            }
            
            lsSQL += " WHERE sBnkActID = " + SQLUtil.toSQL(psBnkActID);
            
            if (poGRider.executeQuery(lsSQL, MASTER_TABLE, psBranchCd, "") <= 0){
                poJSON = new JSONObject();
                poJSON.put("result", "error");
                poJSON.put("message", "Unable to update bank account information!");
                return poJSON;
            }
        }
        
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject saveDetail() throws SQLException, GuanzonException {
        String lsSQL = "INSERT INTO " + DETAIL_TABLE + " SET" +
                        "  sBnkActID = " + SQLUtil.toSQL(psBnkActID) +
                        ", sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
                        ", nLedgerNo = " +
                        " IF(ISNULL(@xLedgerNo := (SELECT nLedgerNo + 1" +
                            "  FROM Bank_Account_Ledger a" +
                            "  WHERE sBnkActID = " + SQLUtil.toSQL(psBnkActID) +
                            "  ORDER BY nLedgerNo DESC LIMIT 1))" +
                            ", 1, @xLedgerNo)" +
                        ", dTransact = " + SQLUtil.toSQL(pdTransact) + 
                        ", sSourceCd = " + SQLUtil.toSQL(psSourceCd) + 
                        ", sSourceNo = " + SQLUtil.toSQL(psSourceNo) + 
                        ", nAmountIn = " + SQLUtil.toSQL(pnAmountIn) + 
                        ", nAmountOt = " + SQLUtil.toSQL(pnAmountOt) + 
                        ", nABalance = " + SQLUtil.toSQL(poMaster.getDouble("nABalance") + (pnATranAmt)) +
                        ", nOBalance = " + SQLUtil.toSQL(poMaster.getDouble("nABalance") + (pnOTranAmt)) +
                        ", dPostedxx = NULL" + 
                        ", cTranStat = '1'" +
                        ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate());

        if (poGRider.executeQuery(lsSQL, DETAIL_TABLE, psBranchCd, "") <= 0) {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "Unable to update transaction ledger!");
            return poJSON;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        return poJSON;
    }

    private boolean loadTransaction() throws SQLException{
        String lsSQL = "SELECT" +
                            "  a.dLastTran" +
                            ", a.dLastPost" +
                            ", a.nOBalance xOBalance" +
                            ", a.nABalance xABalance" +
                            ", a.nOBegBalx" +
                            ", a.nABegBalx" +
                            ", b.dTransact" +
                            ", b.nAmountIn" +
                            ", b.nAmountOt" +
                            ", b.nLedgerNo" +
                            ", b.dPostedxx" +
                        " FROM " + MASTER_TABLE + " a" +
                            " LEFT JOIN Bank_Account_Ledger b" +
                                " ON a.sBnkActID = b.sBnkActID" +
                               " AND b.sSourceCd = " + SQLUtil.toSQL(psSourceCd) +
                               " AND b.sSourceNo = " + SQLUtil.toSQL(psSourceNo);
       
        lsSQL = MiscUtil.addCondition(lsSQL, "a.sBnkActID = " + SQLUtil.toSQL(psBnkActID));
        
        poMaster = poGRider.executeQuery(lsSQL);
        
        return poMaster.next();
    }
    
    private JSONObject processDetail() throws SQLException {        
        if (pnAmountIn + pnAmountOt == 0) {
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "Invalid transaction amount detected!\n" +
                                  "Please verify your entry.");
            return poJSON;
        }

        switch (psSourceCd) {
            case CHECK_DEPOSIT:
                pnATranAmt = 0;
                pnOTranAmt = pnAmountIn;
                break;
            case CASH_DEPOSIT:
            case DEBIT_MEMO:
            case BANK_INTEREST:    
                pnATranAmt = pnAmountIn;
                pnOTranAmt = pnAmountIn;
                break;

            case CHECK_DISBURSEMENT:
            case CHECK_PAYMENT:    
                pnATranAmt = 0;
                pnOTranAmt = -pnAmountOt;
                break;
            case CASH_WITHDRAWAL:
            case CREDIT_MEMO:
            case EPAY_DISBURSEMENT:    
            case WIRED_DISBURSEMENT:    
                pnATranAmt = -pnAmountOt;
                pnOTranAmt = -pnAmountOt;
                break;
        }

        poJSON = new JSONObject();
        poJSON.put("result", "success");
        return poJSON;
    }
    
    /**
    * Deletes a previously recorded transaction from the ledger and updates the master account balances.
    * <p>
    * Typical use cases include rolling back erroneous transactions or removing transactions
    * that were entered in error. This ensures that both the ledger and master balances remain
    * consistent after deletion.
    * </p>
    *
    * @return a {@link JSONObject} containing the result of the operation:
    *         <ul>
    *           <li>"success" if the transaction was deleted and balances updated correctly</li>
    *           <li>"error" with a descriptive message if the ledger deletion or master update fails</li>
    *         </ul>
    *
    * @throws SQLException       if a database access error occurs during ledger deletion
    *                            or master account update.
    * @throws GuanzonException   if application-specific errors occur during transaction deletion.
    *
    * @see #delDetail()
    */
    public JSONObject deleteTransaction() throws SQLException, GuanzonException{
        poJSON = delDetail();
        if ("error".equals((String) poJSON.get("result"))) return poJSON;
        
        String lsSQL = "UPDATE " + MASTER_TABLE + " SET" +
                            "  nOBalance = nOBalance - " + SQLUtil.toSQL(pnOTranAmt) +
                            ", nABalance = nABalance - " + SQLUtil.toSQL(pnATranAmt) +
                            ", sModified = " + SQLUtil.toSQL(poGRider.getServerDate()) +
                        " WHERE sBnkActID = " + SQLUtil.toSQL(psBnkActID);
        
        if (poGRider.executeQuery(lsSQL, MASTER_TABLE, psBranchCd, "") <= 0){
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "Unable to update bank account information!");
            return poJSON;
        }
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        return poJSON;
    }
    
    private JSONObject delDetail() throws SQLException, GuanzonException{
        String lsSQL = "DELETE FROM " + DETAIL_TABLE +
                        " WHERE sBnkActID = " + SQLUtil.toSQL(psBnkActID) +
                          " AND sSourceCd = " + SQLUtil.toSQL(psSourceCd) +
                          " AND sSourceNo = " + SQLUtil.toSQL(psSourceNo);
        
        if (poGRider.executeQuery(lsSQL, "Bank_Account_Ledger", psBranchCd, "") <= 0){
            poJSON = new JSONObject();
            poJSON.put("result", "error");
            poJSON.put("message", "Unable to Update Transaction Ledger!");
            return poJSON;
        }
        
        poJSON = new JSONObject();
        poJSON.put("result", "success");
        return poJSON;
    }
}