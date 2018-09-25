package net.sourceforge.vietpad.utilities;

import java.util.*;
import java.util.regex.*;
import java.text.*;
import java.util.logging.Logger;

/**
 *  Vietnamese text utilities.
 *
 *@author     Quan Nguyen
 *@author     Gero Herrmann
 *@version    1.3, 23 February 2010
 */
public class VietUtilities {
    private static Map<String, String> map;
    private static long mapLastModified = Long.MIN_VALUE;
    
    private final static Logger logger = Logger.getLogger(VietUtilities.class.getName());

    /**
     * Strips accents off words.
     *
     *@param  accented  Accented text to be stripped
     *@return           Plain text
     */
    public static String stripDiacritics(String accented) {
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

        return pattern.matcher(Normalizer.normalize(accented, Normalizer.Form.NFD)).replaceAll("").replace('\u0111', 'd').replace('\u0110', 'D');
    }
}
