#include <jni.h>
#include <stdlib.h>
#include <string.h>

#include "liblouis.h"

JNIEXPORT void JNICALL
Java_com_screenreaders_blindroid_braillekeyboard_LiblouisNative_setDataPath(
        JNIEnv *env, jclass clazz, jstring path) {
    (void) clazz;
    if (path == NULL) {
        return;
    }
    const char *c_path = (*env)->GetStringUTFChars(env, path, NULL);
    if (c_path == NULL) {
        return;
    }
    lou_setDataPath(c_path);
    (*env)->ReleaseStringUTFChars(env, path, c_path);
}

JNIEXPORT jstring JNICALL
Java_com_screenreaders_blindroid_braillekeyboard_LiblouisNative_backTranslate(
        JNIEnv *env, jclass clazz, jstring table, jbyteArray cells) {
    (void) clazz;
    if (table == NULL || cells == NULL) {
        return NULL;
    }

    const char *table_c = (*env)->GetStringUTFChars(env, table, NULL);
    if (table_c == NULL) {
        return NULL;
    }

    jsize len = (*env)->GetArrayLength(env, cells);
    if (len <= 0) {
        (*env)->ReleaseStringUTFChars(env, table, table_c);
        return (*env)->NewString(env, (const jchar *)"", 0);
    }

    jbyte *bytes = (*env)->GetByteArrayElements(env, cells, NULL);
    if (bytes == NULL) {
        (*env)->ReleaseStringUTFChars(env, table, table_c);
        return NULL;
    }

    widechar *inbuf = (widechar *)calloc((size_t)len, sizeof(widechar));
    if (inbuf == NULL) {
        (*env)->ReleaseByteArrayElements(env, cells, bytes, JNI_ABORT);
        (*env)->ReleaseStringUTFChars(env, table, table_c);
        return NULL;
    }

    for (jsize i = 0; i < len; i++) {
        unsigned char cell = (unsigned char)bytes[i];
        inbuf[i] = (widechar)(0x2800 + cell);
    }

    int inlen = (int)len;
    int outcap = (int)(len * 4 + 16);
    widechar *outbuf = (widechar *)calloc((size_t)outcap, sizeof(widechar));
    if (outbuf == NULL) {
        free(inbuf);
        (*env)->ReleaseByteArrayElements(env, cells, bytes, JNI_ABORT);
        (*env)->ReleaseStringUTFChars(env, table, table_c);
        return NULL;
    }

    int outlen = outcap;
    int ok = lou_backTranslateString(table_c, inbuf, &inlen, outbuf, &outlen,
                                     NULL, NULL, 0);

    jstring result = NULL;
    if (ok != 0 && outlen >= 0) {
        result = (*env)->NewString(env, (const jchar *)outbuf, outlen);
    }

    free(outbuf);
    free(inbuf);
    (*env)->ReleaseByteArrayElements(env, cells, bytes, JNI_ABORT);
    (*env)->ReleaseStringUTFChars(env, table, table_c);
    return result;
}
