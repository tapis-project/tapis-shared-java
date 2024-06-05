package edu.utexas.tacc.tapis.shared.ssh.apache;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import edu.utexas.tacc.tapis.shared.exceptions.TapisException;
import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.sftp.client.SftpClient.Attributes;
import org.apache.sshd.sftp.client.SftpClient.CloseableHandle;
import org.apache.sshd.sftp.client.SftpClient.CopyMode;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.apache.sshd.sftp.client.SftpClient.Handle;
import org.apache.sshd.sftp.client.SftpClient.OpenMode;
import org.apache.sshd.sftp.client.SftpVersionSelector;
import org.apache.sshd.sftp.client.impl.DefaultSftpClient;
import org.apache.sshd.sftp.client.impl.DefaultSftpClientFactory;

import edu.utexas.tacc.tapis.shared.exceptions.runtime.TapisRuntimeException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class delegates all real work to the default Apache SftpClient class.
 * Apache sftp does not support the familiar get and put sftp commands.  Use
 * SSHScpClient to copy files and directories between hosts; use this class to
 * do more complicated i/o and run other command like mkdir and stat.
 * 
 * Some Apache data types are exposed on this interface.  See these links for
 * further information:
 * 
 *  https://javadoc.io/doc/org.apache.sshd/sshd-sftp/latest/index.html
 *  https://github.com/apache/mina-sshd
 * 
 * @author rcardone
 */
