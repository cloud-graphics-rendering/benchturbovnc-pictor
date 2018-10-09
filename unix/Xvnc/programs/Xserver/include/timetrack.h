#ifndef STOP_BENCH

#include<sys/ipc.h>
#include<sys/shm.h>

#include <sys/types.h>
#include <unistd.h>
#define TIME_COLUM 10
#define NUM_ROW 10

typedef struct timeTrack{
    int eventID;
    pid_t pid;
    long array[TIME_COLUM];
}timeTrack;

#endif
