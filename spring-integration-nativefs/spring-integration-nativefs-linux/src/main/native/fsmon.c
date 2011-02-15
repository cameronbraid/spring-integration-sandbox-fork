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

//#include "fsmon.h"
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/inotify.h>
#include <malloc.h>

#define EVENT_SIZE  ( sizeof (struct inotify_event) )
#define BUF_LEN     ( 1024 * ( EVENT_SIZE + 16 ) )

void note(char * msg){
    printf(msg, "");
    fflush(stdout);
}


#ifndef _Included_org_springframework_integration_nativefs_fsmon_linux_LinuxInotifyDirectoryMonitor
#define Java_org_springframework_integration_nativefs_fsmon_linux_LinuxInotifyDirectoryMonitor
#ifdef __cplusplus
extern "C" {
#endif

    /* good intro to inotify: http://www.linuxjournal.com/article/8478?page=0,0 */

	JNIEXPORT void JNICALL Java_org_springframework_integration_nativefs_fsmon_linux_LinuxInotifyDirectoryMonitor_start (
		JNIEnv * env, 
		jobject obj, 
		jstring javaSpecifiedPath) 
	{

	    note("starting monitor in C code. \n") ;

	  	int fd = inotify_init();

        note("returned from inotify_init()\n");
		if ( fd < 0 ) {
            note( "inotify_init() returned a bad value!\n");
            perror( "inotify_init" );
		}
		note( "no errors in inotify_init() \n");

		char * path = (char *)(*env)->GetStringUTFChars( env, javaSpecifiedPath , NULL ) ;

		note ("the path is " ) ;
		note(path);
		note( "\n");

		int wd = inotify_add_watch( fd, path,  IN_MOVED_TO| IN_CLOSE_WRITE );
        note( "returned from inotify_add_watch \n") ;
        if(wd<0){
        note( "couldn't add the watch, the return value of inotify_add_watch is -1 \n") ;
        perror( "inotify_add_watch");
        }

		jclass cls = ( *env)->GetObjectClass(env, obj);
		jmethodID mid = (*env)->GetMethodID(env, cls, "fileReceived", "(Ljava/lang/String;Ljava/lang/String;)V");

		if( mid == 0 ) {
			note( "method callback is not valid! \n") ;
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

		note("removing watch \n");
		( void ) inotify_rm_watch( fd, wd );

		note( "closing watch fd \n");
		( void ) close( fd );

		note( "returning. \n");
	}
#ifdef __cplusplus
}
#endif
#endif
