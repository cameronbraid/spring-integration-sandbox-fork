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
 * Mario Gray (mario.gray@gmail.com)
 * Josh Long (josh.long@springsource.com)
 *
 *
 * based on the logic demonstrated in the fsnotifier agent shipped with the IntelliJ IDEA Community Edition project
 *
 *
 * uses code from http://uthash.sourceforge.net/ to handle a hashmap
 *
 */

#include <CoreServices/CoreServices.h>
#include <jni.h>
#include "utstring.h"
#include "uthash.h"

/** 
 * shared map of contexts so that multiple threads of execution can contain references to unique JNI constructs
 */
static struct jnictx *contexts = NULL;  /* the dictionary *must* be intialized to null */

/** 
 * the JNI context is our encapsulation of the various JNI constructs, as well as the path under monitor. We store it in the contexts* uthash and look up entries by the path experiencing an event.
 */
struct jnictx {
    JNIEnv * env ;
	jobject thisPtr;
	jclass classId;
	jmethodID  methodId;
	char * path;
	UT_hash_handle hh ; // to make this hashable
};

/**
 * simple 'DAO' function to add a record to the contexts* hash 
 */
void add_jnictx( char *path, JNIEnv *env, jclass clz, jobject thisPtr, jmethodID mid  ){
    struct jnictx * ctx;
    ctx = (struct jnictx*) malloc( sizeof(struct jnictx) );
    ctx->path = path;
    ctx->env = env;
    ctx->thisPtr = thisPtr;
    ctx->methodId = mid;
    ctx->classId = clz;
    HASH_ADD_KEYPTR( hh, contexts , ctx->path, strlen(ctx->path),ctx );
}

/** 
 * looks up jnictx records by path which is the key
 */
struct jnictx * find_jnictx(char * path){ 
	struct jnictx * c;
	HASH_FIND_STR( contexts, path, c);
	return  c;
}


 /**  
  * this is where we'll inject the JNI notification logic. this doesn't tell us which file 
  * has changed, but it is enough to trigger a rescan from the java side 
  */
void notify_path_changed(char *path){

	struct jnictx * ctx = find_jnictx( path) ;

    JNIEnv * env = (*ctx).env;
    jobject obj = (*ctx).thisPtr ;

    if ((*env)->MonitorEnter(env, obj) != JNI_OK) {
        printf( "couldn't accquire JNI monitor lock!");
        fflush(stdout);
    }

	char * p =  path;
    jclass cls =  (*ctx).classId;
    jmethodID mid = (*ctx).methodId;
    jstring jpath = (*env)->NewStringUTF( env , path  );

    (*env)->CallVoidMethod(env, obj , mid,  jpath );

	if ((*env)->MonitorExit(env, obj) != JNI_OK) {
	 	printf("couldn't release JNI monitor lock!");  
		fflush(stdout);
	}
	
}

void file_system_changed_callback(ConstFSEventStreamRef streamRef, void *clientCallBackInfo, size_t numEvents, void *eventPaths, const FSEventStreamEventFlags eventFlags[], const FSEventStreamEventId eventIds[]) {
    char **paths = eventPaths;
    int i;
    for (i=0; i<numEvents; i++) {
		FSEventStreamEventFlags flags = eventFlags[i];
		if (flags == kFSEventStreamEventFlagMount || flags == kFSEventStreamEventFlagUnmount) {
			// noop 
		} else if (eventFlags[i] != kFSEventStreamEventFlagNone) {
			// noop
		} else {// if ((flags & kFSEventStreamEventFlagMustScanSubDirs) != 0) {
			notify_path_changed( paths[i] );
		}
    }
}

/**
 * initially this logic was being threaded off using pthreads, but the FSEventStreamCreate call itself has its own threading, and there's no need
 * for this extra layer of threading, especially as it makes it difficult to consume from Java because of the extra complexity of threading in JNI code
 */
void *event_processing_thread( char * path ) {

    char *pathToMonitor =  path ;

    CFStringRef mypath = CFStringCreateWithCString(NULL, pathToMonitor, NULL);
	
    CFArrayRef pathsToWatch = CFArrayCreate(NULL, (const void **)&mypath, 1, NULL);

    CFAbsoluteTime latency = 0.3;
    FSEventStreamRef stream = FSEventStreamCreate(NULL, &file_system_changed_callback, NULL ,
             pathsToWatch, kFSEventStreamEventIdSinceNow, latency, kFSEventStreamCreateFlagNoDefer);
	
    FSEventStreamScheduleWithRunLoop(stream, CFRunLoopGetCurrent(), kCFRunLoopDefaultMode);
    FSEventStreamStart(stream);
	
    CFRunLoopRun();
    return NULL;
}

void start_monitor( char * path ){
	if (FSEventStreamCreate == NULL) {
		printf("the file system event stream API isn't available (must be run on OS X 10.5 or later)\n");
		fflush(stdout);
		return ;
	}
	event_processing_thread(  path );
}

/**
 * the shared library exports this function specifically for consumption from Java
 *
**/
#ifndef _Included_org_springframework_integration_nativefs_fsmon_OsXDirectoryMonitor
#define _Included_org_springframework_integration_nativefs_fsmon_OsXDirectoryMonitor
#ifdef __cplusplus
extern "C" {
#endif
	/** used by Java code to start the monitoring process */
	JNIEXPORT void JNICALL Java_org_springframework_integration_nativefs_fsmon_OsXDirectoryMonitor_monitor( JNIEnv * env,  jobject obj,  jstring javaSpecifiedPath)
	{
		char * path = (char *)(*env)->GetStringUTFChars( env, javaSpecifiedPath , NULL ) ;
		jclass cls = (*env)->GetObjectClass( env,  obj);
        jmethodID mid = (*env)->GetMethodID(env, cls, "pathChanged", "(Ljava/lang/String;)V");
	 	add_jnictx(path, env, cls, obj, mid );
		start_monitor( path );
	}

#ifdef __cplusplus
}
#endif
#endif 
 
