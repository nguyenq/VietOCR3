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
package net.sourceforge.vietocr;

import java.awt.Cursor;
import java.io.File;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import net.sourceforge.tess4j.util.ImageIOHelper;
import net.sourceforge.tess4j.util.PdfUtilities;
import net.sourceforge.vietocr.util.Utils;
import net.sourceforge.vietpad.components.SimpleFilter;

public class GuiWithTools extends GuiWithSpellcheck {

    private final String strImageFolder = "ImageFolder";
    File imageFolder;
    FileFilter selectedFilter;
    FileFilter tiffFilter = new SimpleFilter("tif;tiff", "TIFF");
    FileFilter pdfFilter = new SimpleFilter("pdf", "PDF");

    private final static Logger logger = Logger.getLogger(GuiWithTools.class.getName());

    public GuiWithTools() {
        imageFolder = new File(prefs.get(strImageFolder, System.getProperty("user.home")));
    }

    /**
     * Merges multiple images into a multi-page TIFF file.
     *
     * @param evt
     */
    @Override
    void jMenuItemMergeTiffActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser jf = new JFileChooser();
        jf.setDialogTitle(bundle.getString("Select_Input_Images"));
        jf.setCurrentDirectory(imageFolder);
        jf.setMultiSelectionEnabled(true);
        FileFilter jpegFilter = new SimpleFilter("jpg;jpeg", "JPEG");
        FileFilter gifFilter = new SimpleFilter("gif", "GIF");
        FileFilter pngFilter = new SimpleFilter("png", "PNG");
        FileFilter bmpFilter = new SimpleFilter("bmp", "Bitmap");
        FileFilter allImageFilter = new SimpleFilter("tif;tiff;jpg;jpeg;gif;png;bmp", bundle.getString("All_Image_Files"));

        jf.addChoosableFileFilter(tiffFilter);
        jf.addChoosableFileFilter(jpegFilter);
        jf.addChoosableFileFilter(gifFilter);
        jf.addChoosableFileFilter(pngFilter);
        jf.addChoosableFileFilter(bmpFilter);
        jf.addChoosableFileFilter(allImageFilter);

        if (selectedFilter != null) {
            jf.setFileFilter(selectedFilter);
        }

        jf.setAcceptAllFileFilterUsed(false);
        if (jf.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedFilter = jf.getFileFilter();
            final File[] inputs = jf.getSelectedFiles();
            imageFolder = jf.getCurrentDirectory();

            jf = new JFileChooser();
            jf.setDialogTitle(bundle.getString("Save_Multi-page_TIFF_Image"));
            jf.setCurrentDirectory(imageFolder);
            jf.setFileFilter(tiffFilter);
            jf.setAcceptAllFileFilterUsed(false);
            if (jf.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = jf.getSelectedFile();
                if (!(selectedFile.getName().endsWith(".tif") || selectedFile.getName().endsWith(".tiff"))) {
                    selectedFile = new File(selectedFile.getParent(), selectedFile.getName() + ".tif");
                }

                final File outputTiff = selectedFile;
                if (outputTiff.exists()) {
                    outputTiff.delete();
                }

                jLabelStatus.setText(bundle.getString("MergeTIFF_running..."));
                jProgressBar1.setIndeterminate(true);
                jProgressBar1.setString(bundle.getString("MergeTIFF_running..."));
                jProgressBar1.setVisible(true);
                getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                getGlassPane().setVisible(true);

                SwingWorker worker = new SwingWorker<File, Void>() {

                    @Override
                    protected File doInBackground() throws Exception {
                        ImageIOHelper.mergeTiff(inputs, outputTiff);
                        return outputTiff;
                    }

                    @Override
                    protected void done() {
                        jLabelStatus.setText(bundle.getString("MergeTIFFcompleted"));
                        jProgressBar1.setIndeterminate(false);
                        jProgressBar1.setString(bundle.getString("MergeTIFFcompleted"));

                        try {
                            File result = get();
                            JOptionPane.showMessageDialog(GuiWithTools.this, bundle.getString("MergeTIFFcompleted") + result.getName() + bundle.getString("created"), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
                        } catch (InterruptedException ignore) {
                            logger.log(Level.WARNING, ignore.getMessage(), ignore);
                        } catch (java.util.concurrent.ExecutionException e) {
                            String why;
                            Throwable cause = e.getCause();
                            if (cause != null) {
                                if (cause instanceof OutOfMemoryError) {
                                    why = bundle.getString("OutOfMemoryError");
                                } else {
                                    why = cause.getMessage();
                                }
                            } else {
                                why = e.getMessage();
                            }
                            logger.log(Level.SEVERE, why, e);
                            JOptionPane.showMessageDialog(GuiWithTools.this, why, APP_NAME, JOptionPane.ERROR_MESSAGE);
                        } finally {
                            jProgressBar1.setVisible(false);
                            getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                            getGlassPane().setVisible(false);
                        }
                    }
                };

                worker.execute();
            }
        }
    }

