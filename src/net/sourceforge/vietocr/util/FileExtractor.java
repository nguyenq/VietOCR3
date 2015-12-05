package net.sourceforge.vietocr.util;

import com.ice.tar.TarEntry;
import com.ice.tar.TarInputStream;
import java.io.*;
import java.util.zip.*;

public class FileExtractor {

    final static int BUFFER_SIZE = 1024;

    public static void extractCompressedFile(String compressedArchiveName, String destFolder) throws Exception {
        if (compressedArchiveName.toLowerCase().endsWith(".zip")) {
            extractZipFile(compressedArchiveName, destFolder);
        } else if (compressedArchiveName.toLowerCase().endsWith(".tar.gz")) {
            extractTGZ(compressedArchiveName, destFolder);
        } else if (compressedArchiveName.toLowerCase().endsWith(".gz")) {
            extractGZip(compressedArchiveName, destFolder);
        } else {
            File source = new File(compressedArchiveName);
            source.renameTo(new File(destFolder, source.getName()));
        }
    }

    public static void extractZipFile(String filename, String destFolder) throws Exception {
        ZipInputStream zipinputstream = new ZipInputStream(new FileInputStream(filename));
        ZipEntry zipEntry;

        while ((zipEntry = zipinputstream.getNextEntry()) != null) {
            if (zipEntry.isDirectory()) {
//                    new File(zipEntry.getName()).mkdirs();
                continue;
            }
            File outputFile = new File(destFolder, new File(zipEntry.getName()).getName());
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }
            FileOutputStream fos = new FileOutputStream(outputFile);
            byte[] buf = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = zipinputstream.read(buf, 0, BUFFER_SIZE)) > -1) {
                fos.write(buf, 0, bytesRead);
            }
            fos.close();
            zipinputstream.closeEntry();
        }
        zipinputstream.close();
    }

    public static void extractGZip(String filename, String destFolder) throws Exception {
        File inputFile = new File(filename);
        GZIPInputStream gzipinputstream = new GZIPInputStream(new FileInputStream(inputFile));
        File outputFile = new File(destFolder, inputFile.getName().substring(0, inputFile.getName().length() - ".gz".length()));
        FileOutputStream fos = new FileOutputStream(outputFile);
        byte[] buf = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = gzipinputstream.read(buf, 0, BUFFER_SIZE)) > -1) {
            fos.write(buf, 0, bytesRead);
        }
        fos.close();
        gzipinputstream.close();
    }

    public static void extractTarFile(String filename, String destFolder) throws Exception {
        TarInputStream tarinputstream = new TarInputStream(new FileInputStream(filename));
        TarEntry tarEntry;

        while ((tarEntry = tarinputstream.getNextEntry()) != null) {
            if (tarEntry.isDirectory()) {
//                    new File(tarEntry.getName()).mkdirs();
                continue;
            }
            File outputFile = new File(destFolder, new File(tarEntry.getName()).getName());
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }
            FileOutputStream fos = new FileOutputStream(outputFile);
            byte[] buf = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = tarinputstream.read(buf, 0, BUFFER_SIZE)) > -1) {
                fos.write(buf, 0, bytesRead);
            }
            fos.close();
        }
        tarinputstream.close();
    }

    public static void extractTGZ(String filename, String destFolder) throws Exception {
        extractGZip(filename, new File(filename).getParent()); // to the same folder
        extractTarFile(filename.substring(0, filename.length() - ".gz".length()), destFolder);
    }
}
