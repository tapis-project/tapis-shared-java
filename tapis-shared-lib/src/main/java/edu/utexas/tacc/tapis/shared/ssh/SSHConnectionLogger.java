package edu.utexas.tacc.tapis.shared.ssh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SSHConnectionLogger 
 implements com.jcraft.jsch.Logger
{
    // Actual logger.
    private static final Logger _log = LoggerFactory.getLogger(SSHConnectionLogger.class);
    
    /** Assume all Jsch logging is allowed.
     */
    @Override
    public boolean isEnabled(int level) {return true;}

    /** Write debugging statements. 
     */
    @Override
    public void log(int level, String message) {_log.debug(message);}
}
