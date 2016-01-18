package gitlet;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 * @author Sam Khano Tim Chan
 */
public class Main {

    /** Path for the .gitlet directory. */
    private static String gitletDir = ".gitlet/";

    /** Path for the default .gitlet directory. */
    static final String DEFAULT_GIT_DIR = ".gitlet/";

    /** Path to the default commit subdirectory. */
    private static String commitDir = getGitletDir() + "commits/";

    /** Path to directory for staged files. */
    private static String stagedDir = getGitletDir() + "staged/";

    /** Path to directory for removed files. */
    private static String removedDir = getGitletDir() + "removed/";

    /** Path the the repo file. */
    private static String repoP = getGitletDir() + "repo.txt";

    /**
     * Load the current repository object.
     * @return Repo obj
     */
    static Repo loadRepo() {
        return Utils.loadObj(new Repo("temp"), getGitletDir() + "repo.txt");
    }

    /**
     * Load commit with the given COMMITID.
     * @param commitId commit id
     * @return commit
     */
    static Commit loadCommit(String commitId) {
        Commit currCommit = new Commit("temp");
        currCommit = Utils.loadObj(currCommit, Main.commitDir + commitId);
        if (currCommit == null) {
            error("No commit with that id exists.");
        }
        return currCommit;
    }

    /**
     * Load current commit.
     * @param repo current repo
     * @return commit
     */
    static Commit loadCurrCommit(Repo repo) {
        String currCommitId = repo.getCurrentCommit();
        return loadCommit(currCommitId);
    }

    /**
     * init method for gitlet. 1. Create .gitlet directory. 2. Create a commit:
     * contains no files with commit message "initial commit" 3. Have a single
     * branch: master, which initially points to this initial commit, and master
     * will be the current branch.
     */
    static void init() {
        File f = new File(getGitletDir());
        if (f.mkdir()) {
            Repo gRepo = new Repo();
            Utils.saveObj(gRepo, repoP);
            commit("initial commit");
        } else {
            error("A gitlet version-control system already exists"
                    + " in the current directory.");
        }
    }

    /**
     * Add a file to the staged directory.
     * @param file added
     */
    static void add(String file) {
        File f = new File(file);
        if (!f.exists()) {
            error("File does not exist.");
        }

        File checkRemoved = new File(removedDir + file);
        if (checkRemoved.exists()) {
            checkRemoved.delete();
        }

        Repo repo = loadRepo();
        Commit currCommit = loadCurrCommit(repo);
        byte[] cpy = Utils.readContents(f);
        String cpyId = Utils.sha1("blobs", cpy);
        if (!cpyId.equals(currCommit.getBlobs().get(file))) {
            Utils.writeContents(new File(stagedDir + file), cpy);
        }
    }

    /**
     * Create a new commit with the given log message.
     * @param s log message
     */
    static void commit(String s) {
        if (s.isEmpty()) {
            error("Please enter a commit message.");
        }
        Repo gRepo = loadRepo();

        String commitParent = gRepo.getCurrentCommit();
        String timeStamp = Utils.timeStamp();
        Commit c = new Commit(s, timeStamp, commitParent);
        c.setBlobs();
        String commitHash = c.hashId();
        gRepo.updateBranch(commitHash);

        Utils.saveObj(c, commitDir + commitHash);
        Utils.saveObj(gRepo, repoP);
    }

    /**
     * Remove the file from the current working directory.
     * @param file (to be removed)
     */
    static void remove(String file) {
        Repo repo = loadRepo();
        Commit currCommit = loadCurrCommit(repo);

        File fInStaged = new File(stagedDir + file);
        boolean staged = fInStaged.exists();
        Set<String> tracked = currCommit.getTracked();
        File f = new File(file);
        if (!f.exists() && !tracked.contains(file)) {
            error("File does not exist.");
        }

        if (tracked.contains(file) || staged) {
            if (tracked.contains(file)) {
                byte[] temp = new byte[1];
                Utils.writeContents(new File(removedDir + file), temp);
                if (f.exists()) {
                    f.delete();
                }
            }
            if (staged) {
                fInStaged.delete();
            }
        } else {
            error("No reason to remove the file.");
        }
    }

