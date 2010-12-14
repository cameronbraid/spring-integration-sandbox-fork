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
 * @author Mario Gray
 * @author Josh Long
 *
 * based on the logic demonstrated in the fsnotifier agent shipped with the IntelliJ IDEA Community Edition project
 */

#include <CoreServices/CoreServices.h>
#include <sys/mount.h>
#include <jni.h>

 JNIEnv * sharedEnv ; // shared reference to the JNIEnv
 jobject  sharedObj ;     // shared reference to the jobject

// provides the jni context
struct jnictx {
    JNIEnv * env ; /* the reference to the JNIenv when we launched the monitor from Java */
    char * path ;  /* the original path registered*/
    jobject thisPtr;  /* reference to the Java 'this' pointer */

};


 /**  
  * this is where we'll inject the JNI notification logic. this doesn't tell us which file 
  * has changed, but it is enough to trigger a rescan from the java side 
  */
void notifyPathChanged( struct jnictx *ctx, char *path){

    printf( "inside notifyPathChanged");

    JNIEnv * env = (*ctx).env;
    jobject obj = (*ctx).thisPtr ;



    jclass cls = ( *env)->GetObjectClass( env,  obj);
    if(cls)  printf( "retrived cls\n");
    fflush(stdout);

    jmethodID mid = (*env)->GetMethodID(env, cls, "pathChanged", "(Ljava/lang/String;)V");      // (Ljava/lang/String;)V
    if(mid) printf( "the mid has been retreived");
    fflush(stdout);


    jstring jpath = (*env)->NewStringUTF( env , path  );

    if(  jpath) printf("the jpath is not null");
    fflush(stdout);

    (*env)->CallVoidMethod(env, obj , mid,  jpath );

}




void fileSystemChangedCallback(ConstFSEventStreamRef streamRef, void *clientCallBackInfo, size_t numEvents, void *eventPaths, const FSEventStreamEventFlags eventFlags[], const FSEventStreamEventId eventIds[]) {
    struct FSEventStreamContext *ctx = (FSEventStreamContext*) clientCallBackInfo ;
    if(ctx) printf( "FSEventStreamContext ctx \n"); fflush(stdout);

    void *info = (*ctx).info ;
    if( info ) printf( "info is not null\n"); fflush(stdout);

    struct jnictx * jctx = (struct jnictx*) info;

    if( jctx) printf( "jctx is not null"); fflush(stdout);

    char * p = (char*)( (*jctx).path );
    if(p) printf( "the path inside fileSystemChangedCallback: %s \n",p);
    fflush(stdout);

    char **paths = eventPaths;
    int i;
    for (i=0; i<numEvents; i++) {
		FSEventStreamEventFlags flags = eventFlags[i];
 
		if (flags == kFSEventStreamEventFlagMount || flags == kFSEventStreamEventFlagUnmount) {
			// noop 
		}
		else if ((flags & kFSEventStreamEventFlagMustScanSubDirs) != 0) {
			notifyPathChanged(jctx,paths[i]);
		}
		else if (eventFlags[i] != kFSEventStreamEventFlagNone) {
			// noop 
		}
		else {
			notifyPathChanged(jctx,paths[i]);
		}
    }
} 

/** the logic run with a thread */
void *event_processing_thread(void *data) {

	struct jnictx *ctx = (struct jnictx*) data;
	printf( "inside event_processing_thread, the path is: %s \n", (*ctx).path);
	fflush(stdout);

    struct FSEventStreamContext fsc = { NULL, ctx, NULL, NULL, NULL } ;

    /*({
        CFIndex version;
        void *info;
        CFAllocatorRetainCallBack retain;
        CFAllocatorReleaseCallBack release;
        CFAllocatorCopyDescriptionCallBack copyDescription;
    }; */


	char *pathToMonitor =  ctx->path;

    CFStringRef mypath = CFStringCreateWithCString(NULL, (const char*)pathToMonitor, NULL);
	
    CFArrayRef pathsToWatch = CFArrayCreate(NULL, (const void **)&mypath, 1, NULL);
    //void *cbInfo = NULL;
    CFAbsoluteTime latency = 0.3; 	
    FSEventStreamRef stream = FSEventStreamCreate(NULL, &fileSystemChangedCallback, &fsc,
             pathsToWatch, kFSEventStreamEventIdSinceNow, latency, kFSEventStreamCreateFlagNoDefer);
	
    FSEventStreamScheduleWithRunLoop(stream, CFRunLoopGetCurrent(), kCFRunLoopDefaultMode);
    FSEventStreamStart(stream);
	
    CFRunLoopRun();
    return NULL;
}




/** this is the hook we'll export for clients to consume. */
void startMonitor( struct jnictx *ctx ){

    char * path = (*ctx).path ;
    printf( "startMonitor: %s \n", path);
    fflush(stdout);


    if (FSEventStreamCreate == NULL) {
		printf("the file system event stream API isn't available (must be run on OS X 10.5 or later)\n");
		return ;
    }
	
    pthread_t thread_id;
    int threadId = pthread_create(&thread_id, NULL, event_processing_thread, ctx);
	
    if (threadId != 0) {
		printf("couldn't spawn thread for event processing.\n");
		exit(1);
    }
	
    while (TRUE) {
	    // noop
    }
}




#ifndef _Included_org_springframework_integration_nativefs_fsmon_OsXDirectoryMonitor

#define _Included_org_springframework_integration_nativefs_fsmon_OsXDirectoryMonitor

#ifdef __cplusplus
    extern "C" {
#endif


	JNIEXPORT void JNICALL Java_org_springframework_integration_nativefs_fsmon_OsXDirectoryMonitor_monitor( JNIEnv * env,  jobject obj,  jstring javaSpecifiedPath)
	{
		char * path = (char *)(*env)->GetStringUTFChars( env, javaSpecifiedPath , NULL ) ;
		printf( "monitoring %s \n", path);

		fflush(stdout); // multithreaded access compels us to flush the output

		struct jnictx jc = { env, path, obj } ;

        /** make sure we have a valid reference  */
        // if( !sharedEnv )  sharedEnv = env;

        //if(!sharedObj) sharedObj = obj;

		startMonitor(  &jc );
	}

#ifdef __cplusplus
    }
#endif

#endif
