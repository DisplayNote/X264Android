#include <stdio.h>

#if __ANDROID_API__ < 23
#undef stderr
FILE *stderr = &__sF[2];
#endif
