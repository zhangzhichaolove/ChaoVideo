#!/usr/bin/env python3
"""Loopback API/media fixture for the actual minified APK; no external server or media."""
import http.server
import json
import pathlib
import sys
import urllib.parse

media = (pathlib.Path(__file__).resolve().parents[1] /
         'app/src/androidTest/assets/playback-fixtures/multi-track.mkv').read_bytes()


class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        path = urllib.parse.urlsplit(self.path).path
        if path == '/multi-track.mkv':
            start, end = 0, len(media) - 1
            partial = self.headers.get('Range', '').startswith('bytes=')
            if partial:
                first, last = self.headers['Range'][6:].split('-', 1)
                start = int(first)
                if last:
                    end = min(end, int(last))
            if start < 0 or start > end:
                self.send_error(416)
                return
            self.send_response(206 if partial else 200)
            self.send_header('Content-Type', 'video/x-matroska')
            self.send_header('Accept-Ranges', 'bytes')
            if partial:
                self.send_header('Content-Range', f'bytes {start}-{end}/{len(media)}')
            self.send_header('Content-Length', str(end - start + 1))
            self.end_headers()
            self.wfile.write(media[start:end + 1])
            return
        if path not in ('/video/getVideoBanner', '/video/getVideoList'):
            self.send_error(404)
            return
        videos = [{'id': f'release-playback-fixture-{self.server.server_port}', 'title': 'Release playback fixture',
                   'video': 'must-not-guess_1.mkv', 'episodeUrls': ['multi-track.mkv'],
                   'episodes': 99, 'videoDescribe': 'Synthetic pattern with two audio tracks.'}]
        result = videos if path.endswith('getVideoBanner') else {'records': videos}
        body = json.dumps({'success': True, 'msg': '', 'result': result}).encode()
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)


server = http.server.ThreadingHTTPServer(('127.0.0.1', 0), Handler)
pathlib.Path(sys.argv[1]).write_text(str(server.server_port))
server.serve_forever()
