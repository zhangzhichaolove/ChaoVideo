#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../app/src/androidTest/assets/subtitle-fixtures"
ffmpeg -hide_banner -loglevel warning -y -f lavfi -i testsrc=size=240x136:rate=12 \
  -f lavfi -i anullsrc=r=22050:cl=mono -i english.srt -i french.srt \
  -map 0:v -map 1:a -map 2:s -map 3:s -t 30 \
  -c:v libx264 -pix_fmt yuv420p -preset ultrafast -g 12 -sc_threshold 0 \
  -c:a aac -b:a 24k -c:s srt -metadata:s:s:0 language=eng -metadata:s:s:0 title=English \
  -metadata:s:s:1 language=fra -metadata:s:s:1 title=French -disposition:s:0 default -disposition:s:1 0 dual.mkv
ffmpeg -hide_banner -loglevel warning -y -i dual.mkv -map 0:v -map 0:a -c copy plain.mp4
