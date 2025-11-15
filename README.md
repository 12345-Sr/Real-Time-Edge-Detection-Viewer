
# Real-Time Edge Detection - Android + OpenCV (C++) + OpenGL ES + TypeScript Web Viewer

This repository implements the Real-Time Edge Detection assessment. It contains:
- Android app using Camera2 + JNI OpenCV processing (Canny) + GLSurfaceView renderer
- Native C++ code (JNI) using OpenCV to convert NV21 -> grayscale/edges -> RGBA
- Web viewer (static) to display a sample processed frame

Build notes:
- Open with Android Studio (app folder)
- Ensure Android NDK, CMake and OpenCV Android SDK or AAR are available
- Place OpenCV SDK under `opencv/` or copy `.so` to `app/src/main/jniLibs/`
