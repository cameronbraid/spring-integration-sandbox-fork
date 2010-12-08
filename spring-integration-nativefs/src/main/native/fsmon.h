/**
Contributed by:
Josh Long (josh@joshlong.com)
Mario Gray (mario.gray@gmail.com)
*/

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/inotify.h>
#include <malloc.h>

#define EVENT_SIZE  ( sizeof (struct inotify_event) )
#define BUF_LEN     ( 1024 * ( EVENT_SIZE + 16 ) )

#ifndef _Included_org_springframework_integration_nativefs_fsmon_LinuxInotifyDirectoryMonitor
#define _Included_org_springframework_integration_nativefs_fsmon_LinuxInotifyDirectoryMonitor
#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT void JNICALL Java_org_springframework_integration_nativefs_fsmon_LinuxInotifyDirectoryMonitor_startMonitor(JNIEnv *,jobject, jstring);
#ifdef __cplusplus
}
#endif
#endif
