//  $Header
//  $Name

package org.jax.mgi.app.refseqload;

/**
 * Debug stuff
 */
import org.jax.mgi.shr.timing.Stopwatch;

import java.util.*;


//import org.jax.mgi.shr.config.InputDataCfg;
import org.jax.mgi.shr.config.BCPManagerCfg;
import org.jax.mgi.shr.ioutils.RecordDataInterpreter;
import org.jax.mgi.shr.config.SequenceLoadCfg;
import org.jax.mgi.shr.dla.seqloader.SeqloaderConstants;
import org.jax.mgi.shr.dla.seqloader.MergeSplitProcessor;
import org.jax.mgi.shr.dla.seqloader.SeqProcessor;
import org.jax.mgi.shr.dla.seqloader.IncremSeqProcessor;
import org.jax.mgi.shr.dla.seqloader.DRSeqProcessor;
import org.jax.mgi.shr.dla.seqloader.SeqEventDetector;
import org.jax.mgi.shr.dla.seqloader.SequenceInput;
import org.jax.mgi.shr.dla.seqloader.SequenceAttributeResolver;
import org.jax.mgi.shr.dla.seqloader.SeqQCReporter;
import org.jax.mgi.shr.dla.seqloader.GBOrganismChecker;
import org.jax.mgi.shr.dla.seqloader.SeqloaderException;
import org.jax.mgi.shr.dla.seqloader.SeqloaderExceptionFactory;
import org.jax.mgi.shr.dla.seqloader.SequenceResolverException;
import org.jax.mgi.shr.dla.seqloader.RepeatSequenceException;
import org.jax.mgi.shr.dla.seqloader.RepeatSequenceException;
import org.jax.mgi.shr.dla.seqloader.ChangedOrganismException;
import org.jax.mgi.shr.dla.seqloader.ChangedLibraryException;
import org.jax.mgi.shr.dla.seqloader.GBSeqAttributeResolver;
import org.jax.mgi.shr.dla.DLALogger;
import org.jax.mgi.shr.dla.DLAException;
import org.jax.mgi.shr.dla.DLAExceptionHandler;
import org.jax.mgi.shr.dla.DLALoggingException;
import org.jax.mgi.shr.ioutils.InputDataFile;
import org.jax.mgi.shr.ioutils.RecordDataIterator;
import org.jax.mgi.shr.dbutils.SQLDataManager;
import org.jax.mgi.shr.dbutils.SQLDataManagerFactory;
import org.jax.mgi.shr.dbutils.bcp.BCPManager;
import org.jax.mgi.shr.dbutils.dao.SQLStream;
import org.jax.mgi.shr.dbutils.dao.BCP_Inline_Stream;
import org.jax.mgi.shr.dbutils.dao.BCP_Batch_Stream;
import org.jax.mgi.shr.dbutils.dao.BCP_Script_Stream;
import org.jax.mgi.shr.dbutils.ScriptWriter;
import org.jax.mgi.shr.config.ScriptWriterCfg;
import org.jax.mgi.shr.dbutils.ScriptException;
import org.jax.mgi.shr.exception.MGIException;
import org.jax.mgi.shr.ioutils.RecordFormatException;
import org.jax.mgi.shr.ioutils.IOUException;
import org.jax.mgi.shr.cache.KeyNotFoundException;
import org.jax.mgi.dbs.SchemaConstants;
import org.jax.mgi.dbs.mgd.MolecularSource.MSException;
import org.jax.mgi.dbs.mgd.lookup.TranslationException;
import org.jax.mgi.shr.config.ConfigException;
import org.jax.mgi.shr.cache.CacheException;
import org.jax.mgi.shr.dbutils.DBException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Runtime;

/**
 * @is
 * @has
 *   <UL>
 *   <LI>
 *   </UL>
 * @does
 *   <UL>
 *   <LI>
 *   <LI>
 *   <LI>
 *
 *   </UL>
 * @company The Jackson Laboratory
 * @author sc
 * @version 1.0
 */

public class RefSeqloader {

    // configurator for the sequence load
    private SequenceLoadCfg loadCfg;

    // the load mode
    private String loadMode;

    // Checks a record and determines if the sequence is from an organism
    // we want to load
    private GBOrganismChecker organismChecker;

    // Checks a record and determines if the seqid has a sequence we want
    // to load
    private RefSeqidPrefixChecker prefixChecker;

    // Interpretor for GenBank format sequence records
    private RefSequenceInterpreter interpretor;

    // An input data file object for the input file.
    private InputDataFile inData;

    // An iterator that gets one sequence record at a time.
    private RecordDataIterator iterator;

    // Instance of the dataload logger for sending messages to the log files.
    private DLALogger logger;

    // An SQL data manager for providing a connection to the MGD database
    private SQLDataManager mgdSqlMgr;

