cmake_minimum_required(VERSION 3.27)
project(w7ls)

set(CMAKE_CXX_STANDARD 14)

add_executable(w7ls
        arrival_time.cu
        collision_shell.cu
        mandelbrot_set.cu
        test.cu)
