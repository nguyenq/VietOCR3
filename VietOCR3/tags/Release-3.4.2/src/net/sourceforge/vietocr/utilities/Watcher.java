/**
 * Copyright @ 2009 Quan Nguyen
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
package net.sourceforge.vietocr.utilities;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Adapted from http://forums.sun.com/thread.jspa?forumID=31&threadID=5316582
 */
public class Watcher implements Runnable {

    private long lastTime = 0;
    private List<File> lastFiles = new ArrayList<File>();
    private File watchFolder;
    private Queue<File> queue;
    private boolean firstTimeEntered = true;
    private volatile boolean watchEnabled;

    public Watcher(Queue<File> q, File folder) {
        queue = q;
        watchFolder = folder;
    }

    @Override
    public void run() {
        while (true) {
            if (watchEnabled) {
                sniff();
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // not important
                e.printStackTrace();
            }
        }
    }

    private void sniff() {
        final long newTime = watchFolder.lastModified();
        if (lastTime < newTime) {
            // find modified files
            File[] files = watchFolder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().matches(".*\\.(tif|tiff|jpg|jpeg|gif|png|bmp|pdf)$");
                }
            });

            if (firstTimeEntered) {
                firstTimeEntered = false;
            } else {
                for (File file : files) {
                    if (!lastFiles.contains(file)) {
                        System.out.println("New file: " + file);
                        queue.offer(file);
                    }
                }
            }

            lastTime = newTime;
            lastFiles = Arrays.asList(files);
        }
    }

    /**
     * Enables watch function.
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        watchEnabled = enabled;
    }

    /**
     * Sets the folder to watch.
     *
     * @param path
     */
    public void setPath(File path) {
        watchFolder = path;
    }

    public static void main(String[] args) {
        Queue<File> queue = new LinkedList<File>();
        File watchFolder = new File(System.getProperty("user.home"));
        Thread t = new Thread(new Watcher(queue, watchFolder));
        t.start();
    }
}
