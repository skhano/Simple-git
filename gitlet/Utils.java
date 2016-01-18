package gitlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.Set;


/** Assorted utilities.
 *  @author P. N. Hilfinger
 */
class Utils {

    /* SHA-1 HASH VALUES. */

    /** Returns the SHA-1 hash of the concatenation of VALS, which may
     *  be any mixture of byte arrays and Strings. */
    static String sha1(Object... vals) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            for (Object val : vals) {
                if (val instanceof byte[]) {
                    md.update((byte[]) val);
                } else if (val instanceof String) {
                    md.update(((String) val).getBytes(StandardCharsets.UTF_8));
                } else {
                    throw new IllegalArgumentException("improper type to sha1");
                }
            }
            Formatter result = new Formatter();
            for (byte b : md.digest()) {
                result.format("%02x", b);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException excp) {
            throw new IllegalArgumentException("System does not support SHA-1");
        }
    }

    /** Returns the SHA-1 hash of the concatenation of the strings in
     *  VALS. */
    static String sha1(List<Object> vals) {
        return sha1(vals.toArray(new Object[vals.size()]));
    }

    /* FILE DELETION */

    /** Deletes FILE if it exists and is not a directory.  Returns true
     *  if FILE was deleted, and false otherwise.  Refuses to delete FILE
     *  and throws IllegalArgumentException unless the directory designated by
     *  FILE also contains a directory named .gitlet. */
    static boolean restrictedDelete(File file) {
        if (!(new File(file.getParentFile(), ".gitlet")).isDirectory()) {
            throw new IllegalArgumentException("not .gitlet working directory");
        }
        if (!file.isDirectory()) {
            return file.delete();
        } else {
            return false;
        }
    }

    /** Deletes the file named FILE if it exists and is not a directory.
     *  Returns true if FILE was deleted, and false otherwise.  Refuses
     *  to delete FILE and throws IllegalArgumentException unless the
     *  directory designated by FILE also contains a directory named .gitlet. */
    static boolean restrictedDelete(String file) {
        return restrictedDelete(new File(file));
    }

    /* READING AND WRITING FILE CONTENTS */

    /** Return the entire contents of FILE as a byte array.  FILE must
     *  be a normal file.  Throws IllegalArgumentException
     *  in case of problems. */
    static byte[] readContents(File file) {
        if (!file.isFile()) {
            throw new IllegalArgumentException("must be a normal file");
        }
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    /** Write the entire contents of BYTES to FILE, creating or overwriting
     *  it as needed.  Throws IllegalArgumentException in case of problems. */
    static void writeContents(File file, byte[] bytes) {
        try {
            if (file.isDirectory()) {
                throw
                    new IllegalArgumentException("cannot overwrite directory");
            }
            Files.write(file.toPath(), bytes);
        } catch (IOException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    /* DIRECTORIES */

    /** Filter out all but plain files. */
    private static final FilenameFilter PLAIN_FILES =
        new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isFile();
            }
        };

    /** Returns a list of the names of all plain files in the directory DIR, in
     *  lexicographic order as Java Strings.  Returns null if DIR does
     *  not denote a directory. */
    static List<String> plainFilenamesIn(File dir) {
        String[] files = dir.list(PLAIN_FILES);
        if (files == null) {
            return null;
        } else {
            Arrays.sort(files);
            return Arrays.asList(files);
        }
    }

    /** Returns a list of the names of all plain files in the directory DIR, in
     *  lexicographic order as Java Strings.  Returns null if DIR does
     *  not denote a directory. */
    static List<String> plainFilenamesIn(String dir) {
        return plainFilenamesIn(new File(dir));
    }

    /**
     * Serialize an object of type T, given the object's SHA-1 code.
     * @param obj type T
     * @param sha1 code
     * @param <T> t
     */
    static <T> void saveObj(T obj, String sha1) {
        File outFile = new File(sha1);
        try {
            ObjectOutputStream out = new ObjectOutputStream(
                    new FileOutputStream(outFile));
            out.writeObject(obj);
            out.close();
        } catch (IOException excp) {
            Main.error("Could not serialize object");
        }
    }

    /**
     * Deserialize (loaded) the object stored in the file located at the given
     * SHA1 inside of the .gitlet directory.
     * @param tempObj temp object
     * @param sha1 hash code
     * @param <T> t
     * @return deserialized object
     */
    @SuppressWarnings("unchecked")
    static <T> T loadObj(T tempObj, String sha1) {
        T obj = null;
        File inFile = new File(sha1);
        try {
            ObjectInputStream inp = new ObjectInputStream(
                    new FileInputStream(inFile));
            obj = (T) inp.readObject();
            inp.close();
        } catch (IOException | ClassNotFoundException excp) {
            obj = null;
        }
        return obj;
    }

    /**
     * Return the current time stamp.
     * @return time
     */
    static String timeStamp() {
        java.util.Date date = new java.util.Date();
        Timestamp t = new Timestamp(date.getTime());
        return t.toString().substring(0, 10 + 4 + 5);
    }

    /** Recursively delete all files in a directory.
     *  @param dir file path*/
    static void recursiveDelete(File dir) {
        if (dir.isDirectory()) {
            for (File x : dir.listFiles()) {
                recursiveDelete(x);
            }
        }
        dir.delete();
    }

    /** Print out MESSAGE to standard output. */
    static void print(String message) {
        System.out.println(message);
    }

    /**
     * Print all strings in the list.
     * @param lst contents
     */
    static void printAll(List<String> lst) {
        for (String item : lst) {
            print(item);
        }
    }

    /**
     * Print all strings in the set.
     * @param set contents
     */
    static void printAll(Set<String> set) {
        for (String item : set) {
            print(item);
        }
    }

    /**
     * Delete all staged files.
     */
    static void clearStage() {
        File file;
        File staged = new File(Main.getStagedDir());
        for (String s : plainFilenamesIn(staged)) {
            file = new File(Main.getStagedDir() + s);
            file.delete();
        }
    }

    /**
     * Resolve conflicts.
     * @param currCommit current commit
     * @param inC if it is in currCommit
     * @param mergingCommit given commit
     * @param inM if it is in mergingCommit
     * @param f name of the file
     */
    static void resolveConflicts(Commit currCommit, boolean inC,
            Commit mergingCommit, boolean inM, String f) {
        try {
            String head = "<<<<<<< HEAD\n";
            byte[] contents = head.getBytes();
            byte[] newContents = contents;
            if (inC) {
                String path = currCommit.getBlob(f);
                byte[] c = readContents(new File(Main.getGitletDir() + path));
                newContents = concat(contents, c);
            }
            String seperate = "=======\n";
            byte[] contents2 = concat(newContents, seperate.getBytes());
            byte[] contents3 = contents2;
            if (inM) {
                String path = mergingCommit.getBlob(f);
                byte[] m = readContents(new File(Main.getGitletDir() + path));
                contents3 = concat(contents2, m);
            }
            String end = ">>>>>>>\n";
            byte[] finalFile = concat(contents3, end.getBytes());
            Utils.writeContents(new File(f), finalFile);
        } catch (IllegalArgumentException e) {
            System.exit(0);
        }
    }

    /**
     * Concatenates two byte arrays.
     * @param a arr
     * @param b arr
     * @return new byte arr
     */
    static byte[] concat(byte[] a, byte[] b) {
        byte[] c;
        if (a == null) {
            c = new byte[b.length];
            System.arraycopy(b, 0, c, 0, b.length);
        } else if (b == null) {
            c = new byte[a.length];
            System.arraycopy(a, 0, c, 0, a.length);
        } else {
            c = new byte[a.length + b.length];
            System.arraycopy(a, 0, c, 0, a.length);
            System.arraycopy(b, 0, c, a.length, b.length);
        }
        return c;
    }
}
