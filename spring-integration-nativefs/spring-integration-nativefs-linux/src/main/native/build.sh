
#javah -classpath . a.b.c.nativefs.NativeFileSystemMonitor
# http://www.gentoo.org/proj/en/base/amd64/howtos/index.xml?part=1&chap=3
# https://wiki.ubuntu.com/PackagingGuide/SharedLibraries


#JDK_DIR="`which javac`"
#JDK_INCLUDE_DIR="`dirname $JDK_DIR`/../include";

export JDK_INCLUDE_DIR="$JAVA_HOME/include";
touch libsifsmon.so; rm libsifsmon.so;
gcc  -o libsifsmon.so -shared  -fPIC -I$JDK_INCLUDE_DIR -I$JDK_INCLUDE_DIR/linux fsmon.c -lc ;


#java -Djava.library.path=`pwd` -cp . a.b.c.NativeFileSystemMonitor
