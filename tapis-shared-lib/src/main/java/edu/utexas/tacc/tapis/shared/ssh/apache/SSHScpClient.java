package edu.utexas.tacc.tapis.shared.ssh.apache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.sshd.scp.client.ScpClient;
import org.apache.sshd.scp.client.ScpClient.Option;
import org.apache.sshd.scp.client.ScpClientCreator;
import org.apache.sshd.scp.common.ScpTransferEventListener;
import org.apache.sshd.scp.common.helpers.DefaultScpFileOpener;
import org.apache.sshd.scp.common.helpers.ScpTimestampCommandDetails;

import edu.utexas.tacc.tapis.shared.exceptions.runtime.TapisRuntimeException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;

/** This class uses the Apache scp facility to move files and directory between
 * hosts.  When a source directories is specified, its contents are always 
 * recursively copied.  To avoid recursive copy, using methods that take an
 * array of source files and include only the files you want to copy. 
 * 
 * No Apache data types are exposed on this interface.
 * 
 * @author rcardone
 */
public class SSHScpClient 
{
    /* ********************************************************************** */
    /*                               Constants                                */
    /* ********************************************************************** */
    // Define commonly used permissions for upload.
    public static final List<PosixFilePermission> DEFAULT_PERMS = initDefaultPerms();
    public static final List<PosixFilePermission> RWRW_PERMS    = initRWRWPerms();
    public static final List<PosixFilePermission> RWXRWX_PERMS  = initRWXRWXPerms();
    
    /* ********************************************************************** */
    /*                                Fields                                  */
    /* ********************************************************************** */
    // The parent connection.
    private final SSHConnection _sshConnection; 
    private final ScpClient     _scpClient;
    