    /**
     * Helper method that prints out the Commit object given.
     * @param c current commit
     */
    static void printCommit(Commit c) {
        System.out.println("===");
        System.out.println(c);
    }

    /**
     * Print log of commits of current branch.
     */
    static void log() {
        Repo gRepo = loadRepo();
        Commit currCommit = new Commit("temp");
        String currCommitId = gRepo.getCurrentCommit();
        while (currCommitId != null) {
            currCommit = Utils.loadObj(currCommit,
                    Main.commitDir + currCommitId);

            printCommit(currCommit);
            currCommitId = currCommit.getParent();
        }
    }

    /** Interface for printing out certain commit attributes. */
    interface PrintFunction {
        /**
         * Print out a commit object.
         * @param c commit
         */
        void print(Commit c);
    }

    /**
     * Helper method that searches through all commit objects in the commit
     * directory and prints the appropriate output controlled by PROC.
     * @param proc implements PrintFucntion
     */
    static void searchCommits(PrintFunction proc) {
        List<String> commits = Utils.plainFilenamesIn(Main.commitDir);
        Commit currCommit = new Commit("temp");
        for (String commitId : commits) {
            currCommit = Utils.loadObj(currCommit, Main.commitDir + commitId);
            proc.print(currCommit);
        }
    }

    /**
     * Print log of all the commits.
     */
    static void globalLog() {
        searchCommits((Commit c) -> printCommit(c));
    }

    /**
     * Prints out all commit ids with the given log message.
     * @param commitMsg log message for the commit/s
     */
    static void find(String commitMsg) {
        List<String> commits = Utils.plainFilenamesIn(Main.commitDir);
        Commit currCommit = new Commit("temp");
        boolean found = false;
        for (String commitId : commits) {
            currCommit = Utils.loadObj(currCommit, Main.commitDir + commitId);
            String log = currCommit.getLogMessage();
            if (log.contains(commitMsg)) {
                Utils.print(currCommit.hashId());
                found = true;
            }
        }
        if (!found) {
            error("Found no commit with that message.");
        }
    }

    /**
     * Print out the stauts of the current repo.
     */
    static void status() {
        Repo repo = loadRepo();
        Commit currCommit = loadCurrCommit(repo);

        String currBranch = repo.getCurrentBranchName();
        Set<String> branches = repo.getBranchInfo().keySet();
        TreeSet<String> orderedBranches = new TreeSet<>();
        for (String branch : branches) {
            orderedBranches.add(branch);
        }
        System.out.println("=== Branches ===");
        for (String branch : orderedBranches) {
            if (currBranch.equals(branch)) {
                System.out.println("*" + branch);
            } else {
                System.out.println(branch);
            }
        }

        System.out.println();
        System.out.println("=== Staged Files ===");

        List<String> stagedFiles = Utils.plainFilenamesIn(stagedDir);
        Utils.printAll(stagedFiles);

        System.out.println();
        System.out.println("=== Removed Files ===");

        List<String> removedFiles = Utils.plainFilenamesIn(removedDir);
        Utils.printAll(removedFiles);

        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");

        TreeSet<String> modifiedNotStgd = modified(currCommit, stagedFiles,
                removedFiles);
        Utils.printAll(modifiedNotStgd);

        System.out.println();
        System.out.println("=== Untracked Files ===");

        TreeSet<String> untracked = untracked(currCommit, stagedFiles,
                removedFiles);
        Utils.printAll(untracked);
    }

    /**
     * Return the set of untracked files as of now.
     * @param currCommit c
     * @param staged files
     * @param removed files
     * @return untracked files
     */
    static TreeSet<String> untracked(Commit currCommit, List<String> staged,
            List<String> removed) {
        TreeSet<String> untracked = new TreeSet<>();
        TreeSet<String> tracked = currCommit.getTracked();
        List<String> allFiles = Utils.plainFilenamesIn(new File("."));
        for (String file : allFiles) {
            if (!tracked.contains(file) && !staged.contains(file)
                    && !removed.contains(file)) {
                untracked.add(file);
            }
        }
        return untracked;
    }

