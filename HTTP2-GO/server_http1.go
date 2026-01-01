package main

import (
	"fmt"
	"net/http"
	"time"
)

func helloHandler(w http.ResponseWriter, r *http.Request) {
	// 1. Simulate the work
	time.Sleep(2 * time.Second)

	// 2. Print the Client's Port.
	// If these are different, it means different TCP connections!
	fmt.Printf("Received request from: %s\n", r.RemoteAddr)

	w.WriteHeader(http.StatusOK)
	w.Write([]byte("Hello!"))
}

func main() {
	http.HandleFunc("/", helloHandler)
	fmt.Println("HTTP/1.1 Server listening on :8080...")
	http.ListenAndServe(":8080", nil)
}
