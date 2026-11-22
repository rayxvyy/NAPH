package main

import (
	"context"
	"flag"
	"fmt"
	"net"
	"net/http"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"time"

	"github.com/anacrolix/torrent"
	"github.com/anacrolix/torrent/metainfo"
	"github.com/anacrolix/torrent/storage"
)

// Android does not have /etc/resolv.conf, which breaks Go's pure-Go DNS resolver (CGO_ENABLED=0).
// Automatically route DNS through 1.1.1.1 / 8.8.8.8 when /etc/resolv.conf is absent!
func initAndroidDnsFallback() {
	if _, err := os.Stat("/etc/resolv.conf"); err == nil {
		return
	}
	dnsServers := []string{"1.1.1.1:53", "8.8.8.8:53", "9.9.9.9:53"}
	net.DefaultResolver = &net.Resolver{
		PreferGo: true,
		Dial: func(ctx context.Context, network, address string) (net.Conn, error) {
			d := net.Dialer{Timeout: 5 * time.Second}
			var lastErr error
			for _, srv := range dnsServers {
				conn, err := d.DialContext(ctx, "udp", srv)
				if err == nil {
					return conn, nil
				}
				lastErr = err
			}
			return nil, lastErr
		},
	}
}

var defaultTrackers = []string{
	"udp://tracker.opentrackr.org:1337/announce",
	"udp://open.demonii.com:1337/announce",
	"udp://open.stealth.si:80/announce",
	"udp://tracker.torrent.eu.org:451/announce",
	"udp://exodus.desync.com:6969/announce",
	"udp://tracker.publictracker.xyz:6969/announce",
	"udp://zer0day.ch:1337/announce",
	"http://tracker.renfei.net:8080/announce",
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

func main() {
	initAndroidDnsFallback()

	hashFlag := flag.String("hash", "", "BitTorrent infoHash")
	fileIdxFlag := flag.Int("fileIdx", -1, "Requested file index (-1 for largest video)")
	cacheDirFlag := flag.String("cacheDir", "", "Cache directory")
	trackersFlag := flag.String("trackers", "", "Comma-separated extra tracker URLs")
	flag.Parse()

	infoHash := strings.ToLower(strings.TrimSpace(*hashFlag))
	if infoHash == "" {
		fmt.Println("ERROR|missing infoHash")
		os.Exit(1)
	}

	cacheDir := strings.TrimSpace(*cacheDirFlag)
	if cacheDir == "" {
		cacheDir = filepath.Join(os.TempDir(), "naph-p2p-cache")
	}
	if err := os.MkdirAll(cacheDir, 0o755); err != nil {
		fmt.Printf("ERROR|%v\n", err)
		os.Exit(1)
	}

	cfg := torrent.NewDefaultClientConfig()
	cfg.DataDir = cacheDir
	cfg.DefaultStorage = storage.NewFile(cacheDir)
	cfg.Seed = false
	cfg.NoUpload = false
	cfg.ListenPort = 0
	cfg.NoDHT = false

	client, err := torrent.NewClient(cfg)
	if err != nil {
		fmt.Printf("ERROR|%v\n", err)
		os.Exit(1)
	}
	defer client.Close()

	var ih metainfo.Hash
	if err := ih.FromHexString(infoHash); err != nil {
		fmt.Printf("ERROR|invalid infoHash: %v\n", err)
		os.Exit(1)
	}

	allTrackers := make([]string, 0, len(defaultTrackers)+8)
	if strings.TrimSpace(*trackersFlag) != "" {
		for _, tr := range strings.Split(*trackersFlag, ",") {
			tr = strings.TrimSpace(tr)
			if tr != "" {
				allTrackers = append(allTrackers, tr)
			}
		}
	}
	allTrackers = append(allTrackers, defaultTrackers...)

	spec := &torrent.TorrentSpec{
		InfoHash: ih,
		Trackers: [][]string{allTrackers},
	}
	t, _, err := client.AddTorrentSpec(spec)
	if err != nil {
		fmt.Printf("ERROR|%v\n", err)
		os.Exit(1)
	}

	// Wait up to 75s for torrent metadata
	select {
	case <-t.GotInfo():
	case <-time.After(75 * time.Second):
		fmt.Println("ERROR|Timed out fetching torrent metadata from peers")
		os.Exit(1)
	}

	targetFile := pickTargetFile(t, *fileIdxFlag)
	if targetFile == nil {
		fmt.Println("ERROR|No playable video file found in torrent")
		os.Exit(1)
	}

	targetFile.SetPriority(torrent.PiecePriorityNormal)

	ln, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		fmt.Printf("ERROR|%v\n", err)
		os.Exit(1)
	}
	port := ln.Addr().(*net.TCPAddr).Port

	mux := http.NewServeMux()
	mux.HandleFunc("/stream", func(w http.ResponseWriter, r *http.Request) {
		reader := targetFile.NewReader()
		defer reader.Close()
		reader.SetResponsive()
		reader.SetReadahead(24 * 1024 * 1024)
		w.Header().Set("Accept-Ranges", "bytes")
		http.ServeContent(w, r, filepath.Base(targetFile.Path()), time.Now(), reader)
	})

	go func() {
		_ = http.Serve(ln, mux)
	}()

	streamURL := fmt.Sprintf("http://127.0.0.1:%d/stream", port)
	fmt.Printf("READY|%s|%s|%d\n", streamURL, filepath.Base(targetFile.Path()), targetFile.Length())

	var lastBytes int64
	lastTime := time.Now()
	ticker := time.NewTicker(500 * time.Millisecond)
	defer ticker.Stop()

	for range ticker.C {
		now := time.Now()
		dt := now.Sub(lastTime).Seconds()
		completed := targetFile.BytesCompleted()
		var speed int64
		if dt > 0.05 && completed >= lastBytes {
			speed = int64(float64(completed-lastBytes) / dt)
		}
		lastBytes = completed
		lastTime = now

		stats := t.Stats()
		fmt.Printf("STATS|%d|%d|%d|%d|%d\n",
			stats.ActivePeers,
			stats.ConnectedSeeders,
			speed,
			completed,
			targetFile.Length(),
		)
	}
}
