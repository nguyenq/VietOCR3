package com.stibocatalog.hunspell;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.UnsupportedEncodingException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

/**
 * The simple hunspell library frontend which takes care of creating
 * and singleton'ing the library instance (no need to load it more than once
 * per process) .
 *
 * The Hunspell java bindings are licensed under LGPL, see the file COPYING.txt
 * in the root of the distribution for the exact terms.
 *
 * @author Flemming Frandsen (flfr at stibo dot com)
 */
public class Hunspell {

    /**
     * The Singleton instance of Hunspell
     */
    private static Hunspell hunspell = null;
    /**
     * The native library instance, created by JNA.
     */
    private HunspellLibrary hsl = null;

    /**
     * The instance of the HunspellManager, looks for the native lib in the
     * default directories
     */
    public static Hunspell getInstance() throws UnsatisfiedLinkError, UnsupportedOperationException {
        return getInstance(null);
    }

    /**
     * The instance of the HunspellManager, looks for the native lib in
     * the directory specified.
     *
     * @param libDir Optional absolute directory where the native lib can be found. 
     */
    public static Hunspell getInstance(String libDir) throws UnsatisfiedLinkError, UnsupportedOperationException {
        if (hunspell != null) {
            return hunspell;
        }

        hunspell = new Hunspell(libDir);
        return hunspell;
    }

    protected void tryLoad(String libFile) throws UnsupportedOperationException {
        hsl = (HunspellLibrary) Native.loadLibrary(libFile, HunspellLibrary.class);
    }

