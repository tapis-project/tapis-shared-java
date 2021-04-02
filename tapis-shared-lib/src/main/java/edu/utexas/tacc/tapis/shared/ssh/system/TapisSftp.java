package edu.utexas.tacc.tapis.shared.ssh.system;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import edu.utexas.tacc.tapis.shared.ssh.SSHConnection;
import edu.utexas.tacc.tapis.systems.client.gen.model.TSystem;

public final class TapisSftp 
 extends TapisAbstractConnection
{
    /* **************************************************************************** */
    /*                                   Constants                                  */
    /* **************************************************************************** */
    // Local logger.
    private static final Logger _log = LoggerFactory.getLogger(TapisSftp.class);
    
    // Default error stream buffer size.
    private static final int DEFAULT_RESULT_LEN = 1024;
    private static final int DEFAULT_READ_BUFFER_LEN = DEFAULT_RESULT_LEN;
    
    // Various useful posix permission settings.
    public static final int RWRW = 0660;
    public static final int RWXRWX = 0770;
    
    /* **************************************************************************** */
    /*                                    Fields                                    */
    /* **************************************************************************** */
    // Channel created on demand.
    private ChannelSftp          _sftpChannel;
    
    /* **************************************************************************** */
    /*                                Constructors                                  */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* constructor:                                                                 */
    /* ---------------------------------------------------------------------------- */
    public TapisSftp(TSystem system)
    {
        // Save system in superclass.
        super(system);
    }

    /* ---------------------------------------------------------------------------- */
    /* constructor:                                                                 */
    /* ---------------------------------------------------------------------------- */
    public TapisSftp(TSystem system, SSHConnection conn)
    {
        // Save system and connection in superclass.
        super(system, conn);
    }

    /* **************************************************************************** */
    /*                                Public Methods                                */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* openChannel:                                                                 */
    /* ---------------------------------------------------------------------------- */
    /** Create an sftp channel and a connection if one does not already exist.
     * 
     * @throws IOException
     * @throws TapisException
     */
    public void openChannel() throws IOException, TapisException
    {
        getSftpChannel();
    }
    
    /* ---------------------------------------------------------------------------- */
    /* closeChannel:                                                                */
    /* ---------------------------------------------------------------------------- */
    /** Close the channel and reset this object to its original post-construction 
     * state.  A new channel can be opened on this object after calling this method.
     * This method is idempotent. 
     */
    public void closeChannel() 
    {
        closeChannel(_sftpChannel, false);
        _sftpChannel = null;
    }
    
    /* ---------------------------------------------------------------------------- */
    /* closeChannelAndConnection:                                                   */
    /* ---------------------------------------------------------------------------- */
    /** Close the channel and reset this object to its original post-construction 
     * state.  The underlying connection to the system is also closed.  A new channel 
     * can be opened on this object after calling this method.  This method is idempotent. 
     */
    public void closeChannelAndConnection() 
    {
        closeChannel(_sftpChannel, true);
        _sftpChannel = null;
    }
    
    /* ---------------------------------------------------------------------------- */
    /* put:                                                                         */
    /* ---------------------------------------------------------------------------- */
    /** Copy the inputstream's contents to the remote destination.  The destination is
     * always a file and if it already exists it will be overwritten.  The input stream 
     * is not closed.
     * 
     * @param src the input stream
     * @param dest the destination file
     * @throws IOException
     * @throws TapisException
     * @throws SftpException
     */
    public void put(InputStream src, String dest) 
     throws IOException, TapisException, SftpException
    {
        // Initialize the connection if necessary.
        var sftpChannel = getSftpChannel();
        sftpChannel.put(src, dest, ChannelSftp.OVERWRITE);
    }

    /* ---------------------------------------------------------------------------- */
    /* put:                                                                         */
    /* ---------------------------------------------------------------------------- */
    /** Copy the input file or directory's contents to the remote destination.  
     * 
     * @param src the input path name
     * @param dest the destination path name
     * @throws IOException
     * @throws TapisException
     * @throws SftpException
     */
    public void put(String src, String dest) 
     throws IOException, TapisException, SftpException
    {
        // Initialize the connection if necessary.
        var sftpChannel = getSftpChannel();
        sftpChannel.put(src, dest, ChannelSftp.OVERWRITE);
    }

    /* ---------------------------------------------------------------------------- */
    /* chmod:                                                                       */
    /* ---------------------------------------------------------------------------- */
    /** Change the permission on a file or directory on a remote system.
     * 
     * @param mod the posize permissions
     * @param path the remote file or directory
     * @throws IOException
     * @throws TapisException
     * @throws SftpException
     */
    public void chmod(int mod, String path) 
     throws IOException, TapisException, SftpException
    {
        // Initialize the connection if necessary.
        var sftpChannel = getSftpChannel();
        sftpChannel.chmod(mod, path);
    }
    
    /* ---------------------------------------------------------------------------- */
    /* ls :                                                                         */
    /* ---------------------------------------------------------------------------- */
    /** Copy the input file or directory's contents to the remote destination.  
     * 
     * @param src the input path name
     * @param dest the destination path name
     * @throws IOException
     * @throws TapisException
     * @throws SftpException
     */
    public void ls(String pathName) 
     throws IOException, TapisException, SftpException
    {
        // Initialize the connection if necessary.
        var sftpChannel = getSftpChannel();
        sftpChannel.ls(pathName);
    }

    /* **************************************************************************** */
    /*                               Private Methods                                */
    /* **************************************************************************** */
    /* ---------------------------------------------------------------------------- */
    /* getSftpChannel:                                                              */
    /* ---------------------------------------------------------------------------- */
    private ChannelSftp getSftpChannel() throws IOException, TapisException
    {
        // Is the channel already up and running?
        if (_sftpChannel != null) return _sftpChannel;
        
        // Get or establish a connection to the system.
        var conn = getConnection();
        
        // Create the channel but don't connect it yet.
        _sftpChannel = (ChannelSftp) conn.createChannel("sftp");
        return _sftpChannel;
    }
}