    /* ********************************************************************** */
    /*                            Constructors                                */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    /** This constructor does not establish a callback listener.
     * 
     * @param sshConnection non-null connection
     * @throws RuntimeException if connection is null
     */
    SSHScpClient(SSHConnection sshConnection)
     throws RuntimeException
    {
        // Check input.
        if (sshConnection == null) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "SSHScpClient", "sshConnection");
            throw new TapisRuntimeException(msg);
        }
        _sshConnection = sshConnection;
        _scpClient = ScpClientCreator.instance().createScpClient(_sshConnection.getSession());
    }
    
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    /** This constructor allows the caller specify a callback listener to 
     * capture file and directory events.
     * 
     * @param sshConnection non-null connection
     * @param listener custom listener or null
     * @throws RuntimeException invalid input
     */
    SSHScpClient(SSHConnection sshConnection,  ScpTransferEventListener listener)
     throws RuntimeException
    {
        // Check input.
        if (sshConnection == null) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "SSHScpClient", "sshConnection");
            throw new TapisRuntimeException(msg);
        }
        
        // Assign the no-op event listener when null.  Otherwise, 
        // check that the listener is not a proxy.
        if (listener == null) listener = ScpTransferEventListener.EMPTY;
          else ScpTransferEventListener.validateListener(listener);
        
        // Assign the required fields.
        _sshConnection = sshConnection;
        _scpClient = ScpClientCreator.instance().createScpClient(
            _sshConnection.getSession(), DefaultScpFileOpener.INSTANCE, listener);
    }
    
    /* ********************************************************************** */
    /*                            Public Methods                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* downloadFileToFile:                                                    */
    /* ---------------------------------------------------------------------- */
    public void downloadFileToFile(String remoteFile, String localFile, 
                                   boolean preserveAttributes)
     throws IOException
    {
        if (preserveAttributes)
            _scpClient.download(remoteFile, localFile, Option.PreserveAttributes);
        else 
            _scpClient.download(remoteFile, localFile);
    }

    /* ---------------------------------------------------------------------- */
    /* downloadFileToDir:                                                     */
    /* ---------------------------------------------------------------------- */
    public void downloadFileToDir(String remoteFile, String localDir, 
                                  boolean preserveAttributes)
     throws IOException
    {
        if (preserveAttributes)
            _scpClient.download(remoteFile, localDir, Option.PreserveAttributes, 
                                Option.TargetIsDirectory);
        else 
            _scpClient.download(remoteFile, localDir, Option.TargetIsDirectory);
    }

    /* ---------------------------------------------------------------------- */
    /* downloadFilesToDir:                                                    */
    /* ---------------------------------------------------------------------- */
    public void downloadFilesToDir(String[] remoteFiles, String localDir, 
                                   boolean preserveAttributes)
     throws IOException
    {
        if (preserveAttributes)
            _scpClient.download(remoteFiles, localDir, Option.PreserveAttributes, 
                                Option.TargetIsDirectory);
        else 
            _scpClient.download(remoteFiles, localDir, Option.TargetIsDirectory);
    }

    /* ---------------------------------------------------------------------- */
    /* downloadDirToDir:                                                      */
    /* ---------------------------------------------------------------------- */
    public void downloadDirToDir(String remoteDir, String localDir, 
                                 boolean preserveAttributes)
     throws IOException
    {
        if (preserveAttributes)
            _scpClient.download(remoteDir, localDir, Option.PreserveAttributes, 
                                Option.TargetIsDirectory, Option.Recursive);
        else 
            _scpClient.download(remoteDir, localDir, Option.TargetIsDirectory,
                                Option.Recursive);
    }

    /* ---------------------------------------------------------------------- */
    /* downloadFileToStream:                                                  */
    /* ---------------------------------------------------------------------- */
    public void downloadFileToStream(String remoteFile, OutputStream ostream)
     throws IOException
    {
        _scpClient.download(remoteFile, ostream);
    }

    /* ---------------------------------------------------------------------- */
    /* downloadFileToBytes:                                                   */
    /* ---------------------------------------------------------------------- */
    public byte[] downloadFileToBytes(String remoteFile)
     throws IOException
    {
        return _scpClient.downloadBytes(remoteFile);
    }
    
    /* ---------------------------------------------------------------------- */
    /* uploadFileToFile:                                                      */
    /* ---------------------------------------------------------------------- */
    public void uploadFileToFile(String localFile, String remoteFile, 
                                 boolean preserveAttributes)
     throws IOException
    {
        if (preserveAttributes)
            _scpClient.upload(localFile, remoteFile, Option.PreserveAttributes);
        else 
            _scpClient.upload(localFile, remoteFile);
    }
    
    /* ---------------------------------------------------------------------- */
    /* uploadFileToDir:                                                       */
    /* ---------------------------------------------------------------------- */
    public void uploadFileToDir(String localFile, String remoteDir, 
                                boolean preserveAttributes)
     throws IOException
    {
        if (preserveAttributes)
            _scpClient.upload(localFile, remoteDir, Option.PreserveAttributes, 
                              Option.TargetIsDirectory);
        else 
            _scpClient.upload(localFile, remoteDir, Option.TargetIsDirectory);
    }
    
    /* ---------------------------------------------------------------------- */
    /* uploadFilesToDir:                                                      */
    /* ---------------------------------------------------------------------- */
    public void uploadFilesToDir(String[] localFiles, String remoteDir, 
                                 boolean preserveAttributes)
     throws IOException
    {
        if (preserveAttributes)
            _scpClient.upload(localFiles, remoteDir, Option.PreserveAttributes, 
                              Option.TargetIsDirectory);
        else 
            _scpClient.upload(localFiles, remoteDir, Option.TargetIsDirectory);
    }

    /* ---------------------------------------------------------------------- */
    /* uploadDirToDir:                                                        */
    /* ---------------------------------------------------------------------- */
    public void uploadDirToDir(String localDir, String remoteDir, 
                                boolean preserveAttributes)
     throws IOException
    {
        if (preserveAttributes)
            _scpClient.upload(localDir, remoteDir, Option.PreserveAttributes, 
                              Option.TargetIsDirectory, Option.Recursive);
        else 
            _scpClient.upload(localDir, remoteDir, Option.TargetIsDirectory,
                              Option.Recursive);
    }
    
    /* ---------------------------------------------------------------------- */
    /* uploadBytesToFile:                                                     */
    /* ---------------------------------------------------------------------- */
    /** Upload an array of bytes to a remote file.  Standard posix permissions
     * and timestamps can be specified.  If permissions are null, the default
     * permission of owner r/w are applied.  If the time argument is null, the
     * file's last modified and last access time are set to the current time.
     * The times are always expressed as epoch milliseconds. 
     * 
     * Use SSHSftpClient method if the input size is not known in advance.
     * 
     * @param bytes the bytes to be written
     * @param remoteFile the target file
     * @param perms null or the posix permission assigned to the file
     * @param times null or the file's last modification and access times
     * @throws IOException on error
     */
    public void uploadBytesToFile(byte[] bytes, String remoteFile, 
                                  Collection<PosixFilePermission> perms,
                                  FileTimes times)
     throws IOException
    {
        // Assign default permission if none given.
        if (perms == null) perms = DEFAULT_PERMS;
        
        // Assign the times if none given.
        if (times == null) times = new FileTimes();
        var timeDetails = 
            new ScpTimestampCommandDetails(times.lastModifiedMillis, times.lastAccessMillis);
        
        // Push the bytes out.
        _scpClient.upload(bytes, remoteFile, perms, timeDetails);
    }
    
    /* ---------------------------------------------------------------------- */
    /* uploadStreamToFile:                                                    */
    /* ---------------------------------------------------------------------- */
    /** Upload a stream's contents to a remote file.  Standard posix permissions
     * and timestamps can be specified.  If permissions are null, the default
     * permission of owner r/w are applied.  If the time argument is null, the
     * file's last modified and last access time are set to the current time.
     * The times are always expressed as epoch milliseconds. 
     * 
     * Use SSHSftpClient methods if the input size is not known in advance.
     * 
     * @param stream input stream
     * @param remoteFile the target file
     * @param size the number of bytes to be written
     * @param perms null or the posix permission assigned to the file
     * @param times null or the file's last modification and access times
     * @throws IOException on error
     */
    public void uploadStreamToFile(InputStream stream, String remoteFile, long size, 
                                   Collection<PosixFilePermission> perms,
                                   FileTimes times)
     throws IOException
    {
        // Assign default permission if none given.
        if (perms == null) perms = DEFAULT_PERMS;
        
        // Assign the times if none given.
        if (times == null) times = new FileTimes();
        var timeDetails = 
            new ScpTimestampCommandDetails(times.lastModifiedMillis, times.lastAccessMillis);
        
        // Upload the specified number of bytes.
        _scpClient.upload(stream, remoteFile, size, perms, timeDetails);
    }
    
    /* ---------------------------------------------------------------------- */
    /* getConnection:                                                         */
    /* ---------------------------------------------------------------------- */
    public SSHConnection getConnection() {return _sshConnection;}

    /* ********************************************************************** */
    /*                           Private Methods                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* initDefaultPerms:                                                      */
    /* ---------------------------------------------------------------------- */
    private static List<PosixFilePermission> initDefaultPerms()
    {
        final int capacity = 2;
        var list = new ArrayList<PosixFilePermission>(capacity);
        list.add(PosixFilePermission.OWNER_READ);
        list.add(PosixFilePermission.OWNER_WRITE);
        return Collections.unmodifiableList(list);
    }
    
    /* ---------------------------------------------------------------------- */
    /* initRWRWPerms:                                                         */
    /* ---------------------------------------------------------------------- */
    private static List<PosixFilePermission> initRWRWPerms()
    {
        final int capacity = 4;
        var list = new ArrayList<PosixFilePermission>(capacity);
        list.add(PosixFilePermission.OWNER_READ);
        list.add(PosixFilePermission.OWNER_WRITE);
        list.add(PosixFilePermission.GROUP_READ);
        list.add(PosixFilePermission.GROUP_WRITE);
        return Collections.unmodifiableList(list);
    }
    
    /* ---------------------------------------------------------------------- */
    /* initRWXRWXPerms:                                                       */
    /* ---------------------------------------------------------------------- */
    private static List<PosixFilePermission> initRWXRWXPerms()
    {
        final int capacity = 6;
        var list = new ArrayList<PosixFilePermission>(capacity);
        list.add(PosixFilePermission.OWNER_READ);
        list.add(PosixFilePermission.OWNER_WRITE);
        list.add(PosixFilePermission.OWNER_EXECUTE);
        list.add(PosixFilePermission.GROUP_READ);
        list.add(PosixFilePermission.GROUP_WRITE);
        list.add(PosixFilePermission.GROUP_EXECUTE);
        return Collections.unmodifiableList(list);
    }
    
    /* ********************************************************************** */
    /*                           FileTimes Class                              */
    /* ********************************************************************** */
    // Tapis data structure to avoid leaking apache sshd types on interface.
    public static final class FileTimes
    {
        // Epoch time in milliseconds.
        public long lastModifiedMillis;
        public long lastAccessMillis;
        
        public FileTimes() {
            Instant now = Instant.now();
            lastModifiedMillis = now.toEpochMilli();
            lastAccessMillis = now.toEpochMilli();
        }
        
        public FileTimes(long lastModifiedMillis, long lastAccessMillis) {
            this.lastModifiedMillis = lastModifiedMillis;
            this.lastAccessMillis = lastAccessMillis;
        }
    }
}
