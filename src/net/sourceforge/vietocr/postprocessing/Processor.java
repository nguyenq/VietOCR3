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

import java.io.File;
import java.util.*;

public class Processor {

    public static final int PLAIN = 0;  // plain replaces
    public static final int REGEX = 1;  // regex replaces

    public static String postProcess(String text, String langCode) {
        try {
            IPostProcessor processor = ProcessorFactory.createProcessor(ISO639.valueOf(langCode.substring(0, 3)));
            return processor.postProcess(text);
        } catch (Exception exc) {
            return text;
        }
    }

    public static String postProcess(String text, String langCode, String dangAmbigsPath, boolean dangAmbigsEnabled, boolean replaceHyphens) throws Exception {
        if (text.trim().length() == 0) {
            return text;
        }

        if (replaceHyphens) {
            text = TextUtilities.replaceHyphensWithSoftHyphens(text);
        }

        // correct using external x.DangAmbigs.txt file first, if enabled
        if (dangAmbigsEnabled) {
            // replace text based on entries read from an x.DangAmbigs.txt file
            List<LinkedHashMap<String, String>> replaceRules = TextUtilities.loadMap(new File(dangAmbigsPath, langCode + ".DangAmbigs.txt").getPath());
            if (replaceRules.isEmpty() && langCode.length() > 3) {
                replaceRules = TextUtilities.loadMap(new File(dangAmbigsPath, langCode.substring(0, 3) + ".DangAmbigs.txt").getPath()); // falls back on base
            }

            if (replaceRules.isEmpty()) {
                throw new UnsupportedOperationException(langCode);
            }

            LinkedHashMap<String, String> replaceRulesPlain = replaceRules.get(PLAIN);
            Iterator<String> iterPlain = replaceRulesPlain.keySet().iterator();
            while (iterPlain.hasNext()) {
                String key = iterPlain.next();
                String value = replaceRulesPlain.get(key);
                text = text.replace(key, value);
            }

            LinkedHashMap<String, String> replaceRulesRegex = replaceRules.get(REGEX);
            Iterator<String> iterRegex = replaceRulesRegex.keySet().iterator();
            while (iterRegex.hasNext()) {
                String key = iterRegex.next();
                String value = replaceRulesRegex.get(key);
                text = text.replaceAll(key, value);
            }
        }

        // postprocessor
        text = postProcess(text, langCode);

        // correct letter cases
        return TextUtilities.correctLetterCases(text);
    }
}
