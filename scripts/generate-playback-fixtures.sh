#!/usr/bin/env bash
# Synthetic pattern, two distinguishable sine waves and embedded SubRip captions.
set -euo pipefail
cd "$(dirname "$0")/../app/src/androidTest/assets/playback-fixtures"
ffmpeg -hide_banner -loglevel warning -y -f lavfi -i testsrc=size=160x90:rate=12 \
  -f lavfi -i sine=frequency=440:sample_rate=44100 \
  -f lavfi -i sine=frequency=880:sample_rate=44100 -i captions.srt \
  -map 0:v -map 1:a -map 2:a -map 3:s -t 30 \
  -c:v libx264 -pix_fmt yuv420p -preset ultrafast -g 12 -sc_threshold 0 \
  -c:a aac -b:a 24k -c:s srt \
  -metadata:s:a:0 language=eng -metadata:s:a:0 title='English 440 Hz' \
  -metadata:s:a:1 language=jpn -metadata:s:a:1 title='Japanese 880 Hz' \
  -metadata:s:s:0 language=eng -disposition:s:0 default multi-track.mkv
