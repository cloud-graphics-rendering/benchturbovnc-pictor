#ifndef STOP_BENCH

#include<sys/ipc.h>
#include<sys/shm.h>

#include <sys/types.h>
#include <unistd.h>
#define TIME_COLUM 10
#define NUM_ROW 1000

typedef char *XPointer;

typedef struct timeTrack{
    int eventID;
    int valid;
    long long array[TIME_COLUM];
}timeTrack;


/*
 * Extensions need a way to hang private data on some structures.
 */
typedef struct _XExtData {
        int number;             /* number returned by XRegisterExtension */
        struct _XExtData *next; /* next item on list of data for structure */
        int (*free_private)(    /* called to free private storage */
        struct _XExtData *extension
        );
        XPointer private_data;  /* data private to this extension. */
} XExtData;


/*
 * Visual structure; contains information about colormapping possible.
 */
typedef struct {
        XExtData *ext_data;     /* hook for extension to hang data */
        VisualID visualid;      /* visual id of this visual */
#if defined(__cplusplus) || defined(c_plusplus)
        int c_class;            /* C++ class of screen (monochrome, etc.) */
#else
        int class;              /* class of screen (monochrome, etc.) */
#endif
        unsigned long red_mask, green_mask, blue_mask;  /* mask values */
        int bits_per_rgb;       /* log base 2 of distinct color values */
        int map_entries;        /* color map entries */
} Visual;



/*
 * Data structure for "image" data, used by image manipulation routines.
 */
typedef struct _XImage {
    int width, height;          /* size of image */
    int xoffset;                /* number of pixels offset in X direction */
    int format;                 /* XYBitmap, XYPixmap, ZPixmap */
    char *data;                 /* pointer to image data */
    int byte_order;             /* data byte order, LSBFirst, MSBFirst */
    int bitmap_unit;            /* quant. of scanline 8, 16, 32 */
    int bitmap_bit_order;       /* LSBFirst, MSBFirst */
    int bitmap_pad;             /* 8, 16, 32 either XY or ZPixmap */
    int depth;                  /* depth of image */
    int bytes_per_line;         /* accelarator to next line */
    int bits_per_pixel;         /* bits per pixel (ZPixmap) */
    unsigned long red_mask;     /* bits in z arrangment */
    unsigned long green_mask;
    unsigned long blue_mask;
    XPointer obdata;            /* hook for the object routines to hang on */
    struct funcs {              /* image manipulation routines */
        struct _XImage *(*create_image)(
                struct _XDisplay* /* display */,
                Visual*         /* visual */,
                unsigned int    /* depth */,
                int             /* format */,
                int             /* offset */,
                char*           /* data */,
                unsigned int    /* width */,
                unsigned int    /* height */,
                int             /* bitmap_pad */,
                int             /* bytes_per_line */);
        int (*destroy_image)        (struct _XImage *);
        unsigned long (*get_pixel)  (struct _XImage *, int, int);
        int (*put_pixel)            (struct _XImage *, int, int, unsigned long);
        struct _XImage *(*sub_image)(struct _XImage *, int, int, unsigned int, unsigned int);
        int (*add_pixel)            (struct _XImage *, long);
        } f;
} XImage;


#endif
