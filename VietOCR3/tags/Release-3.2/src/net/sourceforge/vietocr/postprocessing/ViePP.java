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

import java.text.Normalizer; // available in Java 6.0

public class ViePP implements IPostProcessor {

    final String TONE = "[\u0300\u0309\u0303\u0301\u0323]?"; // `?~'.
    final String DOT_BELOW = "\u0323?"; // .
    final String MARK = "[\u0306\u0302\u031B]?"; // (^+
    final String VOWEL = "[aeiouy]";

    @Override
    public String postProcess(String text) {
        // Move all of these String replace to external vie.DangAmbigs.txt since it is more
        // efficient using StringBuffer-based string manipulations.
        // The file location also gives users more control over the choice of word corrections.
//        // substitute Vietnamese letters frequently misrecognized by Tesseract
//        text = text.replace("êĩ-", "ết")
//                .replace("tmg", "úng")
//                ;
        
        text = text
                .replaceAll("(?i)(?<=đ)ă\\b", "ã")
                .replaceAll("(?i)(?<=[ch])ă\\b", "ả")
                .replaceAll("(?i)ă(?![cmnpt])", "à")
                .replaceAll("(?i)ẵ(?=[cpt])", "ắ")
                .replaceAll("(?<=\\b[Tt])m", "rư")
                .replaceAll("(?i)\\bl(?=[rh])", "t")
                .replaceAll("(u|ll|r)(?=[gh])", "n")
                .replaceAll("(iii|ln|rn)", "m")
                .replaceAll("(?i)(?<=[qrgsv])ll", "u")
                .replaceAll("(?i)(?<=[cnpt])ll", "h")
                .replaceAll("(?i)[oe](?=h)", "c")
                .replaceAll("\\Bđ", "ớ")
                ;

        String nfdText = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("(?i)(?<![q])(u)(?=o\u031B" + TONE + "\\p{L})", "$1\u031B") // uo+n to u+o+n
                .replaceAll("(?i)(?<=u\u031B)(o)(?=" + TONE + "\\p{L})", "$1\u031B") // u+on to u+o+n
                .replaceAll("(?i)(i)" + TONE + "(?=[eioy])", "$1") // remove mark on i followed by certain vowels
                .replaceAll("(?i)(?<=gi)" + TONE + "(?=[aeiouy])", "") // remove mark on i preceeded by g and followed by any vowel
                .replaceAll("(?i)(?<=[^q]" + VOWEL + "\\p{InCombiningDiacriticalMarks}{0,2})(i)" + TONE + "\\b", "$1") // remove mark on i preceeded by vowels w/ or w/o diacritics
                .replaceAll("(?i)(?<=[aeo]\u0302)['\u2018\u2019]", "\u0301") // ^right-single-quote to ^acute
                .replaceAll("(?i)\u2018([aeo]\u0302)(?!\\p{InCombiningDiacriticalMarks})", "$1\u0300") // left-single-quote+a^ to a^grave
                .replaceAll("(?i)(?<=[aeo]\u0302)h", "\u0301n") // a^+h to a^acute+n
                .replaceAll("(?i)(?<=[uo]" + TONE + ")['\u2018]", "\u031B") // u'+left-single-quote) to u+'
                .replaceAll("(?i)(?<=" + VOWEL + "\\p{InCombiningDiacriticalMarks}{0,2})l\\b", "t") // vowel+diacritics+l to vowel+diacritics+t
                .replaceAll("(?i)(?<=" + VOWEL + "\\p{InCombiningDiacriticalMarks}{0,2})ll\\b", "u") // vowel+diacritics+ll to vowel+diacritics+u
                .replaceAll("\\B\\$(?="+ VOWEL + ")", "S") // replace leading $ followed by vowel with S
                ;

        return Normalizer.normalize(nfdText, Normalizer.Form.NFC);
    }
}