    // A bcp manager for handling bcp inserts to the MGD database
    private BCPManager mgdBcpMgr;

    // ScriptWriter and Script cfg for writing and exec'ing update script
    private ScriptWriterCfg updateScriptCfg = null;
    private ScriptWriter updateScriptWriter = null;

    // ScriptWriter and Script cfg for writing and exec'ing mergeSplit script
    private ScriptWriterCfg mergeSplitScriptCfg = null;
    private ScriptWriter mergeSplitScriptWriter = null;

    private BCP_Script_Stream mgdStream;
    //private BCP_Batch_Stream mgdStream;
    //private BCP_Inline_Stream mgdStream;

    // An SQL data manager for providing a connection to the Radar database
    private SQLDataManager rdrSqlMgr;

    // A bcp manager for handling bcp inserts to the Radar database
    private BCPManager rdrBcpMgr;

    // A stream for handling RDR DAO objects
    private BCP_Inline_Stream rdrStream;

    // A QC reporter for managing all qc reports for the seqloader
    private SeqQCReporter qcReporter = null;

    // resolves GenBank sequence attributes to MGI values
    private SequenceAttributeResolver seqResolver;

    // for processing Merges and Splits after Sequences are loaded
    MergeSplitProcessor mergeSplitProcessor;

    // the sequence processor for the load
    SeqProcessor seqProcessor;

    // file writer for repeated sequences
    private BufferedWriter repeatSeqWriter;

    SeqloaderExceptionFactory eFactory;

    /**
     * For each sequence record in the input
     *    create a SequenceInput object
     *    Call GBSequenceProcessor.processSequence(SequenceInput)
     * :Sequence
     * After all sequences processed call
     *     Call mergeSplitProcessor.process()
     */
    public static void main(String[] args) {
        DLAException e1 = null;
        DLAExceptionHandler eh = null;
        RefSeqloader seqloader = new RefSeqloader();

        //  instantiate objects and initialize variables

        try {
            seqloader.initialize();
        }
        catch (MGIException e) {
            System.out.println(e.toString());
        }
        // load sequences
       try {
           seqloader.load();
       }
       catch (MGIException e) {
           e1 = new DLAException("Sequence loader failed", false);
           e1.setParent(e);
           eh = new DLAExceptionHandler();
           eh.handleException(e1);
           System.out.println(e1.getMessage());
           System.exit(1);
       }

    }

    /**
     * what this method does ...
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return
     * @throws Nothing
     */

