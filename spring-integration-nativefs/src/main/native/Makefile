#gcc  -o libsifsmon.so -shared  -fPIC -I$JDK_INCLUDE_DIR -I$JDK_INCLUDE_DIR/linux fsmon.c -lc ;

jdk_include_dirs	:= $(JAVA_HOME)/include

libsifsmon.so:	fsmon.o
	gcc -shared -lc -o libsifsmon.so fsmon.o

fsmon.o:	fsmon.c
	gcc  -o libsifsmon.so -shared  -fPIC -I$JDK_INCLUDE_DIR -I$JDK_INCLUDE_DIR/linux fsmon.c -lc