    /**
     * Return a set of files that have been modified but not staged for commit.
     * @param currCommit c
     * @param stagedFiles s
     * @param removedFiles r
     * @return modified/deleted files
     */
    static TreeSet<String> modified(Commit currCommit, List<String> stagedFiles,
            List<String> removedFiles) {
        TreeSet<String> modifiedNotStaged = new TreeSet<>();
        File f;
        for (String tracked : currCommit.getTracked()) {
            f = new File(tracked);
            boolean staged = stagedFiles.contains(tracked);
            if (!f.exists() && !staged) {
                if (!removedFiles.contains(tracked)) {
                    modifiedNotStaged.add(tracked + " (deleted)");
                }
            } else if (!staged && !removedFiles.contains(tracked)) {
                String oldBlobId = currCommit.getBlobs().get(tracked);
                byte[] curr = Utils.readContents(new File(tracked));
                String newBlobId = Utils.sha1("blobs", curr);
                if (!oldBlobId.equals(newBlobId)) {
                    modifiedNotStaged.add(tracked + " (modified)");
                }
            }
        }

        for (String staged : stagedFiles) {
            f = new File(staged);
            if (!f.exists()) {
                modifiedNotStaged.add(staged + " (deleted)");
            } else {
                byte[] stg = Utils.readContents(new File(stagedDir + staged));
                String stgBlobId = Utils.sha1("blobs", stg);
                byte[] curr = Utils.readContents(new File(staged));
                String currBlobId = Utils.sha1("blobs", curr);
                if (!stgBlobId.equals(currBlobId)) {
                    modifiedNotStaged.add(staged + " (modified)");
                }
            }
        }
        return modifiedNotStaged;
    }

    /**
     * Checkout whatever ARGS contains.
     * @param args of strings
     */
    static void checkout(String... args) {
        Repo repo = loadRepo();
        switch (args.length) {
        case 2:
            String postBranch = args[1];
            if (!repo.getBranchInfo().keySet().contains(postBranch)) {
                error("No such branch exists.");
            }
            String preBranch = repo.getCurrentBranchName();
            if (postBranch.equals(preBranch)) {
                error("No need to checkout the current branch.");
            }
            String postCommitId = repo.getCommit(postBranch);
            Commit postCommit = loadCommit(postCommitId);
            Commit preCommit = loadCommit(repo.getCurrentCommit());
            List<String> staged = Utils.plainFilenamesIn(stagedDir);
            List<String> removed = Utils.plainFilenamesIn(removedDir);
            TreeSet<String> untracked = untracked(preCommit, staged, removed);
            Set<String> postCommitFiles = postCommit.getBlobs().keySet();
            for (String f : untracked) {
                if (postCommitFiles.contains(f)) {
                    error("There is an untracked file in the way; "
                            + "delete it or add it first.");
                }
            }
            for (String fName : postCommitFiles) {
                overrideFile(postCommit, fName);
            }
            for (String f : preCommit.getBlobs().keySet()) {
                if (!postCommitFiles.contains(f)) {
                    File del = new File(f);
                    del.delete();
                }
            }
            repo.setCurrentBranch(postBranch);
            Utils.clearStage();
            Utils.saveObj(repo, repoP);
            break;
        case 3:
            if (args[1].equals("--")) {
                Commit currCommit = loadCurrCommit(repo);
                overrideFile(currCommit, args[2]);
            } else {
                error("Incorrect operands.");
            }
            break;
        case 4:
            if (args[2].equals("--")) {
                String newCommitId = getFullId(args[1]);
                Commit commit = loadCommit(newCommitId);
                overrideFile(commit, args[3]);
            } else {
                error("Incorrect operands.");
            }
            break;
        default:
            error("Incorrect operands.");
            break;
        }
    }

