/**
 * Copyright 2020 Quan Nguyen
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.sourceforge.vietocr;

/**
 * Holds processing options desired for pre- and post-OCR.
 */
public class ProcessingOptions {

    private boolean deskew;
    private boolean postProcessing;
    private boolean removeLines;
    private boolean removeLineBreaks;
    private boolean textOnlyPdf;
    private boolean correctLetterCases;
    private boolean removeHyphens;
    private boolean replaceHyphens;

    /**
     * @return the deskew
     */
    public boolean isDeskew() {
        return deskew;
    }

    /**
     * @param deskew the deskew to set
     */
    public void setDeskew(boolean deskew) {
        this.deskew = deskew;
    }

    /**
     * @return the postProcessing
     */
    public boolean isPostProcessing() {
        return postProcessing;
    }

    /**
     * @param postProcessing the postProcessing to set
     */
    public void setPostProcessing(boolean postProcessing) {
        this.postProcessing = postProcessing;
    }

    /**
     * @return the removeLines
     */
    public boolean isRemoveLines() {
        return removeLines;
    }

    /**
     * @param removeLines the removeLines to set
     */
    public void setRemoveLines(boolean removeLines) {
        this.removeLines = removeLines;
    }

    /**
     * @return the correctLetterCases
     */
    public boolean isCorrectLetterCases() {
        return correctLetterCases;
    }

    /**
     * @param correctLetterCases the correctLetterCases to set
     */
    public void setCorrectLetterCases(boolean correctLetterCases) {
        this.correctLetterCases = correctLetterCases;
    }

    /**
     * @return the removeLineBreaks
     */
    public boolean isRemoveLineBreaks() {
        return removeLineBreaks;
    }

    /**
     * @param removeLineBreaks the removeLineBreaks to set
     */
    public void setRemoveLineBreaks(boolean removeLineBreaks) {
        this.removeLineBreaks = removeLineBreaks;
    }

    /**
     * @return the removeHyphens
     */
    public boolean isRemoveHyphens() {
        return removeHyphens;
    }

    /**
     * @param removeHyphens the removeHyphens to set
     */
    public void setRemoveHyphens(boolean removeHyphens) {
        this.removeHyphens = removeHyphens;
    }

    /**
     * @return the replaceHyphens
     */
    public boolean isReplaceHyphens() {
        return replaceHyphens;
    }

    /**
     * @param replaceHyphens the replaceHyphens to set
     */
    public void setReplaceHyphens(boolean replaceHyphens) {
        this.replaceHyphens = replaceHyphens;
    }

    /**
     * @return the textOnlyPdf
     */
    public boolean isTextOnlyPdf() {
        return textOnlyPdf;
    }

    /**
     * @param textOnlyPdf the textOnlyPdf to set
     */
    public void setTextOnlyPdf(boolean textOnlyPdf) {
        this.textOnlyPdf = textOnlyPdf;
    }
}
