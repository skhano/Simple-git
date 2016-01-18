package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

/**
 * Commit class for the gitlet version-control system.
 * @author Sam Khano Tim Chan
 */

public class Commit implements Serializable {

    /**
     * Creates an initial commit with respective metadata
     * and no parent.
     * @param message log
     * @param time stamp
     */
    public Commit(String message, String time) {
        _logMessage = message;
        _time = time;
        _blobs = new HashMap<>();
        _tracked = new TreeSet<>();
        _parent = null;
    }

    /**
     * Creates a commit with respective metadata and parent commit.
     * @param message m
     * @param time t
     * @param parent p
     */
    public Commit(String message, String time, String parent) {
        this(message, time);
        _parent = parent;
    }

    /** Dummy Commit constructor for loading a commit. */
    public Commit(String dummy) {
        _logMessage = "";
        _time = "";
        _blobs = new HashMap<>();
        _tracked = new TreeSet<>();
        _parent = null;
    }

    /**
     * Return the hashId for the commit object.
     */
    public String hashId() {
        return Utils.sha1("commits", _logMessage, _time, "x77" + _parent);
    }

    /** Get a blob map for this Commit.
     *  @return blobs hash map */
    public HashMap<String, String> getBlobs() {
        return _blobs;
    }

    /** Set the mappings between staged files and respective blobs
     *  as well as the files this commit tracks. */
    public void setBlobs() {
        Commit parent = null;
        TreeSet<String> parentTracked = null;
        List<String> stagedFiles = Utils.plainFilenamesIn(Main.getStagedDir());
        List<String> removedFiles = Utils
                .plainFilenamesIn(Main.getRemovedDir());
        if (stagedFiles.size() == 0 && removedFiles.size() == 0
                && _parent != null) {
            Main.error("No changes added to the commit.");
        }

        if (_parent != null) {
            parent = Utils.loadObj(this, Main.getCommitDir() + _parent);
            parentTracked = parent._tracked;
            for (String tracked : parentTracked) {
                if (!stagedFiles.contains(tracked)
                        && !removedFiles.contains(tracked)) {
                    _blobs.put(tracked, parent._blobs.get(tracked));
                    _tracked.add(tracked);
                }
            }
        }

        for (String file : stagedFiles) {
            File f = new File(Main.getStagedDir() + file);
            byte[] blob = Utils.readContents(f);
            String blobHash = Utils.sha1("blobs", blob);
            addBlob(blob, file, blobHash);
            f.delete();
        }

        for (String remove : removedFiles) {
            File rm = new File(Main.getRemovedDir() + remove);
            rm.delete();
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((_parent == null) ? 0 : _parent.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Commit other = (Commit) obj;
        if (_parent == null) {
            if (other._parent != null) {
                return false;
            }
        } else if (!_parent.equals(other._parent) || !_time.equals(other._time)
                || !_logMessage.equals(other._logMessage)) {
            return false;
        }
        return true;
    }

    /**
     * Create a new blob and add reference from FILE to blob.
     * @param blob contents
     * @param file name
     * @param hash blob hash code
     */
    public void addBlob(byte[] blob, String file, String hash) {
        _tracked.add(file);
        Utils.writeContents(new File(Main.getGitletDir() + hash), blob);
        _blobs.put(file, hash);
    }

    /** Get the time stamp for this Commit.
     *  @return time stamp*/
    public String getTime() {
        return _time;
    }

    /** Set the time stamp for this Commit.
     *  @param time time stamp*/
    public void setTime(String time) {
        _time = time;
    }

    /** Get the log message for this Commit.
     *  @return log message */
    public String getLogMessage() {
        return _logMessage;
    }

    /** Set the log message for this Commit.
     *  @param logMessage log message
     */
    public void setLogMessage(String logMessage) {
        _logMessage = logMessage;
    }

    /** Get the parent Commit (its SHA-1 code).
     *  @return parent SHA-1*/
    public String getParent() {
        return _parent;
    }

    /** Set the parent for this Commit.
     *  @param parent SHA-1 code for parent
     */
    public void setParent(String parent) {
        _parent = parent;
    }

    /**
     * Returns this commit's set of tracked files.
     * @return tracked files
     */
    public TreeSet<String> getTracked() {
        return _tracked;
    }

    /**
     * Return the blob corresponding to the FILE.
     * @param file name
     * @return blob SHA-1
     */
    public String getBlob(String file) {
        return _blobs.get(file);
    }

    @Override
    public String toString() {
        String ret = String.format("Commit %s%n%s%n%s%n", hashId(), _time,
                _logMessage);
        return ret;
    }

    /** Tracked files in this commit. */
    private TreeSet<String> _tracked;

    /** Mapping between file names and blobs (using SHA-1 code). */
    private HashMap<String, String> _blobs;

    /** Time stamp. */
    private String _time;

    /** Log message for the commit. */
    private String _logMessage;

    /** Parent commit's SHA-1 code. */
    private String _parent;
}
