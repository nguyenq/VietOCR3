/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.sourceforge.vietocr.wia;

import org.codehaus.groovy.scriptom.*;

/**
 *
 * @author Quan
 */
public class WiaScannerAdapterG {

    private ActiveXObject _wiaManager; // CommonDialogClass

    public File ScanImage(FormatID outputFormat, String fileName) throws Exception {
        Scriptom.inApartment
        {
            if (outputFormat == null) {
                throw new IllegalArgumentException("outputFormat");
            }

            ActiveXObject imageObject = null; // "WIA.ImageFile"

            try {
                if (_wiaManager == null) {
                    _wiaManager = new ActiveXObject("WIA.CommonDialog");
                }

                imageObject = _wiaManager.ShowAcquireImage(
                    WiaDeviceType.ScannerDeviceType, WiaImageIntent.GrayscaleIntent,
                    WiaImageBias.MaximizeQuality, outputFormat.getValue(),
                    false, true, true);

                imageObject.SaveFile(fileName);

                return new File(fileName);
            } catch (Exception ex) {
                String message = "Error scanning image";
                throw new WiaOperationException(message, ex);
            } finally {
                if (imageObject != null) {
                    imageObject.safeRelease();
                    //                ComThread.Release();
                }

                if (_wiaManager != null) {
                    _wiaManager.safeRelease();
                }
            }
        }
    }
}

