//  $Header
//  $Name

package org.jax.mgi.app.refseqload;

import java.util.*;
import java.util.regex.*;

import org.jax.mgi.shr.dla.input.SeqDecider;
import org.jax.mgi.shr.dla.loader.seq.SeqloaderConstants;
import org.jax.mgi.shr.config.RefSeqidPrefixCheckerCfg;
import org.jax.mgi.shr.config.ConfigException;
import org.jax.mgi.shr.exception.MGIException;
import org.jax.mgi.shr.dla.log.DLALogger;
import org.jax.mgi.shr.dla.log.DLALoggingException;

/**
 * @is An object that, given a GenBank format sequence record and a set of
 *     SeqDeciders representing Refseq seqid prefixes, determines if any of
 *     the deciders are true for
 *     the sequence record e.g. Given three deciders, NM, NR, and NP: given the
 *     Refseq seqid NM_12345 returns true, given the Refseq seqid XM_12345
 *     returns false
 * @has
 *   <UL>
 *   <LI>A sequence record
 *   <LI>A set of SeqDeciders; each a predicate to identify a given prefix
 *   </UL>
 * @does
 *   <UL>
 *   <LI>Finds the ACCESSION section of a sequence record
 *   <LI>Determines the set of deciders from an RefseqPrefixCheckerCfg
 *       configurator
 *   <LI>Queries each decider; Determines if the seqid is for a
 *       prefix represented by a decider
 *   </UL>
 * @company The Jackson Laboratory
 * @author sc
 * @version 1.0
 */

public class RefSeqidPrefixChecker {
    // expression string, pattern, and matcher to find the classification
    // section of a GenBank format sequence record
    // Note the ? forces searching until the FIRST instance of REFERENCE is found
    // without the ? it will search until the LAST instance
    //private static final String ORG_EXPRESSION = "ORGANISM([\\s\\S]*?)REFERENCE";
    // this one works; all classifications end with a '.' - Actually it doesn't
    // because in the case of organism being 'Mus sp.' it stops and does not
    // get the full classification
    //private static final String EXPRESSION = "ORGANISM([^.]+).*";
    private static final String ACC_EXPRESSION = "ACCESSION([^\\n]*)\\n";
    private Pattern accPattern;
    private Matcher accMatcher;

    // true if any decider returns true
    private boolean isA;

    // count of total records looked at
    private int totalCtr = 0;

    // count of records for which RefseqIdPrefixChecker.checkOrganism returns true
    private int trueCtr = 0;

    // the set of organism deciders to query
    private Vector deciders;

    // Configurator to determine organisms to check
    private RefSeqidPrefixCheckerCfg config;

    // returns true if a given seqid has a given prefix
    private RefSeqIdPrefixInterrogator ri;

    // The logicalDB of the DataProvider that uses GenBank format
    private String logicalDB;

    // DEBUG
    private DLALogger logger;

    /**
    * Constructs an OrganismChecker for a given provider with a set of
    * deciders
    * @assumes nothing
    * @effects nothing
    * @throws ConfigException if error accessing Configuration
    * @throws DLALoggingException if error getting a logger
    */

