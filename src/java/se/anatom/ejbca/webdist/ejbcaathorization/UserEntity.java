/*
 * UserEntity.java
 *
 * Created on den 16 mars 2002, 11:42
 */

package se.anatom.ejbca.webdist.ejbcaathorization;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;
import java.io.Serializable;

import se.anatom.ejbca.webdist.rainterface.DNFieldExtractor;
/**
 * A class representing a user entity. It can be set to match one users dn or an entire organization by matching against ou.
 * The class main method is match() wicth takes a X509Certificate and tries to see if it fullfills set matching requirements. 
 *
 * Matchwith constants points to which part of the certificate to match with.
 * Matchtype constants tells under which contitions the match shall be performed.
 *   
 * @author  Philip Vendil
 */
public class UserEntity implements Serializable {
    // Match type constants.
    public static final int TYPE_EQUALCASE        = 1000;
    public static final int TYPE_EQUALCASEINS     = 1001;
    public static final int TYPE_NOT_EQUALCASE    = 1002;
    public static final int TYPE_NOT_EQUALCASEINS = 1003;
    public static final int TYPE_EQUAL_REGEXP     = 1004;
    public static final int TYPE_NOT_EQUAL_REGEXP = 1005;
    
    // Match with constants.
    // OBSERVE These constants is also used as a prioroty indicator for access rules.
    // The higher values the higher priority.
    public static final int WITH_COUNTRY           = 1;
    public static final int WITH_STATE             = 2;
    public static final int WITH_LOCALE            = 3;
    public static final int WITH_ORGANIZATION      = 4;
    public static final int WITH_ORGANIZATIONUNIT  = 5;
    public static final int WITH_COMMONNAME        = 6;
    public static final int WITH_SERIALNUMBER      = 7;

    
    /** Creates a new instance of UserEntity */
    public UserEntity(int matchwith, int matchtype, String matchvalue) {
        this.matchwith=matchwith;
        this.matchtype=matchtype;
        this.matchvalue=matchvalue; // REMOVE
    }
    
    // Public methods.
    /** Matches the given client X509Certificate to see if it matches it's requirements. */
    public boolean match(X509Certificate certificate) {
        String certstring = certificate.getSubjectX500Principal().toString();
        String serialnumber = certificate.getSerialNumber().toString(16); 
 //       System.out.println("UserEntity : match begin : " + certstring + " with " + this.matchwith + " type " + this.matchtype);
        boolean returnvalue=false;
        String clientstring=null;
        int startindex=0;
        int endindex=0;
        
        // Determine part of certificate to match with.
        DNFieldExtractor dn = new DNFieldExtractor(certstring);
        switch(matchwith){
            case WITH_COUNTRY:
              clientstring= dn.getField(DNFieldExtractor.COUNTRY);
              break;   
            case WITH_STATE:
              clientstring= dn.getField(DNFieldExtractor.STATE); 
              break;  
            case WITH_LOCALE:
              clientstring= dn.getField(DNFieldExtractor.LOCALE); 
              break;                
            case WITH_ORGANIZATION:
              clientstring= dn.getField(DNFieldExtractor.ORGANIZATION); 
              break;
           case WITH_ORGANIZATIONUNIT:
              clientstring= dn.getField(DNFieldExtractor.ORGANIZATIONUNIT); 
              break;             
           case WITH_COMMONNAME:
              clientstring= dn.getField(DNFieldExtractor.COMMONNAME); 
              break;   
            case WITH_SERIALNUMBER:
              if(serialnumber!=null){
                  clientstring= serialnumber.trim();                    
              }
              else{
                clientstring= null;
              }  
              break;   
           default:
        }
        
        // Determine how to match.
        if(clientstring!=null){
          switch(matchtype){  
            case TYPE_EQUALCASE:   
              returnvalue = clientstring.equals(matchvalue);  
              break;
            case TYPE_EQUALCASEINS:
              returnvalue = clientstring.equalsIgnoreCase(matchvalue);  
              break;
            case TYPE_NOT_EQUALCASE:
              returnvalue = !clientstring.equals(matchvalue);
              break;
            case TYPE_NOT_EQUALCASEINS:
              returnvalue = !clientstring.equalsIgnoreCase(matchvalue);  
              break;
            case TYPE_EQUAL_REGEXP:
              returnvalue = clientstring.matches(matchvalue);        
              break;
            case TYPE_NOT_EQUAL_REGEXP:
              returnvalue = !clientstring.matches(matchvalue);        
              break;             
            default:
         }
        }
     //   System.out.println("UserEntity : match end : returns " + returnvalue + " : " + certstring +" : "+ clientstring); 
        return returnvalue;
    }

    // Methods to get and set the individual variables.
    public int getMatchWith(){
      return matchwith;   
    }
    
    public void setMatchWith(int matchwith){
      this.matchwith=matchwith;   
    }
    
    public int getMatchType(){
      return matchtype;   
    }
    
    public void setMatchType(int matchtype){
      this.matchtype=matchtype;   
    }
    
    public String getMatchValue(){
      return matchvalue;   
    }
    
    public void setMatchValue(String matchvalue){
      this.matchvalue=matchvalue;   
    }
    
    /** Method used by the access tree to determine the priority. The priority is the same as match with value. */
    public int getPriority(){
      return matchwith;
    }
    
    // Private methods.
      
    // Private fields.
    private int    matchwith;
    private int    matchtype;
    private String matchvalue;
   
}
