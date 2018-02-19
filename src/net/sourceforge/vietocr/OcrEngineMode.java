/**
 * Copyright @ 2017 Quan Nguyen
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
 * Tesseract OCR Engine mode enum.
 * 
 */
public enum OcrEngineMode {
    
    OEM_TESSERACT_ONLY("0", "0 - Legacy engine only"),
    OEM_LSTM_ONLY("1", "1 - Neural nets LSTM engine only"),
    OEM_TESSERACT_LSTM("2", "2 - Legacy + LSTM engines"),
    OEM_DEFAULT("3", "3 - Default, based on what is available");
    
    private final String val;
    private final String desc;

    private OcrEngineMode(String val, String desc) {
        this.val = val;
        this.desc = desc;
    }

    public String getVal() {
        return val;
    }

    public String getDesc() {
        return desc;
    }
    
    public static OcrEngineMode enumOf(String val) {
        for (OcrEngineMode mode : OcrEngineMode.values()) {
            if (mode.getVal().equals(val)) {
                return mode;
            }
        }
        
        return null;
    }
}