    public RefSeqidPrefixChecker () throws ConfigException, DLALoggingException {
        // create a configurator to get Prefixes from configuration
        config = new RefSeqidPrefixCheckerCfg();

        // create an interrogator to determine a seqid's prefix
        ri = new RefSeqIdPrefixInterrogator();

        // Get the deciders from configuration
        deciders = new Vector();
        if (config.getNM().equals(Boolean.TRUE)) {
          deciders.add(new NMDecider());
        }
        if (config.getNR().equals(Boolean.TRUE)) {
          deciders.add(new NRDecider());
        }
        if (config.getNP().equals(Boolean.TRUE)) {
          deciders.add(new NPDecider());
        }
        if (config.getNC().equals(Boolean.TRUE)) {
          deciders.add(new NCDecider());
        }
        if (config.getNG().equals(Boolean.TRUE)) {
          deciders.add(new NGDecider());
        }
        if (config.getNT().equals(Boolean.TRUE)) {
          deciders.add(new NTDecider());
        }
        if (config.getNW().equals(Boolean.TRUE)) {
          deciders.add(new NWDecider());
        }
        if (config.getNZ().equals(Boolean.TRUE)) {
          deciders.add(new NZDecider());
        }
        if (config.getZP().equals(Boolean.TRUE)) {
          deciders.add(new ZPDecider());
        }
	    if (config.getXM().equals(Boolean.TRUE)) {
          deciders.add(new XMDecider());
        }
        if (config.getXR().equals(Boolean.TRUE)) {
          deciders.add(new XRDecider());
        }
        if (config.getXP().equals(Boolean.TRUE)) {
          deciders.add(new XPDecider());
        }

        // compile expression to find the classification section of a record
        accPattern = Pattern.compile(ACC_EXPRESSION);
        logger = DLALogger.getInstance();
    }

    /**
    * Determines if a sequence record seqid prefix
    * represented by the set of deciders
    * @assumes Nothing
    * @effects Nothing
    * @return true if sequence record prefix is represented by one of
    *         the deciders.
    * @throws Nothing
    */

    public boolean checkPrefix(String record) {
        totalCtr++;
        // reset
        isA = false;

        // find the ACCESSION line of this record
        accMatcher = accPattern.matcher(record);

        if (accMatcher.find() == true) {
            // Determine if we are interested in this sequence
            Iterator i = deciders.iterator();
            while (i.hasNext()) {
                SeqDecider currentDecider = (SeqDecider)i.next();
                String group = accMatcher.group(1);
                // m.group(1) is the
                if(currentDecider.isA(group)) {
                    trueCtr++;
                    isA = true;
                    break;
                }
            }

        }
        /*
        if (isA == false) {
            logger.logdDebug("Not a valid record: " + record, true);
        }
        */
        return isA;
      }

    public boolean isPrefix(String seqid, String prefix ) {
        return ri.isPrefix(seqid, prefix);

    }

    /**
    * Gets the total records looked at, the total records for which checkOrganism
    *  returned true and the count of records for which each decider returned true.
    * @assumes Nothing
    * @effects Nothing
    * @return Vector of Strings, each String contains the decider name
    *         and the count of records for which the decider returned true
    * @throws Nothing
    */
    public Vector getDeciderCounts () {
      Vector v = new Vector();
      v.add("Total records looked at: " + totalCtr + SeqloaderConstants.CRT);
      v.add("Total records processed: " + trueCtr + SeqloaderConstants.CRT);
      Iterator i = deciders.iterator();
            while (i.hasNext()) {
              SeqDecider d = (SeqDecider)i.next();
              String s = "Total " + d.getName() + " records processed: " +
                  d.getTrueCtr() + SeqloaderConstants.CRT;
              v.add(s);
            }
            return v;
    }
      /**
       * @is an object that applies this predicate to the ACCESSION line
       * of a Refseq sequence record "Is the seqid prefix NM?"
       * @has A name, see also superclass
       * @does Returns true if seqid prefix is NM
       * @company The Jackson Laboratory
       * @author sc
       * @version 1.0
       */

      private class NMDecider extends SeqDecider {
        /**
         * Constructs a NMDecider object with the name "NM" which
         * is controlled vocabulary used by the RefSeqIdPrefixInterrogator
         * @assumes Nothing
         * @effects Nothing
         * @param None
         * @throws Nothing
         */

        protected NMDecider() {
          super ("NM");
        }

        /**
         * Determines if 'seqid' has prefix 'NM_' Counts total
   	     * seqids processed and total for which the predicate is true.
         * @assumes Nothing
         * @effects Nothing
         * @param seqid A RefSeq seqid
         * @return true if this predicate is true for 'seqid'
         * @throws MGIException if the sequence interrogator does not support
         *         this decider.
         */

