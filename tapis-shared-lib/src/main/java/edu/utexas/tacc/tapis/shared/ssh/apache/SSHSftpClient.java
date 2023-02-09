package edu.utexas.tacc.tapis.shared.ssh.apache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
 implements AutoCloseable
{
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
       
        // Get connection information.
        _sshConnection = sshConnection;
        var session  = _sshConnection.getSession();
        if (session == null) {
            _sshConnection.close(); // probably not strictly necessary
            String msg =  MsgUtils.getMsg("TAPIS_SSH_NO_SESSION");
            throw new TapisRuntimeException(msg);
        }
        
        // Create local client.
        _sftpClient = (DefaultSftpClient)
            DefaultSftpClientFactory.INSTANCE.createSftpClient(session);
    }
    
    public SSHConnection getConnection() {return _sshConnection;}

    public boolean isClosing() {
        return _sftpClient.isClosing();
    }

    public boolean isOpen() {
        return _sftpClient.isOpen();
    }

    public void close() throws IOException {
        try {_sftpClient.close();}
        catch (Exception e) {
            _sshConnection.close();
            throw e;
        }
    }
    
    public String getName() {
        return _sftpClient.getName();
    }

    public int send(int cmd, Buffer buffer) throws IOException {
        try {return _sftpClient.send(cmd, buffer);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public Buffer receive(int id) throws IOException {
        try {return _sftpClient.receive(id);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public Buffer receive(int id, long idleTimeout) throws IOException {
        try {return _sftpClient.receive(id, idleTimeout);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public Buffer receive(int id, Duration idleTimeout) throws IOException {
        try {return _sftpClient.receive(id, idleTimeout);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public CloseableHandle open(String path) throws IOException {
        try {return _sftpClient.open(path);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public CloseableHandle open(String path, OpenMode... options) throws IOException {
        try {return _sftpClient.open(path, options);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public int negotiateVersion(SftpVersionSelector selector) throws IOException {
        try {return _sftpClient.negotiateVersion(selector);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public void rename(String oldPath, String newPath) throws IOException {
        try {_sftpClient.rename(oldPath, newPath);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public void rename(String oldPath, String newPath, CopyMode... options) throws IOException {
        try {_sftpClient.rename(oldPath, newPath, options);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public int read(Handle handle, long fileOffset, byte[] dst) throws IOException {
        try {return _sftpClient.read(handle, fileOffset, dst);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public int read(Handle handle, long fileOffset, byte[] dst, AtomicReference<Boolean> eofSignalled)
            throws IOException {
        try {return _sftpClient.read(handle, fileOffset, dst, eofSignalled);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public int read(Handle handle, long fileOffset, byte[] dst, int dstOffset, int len) throws IOException {
        try {return _sftpClient.read(handle, fileOffset, dst, dstOffset, len);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public void write(Handle handle, long fileOffset, byte[] src) throws IOException {
        try {_sftpClient.write(handle, fileOffset, src);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public List<DirEntry> readDir(Handle handle) throws IOException {
        try {return _sftpClient.readDir(handle);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public CloseableHandle open(String path, Collection<OpenMode> options) throws IOException {
        try {return _sftpClient.open(path, options);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public void symLink(String linkPath, String targetPath) throws IOException {
        try {_sftpClient.symLink(linkPath, targetPath);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public FileChannel openRemotePathChannel(String path, OpenOption... options) throws IOException {
        try {return _sftpClient.openRemotePathChannel(path, options);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public FileChannel openRemotePathChannel(String path, Collection<? extends OpenOption> options) throws IOException {
        try {return _sftpClient.openRemotePathChannel(path, options);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public FileChannel openRemoteFileChannel(String path, OpenMode... modes) throws IOException {
        try {return _sftpClient.openRemoteFileChannel(path, modes);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public void close(Handle handle) throws IOException {
        try {_sftpClient.close(handle);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public void remove(String path) throws IOException {
        try {_sftpClient.remove(path);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public Collection<DirEntry> readEntries(String path) throws IOException {
        try {return _sftpClient.readEntries(path);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public void rename(String oldPath, String newPath, Collection<CopyMode> options) throws IOException {
        try {_sftpClient.rename(oldPath, newPath, options);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public InputStream read(String path) throws IOException {
        try {return _sftpClient.read(path);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public InputStream read(String path, int bufferSize) throws IOException {
        try {return _sftpClient.read(path, bufferSize);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public InputStream read(String path, OpenMode... mode) throws IOException {
        try {return _sftpClient.read(path, mode);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public InputStream read(String path, int bufferSize, OpenMode... mode) throws IOException {
        try {return _sftpClient.read(path, bufferSize, mode);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public OutputStream write(String path) throws IOException {
        try {return _sftpClient.write(path);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public OutputStream write(String path, int bufferSize) throws IOException {
        try {return _sftpClient.write(path, bufferSize);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public OutputStream write(String path, OpenMode... mode) throws IOException {
        try {return _sftpClient.write(path, mode);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public OutputStream write(String path, int bufferSize, OpenMode... mode) throws IOException {
        try {return _sftpClient.write(path, bufferSize, mode);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public int read(Handle handle, long fileOffset, byte[] dst, int dstOffset, int len,
            AtomicReference<Boolean> eofSignalled) throws IOException {
        try {return _sftpClient.read(handle, fileOffset, dst, dstOffset, len, eofSignalled);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public void write(Handle handle, long fileOffset, byte[] src, int srcOffset, int len) throws IOException {
        try {_sftpClient.write(handle, fileOffset, src, srcOffset, len);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public void mkdir(String path) throws IOException {
        try {_sftpClient.mkdir(path);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public void rmdir(String path) throws IOException {
        try {_sftpClient.rmdir(path);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public CloseableHandle openDir(String path) throws IOException {
        try {return _sftpClient.openDir(path);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public List<DirEntry> readDir(Handle handle, AtomicReference<Boolean> eolIndicator) throws IOException {
        try {return _sftpClient.readDir(handle, eolIndicator);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public String canonicalPath(String path) throws IOException {
        try {return _sftpClient.canonicalPath(path);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public Attributes stat(String path) throws IOException {
        try {return _sftpClient.stat(path);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public Attributes lstat(String path) throws IOException {
        try {return _sftpClient.lstat(path);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public Attributes stat(Handle handle) throws IOException {
        try {return _sftpClient.stat(handle);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public void setStat(String path, Attributes attributes) throws IOException {
        try {_sftpClient.setStat(path, attributes);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public void setStat(Handle handle, Attributes attributes) throws IOException {
        try {_sftpClient.setStat(handle, attributes);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public String readLink(String path) throws IOException {
        try {return _sftpClient.readLink(path);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public void link(String linkPath, String targetPath, boolean symbolic) throws IOException {
        try {_sftpClient.link(linkPath, targetPath, symbolic);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public void lock(Handle handle, long offset, long length, int mask) throws IOException {
        try {_sftpClient.lock(handle, offset, length, mask);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public void unlock(Handle handle, long offset, long length) throws IOException {
        try {_sftpClient.unlock(handle, offset, length);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public Iterable<DirEntry> readDir(String path) throws IOException {
        try {return _sftpClient.readDir(path);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public Iterable<DirEntry> listDir(Handle handle) throws IOException {
        try {return _sftpClient.listDir(handle);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public FileChannel openRemoteFileChannel(String path, Collection<OpenMode> modes) throws IOException {
        try {return _sftpClient.openRemoteFileChannel(path, modes);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public InputStream read(String path, int bufferSize, Collection<OpenMode> mode) throws IOException {
        try {return _sftpClient.read(path, bufferSize, mode);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public InputStream read(String path, Collection<OpenMode> mode) throws IOException {
        try {return _sftpClient.read(path, mode);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public OutputStream write(String path, int bufferSize, Collection<OpenMode> mode) throws IOException {
        try {return _sftpClient.write(path, bufferSize, mode);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }

    public OutputStream write(String path, Collection<OpenMode> mode) throws IOException {
        try {return _sftpClient.write(path, mode);}
        catch (Exception e) {
            try {_sftpClient.close();} catch (Exception e1) {}
            _sshConnection.close();
            throw e;
        }
    }
}