    /**
     * Retrieve full commit Id from abbrev Id. If Id does not exist, return the
     * given Id.
     * @param newCommitId abbreved Id
     * @return full Id
     */
    static String getFullId(String newCommitId) {
        int abbrevLength = newCommitId.length();
        List<String> commitList = Utils.plainFilenamesIn(commitDir);
        String currCommitId;
        for (String commitId : commitList) {
            currCommitId = commitId.substring(0, abbrevLength);
            if (currCommitId.equals(newCommitId)) {
                newCommitId = commitId;
                return newCommitId;
            }
        }
        return newCommitId;
    }

    /**
     * Override the current version of the file given the COMMIT and FILENAME.
     * @param commit version
     * @param fileName name to write over
     */
    static void overrideFile(Commit commit, String fileName) {
        HashMap<String, String> blobs = commit.getBlobs();
        if (blobs.containsKey(fileName)) {
            File file = new File(getGitletDir() + blobs.get(fileName));
            byte[] clone = Utils.readContents(file);
            Utils.writeContents(new File(fileName), clone);
        } else {
            error("File does not exist in that commit.");
        }
    }

    /**
     * Create a new branch named BRANCH.
     * @param branch Name of the branch
     */
    static void branch(String branch) {
        Repo repo = loadRepo();
        repo.newBranch(branch);
        Utils.saveObj(repo, repoP);
    }

    /**
     * Remove BRANCH from the repo.
     * @param branch Name of the branch
     */
    static void removeBranch(String branch) {
        Repo repo = loadRepo();
        repo.removeBranch(branch);
        Utils.saveObj(repo, repoP);
    }

    /**
     * Checks out all the files tracked by the given commit. Removes tracked
     * files that are not present in the given commit. Also moves the current
     * branch's head to that commit node.
     * @param commitId commit
     */
    static void reset(String commitId) {
        Repo repo = loadRepo();

        String postCommitId = getFullId(commitId);
        Commit postCommit = loadCommit(postCommitId);
        String preCommitId = repo.getCurrentCommit();
        Commit preCommit = loadCommit(preCommitId);

        Set<String> postCommitFiles = postCommit.getBlobs().keySet();
        Set<String> preCommitFiles = preCommit.getBlobs().keySet();

        List<String> staged = Utils.plainFilenamesIn(stagedDir);
        List<String> removed = Utils.plainFilenamesIn(removedDir);
        TreeSet<String> untracked = untracked(preCommit, staged, removed);
        for (String f : untracked) {
            if (postCommitFiles.contains(f)) {
                error("There is an untracked file in the way; "
                        + "delete it or add it first.");
            }
        }
        String[] args = new String[4];
        args[0] = "checkout";
        args[1] = postCommitId;
        args[2] = "--";
        for (String postFile : postCommitFiles) {
            args[3] = postFile;
            checkout(args);
        }

        File f;
        for (String preFile : preCommitFiles) {
            if (!postCommitFiles.contains(preFile)) {
                f = new File(preFile);
                f.delete();
            }
        }

        repo.updateBranch(postCommitId);
        Utils.clearStage();
        Utils.saveObj(repo, repoP);
    }

    /**
     * Return the no. of preceding commits including yourself.
     * @param commitId Id of the commit return length of the commit chain
     */
    static int commitChainLength(String commitId) {
        int length = 0;
        Commit currCommit = new Commit("temp");
        while (commitId != null) {
            currCommit = Utils.loadObj(currCommit, Main.commitDir + commitId);
            length++;
            commitId = currCommit.getParent();
        }
        return length;
    }

    /**
     * Return the length of a branch.
     * @param branchName Name of the branch
     * @return length of the branch
     */
    static int branchLength(String branchName) {
        Repo repo = loadRepo();
        return commitChainLength(repo.getCommit(branchName));
    }