        protected boolean is(String seqid) {
          return ri.isPrefix(seqid, name);
       }
     }

    /**
     * @is an object that applies this predicate to the ACCESSION line
     * of a Refseq sequence record "Is the seqid prefix NR?"
     * @has A name, see also superclass
     * @does Returns true if seqid prefix is NR
     * @company The Jackson Laboratory
     * @author sc
     * @version 1.0
     */

      private class NRDecider extends SeqDecider {

        /**
         * Constructs a NRDecider object with the name "NR" which
         * is controlled vocabulary used by the RefSeqIdPrefixInterrogator
         * @assumes Nothing
         * @effects Nothing
         * @param None
         * @throws Nothing
         */

        protected NRDecider() {
          super ("NR");
        }

        /**
         * Determines if 'seqid' has prefix 'NR_' Counts total
         * seqids processed and total for which the predicate is true.
         * @assumes Nothing
         * @effects Nothing
         * @param seqid A RefSeq seqid
         * @return true if this predicate is true for 'seqid'
         * @throws MGIException if the sequence interrogator does not support
         *         this decider.
         */

        protected boolean is(String seqid) {
          return ri.isPrefix(seqid, name);
       }
     }

      /**
       * @is an object that applies this predicate to the ACCESSION line
       * of a Refseq sequence record "Is the seqid prefix NP?"
       * @has A name, see also superclass
       * @does Returns true if seqid prefix is NP
       * @company The Jackson Laboratory
       * @author sc
       * @version 1.0
       */

      private class NPDecider extends SeqDecider {

        /**
         * Constructs a NPDecider object with the name "NP" which
         * is controlled vocabulary used by the RefSeqIdPrefixInterrogator
         * @assumes Nothing
         * @effects Nothing
         * @param None
         * @throws Nothing
         */

        protected NPDecider() {
          super ("NP");
        }

        /**
         * Determines if 'seqid' has prefix 'NP_' Counts total
         * seqids processed and total for which the predicate is true.
         * @assumes Nothing
         * @effects Nothing
         * @param seqid A RefSeq seqid
         * @return true if this predicate is true for 'seqid'
         * @throws MGIException if the sequence interrogator does not support
         *         this decider.
         */

        protected boolean is(String seqid) {
          return ri.isPrefix(seqid, name);
       }
     }

     /**
      * @is an object that applies this predicate to the ACCESSION line
      * of a Refseq sequence record "Is the seqid prefix NC?"
      * @has A name, see also superclass
      * @does Returns true if seqid prefix is NC
      * @company The Jackson Laboratory
      * @author sc
      * @version 1.0
      */

     private class NCDecider extends SeqDecider {
       /**
        * Constructs a NCDecider object with the name "NC" which
        * is controlled vocabulary used by the RefSeqIdPrefixInterrogator
        * @assumes Nothing
        * @effects Nothing
        * @param None
        * @throws Nothing
        */

       protected NCDecider() {
         super ("NC");
       }

       /**
        * Determines if 'seqid' has prefix 'NC_' Counts total
           * seqids processed and total for which the predicate is true.
        * @assumes Nothing
        * @effects Nothing
        * @param seqid A RefSeq seqid
        * @return true if this predicate is true for 'seqid'
        * @throws MGIException if the sequence interrogator does not support
        *         this decider.
        */

       protected boolean is(String seqid) {
         return ri.isPrefix(seqid, name);
      }
    }

    /**
     * @is an object that applies this predicate to the ACCESSION line
     * of a Refseq sequence record "Is the seqid prefix NG?"
     * @has A name, see also superclass
     * @does Returns true if seqid prefix is NG
     * @company The Jackson Laboratory
     * @author sc
     * @version 1.0
     */

    private class NGDecider extends SeqDecider {

      /**
       * Constructs a NGDecider object with the name "NG" which
       * is controlled vocabulary used by the RefSeqIdPrefixInterrogator
       * @assumes Nothing
       * @effects Nothing
       * @param None
       * @throws Nothing
       */

