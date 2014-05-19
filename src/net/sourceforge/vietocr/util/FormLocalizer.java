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
package net.sourceforge.vietocr.util;

import java.lang.reflect.*;
import java.util.ResourceBundle;
import java.awt.Window;
import javax.swing.*;

public class FormLocalizer {

    private Window window;
    private Class windowType;

    /**
     * Constructor.
     * @param window
     * @param windowType
     */
    public FormLocalizer(Window window, Class windowType) {
        this.window = window;
        this.windowType = windowType;
    }

    /**
     * Update UI elements.
     *
     * For this to work, it requires the resource bundle to follow certain naming convention for keys,
     * which are to be composed of the Swing component's name appended with ".Text" or ".ToolTipText".
     * 
     * @param resources
     */
    public void ApplyCulture(ResourceBundle resources) {
        String text;

        // set window's title
        String propertyName = "this.Title";
        if (resources.containsKey(propertyName)) {
            text = resources.getString(propertyName);
            if (window instanceof JFrame) {
                ((JFrame) window).setTitle(text);
            } else if (window instanceof JDialog) {
                ((JDialog) window).setTitle(text);
            }
        }

        // Determine its fields via reflection.
        // If bundle resource available, assign localized text to JFrame and fields with Text property.

        for (Field fieldInfo : windowType.getDeclaredFields()) {
            Class fieldType = fieldInfo.getType();
            try {
                // apply only to non-text Swing components
                if (!isSubclass(fieldType, Class.forName("javax.swing.JComponent")) || isSubclass(fieldType, Class.forName("javax.swing.text.JTextComponent"))) {
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (Method method : fieldType.getMethods()) {
                try {
                    // setText
                    if (method.getName().equals("setText") && method.getReturnType() == void.class) {
                        propertyName = fieldInfo.getName() + ".Text";
                        setFieldValue(resources, propertyName, fieldInfo, method);
                    }

                    // setToolTipText for JButton, JToggleButton, JLabel
                    if ((isSubclass(fieldType, Class.forName("javax.swing.JButton")) || isSubclass(fieldType, Class.forName("javax.swing.JToggleButton")) || isSubclass(fieldType, Class.forName("javax.swing.JLabel"))) && method.getName().equals("setToolTipText") && method.getReturnType() == void.class) {
                        propertyName = fieldInfo.getName() + ".ToolTipText";
                        setFieldValue(resources, propertyName, fieldInfo, method);
                    }

                    // setMnemonic for AbstractButton
                    if (isSubclass(fieldType, Class.forName("javax.swing.AbstractButton")) && method.getName().equals("setMnemonic") && method.getReturnType() == void.class) {
                        propertyName = fieldInfo.getName() + ".Mnemonic";
                        setFieldValue(resources, propertyName, fieldInfo, method);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        window.validate();
    }

    void setFieldValue(ResourceBundle resources, String propertyName, Field fieldInfo, Method method) throws Exception {
        if (resources.containsKey(propertyName)) {
            String text = resources.getString(propertyName);
            fieldInfo.setAccessible(true);
            if (propertyName.endsWith(".Mnemonic")) {
                method.invoke(fieldInfo.get(window), new Object[]{text.charAt(0)});
            } else {
                 method.invoke(fieldInfo.get(window), new Object[]{text});
            }
        }
    }

    /**
     * Return true if class a is either equivalent to class b, or
     * if class a is a subclass of class b, ie if a either "extends"
     * or "implements" b.
     * Note that either or both "Class" objects may represent interfaces.
     *
     * Copyright (c) 1999-2002 The Apache Software Foundation.  All rights reserved.
     */
    static boolean isSubclass(Class a, Class b) {
        // We rely on the fact that for any given java class or
        // primtitive type there is a unique Class object, so
        // we can use object equivalence in the comparisons.
        if (a == b) {
            return true;
        }

        if (a == null || b == null) {
            return false;
        }

        for (Class x = a; x != null; x = x.getSuperclass()) {
            if (x == b) {
                return true;
            }

            if (b.isInterface()) {
                Class interfaces[] = x.getInterfaces();
                for (int i = 0; i < interfaces.length; i++) {
                    if (isSubclass(interfaces[i], b)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