    /**
     * Merge BRANCHNAME with the current branch.
     * @param branchName branch
     */
    static void merge(String branchName) {
        Repo repo = loadRepo();
        if (!repo.containsBranch(branchName)) {
            error("A branch with that name does not exist.");
        }
        List<String> stagedList = Utils.plainFilenamesIn(stagedDir);
        List<String> removedList = Utils.plainFilenamesIn(removedDir);
        if (stagedList.size() != 0 || removedList.size() != 0) {
            error("You have uncommitted changes.");
        }
        if (branchName.equals(repo.getCurrentBranchName())) {
            error("Cannot merge a branch with itself.");
        }
        String currCommitId = repo.getCurrentCommit();
        String mergingCommitId = repo.getCommit(branchName);
        Commit mergingCommit = loadCommit(mergingCommitId);
        Commit currCommit = loadCommit(currCommitId);
        Set<String> mergeFiles = mergingCommit.getTracked();
        TreeSet<String> untracked = untracked(currCommit, stagedList,
                removedList);
        for (String f : untracked) {
            if (mergeFiles.contains(f)) {
                error("There is an untracked file in the way; "
                        + "delete it or add it first.");
            }
        }
        Commit splitPointCommit = findSplitCommit(currCommit, mergingCommit,
                repo, branchName);
        mergeHelper(splitPointCommit, mergingCommit, currCommit, mergeFiles,
                mergingCommitId, repo, branchName);
    }

    /**
     * Find the split point between two commits.
     * @param currCommit head commit of current branch
     * @param mergingCommit head commit of given branch
     * @param repo repository
     * @param branchName name of the given branch
     * @return split point Commit
     */
    static Commit findSplitCommit(Commit currCommit,
            Commit mergingCommit, Repo repo, String branchName) {
        String mergingCommitId = repo.getCommit(branchName);
        int currBranchLength = branchLength(repo.getCurrentBranchName());
        int mergingBranchLength = branchLength(branchName);
        int currMinusMerging = currBranchLength - mergingBranchLength;
        if (currMinusMerging > 0) {
            for (int i = currMinusMerging; i > 0; i--) {
                currCommit = loadCommit(currCommit.getParent());
            }
        }
        if (currMinusMerging < 0) {
            for (int i = currMinusMerging; i < 0; i++) {
                mergingCommit = loadCommit(mergingCommit.getParent());
            }
        }
        if (currCommit.equals(mergingCommit)) {
            if (currMinusMerging < 0) {
                String currBranchName = repo.getCurrentBranchName();
                checkout(new String[] { "checkout", branchName });
                repo.setCurrentBranch(currBranchName);
                repo.updateBranch(mergingCommitId);
                Utils.saveObj(repo, repoP);
                System.out.println("Current branch fast-forwarded.");
                System.exit(0);
            } else {
                error("Given branch is an ancestor of the current branch.");
            }
        }
        while (!currCommit.equals(mergingCommit)) {
            currCommit = loadCommit(currCommit.getParent());
            mergingCommit = loadCommit(mergingCommit.getParent());
        }
        return currCommit;
    }