public class SSHSftpClient
 implements AutoCloseable, Closeable, SSHSession
{
    Logger log = LoggerFactory.getLogger(SSHSftpClient.class);

    // Fields.
    private final SSHConnection     _sshConnection;
    private final DefaultSftpClient _sftpClient;
    
    // Constructor.
    SSHSftpClient(SSHConnection sshConnection) throws IOException
    {
        // Check input.
        if (sshConnection == null) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "SSHSftpClient", "sshConnection");
            throw new TapisRuntimeException(msg);
        }
       
        _sshConnection = sshConnection;
        // See if we still have a connection and session, if not restart one.
        try {
            if (_sshConnection.isClosed()) {
                _sshConnection.restart();
            }
        } catch (TapisException ex) {
            String msg = MsgUtils.getMsg("SSH_POOL_UNABLE_TO_CREATE_SFTP_CLIENT", sshConnection.getHost(),
                    sshConnection.getPort(), sshConnection.getUsername(), sshConnection.getAuthMethod());
            throw new IOException(msg, ex);
        }
        var session  = _sshConnection.getSession();
        if (session == null) {
            String msg =  MsgUtils.getMsg("TAPIS_SSH_NO_SESSION");
            throw new TapisRuntimeException(msg);
        }
        long begin = System.currentTimeMillis();
        _sftpClient = (DefaultSftpClient)
            DefaultSftpClientFactory.INSTANCE.createSftpClient(session);
        log.error("***-*** Session took this long --------------------------> " + (System.currentTimeMillis() - begin));
    }
    
    public SSHConnection getConnection() {return _sshConnection;}

    public boolean isClosing() {
        return _sftpClient.isClosing();
    }

    public boolean isOpen() {
        return _sftpClient.isOpen();
    }

    public void close() throws IOException {
        // request close on sftpClient.  This will gracefully shutdown the connection, but it takes
        // time.
        _sftpClient.close();

        // wait 100 ms for close (max 10 times ... after that just give up).  I think this will be
        // way more than enough, but we could do some tuning if necessary in the future.  The main thing
        // is that we need to wait a bit, but we don't want o wait forever.
        for(int i=0;i<10;i++) {
            if(!_sftpClient.getClientChannel().isClosed()) {
                try {
                    log.trace(String.format("Waiting for sftpClient to close %d", i));
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                // already clsoed, so break out of loop
                break;
            }
        }

        // this is jjust some logging that could come in handy for debugging purposes
        if(_sftpClient.isClosing()) {
            // there's really nothing we can do here but it would be interesting to
            // know when it happens.
            log.trace("SftpClient is in the \"isClosing\" state");
        }

        // this is jjust some logging that could come in handy for debugging purposes
        if(!_sftpClient.getClientChannel().isClosed()) {
            // there's really nothing we can do here but it would be interesting to
            // know when it happens.
            log.error("SftpClient is NOT in the \"isClosed\" state");
        }

        // this is jjust some logging that could come in handy for debugging purposes
        if(_sftpClient.isOpen()) {
            // there's really nothing we can do here but it would be interesting to
            // know when it happens.
            log.error("SftpClient is in the \"isOpen\" state");
        }
    }
    
    public String getName() {
        return _sftpClient.getName();
    }

    public int send(int cmd, Buffer buffer) throws IOException {
        return _sftpClient.send(cmd, buffer);
    }

    public Buffer receive(int id) throws IOException {
        return _sftpClient.receive(id);
    }

    public Buffer receive(int id, long idleTimeout) throws IOException {
        return _sftpClient.receive(id, idleTimeout);
    }

    public Buffer receive(int id, Duration idleTimeout) throws IOException {
        return _sftpClient.receive(id, idleTimeout);
    }

    public CloseableHandle open(String path) throws IOException {
        return _sftpClient.open(path);
    }

    public CloseableHandle open(String path, OpenMode... options) throws IOException {
        return _sftpClient.open(path, options);
    }

    public int negotiateVersion(SftpVersionSelector selector) throws IOException {
        return _sftpClient.negotiateVersion(selector);
    }

    public void rename(String oldPath, String newPath) throws IOException {
        _sftpClient.rename(oldPath, newPath);
    }

    public void rename(String oldPath, String newPath, CopyMode... options) throws IOException {
        _sftpClient.rename(oldPath, newPath, options);
    }

    public int read(Handle handle, long fileOffset, byte[] dst) throws IOException {
        return _sftpClient.read(handle, fileOffset, dst);
    }

    public int read(Handle handle, long fileOffset, byte[] dst, AtomicReference<Boolean> eofSignalled)
            throws IOException {
        return _sftpClient.read(handle, fileOffset, dst, eofSignalled);
    }

    public int read(Handle handle, long fileOffset, byte[] dst, int dstOffset, int len) throws IOException {
        return _sftpClient.read(handle, fileOffset, dst, dstOffset, len);
    }

    public void write(Handle handle, long fileOffset, byte[] src) throws IOException {
        _sftpClient.write(handle, fileOffset, src);
    }

    public List<DirEntry> readDir(Handle handle) throws IOException {
        return _sftpClient.readDir(handle);
    }

    public CloseableHandle open(String path, Collection<OpenMode> options) throws IOException {
        return _sftpClient.open(path, options);
    }

    public void symLink(String linkPath, String targetPath) throws IOException {
        _sftpClient.symLink(linkPath, targetPath);
    }

    public FileChannel openRemotePathChannel(String path, OpenOption... options) throws IOException {
        return _sftpClient.openRemotePathChannel(path, options);
    }

    public FileChannel openRemotePathChannel(String path, Collection<? extends OpenOption> options) throws IOException {
        return _sftpClient.openRemotePathChannel(path, options);
    }

    public FileChannel openRemoteFileChannel(String path, OpenMode... modes) throws IOException {
        return _sftpClient.openRemoteFileChannel(path, modes);
    }

    public void close(Handle handle) throws IOException {
        _sftpClient.close(handle);
    }

    public void remove(String path) throws IOException {
        _sftpClient.remove(path);
    }

    public Collection<DirEntry> readEntries(String path) throws IOException {
        return _sftpClient.readEntries(path);
    }

    public void rename(String oldPath, String newPath, Collection<CopyMode> options) throws IOException {
        _sftpClient.rename(oldPath, newPath, options);
    }

    public InputStream read(String path) throws IOException {
        return _sftpClient.read(path);
    }

    public InputStream read(String path, int bufferSize) throws IOException {
        return _sftpClient.read(path, bufferSize);
    }

    public InputStream read(String path, OpenMode... mode) throws IOException {
        return _sftpClient.read(path, mode);
    }

    public InputStream read(String path, int bufferSize, OpenMode... mode) throws IOException {
        return _sftpClient.read(path, bufferSize, mode);
    }

    public OutputStream write(String path) throws IOException {
        return _sftpClient.write(path);
    }

    public OutputStream write(String path, int bufferSize) throws IOException {
        return _sftpClient.write(path, bufferSize);
    }

    public OutputStream write(String path, OpenMode... mode) throws IOException {
        return _sftpClient.write(path, mode);
    }

    public OutputStream write(String path, int bufferSize, OpenMode... mode) throws IOException {
        return _sftpClient.write(path, bufferSize, mode);
    }

    public int read(Handle handle, long fileOffset, byte[] dst, int dstOffset, int len,
            AtomicReference<Boolean> eofSignalled) throws IOException {
        return _sftpClient.read(handle, fileOffset, dst, dstOffset, len, eofSignalled);
    }

    public void write(Handle handle, long fileOffset, byte[] src, int srcOffset, int len) throws IOException {
        _sftpClient.write(handle, fileOffset, src, srcOffset, len);
    }

    public void mkdir(String path) throws IOException {
        _sftpClient.mkdir(path);
    }

    public void rmdir(String path) throws IOException {
        _sftpClient.rmdir(path);
    }

    public CloseableHandle openDir(String path) throws IOException {
        return _sftpClient.openDir(path);
    }

    public List<DirEntry> readDir(Handle handle, AtomicReference<Boolean> eolIndicator) throws IOException {
        return _sftpClient.readDir(handle, eolIndicator);
    }

    public String canonicalPath(String path) throws IOException {
        return _sftpClient.canonicalPath(path);
    }

    public Attributes stat(String path) throws IOException {
        return _sftpClient.stat(path);
    }

    public Attributes lstat(String path) throws IOException {
        return _sftpClient.lstat(path);
    }

    public Attributes stat(Handle handle) throws IOException {
        return _sftpClient.stat(handle);
    }

    public void setStat(String path, Attributes attributes) throws IOException {
        _sftpClient.setStat(path, attributes);
    }

    public void setStat(Handle handle, Attributes attributes) throws IOException {
        _sftpClient.setStat(handle, attributes);
    }

    public String readLink(String path) throws IOException {
        return _sftpClient.readLink(path);
    }

    public void link(String linkPath, String targetPath, boolean symbolic) throws IOException {
        _sftpClient.link(linkPath, targetPath, symbolic);
    }

    public void lock(Handle handle, long offset, long length, int mask) throws IOException {
        _sftpClient.lock(handle, offset, length, mask);
    }

    public void unlock(Handle handle, long offset, long length) throws IOException {
        _sftpClient.unlock(handle, offset, length);
    }

    public Iterable<DirEntry> readDir(String path) throws IOException {
        return _sftpClient.readDir(path);
    }

    public Iterable<DirEntry> listDir(Handle handle) throws IOException {
        return _sftpClient.listDir(handle);
    }

    public FileChannel openRemoteFileChannel(String path, Collection<OpenMode> modes) throws IOException {
        return _sftpClient.openRemoteFileChannel(path, modes);
    }

    public InputStream read(String path, int bufferSize, Collection<OpenMode> mode) throws IOException {
        return _sftpClient.read(path, bufferSize, mode);
    }

    public InputStream read(String path, Collection<OpenMode> mode) throws IOException {
        return _sftpClient.read(path, mode);
    }

    public OutputStream write(String path, int bufferSize, Collection<OpenMode> mode) throws IOException {
        return _sftpClient.write(path, bufferSize, mode);
    }

    public OutputStream write(String path, Collection<OpenMode> mode) throws IOException {
        return _sftpClient.write(path, mode);
    }
}
