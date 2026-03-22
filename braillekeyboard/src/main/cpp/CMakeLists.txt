cmake_minimum_required(VERSION 3.22)
project(braille_liblouis)

add_library(liblouis STATIC
    liblouis/liblouis/commonTranslationFunctions.c
    liblouis/liblouis/compileTranslationTable.c
    liblouis/liblouis/logging.c
    liblouis/liblouis/lou_backTranslateString.c
    liblouis/liblouis/lou_translateString.c
    liblouis/liblouis/maketable.c
    liblouis/liblouis/metadata.c
    liblouis/liblouis/pattern.c
    liblouis/liblouis/utils.c
)

target_include_directories(liblouis PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/liblouis/liblouis
)

target_compile_definitions(liblouis PRIVATE HAVE_CONFIG_H)

add_library(braille_liblouis_jni SHARED
    braille_liblouis_jni.c
)

target_include_directories(braille_liblouis_jni PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/liblouis/liblouis
)

target_link_libraries(braille_liblouis_jni
    liblouis
    log
)