    /**
     * Merge helper method.
     * @param splitPointCommit s
     * @param mergingCommit m
     * @param currCommit c
     * @param mergeFiles m
     * @param mergingCommitId m
     * @param repo r
     * @param branchName b
     */
    static void mergeHelper(Commit splitPointCommit, Commit mergingCommit,
            Commit currCommit, Set<String> mergeFiles, String mergingCommitId,
            Repo repo, String branchName) {
        Set<String> splitPointFiles = splitPointCommit.getTracked();
        Set<String> currFiles = currCommit.getTracked();
        HashSet<String> fileUniverse = new HashSet<>();
        fileUniverse.addAll(currFiles);
        fileUniverse.addAll(mergeFiles);
        fileUniverse.addAll(splitPointFiles);

        boolean conflict = false;
        for (String f : fileUniverse) {
            String fm = mergingCommit.getBlob(f);
            String fc = currCommit.getBlob(f);
            String fs = splitPointCommit.getBlob(f);
            boolean inS, inC, inM;
            inS = splitPointFiles.contains(f);
            inC = currFiles.contains(f);
            inM = mergeFiles.contains(f);
            if (inS && inC && inM && !fc.equals(fm) && fs.equals(fc)) {
                checkout(new String[] { "checkout", mergingCommitId, "--", f });
                add(f);
            } else if (inM && !inS && !inC) {
                checkout(new String[] { "checkout", mergingCommitId, "--", f });
                add(f);
            } else if (!inM && inS && inC && fc.equals(fs)) {
                remove(f);
            } else if ((!inS && inM && inC && !fc.equals(fm))
                    || (inS && !inC && inM && !fs.equals(fm))
                    || (inS && inC && !inM && !fs.equals(fc))
                    || (inS && inC && inM && !fc.equals(fm) && !fs.equals(fc)
                            && !fs.equals(fm))) {
                conflict = true;
                Utils.resolveConflicts(currCommit, inC, mergingCommit, inM, f);
            }
        }
        if (!conflict) {
            String msg = String.format("Merged %s with %s.",
                    repo.getCurrentBranchName(), branchName);
            commit(msg);
        } else {
            error("Encountered a merge conflict.");
        }
    }

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND> ....
     */
    public static void main(String... args) {
        File f = new File(Main.getGitletDir());
        if (args.length == 0) {
            error("Please enter a command.");
        }
        if (!args[0].equals("init") && !f.exists()) {
            error("Not in an initialized gitlet directory.");
        }
        commandInterpreter(args);
    }

    /**
     * Parse the user's command given the list of arguments.
     * @param args strings
     */
    public static void commandInterpreter(String... args) {
        if (args[0].equals("checkout")) {
            checkout(args);
            System.exit(0);
        }
        if (args.length == 1) {
            switch (args[0]) {
            case "init":
                init();
                break;
            case "log":
                log();
                break;
            case "status":
                status();
                break;
            case "global-log":
                globalLog();
                break;
            case "commit":
                error("Incorrect operands.");
                break;
            default:
                error("No command with that name exists.");
                break;
            }
        } else if (args.length == 2) {
            commmandTwo(args);
        } else if (args.length == 3) {
            switch (args[0]) {
            case "add-remote":
                addRemote(args);
                break;
            case "push":
                push(args);
                break;
            case "fetch":
                fetch(args);
                break;
            case "pull":
                pull(args);
                break;
            default:
                error("Incorrect operands.");
                break;
            }
        } else {
            error("Incorrect operands.");
        }
    }

    /**
     * Running command with length two.
     * @param args args
     */
    private static void commmandTwo(String[] args) {
        switch (args[0]) {
        case "commit":
            commit(args[1]);
            break;
        case "add":
            add(args[1]);
            break;
        case "branch":
            branch(args[1]);
            break;
        case "merge":
            merge(args[1]);
            break;
        case "rm":
            remove(args[1]);
            break;
        case "find":
            find(args[1]);
            break;
        case "rm-branch":
            removeBranch(args[1]);
            break;
        case "reset":
            reset(args[1]);
            break;
        case "rm-remote":
            removeRemote(args[1]);
            break;
        default:
            error("Incorrect operands.");
            break;
        }
    }