    /**
     * Splits a multi-page TIFF to individual TIFF files.
     *
     * @param evt
     */
    @Override
    void jMenuItemSplitTiffActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser jf = new JFileChooser();
        jf.setDialogTitle(bundle.getString("Select_Input_TIFF"));
//        jf.setApproveButtonText("Split");
        jf.setCurrentDirectory(imageFolder);
        jf.addChoosableFileFilter(tiffFilter);

        if (selectedFilter != null) {
            jf.setFileFilter(selectedFilter);
        }

        jf.setAcceptAllFileFilterUsed(false);
        if (jf.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedFilter = jf.getFileFilter();
            final File input = jf.getSelectedFile();
            imageFolder = jf.getCurrentDirectory();

            jLabelStatus.setText(bundle.getString("SplitTIFF_running..."));
            jProgressBar1.setIndeterminate(true);
            jProgressBar1.setString(bundle.getString("SplitTIFF_running..."));
            jProgressBar1.setVisible(true);
            getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            getGlassPane().setVisible(true);

            SwingWorker worker = new SwingWorker<Void, Void>() {

                @Override
                protected Void doInBackground() throws Exception {
                    String basefilename = Utils.stripExtension(input.getPath());
                    List<File> files = ImageIOHelper.createTiffFiles(input, -1, true);

                    // move temp TIFF files to selected folder
                    for (int i = 0; i < files.size(); i++) {
                        String outfilename = String.format("%s-%03d.tif", basefilename, i + 1);
                        File outfile = new File(outfilename);
                        outfile.delete();
                        files.get(i).renameTo(outfile);
                    }
                    return null;
                }

                @Override
                protected void done() {
                    jLabelStatus.setText(bundle.getString("SplitTIFFcompleted"));
                    jProgressBar1.setIndeterminate(false);
                    jProgressBar1.setString(bundle.getString("SplitTIFFcompleted"));

                    try {
                        get();
                        JOptionPane.showMessageDialog(GuiWithTools.this, bundle.getString("SplitTIFFcompleted"), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
                    } catch (InterruptedException ignore) {
                        logger.log(Level.WARNING, ignore.getMessage(), ignore);
                    } catch (java.util.concurrent.ExecutionException e) {
                        String why;
                        Throwable cause = e.getCause();
                        if (cause != null) {
                            if (cause instanceof OutOfMemoryError) {
                                why = bundle.getString("OutOfMemoryError");
                            } else {
                                why = cause.getMessage();
                            }
                        } else {
                            why = e.getMessage();
                        }
                        logger.log(Level.SEVERE, why, e);
                        JOptionPane.showMessageDialog(GuiWithTools.this, why, APP_NAME, JOptionPane.ERROR_MESSAGE);
                    } finally {
                        jProgressBar1.setVisible(false);
                        getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        getGlassPane().setVisible(false);
                    }
                }
            };

            worker.execute();
        }
    }

    /**
     * Merges PDF files.
     *
     * @param evt
     */
    @Override
    void jMenuItemMergePdfActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser jf = new JFileChooser();
        jf.setDialogTitle(bundle.getString("Select_Input_PDFs"));
        jf.setCurrentDirectory(imageFolder);
        jf.setMultiSelectionEnabled(true);
        jf.addChoosableFileFilter(pdfFilter);
        jf.setAcceptAllFileFilterUsed(false);

