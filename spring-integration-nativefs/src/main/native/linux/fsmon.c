/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Contributed by:
 * Josh Long (josh.long@springsource.com)
 * Mario Gray (mario.gray@gmail.com)
 */

#include "fsmon.h" 
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

	JNIEXPORT void JNICALL Java_org_springframework_integration_nativefs_fsmon_LinuxInotifyDirectoryMonitor_startMonitor(
		JNIEnv * env, 
		jobject obj, 
		jstring javaSpecifiedPath) 
	{
	  	int fd = inotify_init();
		if ( fd < 0 )  perror( "inotify_init" );

		char * path = (char *)(*env)->GetStringUTFChars( env, javaSpecifiedPath , NULL ) ;
		int wd = inotify_add_watch( fd, path,  IN_MOVED_TO| IN_CLOSE_WRITE );

		jclass cls = ( *env)->GetObjectClass(env, obj);
		jmethodID mid = (*env)->GetMethodID(env, cls, "fileReceived", "(Ljava/lang/String;Ljava/lang/String;)V");

		if( mid == 0 ) {
			printf( "method callback is not valid!") ;
			return ;
		}

		while(1>0){
			int length = 0;
			int i = 0;
			char buffer[BUF_LEN];
			length = read( fd, buffer, BUF_LEN );

			if ( length < 0 ) {
				perror( "read" );
			}

			while ( i < length ) {
				struct inotify_event *event = ( struct inotify_event * ) &buffer[ i ];
		  		if ( event->len ) {
					if ( event->mask & IN_CLOSE_WRITE || event->mask & IN_MOVED_TO ) {
						char *name = event->name;
						const int mlen = event->len;
						char nc[mlen];
						int indx;
						for(indx=0; indx < event->len; indx++) {
			  				char c  =(char) name[indx];
			  				nc[indx]=c;
						}
						jstring jpath = (*env)->NewStringUTF( env, (const char*) nc  );
						(*env)->CallVoidMethod(env, obj, mid, javaSpecifiedPath, jpath );
					}
		  		}
				i += EVENT_SIZE + event->len;
			}
		}
		( void ) inotify_rm_watch( fd, wd );
		( void ) close( fd );
	}
#ifdef __cplusplus
}
#endif
#endif
