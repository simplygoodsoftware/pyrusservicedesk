#import <Foundation/Foundation.h>

#if __has_attribute(swift_private)
#define AC_SWIFT_PRIVATE __attribute__((swift_private))
#else
#define AC_SWIFT_PRIVATE
#endif

/// The "chats" asset catalog image resource.
static NSString * const ACImageNameChats AC_SWIFT_PRIVATE = @"chats";

/// The "cross" asset catalog image resource.
static NSString * const ACImageNameCross AC_SWIFT_PRIVATE = @"cross";

/// The "fillFilter" asset catalog image resource.
static NSString * const ACImageNameFillFilter AC_SWIFT_PRIVATE = @"fillFilter";

/// The "filter" asset catalog image resource.
static NSString * const ACImageNameFilter AC_SWIFT_PRIVATE = @"filter";

/// The "iiko" asset catalog image resource.
static NSString * const ACImageNameIiko AC_SWIFT_PRIVATE = @"iiko";

/// The "paperclip" asset catalog image resource.
static NSString * const ACImageNamePaperclip AC_SWIFT_PRIVATE = @"paperclip";

#undef AC_SWIFT_PRIVATE
