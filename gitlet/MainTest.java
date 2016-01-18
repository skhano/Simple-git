package gitlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.TreeSet;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import ucb.junit.textui;


public class MainTest {

    /**
     * Deletes existing gitlet system.
     * This method runs before every @Test method. This is important to enforce
     * that all tests are independent and do not interact with one another.
     */
    @Before
    public void setUp() {
        File f = new File(Main.getGitletDir());
        if (f.exists()) {
            Utils.recursiveDelete(f);
        }
    }

    @AfterClass
    public static void tearDown() {
        MainTest test = new MainTest();
        test.setUp();
    }

    /**
     * Assert a new .gitlet directory will be created.
     */
    @Test
    public void initTest() {
        File f = initialize();
        assertTrue(f.exists());

        Repo repo = Main.loadRepo();
        String currCommit = repo.getCurrentCommit();
        assertEquals("master", repo.getCurrentBranchName());
        assertNotNull(currCommit);

        Commit c = new Commit("temp");
        c = Utils.loadObj(c, Main.getCommitDir() + currCommit);

        assertNull(c.getParent());
        assertEquals("initial commit", c.getLogMessage());
    }

    /** Test the add command and make sure files have been staged. */
    @Test
    public void addTest() {
        File f = initialize();

        Main.add("foo.txt");
        File foo = new File(Main.getStagedDir() + "foo.txt");
        assertTrue(foo.exists());

        Main.add("barr.txt");
        File barr = new File(Main.getStagedDir() + "barr.txt");
        assertTrue(barr.exists());
    }

    /** Test commit command. */
    @Test
    public void commitTest() {
        simpleRepo();

        List<String> staged = Utils.plainFilenamesIn(Main.getStagedDir());
        List<String> removed = Utils.plainFilenamesIn(Main.getRemovedDir());
        assertTrue(staged.size() == 0);
        assertTrue(removed.size() == 0);

        Repo repo = Main.loadRepo();
        String commit = repo.getCurrentCommit();
        Commit c2 = new Commit("temp");
        c2 = Utils.loadObj(c2, Main.getCommitDir() + commit);
        assertTrue(c2.getTracked().contains("foo.txt"));
        assertTrue(c2.getTracked().contains("barr.txt"));
        assertEquals("second commit", c2.getLogMessage());

        Commit c1 = Utils.loadObj(c2, Main.getCommitDir() + c2.getParent());
        assertTrue(c1.getTracked().contains("foo.txt"));
        assertEquals("first commit", c1.getLogMessage());

        Commit c0 = Utils.loadObj(c1, Main.getCommitDir() + c1.getParent());
        assertEquals("initial commit", c0.getLogMessage());
    }

    @Test
    public void findTest() {
        simpleRepo();
    }

    @Test
    public void modifiedNotStagedTest() {
        simpleRepo();

        List<String> staged = Utils.plainFilenamesIn(Main.getStagedDir());
        List<String> rmvd = Utils.plainFilenamesIn(Main.getRemovedDir());

        Repo repo = Main.loadRepo();
        Commit c = Main.loadCurrCommit(repo);
        TreeSet<String> modified = Main.modified(c, staged, rmvd);
        for (String r : rmvd) {
            assertFalse(modified.contains(r));
        }
    }

    @Test
    public void untrackedTest() {
        simpleRepo();
        List<String> staged = Utils.plainFilenamesIn(Main.getStagedDir());
        List<String> rmvd = Utils.plainFilenamesIn(Main.getRemovedDir());

        Repo repo = Main.loadRepo();
        Commit c = Main.loadCurrCommit(repo);
        TreeSet<String> untracked = Main.untracked(c, staged, rmvd);
        TreeSet<String> modified = Main.modified(c, staged, rmvd);

        for (String m : modified) {
            assertFalse(untracked.contains(m));
        }

        for (String r : rmvd) {
            assertFalse(untracked.contains(r));
        }
    }

    @Test
    public void branchTest() {
        simpleRepo();
        String b = "cool-beans";
        Main.branch(b);
        Repo repo = Main.loadRepo();
        String currB = repo.getCurrentBranchName();
        assertTrue(repo.getBranchInfo().containsKey(b));
        assertFalse(currB.equals(b));

        Main.removeBranch(b);
        repo = Main.loadRepo();
        assertFalse(repo.getBranchInfo().containsKey(b));
        assertTrue(currB.equals(repo.getCurrentBranchName()));
    }

    @Test
    public void getFullIdTest() {
        simpleRepo();
        Repo repo = Main.loadRepo();
        String fullId = repo.getCurrentCommit();
        assertEquals(fullId, Main.getFullId(fullId.substring(0, 7)));
        assertEquals(fullId, Main.getFullId(fullId.substring(0, 9)));
        assertEquals(fullId, Main.getFullId(fullId.substring(0, 6)));
        assertEquals(fullId, Main.getFullId(fullId.substring(0, 14)));
    }

    @Test
    public void testBranchLength() {
        initialize();

        int one = 1;
        int two = 2;
        Repo repo = Main.loadRepo();
        assertEquals(one, Main.branchLength("master"));

        Main.branch("cool-beans");
        Main.add("foo.txt");
        Main.commit("second commit");
        assertEquals(two, Main.branchLength("master"));
        assertEquals(one, Main.branchLength("cool-beans"));
    }

    @Test
    public void splitTest() {
        initialize();
        File barr = new File("barr.txt");
        File foobarr = new File("foobarr.txt");
        byte[] barrCopy = Utils.readContents(barr);
        byte[] foobarrCopy = Utils.readContents(foobarr);

        Main.add("foo.txt");
        Main.commit("first commit");
        Repo r0 = Main.loadRepo();
        Commit split = Main.loadCurrCommit(r0);
        Main.branch("cool-beans");
        Main.add("barr.txt");
        Main.commit("second master commit");
        Main.add("foobarr.txt");
        Main.commit("third master commit");
        Repo r1 = Main.loadRepo();
        Commit curr = Main.loadCurrCommit(r1);

        Main.checkout(new String[] { "checkout", "cool-beans" });
        Utils.writeContents(barr, barrCopy);
        Utils.writeContents(foobarr, foobarrCopy);
        Main.add("barr.txt");
        Main.commit("second cool commit");
        Repo r2 = Main.loadRepo();
        Commit merging = Main.loadCurrCommit(r2);

        Commit testSplit = Main.findSplitCommit(merging, curr, r2, "master");
        assertTrue(split.equals(testSplit));
    }

    /** Initialize a new repository. */
    public static File initialize() {
        File f = new File(Main.getGitletDir());
        Main.init();
        return f;
    }

    /** Basic add and commit sequence after calling init. */
    public static void simpleRepo() {
        File f = initialize();
        Main.add("foo.txt");
        Main.commit("first commit");
        Main.add("barr.txt");
        Main.commit("second commit");
    }

    public static void main(String[] ignored) {
        textui.runClasses(MainTest.class);
    }
}
