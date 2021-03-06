package org.jax.mgi.app.refseqload;

import java.util.*;
import java.util.regex.*;
import java.sql.*;

import org.jax.mgi.shr.dla.input.SequenceInterpreter;
import org.jax.mgi.shr.dla.input.SequenceInput;
import org.jax.mgi.shr.dla.loader.seq.SeqloaderConstants;
import org.jax.mgi.shr.dla.input.DateConverter;
import org.jax.mgi.dbs.mgd.loads.Acc.AccessionRawAttributes;
import org.jax.mgi.dbs.mgd.loads.SeqRefAssoc.RefAssocRawAttributes;
import org.jax.mgi.dbs.mgd.loads.Seq.SequenceRawAttributes;
import org.jax.mgi.shr.dla.input.genbank.GBFormatInterpreter;
import org.jax.mgi.shr.dla.input.genbank.GBOrganismChecker;
import org.jax.mgi.shr.config.ConfigException;
import org.jax.mgi.shr.ioutils.RecordFormatException;
import org.jax.mgi.shr.stringutil.StringLib;
import org.jax.mgi.dbs.mgd.loads.SeqSrc.MSRawAttributes;

    /**
     * @is An object that parses a GenBank sequence record and obtains values
     *     from a Configurator to create a SequenceInput data object.<BR>
     *     Determines if a GenBank sequence record is valid.
     * @has
     *   <UL>
     *   <LI>A SequenceInput object into which it bundles:
     *   <LI>A SequenceRawAttributes object
     *   <LI>An AccessionRawAttributes object for its primary seqid
     *   <LI>One AccessionRawAttributes object for each secondary seqid
     *   <LI> A RefAssocRawAttributes object for each reference that has a
     *        PubMed and/or Medline id
     *   <LI> A MSRawAttributes
     *   <LI> A set of String constants for parsing
     *   </UL>
     * @does
     *   <UL>
     *   <LI>Determines if a GenBank sequence record is valid
     *   <LI>Parses a GenBank sequence record
     *   </UL>
     * @company The Jackson Laboratory
     * @author sc
     * @version 1.0
     */

public class RefSequenceInterpreter extends GBFormatInterpreter {
        private SequenceInput seqInput;
        private RefSeqidPrefixChecker prefixChecker;

        public RefSequenceInterpreter(GBOrganismChecker oc, RefSeqidPrefixChecker pc)
            throws ConfigException {
            super(oc);
            prefixChecker = pc;
        }

        /**
         * Parses a sequence record and  creates a SequenceInput object from
         * Configuration and parsed values. Sets sequence Quality for RefSeq
         * sequences by seqid prefix
         * @assumes Nothing
         * @effects Nothing
         * @param rcd A sequence record
         * @return A SequenceInput object representing 'rcd'
         * @throws RecordFormatException if we can't parse an attribute because of
         *         record formatting errors
         * @notes See http://www.ncbi.nlm.nih.gov/RefSeq/key.html for a key
         * to the RefSeq accession format
         */
        public Object interpret(String rcd) throws RecordFormatException {
            seqInput = (SequenceInput)super.interpret(rcd);
            String seqid = seqInput.getPrimaryAcc().getAccID();
            if (prefixChecker.isPrefix(seqid, "NM") == true ||
                prefixChecker.isPrefix(seqid, "NR") == true ||
                prefixChecker.isPrefix(seqid, "NP") == true) {
                seqInput.getSeq().setQuality(SeqloaderConstants.HIGH_QUAL);
            }
            else if (prefixChecker.isPrefix(seqid, "NZ") == true ||
                     prefixChecker.isPrefix(seqid, "ZP") == true) {
                seqInput.getSeq().setQuality(SeqloaderConstants.LOW_QUAL);
            }
            else {
                seqInput.getSeq().setQuality(SeqloaderConstants.MED_QUAL);
            }
            return seqInput;
        }

        /**
         * Determines whether this sequence is for an organism and has a seqid
         * prefix we want to interpret
         * interpret
         * @assumes Nothing
         * @effects Nothing
         * @param record A GenBank sequence record
         * @return true if we want to load this sequence
         * @throws Nothing
         */

        public boolean isValid(String record) {
            boolean isValidOrganism = super.isValid(record);
            // if it is an organism we are interested in, check seqid prefix
            if (isValidOrganism == true) {
                return (prefixChecker.checkPrefix(record));
            }
            else {
                return isValidOrganism;
            }
        }
    }
