package main

import (
	"context"
	"fmt"
	"log"
	"sync"
	"time"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
	pb "google.golang.org/grpc/examples/helloworld/helloworld"
)

func main() {
	// 1. Create ONLY ONE TCP connection to the server
	conn, err := grpc.NewClient("localhost:50051", grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		log.Fatalf("did not connect: %v", err)
	}
	defer conn.Close()
	c := pb.NewGreeterClient(conn)

	var wg sync.WaitGroup
	start := time.Now()

	fmt.Println("--- Starting 5 Concurrent Requests ---")

	// 2. Launch 5 requests at the exact same time
	for i := 0; i < 5; i++ {
		wg.Add(1)
		go func(id int) {
			defer wg.Done()
			// Each request reuses 'c' (which uses the single 'conn')
			_, err := c.SayHello(context.Background(), &pb.HelloRequest{Name: fmt.Sprintf("Request-%d", id)})
			if err != nil {
				log.Fatalf("Error: %v", err)
			}
			fmt.Printf("Finished Request %d\n", id)
		}(i)
	}

	wg.Wait()
	fmt.Printf("--- All requests finished in %v ---\n", time.Since(start))
}
