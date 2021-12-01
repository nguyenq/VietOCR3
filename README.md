## VietOCR 

A Java GUI frontend for Tesseract OCR engine. Supports optical character recognition for Vietnamese and other languages supported by Tesseract.

VietOCR is released and distributed under the [Apache License, v2.0](http://www.apache.org/licenses/LICENSE-2.0).

## Features

* Multi-platform
* PDF, TIFF, JPEG, GIF, PNG, BMP image formats
* Multi-page TIFF images
* Screenshots
* Selection box
* File drag-and-drop
* Paste image from clipboard
* Text search and replace
* Postprocessing for Vietnamese to boost accuracy rate
* Vietnamese input methods
* Localized user interface for many languages ([Localization project](https://www.transifex.com/projects/p/vietocr/))
* Integrated scanning support
* Watch folder monitor for support of batch processing
* Custom text replacement in postprocessing
* Spellcheck with Hunspell
* Support for downloading and installing language data packs and appropriate spell dictionaries

## Instructions

To launch the program from the command line:
```
java -jar VietOCR.jar
```
or for CLI option:
```
java -jar VietOCR.jar imagefile outputfile [-l lang] [--psm pagesegmode] [text|hocr|pdf|pdf_textonly|unlv|box|alto|tsv|lstmbox|wordstrbox] [postprocessing] [correctlettercases] [deskew] [removelines] [removelinebreaks]
```

## Dependencies
* [Java Runtime Environment 8 or later](https://www.oracle.com/java/technologies/downloads/)
* On Windows: [Microsoft Visual C++ 2022 Redistributable Package](https://visualstudio.microsoft.com/downloads/)
* [GPL Ghostscript](http://www.ghostscript.com)