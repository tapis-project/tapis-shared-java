package edu.utexas.tacc.tapis.shared.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.utexas.tacc.tapis.client.shared.exceptions.TapisClientException;
import edu.utexas.tacc.tapis.files.client.FilesClient;
import edu.utexas.tacc.tapis.files.client.gen.model.FileInfo;
import edu.utexas.tacc.tapis.files.client.gen.model.FileTypeEnum;
import edu.utexas.tacc.tapis.shared.exceptions.runtime.TapisRuntimeException;
import edu.utexas.tacc.tapis.shared.i18n.MsgUtils;

/** Return the list of files in the subtree rooted at the path supplied on the
 * constructor.  This class performs a depth-first search of the subtree's directory 
 * structure, returning all files retrieved the system specified on the constructor.
 * 
 * @author rcardone
 */
public class FilesListSubtree 
{
    /* ********************************************************************** */
    /*                                Constants                               */
    /* ********************************************************************** */
    // Tracing.
    private static final Logger _log = LoggerFactory.getLogger(FilesListSubtree.class);
    
    // Initial capacities.
    private static final int INITIAL_LIST_SIZE  = 500;
    private static final int INITIAL_STACK_SIZE = 50; 
    
    // Retrieval block size.
    private static final int LIMIT = 1000;
    
    // Files service constants.
    private static final String DIR  = "dir";
    private static final String FILE = "file";

    /* ********************************************************************** */
    /*                                 Fields                                 */
    /* ********************************************************************** */
    // Initial parameters.
    private final FilesClient _filesClient;
    private final String      _systemId;
    private final String      _path;
    
    // Work collections.
    private final ArrayList<FileInfo> _resultList = new ArrayList<>(INITIAL_LIST_SIZE);
    private final ArrayDeque<String>  _dirStack   = new ArrayDeque<>(INITIAL_STACK_SIZE);
    
    // Dynamically set when in a shared application context.
    private String _sharedAppCtx;
    
    /* ********************************************************************** */
    /*                              Constructors                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* constructor:                                                           */
    /* ---------------------------------------------------------------------- */
    public FilesListSubtree(FilesClient filesClient, String systemId, String path)
    {
        // ------------------------- Check Input -------------------------
        if (filesClient == null) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "listSubtree", "filesClient");
            throw new TapisRuntimeException(msg);
        }
        if (StringUtils.isBlank(systemId)) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "listSubtree", "systemId");
            throw new TapisRuntimeException(msg);
        }
        // Empty strings are allowed.
        if (path == null) {
            String msg = MsgUtils.getMsg("TAPIS_NULL_PARAMETER", "listSubtree", "path");
            throw new TapisRuntimeException(msg);
        }
        
        _filesClient = filesClient;
        _systemId    = systemId;
        _path        = path;
    }
    
    /* ********************************************************************** */
    /*                            Public Methods                              */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* list:                                                                  */
    /* ---------------------------------------------------------------------- */
    public List<FileInfo> list() throws TapisClientException
    {
        // ------------------------- Walk the Tree -----------------------
        // List parameters that don't change.
        final int offset = 0;
        final boolean recurse = false;
        
        // Make the initial call to the top level recursive method.
        listPath(_path, LIMIT, offset, recurse);
        
        // Process all directories previously discovered.
        while (!_dirStack.isEmpty()) listPath(_dirStack.pop(), LIMIT, offset, recurse);
        
        // Return the trimmed list of files in the subtree.
        _resultList.trimToSize();
        return _resultList;
    }
    

    /* ---------------------------------------------------------------------- */
    /* getSharedAppCtx:                                                       */
    /* ---------------------------------------------------------------------- */
    public String getSharedAppCtx() {return _sharedAppCtx;}

    /* ---------------------------------------------------------------------- */
    /* setSharedAppCtx:                                                       */
    /* ---------------------------------------------------------------------- */
    public void setSharedAppCtx(String sharedAppCtx) {_sharedAppCtx = sharedAppCtx;}
    
    /* ********************************************************************** */
    /*                            Private Methods                             */
    /* ********************************************************************** */
    /* ---------------------------------------------------------------------- */
    /* listPath:                                                              */
    /* ---------------------------------------------------------------------- */
    private void listPath(String path, int limit, int offset, boolean recurse) 
     throws TapisClientException
    {
    	// Disallow impersonation.
        final String nullImpersonationId = null;
        
        // Get the list for the current path.
        int numRetrieved = 0;
        while (true) {
            // Trace for performance info.
            if (_log.isDebugEnabled())
                _log.debug(MsgUtils.getMsg("FILES_LIST_SUBTREE", _systemId, path, 
                                           limit, offset, recurse, _sharedAppCtx));
                
            // Make the remote call to the Files service.
            List<FileInfo> list = 
                _filesClient.listFiles(_systemId, path, limit, offset+numRetrieved, recurse, nullImpersonationId, _sharedAppCtx);
            
            // Process result list.
            for (var item : list) {
                if (FileTypeEnum.DIR == item.getType())
                    _dirStack.push(item.getPath());
                else if (FileTypeEnum.FILE == item.getType())
                    _resultList.add(item);
                else 
                    if (_log.isWarnEnabled())
                       _log.warn(MsgUtils.getMsg("FILES_IGNORE_ITEM_TYPE", item.getType(), 
                                                 item.getPath()));
            }
            
            // Are there possibly more items to retrieve on this path?
            if (list.size() == limit) {
                numRetrieved += limit;
                continue;    
            }
            
            // We're done on this path.
            break;
        }
    }
}
