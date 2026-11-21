package main

/*
#include <stdlib.h>
*/
import "C"

import (
	"fmt"
	"net"
	"net/http"
	"os"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
	"sync"
	"time"
	"unsafe"

	"github.com/anacrolix/torrent"
	"github.com/anacrolix/torrent/metainfo"
	"github.com/anacrolix/torrent/storage"
)

type activeStream struct {
	infoHash  string
	torrent   *torrent.Torrent
	file      *torrent.File
	cacheDir  string
	lastBytes int64
	lastCheck time.Time
	speedBps  int64
}

var (
	mu            sync.Mutex
	client        *torrent.Client
	serverStarted bool
	serverPort    int
	streams       = make(map[string]*activeStream)
)

func ensureServerAndClient(cacheDir string) error {
	if err := os.MkdirAll(cacheDir, 0o755); err != nil {
		return err
	}

	if client == nil {
		cfg := torrent.NewDefaultClientConfig()
		cfg.DataDir = cacheDir
		cfg.DefaultStorage = storage.NewFile(cacheDir)
		cfg.Seed = false
		cfg.NoUpload = false
		cfg.ListenPort = 0
		cfg.DisableIPv6 = false
		cfg.NoDHT = false
		c, err := torrent.NewClient(cfg)
		if err != nil {
			return err
		}
		client = c
	}

	if !serverStarted {
		ln, err := net.Listen("tcp", "127.0.0.1:0")
		if err != nil {
			return err
		}
		serverPort = ln.Addr().(*net.TCPAddr).Port
		serverStarted = true

		mux := http.NewServeMux()
		mux.HandleFunc("/stream/", handleStreamHTTP)
		go func() {
			_ = http.Serve(ln, mux)
		}()
	}

	return nil
}

func isVideoFile(path string) bool {
	ext := strings.ToLower(filepath.Ext(path))
	switch ext {
	case ".mkv", ".mp4", ".avi", ".mov", ".webm", ".m4v", ".ts", ".m2ts":
		return true
	default:
		return false
	}
}

func pickTargetFile(t *torrent.Torrent, requestedIdx int) *torrent.File {
	files := t.Files()
	if len(files) == 0 {
		return nil
	}
	if requestedIdx >= 0 && requestedIdx < len(files) {
		return files[requestedIdx]
	}
	// Otherwise pick the largest video file in the torrent
	sorted := make([]*torrent.File, len(files))
	copy(sorted, files)
	sort.Slice(sorted, func(i, j int) bool {
		iv := isVideoFile(sorted[i].Path())
		jv := isVideoFile(sorted[j].Path())
		if iv != jv {
			return iv
		}
		return sorted[i].Length() > sorted[j].Length()
	})
	return sorted[0]
}

func handleStreamHTTP(w http.ResponseWriter, r *http.Request) {
	// Path format: /stream/<infoHash>/<fileIdx>
	trimmed := strings.TrimPrefix(r.URL.Path, "/stream/")
	parts := strings.Split(trimmed, "/")
	if len(parts) < 1 || parts[0] == "" {
		http.Error(w, "missing infoHash", http.StatusBadRequest)
		return
	}
	hash := strings.ToLower(parts[0])
	reqIdx := -1
	if len(parts) >= 2 {
		if parsed, err := strconv.Atoi(parts[1]); err == nil {
			reqIdx = parsed
		}
	}

	mu.Lock()
	st, ok := streams[hash]
	mu.Unlock()
	if !ok || st == nil || st.torrent == nil {
		http.Error(w, "stream not active", http.StatusNotFound)
		return
	}

	// Wait for torrent metadata if still fetching from magnet DHT/trackers
	select {
	case <-st.torrent.GotInfo():
	case <-r.Context().Done():
		return
	case <-time.After(45 * time.Second):
		http.Error(w, "timeout waiting for torrent metadata", http.StatusGatewayTimeout)
		return
	}

	mu.Lock()
	if st.file == nil {
		st.file = pickTargetFile(st.torrent, reqIdx)
		if st.file != nil {
			st.file.SetPriority(torrent.PiecePriorityNormal)
		}
	}
	f := st.file
	mu.Unlock()

	if f == nil {
		http.Error(w, "no playable file in torrent", http.StatusNotFound)
		return
	}

	reader := f.NewReader()
	defer reader.Close()
	reader.SetResponsive()
	reader.SetReadahead(32 * 1024 * 1024) // 32 MB sequential readahead for smooth 4K/1080p streaming

	w.Header().Set("Accept-Ranges", "bytes")
	w.Header().Set("Content-Type", "application/octet-stream")
	http.ServeContent(w, r, filepath.Base(f.Path()), time.Now(), reader)
}

