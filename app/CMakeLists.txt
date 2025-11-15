
cmake_minimum_required(VERSION 3.4.1)
project(edgedetect-native)
set(CMAKE_CXX_STANDARD 14)

# If you are using OpenCV Android SDK unpacked at <projectRoot>/opencv
set(OpenCV_DIR ${CMAKE_SOURCE_DIR}/../opencv/sdk/native/jni)
find_package(OpenCV REQUIRED COMPONENTS core imgproc)
include_directories(${OpenCV_INCLUDE_DIRS})

add_library(native-lib SHARED src/native-lib.cpp)
find_library(log-lib log)

target_link_libraries(native-lib ${OpenCV_LIBS} ${log-lib})