      protected NGDecider() {
        super ("NG");
      }

      /**
       * Determines if 'seqid' has prefix 'NG_' Counts total
       * seqids processed and total for which the predicate is true.
       * @assumes Nothing
       * @effects Nothing
       * @param seqid A RefSeq seqid
       * @return true if this predicate is true for 'seqid'
       * @throws MGIException if the sequence interrogator does not support
       *         this decider.
       */

      protected boolean is(String seqid) {
        return ri.isPrefix(seqid, name);
     }
   }
   /**
    * @is an object that applies this predicate to the ACCESSION line
    * of a Refseq sequence record "Is the seqid prefix NT?"
    * @has A name, see also superclass
    * @does Returns true if seqid prefix is NT
    * @company The Jackson Laboratory
    * @author sc
    * @version 1.0
    */

   private class NTDecider extends SeqDecider {

     /**
      * Constructs a NTDecider object with the name "NT" which
      * is controlled vocabulary used by the RefSeqIdPrefixInterrogator
      * @assumes Nothing
      * @effects Nothing
      * @param None
      * @throws Nothing
      */

     protected NTDecider() {
       super ("NT");
     }

     /**
      * Determines if 'seqid' has prefix 'NT_' Counts total
      * seqids processed and total for which the predicate is true.
      * @assumes Nothing
      * @effects Nothing
      * @param seqid A RefSeq seqid
      * @return true if this predicate is true for 'seqid'
      * @throws MGIException if the sequence interrogator does not support
      *         this decider.
      */

     protected boolean is(String seqid) {
       return ri.isPrefix(seqid, name);
    }
  }

  /**
   * @is an object that applies this predicate to the ACCESSION line
   * of a Refseq sequence record "Is the seqid prefix NW?"
   * @has A name, see also superclass
   * @does Returns true if seqid prefix is NW
   * @company The Jackson Laboratory
   * @author sc
   * @version 1.0
   */

  private class NWDecider extends SeqDecider {

    /**
     * Constructs a NWDecider object with the name "NW" which
     * is controlled vocabulary used by the RefSeqIdPrefixInterrogator
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @throws Nothing
     */

    protected NWDecider() {
      super ("NW");
    }

    /**
     * Determines if 'seqid' has prefix 'NW_' Counts total
     * seqids processed and total for which the predicate is true.
     * @assumes Nothing
     * @effects Nothing
     * @param seqid A RefSeq seqid
     * @return true if this predicate is true for 'seqid'
     * @throws MGIException if the sequence interrogator does not support
     *         this decider.
     */

    protected boolean is(String seqid) {
      return ri.isPrefix(seqid, name);
   }
 }

 /**
  * @is an object that applies this predicate to the ACCESSION line
  * of a Refseq sequence record "Is the seqid prefix NZ?"
  * @has A name, see also superclass
  * @does Returns true if seqid prefix is NZ
  * @company The Jackson Laboratory
  * @author sc
  * @version 1.0
  */

 private class NZDecider extends SeqDecider {

   /**
    * Constructs a NZDecider object with the name "NZ" which
    * is controlled vocabulary used by the RefSeqIdPrefixInterrogator
    * @assumes Nothing
    * @effects Nothing
    * @param None
    * @throws Nothing
    */

   protected NZDecider() {
     super ("NZ");
   }

   /**
    * Determines if 'seqid' has prefix 'NZ_' Counts total
    * seqids processed and total for which the predicate is true.
    * @assumes Nothing
    * @effects Nothing
    * @param seqid A RefSeq seqid
    * @return true if this predicate is true for 'seqid'
    * @throws MGIException if the sequence interrogator does not support
    *         this decider.
    */

   protected boolean is(String seqid) {
     return ri.isPrefix(seqid, name);
  }
}

