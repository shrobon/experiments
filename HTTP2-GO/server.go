package main

import (
	"context"
	"fmt"
	"log"
	"net"
	"time"

	"google.golang.org/grpc"
	pb "google.golang.org/grpc/examples/helloworld/helloworld"
	"google.golang.org/grpc/peer"
)

type server struct {
	pb.UnimplementedGreeterServer
}

// SayHello simulates a slow process (e.g., DB query)
func (s *server) SayHello(ctx context.Context, in *pb.HelloRequest) (*pb.HelloReply, error) {
	// 2. Extract the "Peer" (the client connection info) from the context
	if p, ok := peer.FromContext(ctx); ok {
		fmt.Printf("Received request from: %s\n", p.Addr.String())
	}

	// Simulate 2 seconds of work
	time.Sleep(2 * time.Second)

	return &pb.HelloReply{Message: "Hello " + in.GetName()}, nil
}

func main() {
	lis, err := net.Listen("tcp", ":50051")
	if err != nil {
		log.Fatalf("failed to listen: %v", err)
	}
	s := grpc.NewServer()
	pb.RegisterGreeterServer(s, &server{})
	fmt.Println("Server listening on :50051...")
	if err := s.Serve(lis); err != nil {
		log.Fatalf("failed to serve: %v", err)
	}
}
