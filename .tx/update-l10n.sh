#!/bin/sh

tx pull --all --force --minimum-perc 75 --mode onlytranslated
# Re-pull readme files with untranslated strings in English (instead of omitted)
tx pull --translations --force --minimum-perc 75 vietocr.readme_html
for i in `find . -name '*_??.properties' | grep -v '_vi\.properties'`; do
  native2ascii -encoding ISO-8859-1 "$i" "$i.tmp"
  awk '!/^[^=]+=$/' "$i.tmp" > "$i"
  rm "$i.tmp"
done
