#!/bin/sh
#
#  $Header
#  $Name
#
#  refseqload.sh
###########################################################################
#
#  Purpose:  This script controls the execution of the RefSeq Sequence Load
#
#  Usage:
#
#      refseqload.sh
#
#  Env Vars:
#
#      See the configuration file
#
#  Inputs:
#
#      - Common configuration file (/usr/local/mgi/etc/common.config.sh)
#      - RefSeq load configuration file (refseqload.config)
#      - One or more RefSeq input files 
#
#  Outputs:
#
#      - An archive file
#      - Log files defined by the environment variables ${LOG_PROC},
#        ${LOG_DIAG}, ${LOG_CUR} and ${LOG_VAL}
#      - BCP files for for inserts to each database table to be loaded
#      - SQL script file for updates
#      - Records written to the database tables
#      - Exceptions written to standard error
#      - Configuration and initialization errors are written to a log file
#        for the shell script
#
#  Exit Codes:
#
#      0:  Successful completion
#      1:  Fatal error occurred
#      2:  Non-fatal error occurred
#
#  Assumes:  Nothing
#
#  Implementation:  
#
#  Notes:  None
#
###########################################################################

#
#  Set up a log file for the shell script in case there is an error
#  during configuration and initialization.
#
cd `dirname $0`/..
LOG=`pwd`/refseqload.log
rm -f ${LOG}

#
#  Verify the argument(s) to the shell script.
#
if [ $# -ne 0 ]
then
    echo "Usage: $0" | tee -a ${LOG}
    exit 1
fi

#
#  Establish the configuration file names.
#
CONFIG_COMMON=`pwd`/common.config.sh
CONFIG_REFSEQLOAD=`pwd`/refseqload.config
echo ${CONFIG_REFSEQLOAD}

#
#  Make sure the configuration files are readable.
#
if [ ! -r ${CONFIG_COMMON} ]
then
    echo "Cannot read configuration file: ${CONFIG_COMMON}" | tee -a ${LOG}
    exit 1
fi
if [ ! -r ${CONFIG_REFSEQLOAD} ]
then
    echo "Cannot read configuration file: ${CONFIG_REFSEQLOAD}" | tee -a ${LOG}
    exit 1
fi

#
#  Concatenate the configuration files together to produce one configuration
#  file that can be run to set up the environment.
#
CONFIG_RUNTIME=`pwd`/runtime.config
cat ${CONFIG_COMMON} ${CONFIG_REFSEQLOAD} > ${CONFIG_RUNTIME}
. ${CONFIG_RUNTIME}

echo "javaruntime:${JAVARUNTIMEOPTS}"
echo "classpath:${CLASSPATH}"
echo "dbserver:${MGD_DBSERVER}"
echo "database:${MGD_DBNAME}"

#
#  Include the DLA library functions.
#
. ${DLAFUNCTIONS}

#
#  Function that performs cleanup tasks for the job stream prior to
#  termination.
#
shutDown ()
{
    #
    # call DLA library function
    #
    postload

    #
    #  Mail the logs to the support staff.
    #
    if [ "${MAIL_LOG_PROC}" != "" ]
    then
        mailLog ${MAIL_LOG_PROC} "RefSeq Load - Process Summary Log" \
	    ${LOG_PROC} | tee -a ${LOG}
    fi

    if [ "${MAIL_LOG_CUR}" != "" ]
    then
        mailLog ${MAIL_LOG_CUR} "RefSeq Load - Curator Summary Log" \
	    ${LOG_CUR} | tee -a ${LOG}
    fi
}

#
# Function that runs to java load
#

run ()
{
    #
    # log time and input files to process
    #
    echo "\n`date`" >> ${LOG_PROC}
    echo "Files from stdin: ${CAT_METHOD} ${PIPED_INFILES}" | tee -a ${LOG_DIAG} 
    #
    # run refseqload
    #
    ${CAT_METHOD}  ${PIPED_INFILES}  | \
	${JAVA_RUN} ${JAVARUNTIMEOPTS} -classpath ${CLASSPATH} \
	-DCONFIG=${CONFIG_REFSEQLOAD} -DJOBKEY=${JOBKEY} ${REFSEQLOAD_APP} | \
	tee -a  ${LOGDIR}/stdouterr

    STAT=$?
    if [ ${STAT} -ne 0 ]
    then
	echo "refseqload processing failed.  \
	    Return status: ${STAT}" >> ${LOG_PROC}
	shutDown
	exit 1
    fi
}

##################################################################
# main
##################################################################

#
# createArchive, startLog, getConfigEnv, get job key
#
preload

#
# need to partition if these tables are empty
#
echo 'Partitioning ACC_Accession, SEQ_Sequence, SEQ_Source_Assoc'
${MGD_DBSCHEMADIR}/partition/ACC_Accession_create.object
${MGD_DBSCHEMADIR}/partition/SEQ_Sequence_create.object
${MGD_DBSCHEMADIR}/partition/SEQ_Source_Assoc_create.object

#
# run the load
#
run

#
# run any repeat files if configured to do so
#
if [ ${PROCESS_REPEATS} = true ]
then
    while [ -s ${SEQ_REPEAT_FILE} ]
    # while repeat file exists and is not length 0
    do
	# rename the repeat file
	mv ${SEQ_REPEAT_FILE} ${REPEAT_TO_PROCESS}

	# set the cat method
	CAT_METHOD=/usr/bin/cat

	# set the input file name
	PIPED_INFILES=${REPEAT_TO_PROCESS}

	# run the load
	run

	# remove the repeat file we just ran
	echo "Removing ${REPEAT_TO_PROCESS}"
	rm ${REPEAT_TO_PROCESS}
    done

fi

#
# run msp qc reports
#
${MSP_QCRPT} ${RADAR_DBSCHEMADIR} ${MGD_DBNAME} ${JOBKEY} ${RPTDIR}
STAT=$?
if [ ${STAT} -ne 0 ]
then
    echo "Running MSP QC reports failed.  Return status: ${STAT}" >> ${LOG_PROC}
    shutDown
    exit 1
fi

#
# run seqload qc reports
#
${SEQLOAD_QCRPT} ${RADAR_DBSCHEMADIR} ${MGD_DBNAME} ${JOBKEY} ${RPTDIR}
STAT=$?
if [ ${STAT} -ne 0 ]
then
    echo "Running seqloader QC reports failed.  Return status: ${STAT}" >> ${LOG_PROC}
    shutDown
    exit 1
fi

#
# run postload cleanup and email logs
#
shutDown

exit 0

$Log

###########################################################################
#
# Warranty Disclaimer and Copyright Notice
#
#  THE JACKSON LABORATORY MAKES NO REPRESENTATION ABOUT THE SUITABILITY OR
#  ACCURACY OF THIS SOFTWARE OR DATA FOR ANY PURPOSE, AND MAKES NO WARRANTIES,
#  EITHER EXPRESS OR IMPLIED, INCLUDING MERCHANTABILITY AND FITNESS FOR A
#  PARTICULAR PURPOSE OR THAT THE USE OF THIS SOFTWARE OR DATA WILL NOT
#  INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS, OR OTHER RIGHTS.
#  THE SOFTWARE AND DATA ARE PROVIDED "AS IS".
#
#  This software and data are provided to enhance knowledge and encourage
#  progress in the scientific community and are to be used only for research
#  and educational purposes.  Any reproduction or use for commercial purpose
#  is prohibited without the prior express written permission of The Jackson
#  Laboratory.
#
# Copyright \251 1996, 1999, 2002, 2003 by The Jackson Laboratory
#
# All Rights Reserved
#
###########################################################################