        if (jf.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            final File[] inputPdfs = jf.getSelectedFiles();
            imageFolder = jf.getCurrentDirectory();

            jf = new JFileChooser();
            jf.setDialogTitle(bundle.getString("Save_Merged_PDF"));
            jf.setCurrentDirectory(imageFolder);
            jf.setFileFilter(pdfFilter);
            jf.setAcceptAllFileFilterUsed(false);
            if (jf.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = jf.getSelectedFile();
                if (!(selectedFile.getName().endsWith(".pdf"))) {
                    selectedFile = new File(selectedFile.getParent(), selectedFile.getName() + ".pdf");
                }

                final File outputPdf = selectedFile;

                jLabelStatus.setText(bundle.getString("MergePDF_running..."));
                jProgressBar1.setIndeterminate(true);
                jProgressBar1.setString(bundle.getString("MergePDF_running..."));
                jProgressBar1.setVisible(true);
                getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                getGlassPane().setVisible(true);

                SwingWorker worker = new SwingWorker<File, Void>() {

                    @Override
                    protected File doInBackground() throws Exception {
                        PdfUtilities.mergePdf(inputPdfs, outputPdf);
                        return outputPdf;
                    }

                    @Override
                    protected void done() {
                        jLabelStatus.setText(bundle.getString("MergePDFcompleted"));
                        jProgressBar1.setIndeterminate(false);
                        jProgressBar1.setString(bundle.getString("MergePDFcompleted"));

                        try {
                            File result = get();
                            JOptionPane.showMessageDialog(GuiWithTools.this, bundle.getString("MergePDFcompleted") + result.getName() + bundle.getString("created"), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
                        } catch (InterruptedException ignore) {
                            logger.log(Level.WARNING, ignore.getMessage(), ignore);
                        } catch (java.util.concurrent.ExecutionException e) {
                            String why;
                            Throwable cause = e.getCause();
                            if (cause != null) {
                                if (cause instanceof OutOfMemoryError) {
                                    why = bundle.getString("OutOfMemoryError");
                                } else {
                                    why = cause.getMessage();
                                }
                            } else {
                                why = e.getMessage();
                            }
                            logger.log(Level.SEVERE, why, e);
                            JOptionPane.showMessageDialog(GuiWithTools.this, why, APP_NAME, JOptionPane.ERROR_MESSAGE);
                        } finally {
                            jProgressBar1.setVisible(false);
                            getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                            getGlassPane().setVisible(false);
                        }
                    }
                };

                worker.execute();
            }
        }
    }

    /**
     * Splits a PDF file into smaller files.
     *
     * @param evt
     */
    @Override
    void jMenuItemSplitPdfActionPerformed(java.awt.event.ActionEvent evt) {
        SplitPdfDialog dialog = new SplitPdfDialog(this, true);
        if (dialog.showDialog() == JOptionPane.OK_OPTION) {
            final SplitPdfArgs args = dialog.getArgs();

            jLabelStatus.setText(bundle.getString("SplitPDF_running..."));
            jProgressBar1.setIndeterminate(true);
            jProgressBar1.setString(bundle.getString("SplitPDF_running..."));
            jProgressBar1.setVisible(true);
            getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            getGlassPane().setVisible(true);

            SwingWorker worker = new SwingWorker<String, Void>() {

                @Override
                protected String doInBackground() throws Exception {
                    File inputFile = new File(args.getInputFilename());
                    String outputFilename = args.getOutputFilename();
                    File outputFile = new File(outputFilename);

                    if (args.isPages()) {
                        PdfUtilities.splitPdf(inputFile, outputFile, Integer.parseInt(args.getFromPage()), Integer.parseInt(args.getToPage()));
                    } else {
                        if (outputFilename.endsWith(".pdf")) {
                            outputFilename = outputFilename.substring(0, outputFilename.lastIndexOf(".pdf"));
                        }

                        int pageCount = PdfUtilities.getPdfPageCount(inputFile);
                        if (pageCount == 0) {
                            throw new RuntimeException("Split PDF failed.");
                        }

                        int pageRange = Integer.parseInt(args.getNumOfPages());
                        int startPage = 1;

                        while (startPage <= pageCount) {
                            int endPage = startPage + pageRange - 1;
                            outputFile = new File(outputFilename + startPage + ".pdf");
                            PdfUtilities.splitPdf(inputFile, outputFile, startPage, endPage);
                            startPage = endPage + 1;
                        }
                    }

                    return outputFilename;
                }

                @Override
                protected void done() {
                    jLabelStatus.setText(bundle.getString("SplitPDF_completed."));
                    jProgressBar1.setIndeterminate(false);
                    jProgressBar1.setString(bundle.getString("SplitPDF_completed."));

                    try {
                        String result = get();
                        JOptionPane.showMessageDialog(GuiWithTools.this, bundle.getString("SplitPDF_completed.") + bundle.getString("check_output_in") + new File(result).getParent());
                    } catch (InterruptedException ignore) {
                        logger.log(Level.WARNING, ignore.getMessage(), ignore);
                    } catch (java.util.concurrent.ExecutionException e) {
                        String why;
                        Throwable cause = e.getCause();
                        if (cause != null) {
                            if (cause instanceof OutOfMemoryError) {
                                why = bundle.getString("OutOfMemoryError");
                            } else {
                                why = cause.getMessage();
                            }
                        } else {
                            why = e.getMessage();
                        }
                        logger.log(Level.SEVERE, why, e);
                        JOptionPane.showMessageDialog(GuiWithTools.this, why, APP_NAME, JOptionPane.ERROR_MESSAGE);
                    } finally {
                        jProgressBar1.setVisible(false);
                        getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        getGlassPane().setVisible(false);
                    }
                }
            };

            worker.execute();
        }
    }

