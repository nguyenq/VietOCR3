/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sourceforge.vietocr;

public class SplitPdfArgs {
    private String inputFilename;
    private String outputFilename;
    private String fromPage;
    private String toPage;
    private String numOfPages;
    private boolean pages;

    /**
     * @return the inputFilename
     */
    public String getInputFilename() {
        return inputFilename;
    }

    /**
     * @param inputFilename the inputFilename to set
     */
    public void setInputFilename(String inputFilename) {
        this.inputFilename = inputFilename;
    }

    /**
     * @return the outputFilename
     */
    public String getOutputFilename() {
        return outputFilename;
    }

    /**
     * @param outputFilename the outputFilename to set
     */
    public void setOutputFilename(String outputFilename) {
        this.outputFilename = outputFilename;
    }

    /**
     * @return the fromPage
     */
    public String getFromPage() {
        return fromPage;
    }

    /**
     * @param fromPage the fromPage to set
     */
    public void setFromPage(String fromPage) {
        this.fromPage = fromPage;
    }

    /**
     * @return the toPage
     */
    public String getToPage() {
        return toPage;
    }

    /**
     * @param toPage the toPage to set
     */
    public void setToPage(String toPage) {
        this.toPage = toPage;
    }

    /**
     * @return the numOfPages
     */
    public String getNumOfPages() {
        return numOfPages;
    }

    /**
     * @param numOfPages the numOfPages to set
     */
    public void setNumOfPages(String numOfPages) {
        this.numOfPages = numOfPages;
    }

    /**
     * @return the pages
     */
    public boolean isPages() {
        return pages;
    }

    /**
     * @param pages the pages to set
     */
    public void setPages(boolean pages) {
        this.pages = pages;
    }
}
