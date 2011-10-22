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

package net.sourceforge.vietocr.wia;

/**
 *
 * @author Quan
 */
public enum FormatID {
    wiaFormatBMP("{B96B3CAB-0728-11D3-9D7B-0000F81EF32E}"),
    wiaFormatPNG("{B96B3CAF-0728-11D3-9D7B-0000F81EF32E}"),
    wiaFormatGIF("{B96B3CB0-0728-11D3-9D7B-0000F81EF32E}"),
    wiaFormatJPEG("{B96B3CAE-0728-11D3-9D7B-0000F81EF32E}"),
    wiaFormatTIFF("{B96B3CB1-0728-11D3-9D7B-0000F81EF32E}");

    private final String value;

    FormatID(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
