
export JDK_INCLUDE_DIR=/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Headers/

gcc  -dynamiclib -fPIC -I$JDK_INCLUDE_DIR -I$JDK_INCLUDE_DIR/ -lc -ldl -framework CoreServices -o libsifsmon.dylib fsmon.c