    private void initialize () throws MGIException {
        MGIException.setOkToStackTrace(true);
        // get a configurator then get load mode
        loadCfg = new SequenceLoadCfg();
        loadMode = loadCfg.getLoadMode();

        // get a dataload logger
        this.logger = DLALogger.getInstance();
        //logger.setDebug(true);
        logger.logpInfo("Perform initialization", false);
        logger.logdInfo("Perform initialization",true);

         // an InputDataFile has a Configurator from which itgets its file name
        this.inData = new InputDataFile();

        // create an organism checker to pass to the interpreter
        organismChecker = new GBOrganismChecker();

        // create a prefix checker to pass to the interpreter
        prefixChecker = new RefSeqidPrefixChecker();
        // Create an interpretor and get an iterator that uses that interpreter
        interpretor = new RefSequenceInterpreter(organismChecker, prefixChecker);
        iterator = inData.getIterator(interpretor);

        /**
         * Set up MGD stream
         */
        // Create a SQLDataManager for the MGD database from the factory.
        mgdSqlMgr = SQLDataManagerFactory.getShared(SchemaConstants.MGD);
        mgdSqlMgr.setLogger(logger);

        // Create a bcp manager that has been configured for the MGD database.
        mgdBcpMgr = new BCPManager(new BCPManagerCfg("MGD"));

        // Provide the bcp manager with the SQL data manager and the logger.
        mgdBcpMgr.setSQLDataManager(mgdSqlMgr);
        mgdBcpMgr.setLogger(logger);

        // Create a stream for handling MGD DAO objects.
        //mgdStream = new BCP_Batch_Stream(mgdSqlMgr, mgdBcpMgr);
        //mgdStream = new BCP_Inline_Stream(mgdSqlMgr, mgdBcpMgr);
        updateScriptCfg = new ScriptWriterCfg("MGD");
        updateScriptWriter = new ScriptWriter(updateScriptCfg, mgdSqlMgr);
        mgdStream = new BCP_Script_Stream(updateScriptWriter, mgdBcpMgr);

        /**
         * Set up RDR stream
         */
        // Create a SQLDataManager for the Radar database from the factory.
        rdrSqlMgr = SQLDataManagerFactory.getShared(SchemaConstants.RADAR);

        // Create a bcp manager that has been configured for the MGD database.
        rdrBcpMgr = new BCPManager(new BCPManagerCfg("RADAR"));

        // Provide the bcp manager with the SQL data manager and the logger.
        rdrBcpMgr.setSQLDataManager(rdrSqlMgr);
        rdrBcpMgr.setLogger(logger);

        // Create qc reporter
        rdrStream = new BCP_Inline_Stream(rdrSqlMgr, rdrBcpMgr);
        qcReporter = new SeqQCReporter(rdrStream);

        seqResolver = new GBSeqAttributeResolver();
        if (loadMode.equals(SeqloaderConstants.INCREM_INITIAL_LOAD_MODE)) {
            seqProcessor = new IncremSeqProcessor(mgdStream,
                                                  rdrStream,
                                                  seqResolver);
                                                  //mergeSplitProcessor,
                                                  //repeatSeqWriter);
        }
        else if (loadMode.equals(SeqloaderConstants.INCREM_LOAD_MODE)) {
            mergeSplitProcessor = new MergeSplitProcessor(qcReporter);

            // Note: here I want to use the default prefixing, so normally
            // wouldn't need to pass a Configurator, but the ScriptWriter(sqlMgr)
            // is a protected constructor
            mergeSplitScriptCfg = new ScriptWriterCfg();
            mergeSplitScriptWriter = new ScriptWriter(mergeSplitScriptCfg, mgdSqlMgr);

            // passing in a null merge split processor until it is tested
            seqProcessor = new IncremSeqProcessor(mgdStream,
                                                  rdrStream,
                                                  qcReporter,
                                                  seqResolver,
                                                  mergeSplitProcessor,
                                                  repeatSeqWriter);

            // sequence loader exception factory
            eFactory = new SeqloaderExceptionFactory();
            try {
                repeatSeqWriter = new BufferedWriter(
                    new FileWriter(loadCfg.getRepeatFileName()));
            }
            catch (IOException e) {
                SeqloaderException e1 =
                    (SeqloaderException) eFactory.getException(
                        SeqloaderExceptionFactory.RepeatFileIOException, e);
                throw e1;
            }

        }
        else if (loadMode.equals(SeqloaderConstants.DELETE_RELOAD_MODE)) {
            seqProcessor = new DRSeqProcessor(mgdStream, seqResolver);
        }
    }
    /**
     * what this method does ...
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return
     * @throws ConfigException if problem creating MergeSplitProcessor or
     *          IncremSeqProcessor
     * @throws CacheExeption if problem creating MergeSplitProcessor or
     *         IncremSeqProcessor
     * @throws DBException if problem creating MergeSplitProcessor or
     *         IncremSeqProcessor
     * @throws KeyNotFoundException calling MergeSplitProcessor
     * @throws MSException if problem creating IncremSeqProcessor
     * @throws IOUException calling SequenceInput iterator.next()
     * @throws TranslationException - not really thrown because translators are
     *         set to return null instead of raising an exception
     * @throws If error creating writer for or writing to repeatSequenceWriter
     */

