package main

import (
	"fmt"
	"io"
	"net/http"
	"sync"
	"time"
)

func main() {
	// We use a shared HTTP client (which supports connection pooling)
	client := &http.Client{}
	var wg sync.WaitGroup
	start := time.Now()

	fmt.Println("--- Starting 5 Concurrent HTTP/1.1 Requests ---")

	for i := 0; i < 5; i++ {
		wg.Add(1)
		go func(id int) {
			defer wg.Done()

			// Hitting the server
			resp, err := client.Get("http://localhost:8080")
			if err != nil {
				fmt.Printf("Request %d failed: %v\n", id, err)
				return
			}
			// Must read and close body to allow connection reuse (if possible)
			io.Copy(io.Discard, resp.Body)
			resp.Body.Close()

			fmt.Printf("Finished Request %d\n", id)
		}(i)
	}

	wg.Wait()
	fmt.Printf("--- All requests finished in %v ---\n", time.Since(start))
}