    /**
     * @is an object that applies this predicate to the ACCESSION line
     * of a Refseq sequence record "Is the seqid prefix ZP?"
     * @has A name, see also superclass
     * @does Returns true if seqid prefix is ZP
     * @company The Jackson Laboratory
     * @author sc
     * @version 1.0
     */

    private class ZPDecider extends SeqDecider {

      /**
       * Constructs a ZPDecider object with the name "ZP" which
       * is controlled vocabulary used by the RefSeqIdPrefixInterrogator
       * @assumes Nothing
       * @effects Nothing
       * @param None
       * @throws Nothing
       */

      protected ZPDecider() {
        super ("ZP");
      }

      /**
       * Determines if 'seqid' has prefix 'ZP_' Counts total
       * seqids processed and total for which the predicate is true.
       * @assumes Nothing
       * @effects Nothing
       * @param seqid A RefSeq seqid
       * @return true if this predicate is true for 'seqid'
       * @throws MGIException if the sequence interrogator does not support
       *         this decider.
       */

      protected boolean is(String seqid) {
        return ri.isPrefix(seqid, name);
     }
   }


     /**
      * @is an object that applies this predicate to the ACCESSION line
      * of a Refseq sequence record "Is the seqid prefix XM?"
      * @has A name, see also superclass
      * @does Returns true if seqid prefix is XM
      * @company The Jackson Laboratory
      * @author sc
      * @version 1.0
      */

      private class XMDecider extends SeqDecider {

        /**
         * Constructs a XMDecider object with the name "XM" which
         * is controlled vocabulary used by the RefSeqIdPrefixInterrogator
         * @assumes Nothing
         * @effects Nothing
         * @param None
         * @throws Nothing
         */

        protected XMDecider() {
          super ("XM");
        }

        /**
         * Determines if 'seqid' has prefix 'XM_' Counts total
   	     * seqids processed and total for which the predicate is true.
         * @assumes Nothing
         * @effects Nothing
         * @param seqid A RefSeq seqid
         * @return true if this predicate is true for 'seqid'
         * @throws MGIException if the sequence interrogator does not support
         *         this decider.
         */

        protected boolean is(String seqid) {
          return ri.isPrefix(seqid, name);
       }
     }
       /**
       * @is an object that applies this predicate to the ACCESSION line
       * of a Refseq sequence record "Is the seqid prefix XR?"
       * @has A name, see also superclass
       * @does Returns true if seqid prefix is XR
       * @company The Jackson Laboratory
       * @author sc
       * @version 1.0
       */

      private class XRDecider extends SeqDecider {

        /**
         * Constructs a XRDecider object with the name "XR" which
         * is controlled vocabulary used by the RefSeqIdPrefixInterrogator
         * @assumes Nothing
         * @effects Nothing
         * @param None
         * @throws Nothing
         */

        protected XRDecider() {
          super ("XR");
        }

        /**
         * Determines if 'seqid' has prefix 'XR_' Counts total
   	 * seqids processed and total for which the predicate is true.
         * @assumes Nothing
         * @effects Nothing
         * @param seqid A RefSeq seqid
         * @return true if this predicate is true for 'seqid'
         * @throws MGIException if the sequence interrogator does not support
         *         this decider.
         */

        protected boolean is(String seqid) {
          return ri.isPrefix(seqid, name);
       }
     }
      /**
      * @is an object that applies this predicate to the ACCESSION line
      * of a Refseq sequence record "Is the seqid prefix XP?"
      * @has A name, see also superclass
      * @does Returns true if seqid prefix is XP
      * @company The Jackson Laboratory
      * @author sc
      * @version 1.0
      */

     private class XPDecider extends SeqDecider {

        /**
         * Constructs a XPDecider object with the name "XP" which
         * is controlled vocabulary used by the RefSeqIdPrefixInterrogator
         * @assumes Nothing
         * @effects Nothing
         * @param None
         * @throws Nothing
         */

        protected XPDecider() {
          super ("XP");
        }

