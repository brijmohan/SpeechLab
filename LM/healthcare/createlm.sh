#!/bin/sh
text2wfreq < language_model.txt | wfreq2vocab > lm.tmp.vocab
text2idngram -vocab lm.tmp.vocab -idngram lm.idngram < language_model.txt
idngram2lm -vocab_type 0 -idngram lm.idngram -vocab lm.tmp.vocab -arpa health.arpa
sphinx_lm_convert -i health.arpa -o health.lm.DMP