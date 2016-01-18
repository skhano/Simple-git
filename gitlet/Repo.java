package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

/**
 * Backbone of all the branches.
 * @author Tim Chan, Sam Khano
 *
 */
public class Repo implements Serializable {

    /** Default constructor. */
    public Repo() {
        File staged = new File(Main.getStagedDir());
        File commits = new File(Main.getCommitDir());
        File removed = new File(Main.getRemovedDir());
        staged.mkdir();
        commits.mkdir();
        removed.mkdir();
        branchInfo = new HashMap<>();
        remoteServer = new HashMap<>();
        branchInfo.put("master", null);
    }

    /** Temp Repo object constructor to be used for loading.
     *  @param t temp variable */
    public Repo(String t) {
    }

    /** Getter method for the name of current branch.
     * @return name of the current branch.
     */
    public String getCurrentBranchName() {
        return currentBranchName;
    }

    /** Getter method for the SHA-1 of the head commit.
     * @return name of the current commit.
     */
    public String getCurrentCommit() {
        return getCommit(currentBranchName);
    }

    /** Return the branch information. */
    public HashMap<String, String> getBranchInfo() {
        return branchInfo;
    }

    /** Setter method for the name of current branch.
     * @param currentBranch name of the current branch.
     */
    public void setCurrentBranch(String currentBranch) {
        this.currentBranchName = currentBranch;
    }

    /** Getter method for the node SHA associated with the BranchName.
     * @param branchName name of the branch.
     * @return SHA associated with the BranchName.
     */
    public String getCommit(String branchName) {
        assert branchInfo.containsKey(branchName);
        return branchInfo.get(branchName);
    }

    /**
     * Create a new branch.
     * @param branchName name of the branch.
     */
    public void newBranch(String branchName) {
        if (branchInfo.containsKey(branchName)) {
            Main.error("A branch with that name already exists.");
        }
        branchInfo.put(branchName, branchInfo.get(currentBranchName));
    }

    /**
     * Remove an existing branch. Abort if you are deleting the current branch.
     * @param branchName name of the branch to be deleted.
     */
    public void removeBranch(String branchName) {
        if (!branchInfo.containsKey(branchName)) {
            Main.error("A branch with that name does not exist.");
        } else if (currentBranchName.equals(branchName)) {
            Main.error("Cannot remove the current branch.");
        }
        branchInfo.remove(branchName);
    }

    /**
     * Updates the current branch to point to the new commit.
     * @param commitId SHA1 of the commit.
     */
    public void updateBranch(String commitId) {
        branchInfo.put(currentBranchName, commitId);
    }

    /**
     * Updates the branch to point to the new commit.
     * @param branchName name of the branch
     * @param commitId SHA1 of the commit.
     */
    public void updateBranch(String branchName, String commitId) {
        branchInfo.put(branchName, commitId);
    }

    /**
     * Check if branch exists.
     * @param branchName name of the branch
     * @return boolean
     */
    public boolean containsBranch(String branchName) {
        return branchInfo.containsKey(branchName);
    }


    /** Name of current branch.*/
    private String currentBranchName = "master";

    /** Map<Branch name, SHA1 of the branch's head commit>.*/
    private HashMap<String, String> branchInfo;

    /** Map<Remote name, string of remote path>. */
    private HashMap<String, String> remoteServer;

    /**
     * Add a new remove server.
     * @param remoteName name of the remote sever
     * @param remoteAddress address of the remote server
     */
    public void addRemote(String remoteName, String remoteAddress) {
        if (remoteServer.containsKey(remoteName)) {
            Main.error("A remote with that name already exists.");
        }
        remoteServer.put(remoteName, remoteAddress);
    }

    /**
     * Removing a server.
     * @param remoteName Name of the remote server
     */
    public void removeRemote(String remoteName) {
        if (!remoteServer.containsKey(remoteName)) {
            Main.error("A remote with that name does not exist.");
        }
        remoteServer.remove(remoteName);
    }

    /**
     * Check if this repo contains the name of the given remote server.
     * @param remoteName remote server name
     * @return boolean
     */
    public boolean containsRemote(String remoteName) {
        return remoteServer.containsKey(remoteName);
    }

    /**
     * Getter for the unresolved repo path with given name.
     * @param remoteName name of the remote repo
     * @return unresolved path as string
     */
    public String getRepoPath(String remoteName) {
        return remoteServer.get(remoteName);
    }

}
