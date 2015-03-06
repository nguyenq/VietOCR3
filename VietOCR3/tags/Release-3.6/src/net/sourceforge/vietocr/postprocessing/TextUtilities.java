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

public class TextUtilities {

    private static Map<String, String> map;
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
     * Corrects common Tesseract OCR errors.
     *
     * @param input
     * @return
     */
    public static String correctOCRErrors(String input) {
        // substitute letters frequently misrecognized by Tesseract 2.03
        return input.replaceAll("\\b1(?=\\p{L}+\\b)", "l") // 1 to l
                .replaceAll("\\b11(?=\\p{L}+\\b)", "n") // 11 to n
                .replaceAll("\\bI(?![mn]+\\b)", "l") // I to l
                .replaceAll("(?<=\\b\\p{L}*)0(?=\\p{L}*\\b)", "o") // 0 to o
                //                .replaceAll("(?<!\\.) S(?=\\p{L}*\\b)", " s") // S to s
                //                .replaceAll("(?<![cn])h\\b", "n")
                ;
    }

    public static Map<String, String> loadMap(String dangAmbigsFile) {
        try {
            File dataFile = new File(dangAmbigsFile);
            long fileLastModified = dataFile.lastModified();
            if (map == null) {
                map = new LinkedHashMap<String, String>();
            } else {
                if (fileLastModified <= mapLastModified) {
                    return map; // no need to reload map
                }
                map.clear();
            }
            mapLastModified = fileLastModified;

            InputStreamReader stream = new InputStreamReader(new FileInputStream(dangAmbigsFile), "UTF8");
            BufferedReader bs = new BufferedReader(stream);
            String str;
            while ((str = bs.readLine()) != null) {
                // strip BOM character
                if (str.length() > 0 && str.charAt(0) == '\ufeff') {
                    str = str.substring(1);
                }
                // skip empty line or line starts with #
                if (str.trim().length() == 0 || str.trim().startsWith("#")) {
                    continue;
                }
                int index = str.indexOf('=');
                if (index <= 0) {
                    continue;
                }

                String key = str.substring(0, index);
                String value = str.substring(index + 1);
                map.put(key, value);
            }
            bs.close();
            stream.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        return map;
    }
}