    /**
     * Attempts to append the current branch's commits to the end of the given
     * branch at the given remote.
     * @param args args
     */
    static void push(String[] args) {
        Repo localRepo = loadRepo();
        String remoteName = args[1];
        String remoteBranchName = args[2];
        if (!localRepo.containsRemote(remoteName)) {
            error("Remote directory not found.");
        }
        File remotePath = new File(localRepo.getRepoPath(remoteName)
                .replace("/", java.io.File.separator));
        if (!remotePath.exists()) {
            error("Remote directory not found.");
        }
        Commit currCommit = new Commit("temp");
        ArrayList<Commit> localRepoCommits = new ArrayList<>();
        ArrayList<String> localRepoCommitId = new ArrayList<>();
        String currCommitId = localRepo.getCurrentCommit();
        while (currCommitId != null) {
            currCommit = Utils.loadObj(currCommit,
                    Main.commitDir + currCommitId);
            localRepoCommits.add(currCommit);
            localRepoCommitId.add(currCommitId);
            currCommitId = currCommit.getParent();
        }
        Repo remoteRepo = changeR(localRepo, remoteName);
        currCommitId = remoteRepo.getBranchInfo().get(remoteBranchName);
        if (currCommitId == null) {
            remoteRepo.updateBranch(remoteBranchName, "unset");
        } else if (!localRepoCommitId.contains(currCommitId)) {
            error("Please pull down remote changes before pushing.");
        }
        ArrayList<Commit> remoteRepoCommits = new ArrayList<>();
        while (currCommitId != null) {
            currCommit = Utils.loadObj(currCommit,
                    Main.commitDir + currCommitId);
            remoteRepoCommits.add(0, currCommit);
            currCommitId = currCommit.getParent();
        }
        resetPath();
        HashMap<String, byte[]> pushBlobs = new HashMap<>();
        int aheadBy = localRepoCommits.size() - remoteRepoCommits.size();
        for (int i = 0; i < aheadBy; i++) {
            HashMap<String, String> blobMap = localRepoCommits.get(i)
                    .getBlobs();
            for (String fileName : blobMap.keySet()) {
                pushBlobs.put(blobMap.get(fileName), Utils.readContents(
                        new File(getGitletDir() + blobMap.get(fileName))));
            }
        }
        changeRepo(localRepo, remoteName);
        for (int i = 0; i < aheadBy; i++) {
            Utils.saveObj(localRepoCommits.get(i),
                    getCommitDir() + localRepoCommitId.get(i));
        }
        for (String blobId : pushBlobs.keySet()) {
            Utils.writeContents(new File(getGitletDir() + blobId),
                    pushBlobs.get(blobId));
        }
        remoteRepo.updateBranch(remoteBranchName, localRepo.getCurrentCommit());
        Utils.saveObj(remoteRepo, getRepoP());
    }

    /**
     * Helper to change Repo and return loaded Repo.
     * @param local repo
     * @param remote name
     * @return loaded repo
     */
    static Repo changeR(Repo local, String remote) {
        changeRepo(local, remote);
        return loadRepo();
    }

    /**
     * Brings down commits from the remote gitlet into the local gitlet.
     * @param args args
     */
    static void fetch(String[] args) {
        Repo localRepo = loadRepo();
        String remoteName = args[1];
        String remoteBranchName = args[2];
        if (!localRepo.containsRemote(remoteName)) {
            error("Remote directory not found.");
        }
        File remotePath = new File(localRepo.getRepoPath(remoteName)
                .replace("/", java.io.File.separator));
        if (!remotePath.exists()) {
            error("Remote directory not found.");
        }
        changeRepo(localRepo, remoteName);
        Repo remoteRepo = loadRepo();

        if (!remoteRepo.getBranchInfo().containsKey(remoteBranchName)) {
            error("That remote does not have that branch.");
        }

        Commit currCommit = new Commit("temp");
        ArrayList<Commit> remoteRepoCommits = new ArrayList<>();
        ArrayList<String> remoteRepoCommitId = new ArrayList<>();
        String currCommitId = remoteRepo.getBranchInfo().get(remoteBranchName);
        while (currCommitId != null) {
            currCommit = Utils.loadObj(currCommit,
                    getCommitDir() + currCommitId);
            remoteRepoCommits.add(currCommit);
            remoteRepoCommitId.add(currCommitId);
            currCommitId = currCommit.getParent();
        }

        HashMap<String, byte[]> pullBlobs = new HashMap<>();
        for (Commit remoteCommit : remoteRepoCommits) {
            HashMap<String, String> blobMap = remoteCommit.getBlobs();
            for (String fileName : blobMap.keySet()) {
                String blobId = blobMap.get(fileName);
                pullBlobs.put(blobId, Utils.readContents(
                        new File(localRepo.getRepoPath(remoteName) + blobId)));
            }
        }

        resetPath();

        for (int i = 0; i < remoteRepoCommitId.size(); i++) {
            Utils.saveObj(remoteRepoCommits.get(i),
                    getCommitDir() + remoteRepoCommitId.get(i));
        }

        for (String blobId : pullBlobs.keySet()) {
            Utils.writeContents(new File(getGitletDir() + blobId),
                    pullBlobs.get(blobId));
        }
        localRepo.updateBranch(remoteName + "/" + remoteBranchName,
                remoteRepoCommitId.get(0));
        Utils.saveObj(localRepo, repoP);
    }

