#!/bin/sh

tx pull -a -f --minimum-perc 75
for i in `find . -name '*_??.properties' | grep -v '_vi\.properties'`; do native2ascii -encoding ISO-8859-1 $i $i.tmp; mv $i.tmp $i; done
