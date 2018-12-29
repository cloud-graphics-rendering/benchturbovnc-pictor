#ifndef STOP_BENCH

#include<sys/ipc.h>
#include<sys/shm.h>

#include <sys/types.h>
#include <unistd.h>
#define TIME_COLUM 10
#define NUM_ROW 100

typedef struct timeTrack{
    int eventID;
    int valid;
    long array[TIME_COLUM];
}timeTrack;

#endif
