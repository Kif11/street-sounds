all:
	g++ -std=c++11 -ggdb `pkg-config --cflags --libs /usr/local/Cellar/opencv/3.3.1_1/lib/pkgconfig/opencv.pc` -arch x86_64 /usr/local/lib/libzbar.dylib src/main.cpp src/qr_chunk.cpp src/base_64.cpp -o bin/main
