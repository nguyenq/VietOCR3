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
public enum WiaDeviceType {
    UnspecifiedDeviceType(0),
    ScannerDeviceType(1),
    CameraDeviceType(2),
    VideoDeviceType(3);
    
    private final int value;

    WiaDeviceType(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
}
