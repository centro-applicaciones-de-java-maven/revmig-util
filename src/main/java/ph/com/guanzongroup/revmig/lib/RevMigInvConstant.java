/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ph.com.guanzongroup.revmig.lib;

/**
 *
 * @author Administrator
 */
public class RevMigInvConstant {
    public final static String PURCHASE_RECEIVING = "GNDA";       
    public final static String BRANCH_TRANSFER = "GNDv";
    public final static String BRANCH_TRANSFER_ACCEPTANCE = "GNDl";
    public final static String BRANCH_JOBORDER = "GNJO";          


    //public final static String BRANCH_TRANSFER_ACCEPTANCE = "AcDl";
    //public final static String BRANCH_TRANSFER = "AcDv";
    
    //public final static String BRANCH_JOBORDER = "JOxx";          //JOxx
    
    public final static String PURCHASE_ORDER_QUOTATION = "POQt";           //POxx
    public final static String PURCHASE_ORDER_QUOTATION_REQUEST = "POQR";           //POxx

    public final static String PURCHASE_ORDER = "POxx";           //POxx
    public final static String PURCHASE_ORDER_CANCELLATION = "POCx";           //POxx
    //public final static String PURCHASE_RECEIVING = "PODA";       //PODA 

    public final static String PURCHASE_RETURN = "PORt";          //PORt
    public final static String PURCHASE_REPLACEMENT = "PORp";     //PORp
    
    public final static String SALES = "SLxx";  //Slxx
    public final static String SALES_GIVEAWAY = "SLGA";
    public final static String SALES_GIVEAWAY_RELEASE = "GARl";    //Added     
    public final static String SALES_RETURN = "SLRt";  
    public final static String SALES_REPLACEMENT = "SLRp";

    public final static String IMPOUND = "IMxx";
    public final static String IMPOUND_RELEASE = "IMRl";
    
    public final static String WARRANTY_RELEASE = "WRRl";         //WRRl
    
    public final static String BRANCH_ORDER = "BOxx";             //BOxx
    public final static String BRANCH_ORDER_CONFIRMATION = "BOCn";      //BOCn
    public final static String BRANCH_ORDER_WAREHOUSE_CANCELLATION = "BOWC";     
    public final static String BRANCH_ORDER_BRANCH_CANCELLATION = "BOBC";     
    
    public final static String CUSTOMER_ORDER = "COxx";           //COxx    
    public final static String CUSTOMER_ORDER_CANCELLATION = "COCa";   //COCa

    public final static String RETAIL_ORDER = "ROxx";             //ROxx
    public final static String RETAIL_ORDER_CANCELLATION = "ROCa"; //ROCa

    public final static String WHOLESALE = "WLxx";
    public final static String WHOLESALE_RETURN = "WLRt";
    public final static String WHOLESALE_REPLACEMENT = "WLRp";
    
    public final static String GCARD_REDEMPTION = "GCRd";             //GCRd 
    
    public final static String CREDIT_MEMO = "AJCm";
    public final static String DEBIT_MEMO = "AJDm";

    //+Inv_Master->nQtyOnHnd
    //+Inv_Ledger->nQtyInxxx 
    public static String getDebitTrans(){
        return BRANCH_TRANSFER_ACCEPTANCE + ":" +
               PURCHASE_RECEIVING + ":" +
               PURCHASE_REPLACEMENT + ":" +
               SALES_RETURN + ":" +
               WHOLESALE_RETURN + ":" + 
               IMPOUND + ":" + 
               DEBIT_MEMO; 
    }

    //-Inv_Master->nQtyOnHnd
    //+Inv_Ledger->nQtyOutxx
    public static String getCreditTrans(){
        return BRANCH_TRANSFER + ":" +
               BRANCH_JOBORDER + ":" +
               PURCHASE_RETURN + ":" +
               SALES + ":" +
               SALES_GIVEAWAY + ":" +
               SALES_GIVEAWAY_RELEASE + ":" +
               SALES_REPLACEMENT + ":" + 
               WARRANTY_RELEASE + ":" +
               WHOLESALE + ":" + 
               WHOLESALE_REPLACEMENT + ":" + 
               IMPOUND_RELEASE + ":" + 
               CREDIT_MEMO; 
    }

    
}
