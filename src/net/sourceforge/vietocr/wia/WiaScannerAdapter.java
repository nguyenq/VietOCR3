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

import java.io.File;
import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.*;

/**
 *
 * @author Quan Nguyen (nguyenq@users.sf.net)
 */
public final class WiaScannerAdapter {

    private ActiveXComponent _wiaManager; // CommonDialogClass
    private ActiveXComponent _imageProcess; // Wia.ImageProcess
    private final Variant True = new Variant(true);
    private final Variant False = new Variant(false);

    public File ScanImage(FormatID outputFormat, String fileName) throws Exception {
        if (outputFormat == null) {
            throw new IllegalArgumentException("outputFormat");
        }

        Dispatch imageObject = null; // "WIA.ImageFile"

        try {
            if (_wiaManager == null) {
                _wiaManager = new ActiveXComponent("WIA.CommonDialog");
            }

            imageObject = Dispatch.callN(_wiaManager, "ShowAcquireImage", (Object[]) new Variant[]{
                new Variant(WiaDeviceType.ScannerDeviceType.getValue()), new Variant(WiaImageIntent.GrayscaleIntent.getValue()),
                new Variant(WiaImageBias.MaximizeQuality.getValue()), new Variant(outputFormat.getValue()),
                False, True, True}).getDispatch();

            // WIA may not return the scanned image in the requested format; if so, convert to the target image format
            // https://msdn.microsoft.com/en-us/library/windows/desktop/ms630826(v=vs.85).aspx#SharedSample002
            Variant formatID = Dispatch.get(imageObject, "FormatID");

            if (!formatID.getString().equals(outputFormat.getValue())) {
                if (_imageProcess == null) {
                    _imageProcess = new ActiveXComponent("Wia.ImageProcess");
                }

//                imageProcess.Filters.Add(imageProcess.FilterInfos("Convert").FilterID)
                Dispatch filterInfos = _imageProcess.getProperty("FilterInfos").toDispatch();
                Dispatch convertFilterInfo = Dispatch.call(filterInfos, "Item", new Variant("Convert")).toDispatch();
                Variant filterID = Dispatch.get(convertFilterInfo, "FilterID");
                Dispatch filters = _imageProcess.getProperty("Filters").toDispatch();
                Dispatch.call(filters, "Add", filterID);
//                imageProcess.Filters(1).Properties("FormatID").Value = outputFormat;
                Dispatch firstFilter = Dispatch.call(filters, "Item", new Variant(1)).toDispatch();
                Dispatch formatIDProperty = Dispatch.call(firstFilter, "Properties", new Variant("FormatID")).toDispatch();
                Dispatch.put(formatIDProperty, "Value", outputFormat.getValue());
//                imageObject = imageProcess.Apply(imageObject);
                Variant convertedImageObject = Dispatch.call(_imageProcess, "Apply", imageObject);
                if (!convertedImageObject.isNull()) {
                    // release scanned image's resources
                    if (imageObject != null) {
                        imageObject.safeRelease();
                    }

                    // new ImageFile object with all the filters applied successfully
                    imageObject = convertedImageObject.toDispatch();
                }
            }

            Dispatch.call(imageObject, "SaveFile", fileName);

            return new File(fileName);
        } catch (Exception ex) {
            String message = "Error Scanning Image";
            if (ex.getMessage().toLowerCase().contains("cancelled")) {
                message = "Scanning Operation";
            }
            throw new WiaOperationException(message, ex);
        } finally {
            if (imageObject != null) {
                imageObject.safeRelease();
            }

            // cleanup unmanaged resources
            if (_imageProcess != null) {
                _imageProcess.safeRelease();
            }
            if (_wiaManager != null) {
                _wiaManager.safeRelease();
            }
        }
    }
}