    private void load ()
        throws ConfigException, CacheException, DBException,
    KeyNotFoundException, IOUException, DLALoggingException,
     MSException, TranslationException, ScriptException,
     SeqloaderException {

        // DEBUG stuff
        // Timing the load
        Stopwatch loadStopWatch = new Stopwatch();
        loadStopWatch.start();

        // For memory usage
        Runtime runTime = Runtime.getRuntime();

        // Timing individual sequence processing
        Stopwatch sequenceStopWatch = new Stopwatch();


        long runningFreeMemory = 0;
        long currentFreeMemory = 0;
        int seqCtr = 0;

        // Data object representing the current record in the input
        SequenceInput si;

        // number of valid sequences WITHOUT processing errors:
        int passedCtr = 0;

        // number of valid sequences WITH processing errors
        int errCtr = 0;

        // get the next record
        while (iterator.hasNext()) {
          sequenceStopWatch.reset();
          sequenceStopWatch.start();
          try {

              // interpret next record

              si = (SequenceInput) iterator.next();
              //System.out.println(si.getPrimaryAcc().getAccID());
          }
          catch (RecordFormatException e) {
              logger.logdErr(e.getMessage());
              errCtr++;
              continue;
          }
          // Note:
          // Exceptions that rise from resolving accessions are thrown
          // out to main; indicates a bad LogicalDB value in the config file
          // Exceptions that rise resolving reference associations are
          // are thrown out to main; indicates a logicalDB other than MEDLINE
          // or PubMed

          try {
            if (loadMode.equals(SeqloaderConstants.INCREM_INITIAL_LOAD_MODE)) {
                seqProcessor.processAddEvent(si);
            }
            else {
              seqProcessor.processSequence(si);
            }

            //DEBUG
            seqCtr = passedCtr + errCtr;
            currentFreeMemory = runTime.freeMemory();
            runningFreeMemory = runningFreeMemory + currentFreeMemory;
            if (seqCtr  > 0 && seqCtr % 1000 == 0) {
                logger.logdInfo("Processed " + seqCtr + " input records", false);
                //System.gc();
                //logger.logdInfo("Total Memory Available to the VM: " + runTime.totalMemory(), false);
                //logger.logdInfo("Free Memory Available: " + currentFreeMemory, false);
            }
          }
          // if we can't resolve SEQ_Sequence attributes, go to the next
          // sequence in the input
          catch (SequenceResolverException e) {
            logger.logdErr(e.getMessage() + " Sequence: " + si.getPrimaryAcc().getAccID());
            errCtr++;
            continue;
          }
          // if we can't resolve the source for a sequence, go to the next
          // sequence in the input
          catch (MSException e) {
            logger.logdErr(e.getMessage() + " Sequence: " + si.getPrimaryAcc().getAccID());
            errCtr++;
            continue;
          }
          // if we've found a repeated sequence in the input, go to the next
          // sequence in the input
          catch (RepeatSequenceException e) {
            logger.logdInfo(e.getMessage() + " Sequence: " + si.getPrimaryAcc().getAccID(), true);
            continue;
          }
          catch (ChangedOrganismException e) {
            logger.logdInfo(e.getMessage() + " Sequence: " + si.getPrimaryAcc().getAccID(), true);
            errCtr++;
            continue;
          }
          catch (ChangedLibraryException e) {
            logger.logdInfo(e.getMessage() + " Sequence: " + si.getPrimaryAcc().getAccID(), true);
            errCtr++;
            continue;

          }

          passedCtr++;
          // Too much of a dog to do every sequence
          //System.gc();
          sequenceStopWatch.stop();
          logger.logdInfo("MEM&TIME: " + (passedCtr + errCtr) + "\t" + currentFreeMemory + "\t" + sequenceStopWatch.time(), false);
        }
        loadStopWatch.stop();
        double totalLoadTime = loadStopWatch.time();

        // report total time for RefSeqloader.load()
        logger.logdDebug("Total RefSeqloader.load() time in seconds: " + totalLoadTime +
                         " time in minutes: " + (totalLoadTime/60));

        // report Sequence Lookup execution times
        seqCtr = passedCtr + errCtr;
        logger.logdDebug("Total Valid Sequences Processed = " + seqCtr + " and " + errCtr + " had errors");
        logger.logdDebug("Average Processing Time/Sequence = " + (totalLoadTime / seqCtr));
        if (seqCtr > 0) {
          logger.logdDebug("Average SequenceLookup time = " +
                           (seqProcessor.runningLookupTime / seqCtr));

          logger.logdDebug("Greatest SequenceLookup time = " + seqProcessor.highLookupTime);
          logger.logdDebug("Least SequenceLookup time = " + seqProcessor.lowLookupTime);
          // report MSProcessor execution times
          logger.logdDebug("Average MSProcessor time = " +
                         (seqProcessor.runningMSPTime / seqCtr));
          logger.logdDebug("Greatest MSProcessor time = " + seqProcessor.highMSPTime);
          logger.logdDebug("Least MSProcessor time = " + seqProcessor.lowMSPTime);
          // report free memory average
          logger.logdDebug("Average Free Memory = " + runningFreeMemory / seqCtr);
          logger.logdDebug("Organism Decider Counts:");
        }
        Vector deciderCts = organismChecker.getDeciderCounts();
        for (Iterator i = deciderCts.iterator(); i.hasNext();) {
            logger.logdDebug((String)i.next());
        }
        logger.logdDebug("Prefix Decider Counts:");
        deciderCts = prefixChecker.getDeciderCounts();
        for (Iterator i = deciderCts.iterator(); i.hasNext();) {
            logger.logdDebug((String)i.next());
        }

        logger.logdDebug("Processing Merge/Splits");
        // Process merges and splits if we have a MergeSplitProcessor
        if(mergeSplitProcessor != null) {
              mergeSplitProcessor.process(mergeSplitScriptWriter);
              mergeSplitScriptWriter.execute();
        }
        logger.logdDebug("Finished processing Merge/Splits");

        // processes inserts, deletes and updates to the database; method depends
        // on the type of stream
        mgdStream.close();

        // process qc inserts to the radar database
        rdrStream.close();
        if (loadMode.equals(SeqloaderConstants.INCREM_LOAD_MODE)) {
            // close the repeat sequence writer
            try {
              repeatSeqWriter.close();
            }
            catch (IOException e) {
              SeqloaderException e1 =
                  (SeqloaderException) eFactory.getException(
                      SeqloaderExceptionFactory.RepeatFileIOException, e);
              throw e1;
          }
        }
    }
}
// $Log