    /**
     * Constructor for the library, loads the native lib.
     *
     * Loading is done in the first of the following three ways that works:
     * 1) Unmodified load in the provided directory.
     * 2) libFile stripped back to the base name (^lib(.*)\.so on unix)
     * 3) The library is searched for in the classpath, extracted to disk and loaded.
     *
     * @param libDir Optional absolute directory where the native lib can be found. 
     * @throws UnsupportedOperationException if the OS or architecture is simply not supported.
     */
    protected Hunspell(String libDir) throws UnsatisfiedLinkError, UnsupportedOperationException {

        String libFile = libDir != null ? libDir + "/" + libName() : libNameBare();
        try {
            hsl = (HunspellLibrary) Native.loadLibrary(libFile, HunspellLibrary.class);
        } catch (UnsatisfiedLinkError urgh) {

            // Oh dear, the library was not found in the file system, let's try the classpath
            libFile = libName();
            InputStream is = Hunspell.class.getResourceAsStream("/" + libFile);
            if (is == null) {
                throw new UnsatisfiedLinkError("Can't find " + libFile
                        + " in the filesystem nor in the classpath\n"
                        + urgh);
            }

            // Extract the library from the classpath into a temp file.
            File lib;
            FileOutputStream fos = null;
            try {
                lib = File.createTempFile("jna", "." + libFile);
                lib.deleteOnExit();
                fos = new FileOutputStream(lib);
                int count;
                byte[] buf = new byte[1024];
                while ((count = is.read(buf, 0, buf.length)) > 0) {
                    fos.write(buf, 0, count);
                }

            } catch (IOException e) {
                throw new Error("Failed to create temporary file for " + libFile, e);

            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                }
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                    }
                }
            }
            System.out.println("Loading temp lib: " + lib.getAbsolutePath());
            hsl = (HunspellLibrary) Native.loadLibrary(lib.getAbsolutePath(), HunspellLibrary.class);
        }
    }

    /**
     * Calculate the filename of the native hunspell lib.
     * The files have completely different names to allow them to live
     * in the same directory and avoid confusion.
     */
    public static String libName() throws UnsupportedOperationException {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.startsWith("windows")) {
            return libNameBare() + ".dll";

        } else if (os.startsWith("mac os x")) {
            return "lib" + libNameBare() + ".dylib";

        } else {
            return "lib" + libNameBare() + ".so";
        }
    }

    public static String libNameBare() throws UnsupportedOperationException {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        // Annoying that Java doesn't have consistent names for the arch types:
        boolean x86 = arch.equals("x86") || arch.equals("i386") || arch.equals("i686");
        boolean amd64 = arch.equals("x86_64") || arch.equals("amd64") || arch.equals("ia64n");

        if (os.startsWith("windows")) {
            if (x86) {
                return "libhunspell";
            }
            //if (amd64) {
            // Note: No bindings exist for this yet (no JNA support).
            //	return "hunspell-win-x86-64";
            //}

        } else if (os.startsWith("mac os x")) {
            if (x86) {
                return "hunspell-darwin-x86-32";
            }
            if (arch.equals("ppc")) {
                return "hunspell-darwin-ppc-32";
            }

        } else if (os.startsWith("linux")) {
            if (x86) {
                return "hunspell-1.3";
            }
            if (amd64) {
                return "hunspell-1.3";
            }

        } else if (os.startsWith("sunos")) {
            //if (arch.equals("sparc")) {
            //	return "hunspell-sunos-sparc-64";
            //}
        }

        throw new UnsupportedOperationException("Unknown OS/arch: " + os + "/" + arch);
    }
    /**
     * This is the cache where we keep the already loaded dictionaries around
     */
    private HashMap<String, Dictionary> map = new HashMap<String, Dictionary>();

    /**
     * Gets an instance of the dictionary. 
     *
     * @param baseFileName the base name of the dictionary, 
     * passing /dict/da_DK means that the files /dict/da_DK.dic
     * and /dict/da_DK.aff get loaded
     */
    public Dictionary getDictionary(String baseFileName)
            throws FileNotFoundException, UnsupportedEncodingException {

        // TODO: Detect if the dictionary files have changed and reload if they have.
        if (map.containsKey(baseFileName)) {
            return map.get(baseFileName);

        } else {
            Dictionary d = new Dictionary(baseFileName);
            map.put(baseFileName, d);
            return d;
        }
    }

    /**
     * Removes a dictionary from the internal cache
     *
     * @param baseFileName the base name of the dictionary, as passed to
     * getDictionary()
     */
    public void destroyDictionary(String baseFileName) {
        if (map.containsKey(baseFileName)) {
            map.remove(baseFileName);
        }
    }

    /**
     * Class representing a single dictionary.
     */
    public class Dictionary {

        /**
         * The pointer to the hunspell object as returned by the hunspell
         * constructor.
         */
        private Pointer hunspellDict = null;
        /**
         * The encoding used by this dictionary
         */
        private String encoding;

        /**
         * Creates an instance of the dictionary.
         * @param baseFileName the base name of the dictionary,
         */
        Dictionary(String baseFileName) throws FileNotFoundException,
                UnsupportedEncodingException {
            File dic = new File(baseFileName + ".dic");
            File aff = new File(baseFileName + ".aff");

            if (!dic.canRead() || !aff.canRead()) {
                throw new FileNotFoundException("The dictionary files "
                        + baseFileName
                        + "(.aff|.dic) could not be read.");
            }

            hunspellDict = hsl.Hunspell_create(aff.toString(), dic.toString());
            encoding = hsl.Hunspell_get_dic_encoding(hunspellDict);

            // This will blow up if the encoding doesn't exist
            stringToBytes("test");
        }

        /**
         * Deallocate the dictionary.
         */
        public void destroy() {
            if (hsl != null && hunspellDict != null) {
                hsl.Hunspell_destroy(hunspellDict);
                hunspellDict = null;
            }
        }

        /**
         * Check if a word is spelled correctly
         *
         * @param word The word to check.
         */
        public boolean misspelled(String word) {
            try {
                return hsl.Hunspell_spell(hunspellDict, stringToBytes(word)) == 0;
            } catch (UnsupportedEncodingException e) {
                return true; // this should probably never happen.
            }
        }

        /**
         * Convert a Java string to a zero terminated byte array, in the
         * encoding of the dictionary, as expected by the hunspell functions.
         */
        protected byte[] stringToBytes(String str)
                throws UnsupportedEncodingException {
            return (str + "\u0000").getBytes(encoding);
        }

        /**
         * Returns a list of suggestions
         *
         * @param word The word to check and offer suggestions for
         */
        public List<String> suggest(String word) {
            List<String> res = new ArrayList<String>();
            try {
                int suggestionsCount = 0;
                PointerByReference suggestions = new PointerByReference();
                suggestionsCount = hsl.Hunspell_suggest(
                        hunspellDict, suggestions, stringToBytes(word));

                // Get each of the suggestions out of the pointer array.
                Pointer[] pointerArray = suggestions.getValue().
                        getPointerArray(0, suggestionsCount);

                for (int i = 0; i < suggestionsCount; i++) {

                    /* This only works for 8 bit chars, luckily hunspell uses either
                    8 bit encodings or utf8, if someone implements support in
                    hunspell for utf16 we are in trouble.
                     */
                    long len = pointerArray[i].indexOf(0, (byte) 0);
                    if (len != -1) {
                        if (len > Integer.MAX_VALUE) {
                            throw new RuntimeException(
                                    "String improperly terminated: " + len);
                        }
                        byte[] data = pointerArray[i].getByteArray(0, (int) len);
                        res.add(new String(data, encoding));
                    }
                }

            } catch (UnsupportedEncodingException ex) {
            } // Shouldn't happen...

            return res;
        }

        /**
         * Returns a list of stems
         *
         * @param word The word to get stems for
         * @return List of stems or null if the word doesn't exist in dictionary
         */
        public List<String> stem(String word) {
            List<String> res = new ArrayList<String>();
            try {
                int stemsCount = 0;

                PointerByReference stems = new PointerByReference();
                stemsCount = hsl.Hunspell_stem(
                        hunspellDict, stems, stringToBytes(word));

                if (stemsCount == 0) {
                    return null;
                }

                Pointer[] pointerArray = stems.getValue().
                        getPointerArray(0, stemsCount);

                for (int i = 0; i < stemsCount; i++) {
                    /* Flemming's comment... */
                    /* This only works for 8 bit chars, luckily hunspell uses either
                    8 bit encodings or utf8, if someone implements support in
                    hunspell for utf16 we are in trouble.
                     */
                    long len = pointerArray[i].indexOf(0, (byte) 0);
                    if (len != -1) {
                        if (len > Integer.MAX_VALUE) {
                            throw new RuntimeException(
                                    "String improperly terminated: " + len);
                        }
                        byte[] data = pointerArray[i].getByteArray(0, (int) len);
                        res.add(new String(data, encoding));
                    }
                }
            } catch (UnsupportedEncodingException ex) {
            }

            return res;
        }
    }
}