        /**
         * Determines if 'seqid' has prefix 'XP_' Counts total
         * seqids processed and total for which the predicate is true.
         * @assumes Nothing
         * @effects Nothing
         * @param seqid A RefSeq seqid
         * @return true if this predicate is true for 'seqid'
         * @throws MGIException if the sequence interrogator does not support
         *         this decider.
         */

        protected boolean is(String seqid) {
          return ri.isPrefix(seqid, name);
       }
     }

     /**
        * @is an object that queries a Refseq seqid to determine if
        *     it has a particular prefix
        * @has a mapping of controlled vocabulary
        *       to string expressions e.g. "NM" : "NM_"
        * @does Given a Refseq seqid string, e.g. NM12345<BR>
        *      and a controlled vocabulary string, e.g. "NM"<BR>
        *      determine if the seqid has the prefix represented by "NM"
        * @company The Jackson Laboratory
        * @author sc
        * @version 1.0
        */

   private class RefSeqIdPrefixInterrogator {

       // a hash map data structure that maps seqid prefix controlled vocab
       // to a String expression.
       private String NM;
       private String NR;
       private String NP;
       private String NC;
       private String NG;
       private String NT;
       private String NW;
       private String NZ;
       private String ZP;
       private String XM;
       private String XR;
       private String XP;

       // load HashMap with controlled vocab keys and string expression values
       private HashMap expressions = new HashMap();

       protected RefSeqIdPrefixInterrogator() {
          NM = "NM_";
          NR = "NR_";
          NP = "NP_";
          NC = "NC_";
          NG = "NG_";
          NT = "NT_";
          NW = "NW_";
          NZ = "NZ_";
          ZP = "ZP_";
          XM = "XM_";
          XR = "XR_";
          XP = "XP_";
          expressions.put("NM", NM);
          expressions.put("NR", NR);
          expressions.put("NP", NP);
          expressions.put("NC", NC);
          expressions.put("NG", NG);
          expressions.put("NT", NT);
          expressions.put("NW", NW);
          expressions.put("NZ", NZ);
          expressions.put("ZP", ZP);
          expressions.put("XM", XM);
          expressions.put("XR", XR);
          expressions.put("XP", XP);

        }

       /**
        * Determines whether a seqid has a particular prefix
        * @assumes "prefix" is a valid controlled vocabulary for "seqid"
        * @effects Nothing
        * @param seqid A GenBank sequence classification string
        * @param prefix a decider name for determining expression to apply to
        *        classification
        * @return true if "seqid" has prefix "prefix"
        * @throws Nothing
        */

         private boolean isPrefix (String seqid, String prefix) {
            // get the string expression that is mapped to 'prefix'

            if(seqid.indexOf(
                  (String)expressions.get(prefix)) >  -1) {
                return true;
            }
            else {
               return false;
            }
         }
   }
}

/**************************************************************************
*
* Warranty Disclaimer and Copyright Notice
*
*  THE JACKSON LABORATORY MAKES NO REPRESENTATION ABOUT THE SUITABILITY OR
*  ACCURACY OF THIS SOFTWARE OR DATA FOR ANY PURPOSE, AND MAKES NO WARRANTIES,
*  EITHER EXPRESS OR IMPLIED, INCLUDING MERCHANTABILITY AND FITNESS FOR A
*  PARTICULAR PURPOSE OR THAT THE USE OF THIS SOFTWARE OR DATA WILL NOT
*  INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS, OR OTHER RIGHTS.
*  THE SOFTWARE AND DATA ARE PROVIDED "AS IS".
*
*  This software and data are provided to enhance knowledge and encourage
*  progress in the scientific community and are to be used only for research
*  and educational purposes.  Any reproduction or use for commercial purpose
*  is prohibited without the prior express written permission of The Jackson
*  Laboratory.
*
* Copyright \251 1996, 1999, 2002, 2003 by The Jackson Laboratory
*
* All Rights Reserved
**************************************************************************/