//export NaphP2PStartStream
func NaphP2PStartStream(magnetC *C.char, infoHashC *C.char, fileIdx C.int, cacheDirC *C.char) *C.char {
	magnetUri := C.GoString(magnetC)
	hash := strings.ToLower(strings.TrimSpace(C.GoString(infoHashC)))
	cacheDir := C.GoString(cacheDirC)

	mu.Lock()
	defer mu.Unlock()

	if err := ensureServerAndClient(cacheDir); err != nil {
		return C.CString("")
	}

	// Reuse if already active
	if existing, ok := streams[hash]; ok && existing != nil {
		url := fmt.Sprintf("http://127.0.0.1:%d/stream/%s/%d", serverPort, hash, int(fileIdx))
		return C.CString(url)
	}

	var t *torrent.Torrent
	var err error
	if strings.HasPrefix(magnetUri, "magnet:") {
		t, err = client.AddMagnet(magnetUri)
	} else {
		var ih metainfo.Hash
		if parseErr := ih.FromHexString(hash); parseErr == nil {
			t, _ = client.AddTorrentInfoHash(ih)
		}
	}
	if err != nil || t == nil {
		return C.CString("")
	}

	st := &activeStream{
		infoHash:  hash,
		torrent:   t,
		cacheDir:  cacheDir,
		lastCheck: time.Now(),
	}
	streams[hash] = st

	// Kick off metadata fetch & sequential priority in background
	go func(s *activeStream, idx int) {
		select {
		case <-s.torrent.GotInfo():
			mu.Lock()
			s.file = pickTargetFile(s.torrent, idx)
			if s.file != nil {
				s.file.SetPriority(torrent.PiecePriorityNormal)
			}
			mu.Unlock()
		case <-time.After(60 * time.Second):
		}
	}(st, int(fileIdx))

	url := fmt.Sprintf("http://127.0.0.1:%d/stream/%s/%d", serverPort, hash, int(fileIdx))
	return C.CString(url)
}

//export NaphP2PGetPeers
func NaphP2PGetPeers(infoHashC *C.char) C.int {
	hash := strings.ToLower(strings.TrimSpace(C.GoString(infoHashC)))
	mu.Lock()
	st := streams[hash]
	mu.Unlock()
	if st == nil || st.torrent == nil {
		return 0
	}
	stats := st.torrent.Stats()
	return C.int(stats.ActivePeers)
}

//export NaphP2PGetSeeds
func NaphP2PGetSeeds(infoHashC *C.char) C.int {
	hash := strings.ToLower(strings.TrimSpace(C.GoString(infoHashC)))
	mu.Lock()
	st := streams[hash]
	mu.Unlock()
	if st == nil || st.torrent == nil {
		return 0
	}
	stats := st.torrent.Stats()
	return C.int(stats.ConnectedSeeders)
}

//export NaphP2PGetSpeedBps
func NaphP2PGetSpeedBps(infoHashC *C.char) C.longlong {
	hash := strings.ToLower(strings.TrimSpace(C.GoString(infoHashC)))
	mu.Lock()
	defer mu.Unlock()
	st := streams[hash]
	if st == nil || st.torrent == nil {
		return 0
	}
	now := time.Now()
	completed := st.torrent.BytesCompleted()
	elapsed := now.Sub(st.lastCheck).Seconds()
	if elapsed >= 0.5 {
		delta := completed - st.lastBytes
		if delta < 0 {
			delta = 0
		}
		st.speedBps = int64(float64(delta) / elapsed)
		st.lastBytes = completed
		st.lastCheck = now
	}
	return C.longlong(st.speedBps)
}

//export NaphP2PStopStream
func NaphP2PStopStream(infoHashC *C.char, deleteFiles C.int) C.longlong {
	hash := strings.ToLower(strings.TrimSpace(C.GoString(infoHashC)))
	mu.Lock()
	st, ok := streams[hash]
	if ok {
		delete(streams, hash)
	}
	mu.Unlock()

	var freed int64
	if st != nil && st.torrent != nil {
		freed = st.torrent.BytesCompleted()
		st.torrent.Drop()
		if deleteFiles != 0 && st.cacheDir != "" {
			_ = os.RemoveAll(st.cacheDir)
		}
	}
	return C.longlong(freed)
}

//export NaphP2PFreeString
func NaphP2PFreeString(ptr *C.char) {
	if ptr != nil {
		C.free(unsafe.Pointer(ptr))
	}
}

func main() {}
