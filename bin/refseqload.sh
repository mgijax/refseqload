#!/bin/sh
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
#      - Common configuration file -
#		/usr/local/mgiconfig/master.config.sh
#      - RefSeq load configuration file - refseqload.config
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
CONFIG_LOAD=`pwd`/refseqload.config

#
#  Make sure the configuration file is readable.
#
if [ ! -r ${CONFIG_LOAD} ]
then
    echo "Cannot read configuration file: ${CONFIG_LOAD}" | tee -a ${LOG}
    exit 1
fi

#
# Source the GenBank Load configuration files
#
. ${CONFIG_LOAD}

#
#  Make sure the master configuration file is readable
#

if [ ! -r ${CONFIG_MASTER} ]
then
    echo "Cannot read configuration file: ${CONFIG_MASTER}"
    exit 1
fi

echo "javaruntime:${JAVARUNTIMEOPTS}"
echo "classpath:${CLASSPATH}"
echo "dbserver:${MGD_DBSERVER}"
echo "database:${MGD_DBNAME}"

#
#  Source the DLA library functions.
#
if [ "${DLAJOBSTREAMFUNC}" != "" ]
then
    if [ -r ${DLAJOBSTREAMFUNC} ]
    then
        . ${DLAJOBSTREAMFUNC}
    else
        echo "Cannot source DLA functions script: ${DLAJOBSTREAMFUNC}"
        exit 1
    fi
else
    echo "Environment variable DLAJOBSTREAMFUNC has not been defined."
fi

#
# Function that runs to java load
#

run ()
{
    #
    # log time and input files to process
    #
    echo "" >> ${LOG_PROC}
    echo "`date`" >> ${LOG_PROC}
    echo "Files read from stdin: ${APP_CAT_METHOD} ${APP_INFILES}" | \
	tee -a ${LOG_DIAG} ${LOG_PROC}
    #
    # run refseqload
    #
    ${APP_CAT_METHOD}  ${APP_INFILES}  | \
	${JAVA} ${JAVARUNTIMEOPTS} -classpath ${CLASSPATH} \
	-DCONFIG=${CONFIG_MASTER},${CONFIG_LOAD} \
	-DJOBKEY=${JOBKEY} ${DLA_START}

    STAT=$?
    checkStatus ${STAT} "${REFSEQLOAD}"
}

##################################################################
##################################################################
#
# main
#
##################################################################
##################################################################

#
# createArchive including OUTPUTDIR, startLog, getConfigEnv, get job key
#
preload ${OUTPUTDIR}


#
# rm all files/dirs from OUTPUTDIR and RPTDIR
#
cleanDir ${OUTPUTDIR} ${RPTDIR}

# if we are processing the non-cums (incremental mode)
# get a set of files, 1 file or set < configured value in MB (compressed)

echo "checking APP_RADAR_INPUT: ${APP_RADAR_INPUT}"

if [ ${APP_RADAR_INPUT} = true -a ${SEQ_LOAD_MODE} = incremental ]
then
    echo 'Getting files to Process' | tee -a ${LOG_DIAG}
    # set the input files to empty string
    APP_INFILES=""

    APP_INFILES=`${RADAR_DBUTILS}/bin/getFilesToProcess.csh \
        ${RADAR_DBSCHEMADIR} ${JOBSTREAM} ${SEQ_PROVIDER} ${APP_RADAR_MAX}` 
    STAT=$?
    checkStatus ${STAT} "${RADAR_DBUTILS}/bin/getFilesToProcess.csh"

    # if no input files report and shutdown gracefully
    if [ "${APP_INFILES}" = "" ]
    then
        echo "No files to process" | tee -a ${LOG_PROC} ${LOG_DIAG}
        shutDown
        exit 0
    fi

    echo 'Done getting files to Process' | tee -a ${LOG_DIAG}

fi

# save to new variable, when repeats are processed APP_INFILES
# use FILES_PROCESSED to log processed files
FILES_PROCESSED=${APP_INFILES}

# if we get here then APP_INFILES not set in configuration this is an error
if [ "${APP_INFILES}" = "" ]
then
    # set STAT for endJobStream.py called from postload in shutDown
    STAT=1
    checkStatus ${STAT} "APP_RADAR_INPUT=${APP_RADAR_INPUT}. SEQ_LOAD_MODE=${SEQ_LOAD_MODE}. Check that  APP_INFILES has been configured."
fi

#
# run the load
#
run

#
# run any repeat files if configured to do so
#
ctr=1
if [ ${APP_PROCESS_REPEATS} = true ]
then
    while [ -s ${SEQ_REPEAT_FILE} ]
    # while repeat file exists and is not length 0
    do
	# rename the repeat file
	mv ${SEQ_REPEAT_FILE} ${APP_REPEAT_TO_PROCESS}

	# set the cat method
	APP_CAT_METHOD=/usr/bin/cat

	# set the input file name
	APP_INFILES=${APP_REPEAT_TO_PROCESS}

	# run the load
	run

        echo "saving repeat file ${APP_REPEAT_TO_PROCESS}.${ctr}"
        mv ${APP_REPEAT_TO_PROCESS} ${APP_REPEAT_TO_PROCESS}.${ctr}
        ctr=`expr ${ctr} + 1`
    done
fi

# update serialization on mgi_reference_assoc, seq_source_assoc
cat - <<EOSQL | ${PG_DBUTILS}/bin/doisql.csh $0 | tee -a ${LOG_DIAG}

select setval('mgi_reference_assoc_seq', (select max(_Assoc_key) + 1 from MGI_Reference_Assoc));
select setval('seq_source_assoc_seq', (select max(_Assoc_key) + 1 from SEQ_Source_Assoc));

EOSQL

# if we are processing the non-cums (incremental mode)
# log the non-cums we processed
if [  ${APP_RADAR_INPUT} = true -a ${SEQ_LOAD_MODE} = incremental ]
then
    echo "Logging processed files ${FILES_PROCESSED}" >> ${LOG_DIAG}
    for file in ${FILES_PROCESSED}
    do
        ${RADAR_DBUTILS}/bin/logProcessedFile.csh ${RADAR_DBSCHEMADIR} \
            ${JOBKEY} ${file} ${SEQ_PROVIDER}
        STAT=$?
	checkStatus ${STAT} "${RADAR_DBUTILS}/bin/logProcessedFile.csh"
    done
    echo 'Done logging processed files' >> ${LOG_DIAG}
fi

#
# run postload cleanup and email logs
#
shutDown

exit 0
