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

public class WiaOperationException extends Exception {

    private WiaScannerError _errorCode;

    public WiaOperationException(WiaScannerError errorCode) {
        super();
        _errorCode = errorCode;
    }

    public WiaOperationException(String message, WiaScannerError errorCode) {
        super(message);
        _errorCode = errorCode;
    }

    public WiaOperationException(String message, Exception innerException) {
        super(message, innerException);
//        COMException comException = (COMException) innerException;
//
//        if (comException != null) {
//            _errorCode = (WiaScannerError) comException.getErrorCode();
//        }
    }

    public WiaOperationException(String message, Exception innerException, WiaScannerError errorCode) {
        super(message, innerException);
        _errorCode = errorCode;
    }

//    public WiaOperationException(System.Runtime.Serialization.SerializationInfo info, System.Runtime.Serialization.StreamingContext context) {
//        super(info, context);
//        info.AddValue("ErrorCode", (long) _errorCode);
//    }

    public WiaScannerError getErrorCode() {
        return _errorCode;
    }

    void setErrorCode(WiaScannerError value) {
        _errorCode = value;
    }

    public String getWIAMessage() {
        String fullMessage = this.getCause().getMessage();
        String description = "Description: ";
        int index = fullMessage.indexOf(description);
        return index == -1 ? fullMessage : fullMessage.substring(index + description.length());
    }


//    void GetObjectData(System.Runtime.Serialization.SerializationInfo info, System.Runtime.Serialization.StreamingContext context) {
//        super().GetObjectData(info, context);
//        _errorCode = (WiaScannerError) info.GetUInt32("ErrorCode");
//    }
}
