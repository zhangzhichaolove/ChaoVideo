#!/usr/bin/env bash
# Synthetic test pattern and sine wave only; requires ffmpeg with libx264.
set -euo pipefail
cd "$(dirname "$0")/../app/src/androidTest/assets/offline-fixtures"
mkdir -p hls dash
ffmpeg -y -f lavfi -i testsrc=size=160x90:rate=12 -f lavfi -i sine=frequency=440:sample_rate=44100 \
  -t 4 -c:v libx264 -pix_fmt yuv420p -preset ultrafast -g 12 -sc_threshold 0 \
  -c:a aac -b:a 32k -movflags +faststart sample.mp4
ffmpeg -y -i sample.mp4 -c copy -hls_time 1 -hls_playlist_type vod \
  -hls_segment_filename 'hls/segment%d.ts' hls/index.m3u8
ffmpeg -y -i sample.mp4 -c copy -seg_duration 1 -use_template 1 -use_timeline 1 -f dash dash/manifest.mpd
