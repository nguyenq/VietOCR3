/**
 * Copyright @ 2008 Quan Nguyen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sourceforge.vietocr.postprocessing;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import net.sourceforge.vietocr.Gui;
import net.sourceforge.vietpad.utilities.SpellCheckHelper;

public class TextUtilities {

    public static final String SOFT_HYPHEN = "\u00AD";

    private static ArrayList<LinkedHashMap<String, String>> maps;
    private static long mapLastModified = Long.MIN_VALUE;

    /**
     * Corrects letter cases.
     *
     * @param input
     * @return
     */
    public static String correctLetterCases(String input) {
        StringBuffer strB = new StringBuffer();

        // lower uppercase letters ended by lowercase letters except the first letter
        Matcher matcher = Pattern.compile("(?<=\\p{L}+)(\\p{Lu}+)(?=\\p{Ll}+)").matcher(input);
        while (matcher.find()) {
            matcher.appendReplacement(strB, matcher.group().toLowerCase());
        }
        matcher.appendTail(strB);

        // lower uppercase letters begun by lowercase letters
        matcher = Pattern.compile("(?<=\\p{Ll}+)(\\p{Lu}+)").matcher(strB.toString());
        strB.setLength(0);
        while (matcher.find()) {
            matcher.appendReplacement(strB, matcher.group().toLowerCase());
        }
        matcher.appendTail(strB);

        return strB.toString();
    }

    /**
     * Loads text replacement rules from <code>dangAmbigsFile.txt</code> file.
     *
     * @param dangAmbigsFile
     * @return
     */
    public static List<LinkedHashMap<String, String>> loadMap(String dangAmbigsFile) {
        try {
            File dataFile = new File(dangAmbigsFile);
            long fileLastModified = dataFile.lastModified();
            if (maps == null) {
                maps = new ArrayList<LinkedHashMap<String, String>>();
            } else {
                if (fileLastModified <= mapLastModified) {
                    return maps; // no need to reload map
                }
                maps.clear();
            }
            mapLastModified = fileLastModified;

            for (int i = Processor.PLAIN; i <= Processor.REGEX; i++) {
                maps.add(new LinkedHashMap<String, String>());
            }

            InputStreamReader stream = new InputStreamReader(new FileInputStream(dangAmbigsFile), "UTF8");
            BufferedReader bs = new BufferedReader(stream);
            String str;
            while ((str = bs.readLine()) != null) {
                // strip BOM character
                if (str.length() > 0 && str.charAt(0) == '\uFEFF') {
                    str = str.substring(1);
                }
                // skip empty line or line starts with # or without tab delimiters
                if (str.trim().length() == 0 || str.trim().startsWith("#") || !str.contains("\t")) {
                    continue;
                }

                str = str.replaceAll("\t+", "\t");
                String[] parts = str.split("\t");
                if (parts.length < 3) {
                    continue;
                }

                Integer type = Integer.parseInt(parts[0]);
                String key = parts[1];
                String value = parts[2];

                if (type < Processor.PLAIN || type > Processor.REGEX) {
                    continue;
                }

                LinkedHashMap<String, String> hmap = maps.get(type);
                hmap.put(key, value);
            }

            bs.close();
            stream.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        return maps;
    }

    /**
     * Replaces hard hyphens at end of line with soft hyphens.
     *
     * @param input
     * @return
     */
    public static String replaceHyphensWithSoftHyphens(String input) {
        SpellCheckHelper spellCheck = new SpellCheckHelper(null, Gui.getCurrentLocaleId());
        if (!spellCheck.initializeSpellCheck()) {
            return null;
        }

        Matcher m = Pattern.compile("(\\b\\p{L}+)(-|\u2010|\u2011|\u2012|\u2013|\u2014|\u2015)\n(\\p{L}+\\b)").matcher(input);
        StringBuffer strB = new StringBuffer();

        while (m.find()) {
            String before = m.group(1);
            String after = m.group(3);
            char last = before.charAt(before.length() - 1);
            char first = after.charAt(0);
            if (Character.isUpperCase(first) && Character.isUpperCase(last) || Character.isLowerCase(first) && Character.isLowerCase(last)) {
                String word = before + after;
                if (!spellCheck.isMispelled(word)) {
                    m.appendReplacement(strB, before + SOFT_HYPHEN + "\n" + after);
                }
            }
        }
        m.appendTail(strB);

        return strB.toString();
    }
}
