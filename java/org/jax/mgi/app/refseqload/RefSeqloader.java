//  $Header$
//  $Name$

package org.jax.mgi.app.refseqload;

import org.jax.mgi.shr.timing.Stopwatch;
import org.jax.mgi.shr.dla.input.genbank.GBOrganismChecker;
import org.jax.mgi.shr.dla.loader.seq.SeqLoader;
import org.jax.mgi.shr.dla.input.genbank.GBInputFile;
import org.jax.mgi.shr.exception.MGIException;
import java.util.Vector;
import java.util.Iterator;


/**
 * @is an object which extends Seqloader and implements the Seqloader
 * getDataIterator method to set Seqloader's OrganismChecker and
 * RecordDataIterator
 *
 * @has a RefSeqidPrefixChecker to determine the prefix of a RefSeq seqid
 *
 * @does
 * <UL>
 * <LI>implements superclass (Seqloader) getDataIterator to set
 *     OrganismChecker with a GBOrganismChecker and create a RecordDataIterator
 *     on a GBInputFile
 * <LI>It has an empty implementation of the superclass (DLALoader)
 *     preProcess method
 * <LI>It implements the superclass (Seqloader)appPostProcess method to log
 *     counts of Sequences with each RefSeq prefix
 * <LI>It overrides the superclass initialize() method to create a
 *     RefSeqidPrefixChecker
 * </UL>
 * @author sc
 * @version 1.0
 */

public class RefSeqloader extends SeqLoader {

    // we load only a subset of refseq seqid prefixes
    RefSeqidPrefixChecker pc;

    /**
      * This load has no preprocessing
      * @assumes nothing
      * @effects noting
      * @throws MGIException if errors occur during preprocessing
      */

    protected void preprocess() { }

    /**
     * creates and sets the superclass OrganismChecker and RecordDataIterator
     * with a GBOrganismChecker and creates and creates a GBInputFile
     * with a GBSequenceInterpretor; gets an iterator from the GBInputFile
     * @assumes nothing
     * @effects nothing
     * @throws MGIException
     */

    protected void getDataIterator() throws MGIException {

        // create an organism checker for the interpreter
        // need to create and pass to interpretor as base class - not as
        // OrganismChecker
        GBOrganismChecker oc = new GBOrganismChecker();

        // Cant override initialize to instantiate pc; super.initialize calls
        // this method and pc won't be set yet.
        pc = new RefSeqidPrefixChecker();

        // set oc in the superclass for reporting purposes
        super.organismChecker = oc;

        // Create a GBInputfile
        //RefSequenceInterpreter interp = new RefSequenceInterpreter(oc, pc);
        GBInputFile inData = new GBInputFile();

        // get an iterator for the GBInputFile witha RefSequenceInterpreter
        super.iterator = inData.getIterator(new RefSequenceInterpreter(oc, pc));
    }

    /**
     * reports RefSeq prefix counts
     * @assumes nothing
     * @effects Writes to a log file
     * @throws MGIException
     */
   protected void appPostProcess() throws MGIException {
       logger.logdInfo("Prefix Counts for organisms we are processing:", false);
       Vector deciderCts = pc.getDeciderCounts();
       for (Iterator i = deciderCts.iterator(); i.hasNext();) {
           String line = (String) i.next();
           logger.logdInfo( line, false);
       }
   }
}
// $Log
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
 *
 **************************************************************************/