    @Override
    void jMenuItemConvertPdfActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser jf = new JFileChooser();
        jf.setDialogTitle(bundle.getString("Select_Input_PDF"));
        jf.setCurrentDirectory(imageFolder);
        jf.addChoosableFileFilter(pdfFilter);
        jf.setAcceptAllFileFilterUsed(false);

        if (jf.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            final File inputPdf = jf.getSelectedFile();
            imageFolder = jf.getCurrentDirectory();

            jf = new JFileChooser();
            jf.setDialogTitle(bundle.getString("Save_Multi-page_TIFF_Image"));
            jf.setCurrentDirectory(imageFolder);
            jf.setFileFilter(tiffFilter);
            jf.setAcceptAllFileFilterUsed(false);
            if (jf.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = jf.getSelectedFile();
                if (!(selectedFile.getName().endsWith(".tif") || selectedFile.getName().endsWith(".tiff"))) {
                    selectedFile = new File(selectedFile.getParent(), selectedFile.getName() + ".tif");
                }

                final File targetFile = selectedFile;

                jLabelStatus.setText(bundle.getString("ConvertPDF_running..."));
                jProgressBar1.setIndeterminate(true);
                jProgressBar1.setString(bundle.getString("ConvertPDF_running..."));
                jProgressBar1.setVisible(true);
                getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                getGlassPane().setVisible(true);

                SwingWorker worker = new SwingWorker<File, Void>() {

                    @Override
                    protected File doInBackground() throws Exception {
                        File outputTiffFile = PdfUtilities.convertPdf2Tiff(inputPdf);
                        Files.move(outputTiffFile.toPath(), targetFile.toPath(), REPLACE_EXISTING);
                        return targetFile;
                    }

                    @Override
                    protected void done() {
                        jLabelStatus.setText(bundle.getString("ConvertPDF_completed"));
                        jProgressBar1.setIndeterminate(false);
                        jProgressBar1.setString(bundle.getString("ConvertPDF_completed"));

                        try {
                            File result = get();
                            JOptionPane.showMessageDialog(GuiWithTools.this, bundle.getString("ConvertPDF_completed") + result.getName() + bundle.getString("created"), APP_NAME, JOptionPane.INFORMATION_MESSAGE);
                        } catch (InterruptedException ignore) {
                            logger.log(Level.WARNING, ignore.getMessage(), ignore);
                        } catch (java.util.concurrent.ExecutionException e) {
                            String why;
                            Throwable cause = e.getCause();
                            if (cause != null) {
                                if (cause instanceof OutOfMemoryError) {
                                    why = bundle.getString("OutOfMemoryError");
                                } else {
                                    why = cause.getMessage();
                                }
                            } else {
                                why = e.getMessage();
                            }
                            logger.log(Level.SEVERE, why, e);
                            JOptionPane.showMessageDialog(GuiWithTools.this, why, APP_NAME, JOptionPane.ERROR_MESSAGE);
                        } finally {
                            jProgressBar1.setVisible(false);
                            getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                            getGlassPane().setVisible(false);
                        }
                    }
                };

                worker.execute();
            }
        }
    }

    @Override
    void quit() {
        prefs.put(strImageFolder, imageFolder.getPath());
        super.quit();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        selectedUILang = prefs.get(strUILanguage, "en");
        Locale.setDefault(getLocale(selectedUILang));

        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                new GuiWithTools().setVisible(true);
            }
        });
    }
}
