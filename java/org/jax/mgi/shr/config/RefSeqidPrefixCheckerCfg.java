// $Header
//  $Name

package org.jax.mgi.shr.config;

import org.jax.mgi.shr.config.Configurator;
import org.jax.mgi.shr.config.ConfigException;

/**
 * @is an object that retrieves Configuration pararmeters for a
 *     RefSeqidPrefixChecker
 * @has Nothing
 *   <UL>
 *   <LI> a configuration manager
 *   </UL>
 * @does
 *   <UL>
 *   <LI> provides methods to retrieve Configuration parameters
 *          that are specific to a RefSeqidPrefixChecker
 *   </UL>
 * @company The Jackson Laboratory
 * @author sc
 * @version 1.0
 */


public class RefSeqidPrefixCheckerCfg extends Configurator {
    /**
     * Constructs a configurator for a RefSeqidPrefixChecker object
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @throws ConfigException if a configuration manager cannot be obtained
     */

    public RefSeqidPrefixCheckerCfg() throws ConfigException {

    }

    /**
     * Gets whether to load NM prefixes or not
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return Boolean true if we are loading NM prefix
     * @throws ConfigException if "NM" not found in configuration file
     */

    public Boolean getNM() throws ConfigException {
        return new Boolean(getConfigString("NM"));
    }

    /**
     * Gets whether to load NR prefixes or not
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return Boolean true if we are loading NR prefix
     * @throws ConfigException if "NR" not found in configuration file
     */

    public Boolean getNR() throws ConfigException {
        return new Boolean(getConfigString("NR"));
    }
    /**
     * Gets whether to load NP prefixes or not
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return Boolean true if we are loading NP prefix
     * @throws ConfigException if "NP" not found in configuration file
     */

    public Boolean getNP() throws ConfigException {
        return new Boolean(getConfigString("NP"));
    }

    /**
     * Gets whether to load NC prefixes or not
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return Boolean true if we are loading NC prefix
     * @throws ConfigException if "NC" not found in configuration file
     */

    public Boolean getNC() throws ConfigException {
        return new Boolean(getConfigString("NC"));
    }

    /**
     * Gets whether to load NG prefixes or not
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return Boolean true if we are loading NG prefix
     * @throws ConfigException if "NG" not found in configuration file
     */

    public Boolean getNG() throws ConfigException {
        return new Boolean(getConfigString("NG"));
    }

    /**
     * Gets whether to load NT prefixes or not
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return Boolean true if we are loading NT prefix
     * @throws ConfigException if "NT" not found in configuration file
     */

    public Boolean getNT() throws ConfigException {
        return new Boolean(getConfigString("NT"));
    }

    /**
     * Gets whether to load NW prefixes or not
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return Boolean true if we are loading NW prefix
     * @throws ConfigException if "NW" not found in configuration file
     */

    public Boolean getNW() throws ConfigException {
        return new Boolean(getConfigString("NW"));
    }

    /**
     * Gets whether to load NZ prefixes or not
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return Boolean true if we are loading NZ prefix
     * @throws ConfigException if "NZ" not found in configuration file
     */

    public Boolean getNZ() throws ConfigException {
        return new Boolean(getConfigString("NZ"));
    }

    /**
     * Gets whether to load ZP prefixes or not
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return Boolean true if we are loading ZP prefix
     * @throws ConfigException if "ZP" not found in configuration file
     */

    public Boolean getZP() throws ConfigException {
        return new Boolean(getConfigString("ZP"));
    }

    /**
     * Gets whether to load XM prefixes or not
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return Boolean true if we are loading XM prefix
     * @throws ConfigException if "XM" not found in configuration file
     */

    public Boolean getXM() throws ConfigException {
        return new Boolean(getConfigString("XM"));
    }

    /**
     * Gets whether to load XR prefixes or not
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return Boolean true if we are loading XR prefix
     * @throws ConfigException if "XR" not found in configuration file
     */

    public Boolean getXR() throws ConfigException {
        return new Boolean(getConfigString("XR"));
    }
    /**
     * Gets whether to load XP prefixes or not
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return Boolean true if we are loading XP prefix
     * @throws ConfigException if "XP" not found in configuration file
     */

    public Boolean getXP() throws ConfigException {
        return new Boolean(getConfigString("XP"));
    }

}

//  $Log

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
