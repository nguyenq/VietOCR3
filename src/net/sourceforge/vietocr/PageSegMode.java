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
package net.sourceforge.vietocr;

/**
 * Tesseract Page Segmentation Mode enum.
 * 
 */
public enum PageSegMode {

//    PSM_OSD_ONLY("OsdOnly", "0 - Orientation and script detection (OSD) only"),
//    PSM_AUTO_OSD("AutoOsd", "1 - Automatic page segmentation with OSD"),
//    PSM_AUTO_ONLY("AutoOnly", "2 - Automatic page segmentation, but no OSD, or OCR"),
//    PSM_AUTO("Auto", "3 - Fully automatic page segmentation, but no OSD (default)"),
//    PSM_SINGLE_COLUMN("SingleColumn", "4 - Assume a single column of text of variable sizes"),
//    PSM_SINGLE_BLOCK_VERT_TEXT("SingleBlockVertText", "5 - Assume a single uniform block of vertically aligned text"),
//    PSM_SINGLE_BLOCK("SingleBlock", "6 - Assume a single uniform block of text"),
//    PSM_SINGLE_LINE("SingleLine", "7 - Treat the image as a single text line"),
//    PSM_SINGLE_WORD("SingleWord", "8 - Treat the image as a single word"),
//    PSM_CIRCLE_WORD("CircleWord", "9 - Treat the image as a single word in a circle"),
//    PSM_SINGLE_CHAR("SingleChar", "10 - Treat the image as a single character");
//    PSM_SPARSE_TEXT("SpareText", "11 - Find as much text as possible in no particular order"),
//    PSM_SPARSE_TEXT_OSD("SpareTextOsd", "12 - Sparse text with orientation and script detection"),
//    PSM_COUNT("Count", "13 - Number of enum entries");
    
    PSM_OSD_ONLY("0", "0 - Orientation and script detection (OSD) only"),
    PSM_AUTO_OSD("1", "1 - Automatic page segmentation with OSD"),
    PSM_AUTO_ONLY("2", "2 - Automatic page segmentation, but no OSD, or OCR"),
    PSM_AUTO("3", "3 - Fully automatic page segmentation, but no OSD (default)"),
    PSM_SINGLE_COLUMN("4", "4 - Assume a single column of text of variable sizes"),
    PSM_SINGLE_BLOCK_VERT_TEXT("5", "5 - Assume a single uniform block of vertically aligned text"),
    PSM_SINGLE_BLOCK("6", "6 - Assume a single uniform block of text"),
    PSM_SINGLE_LINE("7", "7 - Treat the image as a single text line"),
    PSM_SINGLE_WORD("8", "8 - Treat the image as a single word"),
    PSM_CIRCLE_WORD("9", "9 - Treat the image as a single word in a circle"),
    PSM_SINGLE_CHAR("10", "10 - Treat the image as a single character"),
    PSM_SPARSE_TEXT("11", "11 - Find as much text as possible in no particular order"),
    PSM_SPARSE_TEXT_OSD("12", "12 - Sparse text with orientation and script detection"),
    PSM_COUNT("13", "13 - Raw line. Treat the image as a single text line, bypassing hacks that are Tesseract-specific");
    
    private final String val;
    private final String desc;

    private PageSegMode(String val, String desc) {
        this.val = val;
        this.desc = desc;
    }

    public String getVal() {
        return val;
    }

    public String getDesc() {
        return desc;
    }
    
    public static PageSegMode enumOf(String val) {
        for (PageSegMode mode : PageSegMode.values()) {
            if (mode.getVal().equals(val)) {
                return mode;
            }
        }
        
        return null;
    }
}