    /**
     * Fetches branch [remote name]/[remote branch name] as for the fetch
     * command, and then merges that fetch into the current branch.
     * @param args args
     */
    static void pull(String[] args) {
        String remoteName = args[1];
        String remoteBranchName = args[2];
        fetch(new String[] { "fetch", remoteName, remoteBranchName });
        merge(remoteName + "/" + remoteBranchName);
    }

    /**
     * Adding a new remote server.
     * @param args args
     */
    static void addRemote(String[] args) {
        String remoteName = args[1];
        Repo repo = loadRepo();
        if (repo.containsBranch(remoteName)) {
            error("A remote with that name already exists.");
        }
        repo.addRemote(remoteName, args[2] + "/");
        Utils.saveObj(repo, repoP);
    }

    /**
     * Remove a server.
     * @param remoteName Name of the remote server.
     */
    static void removeRemote(String remoteName) {
        Repo repo = loadRepo();
        repo.removeRemote(remoteName);
        Utils.saveObj(repo, repoP);
    }

    /**
     * Access a different repo named REPO.
     * @param repo name of the repo.
     * @param remoteRepoName name to the desired repo
     */
    static void changeRepo(Repo repo, String remoteRepoName) {
        String repoPath = repo.getRepoPath(remoteRepoName);
        setGitletDir(repoPath.replace("/", java.io.File.separator));
        setStagedDir(getGitletDir() + "staged/");
        setCommitDir(getGitletDir() + "commits/");
        setRemovedDir(getGitletDir() + "removed/");
        setRepoP(getGitletDir() + "repo.txt");
    }

    /**
     * Reset the path of git directory to the local repo.
     */
    static void resetPath() {
        setGitletDir(DEFAULT_GIT_DIR);
        setStagedDir(getGitletDir() + "staged/");
        setCommitDir(getGitletDir() + "commits/");
        setRemovedDir(getGitletDir() + "removed/");
        setRepoP(getGitletDir() + "repo.txt");
    }

    /**
     * Reports an error and exit program.
     * @param err error
     */
    static void error(String err) {
        System.err.println(err);
        System.exit(0);
    }

    /**
     * Getter for gitletDir.
     * @return string of the path
     */
    public static String getGitletDir() {
        return gitletDir;
    }

    /**
     * Setter for gitletDir.
     * @param gitletPath string of the path
     */
    public static void setGitletDir(String gitletPath) {
        gitletDir = gitletPath;
    }

    /**
     * Getter for Commit Dir.
     * @return string of the path
     */
    public static String getCommitDir() {
        return commitDir;
    }

    /**
     * Setter for commitDir.
     * @param commitPath string of the path
     */
    public static void setCommitDir(String commitPath) {
        Main.commitDir = commitPath;
    }

    /**
     * Getter for stagedDir.
     * @return string of the path
     */
    public static String getStagedDir() {
        return stagedDir;
    }

    /**
     * Setter for stagedDir.
     * @param stagedPath string of the path
     */
    public static void setStagedDir(String stagedPath) {
        Main.stagedDir = stagedPath;
    }

    /**
     * Getter for removedDir.
     * @return string of the path
     */
    public static String getRemovedDir() {
        return removedDir;
    }

    /**
     * Setter for removedDir.
     * @param removedPath string of the path
     */
    public static void setRemovedDir(String removedPath) {
        Main.removedDir = removedPath;
    }

    /**
     * Getter for repoDir.
     * @return string of the path
     */
    public static String getRepoP() {
        return repoP;
    }

    /**
     * Setter for repoP.
     * @param repoPath string of the path
     */
    public static void setRepoP(String repoPath) {
        Main.repoP = repoPath;
    }
}
