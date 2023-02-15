#if 0
#elif defined(__arm64__) && __arm64__
// Generated by Apple Swift version 5.7.2 (swiftlang-5.7.2.135.5 clang-1400.0.29.51)
#ifndef PYRUSSERVICEDESK_SWIFT_H
#define PYRUSSERVICEDESK_SWIFT_H
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wgcc-compat"

#if !defined(__has_include)
# define __has_include(x) 0
#endif
#if !defined(__has_attribute)
# define __has_attribute(x) 0
#endif
#if !defined(__has_feature)
# define __has_feature(x) 0
#endif
#if !defined(__has_warning)
# define __has_warning(x) 0
#endif

#if __has_include(<swift/objc-prologue.h>)
# include <swift/objc-prologue.h>
#endif

#pragma clang diagnostic ignored "-Wduplicate-method-match"
#pragma clang diagnostic ignored "-Wauto-import"
#if defined(__OBJC__)
#include <Foundation/Foundation.h>
#endif
#if defined(__cplusplus)
#include <cstdint>
#include <cstddef>
#include <cstdbool>
#else
#include <stdint.h>
#include <stddef.h>
#include <stdbool.h>
#endif

#if !defined(SWIFT_TYPEDEFS)
# define SWIFT_TYPEDEFS 1
# if __has_include(<uchar.h>)
#  include <uchar.h>
# elif !defined(__cplusplus)
typedef uint_least16_t char16_t;
typedef uint_least32_t char32_t;
# endif
typedef float swift_float2  __attribute__((__ext_vector_type__(2)));
typedef float swift_float3  __attribute__((__ext_vector_type__(3)));
typedef float swift_float4  __attribute__((__ext_vector_type__(4)));
typedef double swift_double2  __attribute__((__ext_vector_type__(2)));
typedef double swift_double3  __attribute__((__ext_vector_type__(3)));
typedef double swift_double4  __attribute__((__ext_vector_type__(4)));
typedef int swift_int2  __attribute__((__ext_vector_type__(2)));
typedef int swift_int3  __attribute__((__ext_vector_type__(3)));
typedef int swift_int4  __attribute__((__ext_vector_type__(4)));
typedef unsigned int swift_uint2  __attribute__((__ext_vector_type__(2)));
typedef unsigned int swift_uint3  __attribute__((__ext_vector_type__(3)));
typedef unsigned int swift_uint4  __attribute__((__ext_vector_type__(4)));
#endif

#if !defined(SWIFT_PASTE)
# define SWIFT_PASTE_HELPER(x, y) x##y
# define SWIFT_PASTE(x, y) SWIFT_PASTE_HELPER(x, y)
#endif
#if !defined(SWIFT_METATYPE)
# define SWIFT_METATYPE(X) Class
#endif
#if !defined(SWIFT_CLASS_PROPERTY)
# if __has_feature(objc_class_property)
#  define SWIFT_CLASS_PROPERTY(...) __VA_ARGS__
# else
#  define SWIFT_CLASS_PROPERTY(...)
# endif
#endif

#if __has_attribute(objc_runtime_name)
# define SWIFT_RUNTIME_NAME(X) __attribute__((objc_runtime_name(X)))
#else
# define SWIFT_RUNTIME_NAME(X)
#endif
#if __has_attribute(swift_name)
# define SWIFT_COMPILE_NAME(X) __attribute__((swift_name(X)))
#else
# define SWIFT_COMPILE_NAME(X)
#endif
#if __has_attribute(objc_method_family)
# define SWIFT_METHOD_FAMILY(X) __attribute__((objc_method_family(X)))
#else
# define SWIFT_METHOD_FAMILY(X)
#endif
#if __has_attribute(noescape)
# define SWIFT_NOESCAPE __attribute__((noescape))
#else
# define SWIFT_NOESCAPE
#endif
#if __has_attribute(ns_consumed)
# define SWIFT_RELEASES_ARGUMENT __attribute__((ns_consumed))
#else
# define SWIFT_RELEASES_ARGUMENT
#endif
#if __has_attribute(warn_unused_result)
# define SWIFT_WARN_UNUSED_RESULT __attribute__((warn_unused_result))
#else
# define SWIFT_WARN_UNUSED_RESULT
#endif
#if __has_attribute(noreturn)
# define SWIFT_NORETURN __attribute__((noreturn))
#else
# define SWIFT_NORETURN
#endif
#if !defined(SWIFT_CLASS_EXTRA)
# define SWIFT_CLASS_EXTRA
#endif
#if !defined(SWIFT_PROTOCOL_EXTRA)
# define SWIFT_PROTOCOL_EXTRA
#endif
#if !defined(SWIFT_ENUM_EXTRA)
# define SWIFT_ENUM_EXTRA
#endif
#if !defined(SWIFT_CLASS)
# if __has_attribute(objc_subclassing_restricted)
#  define SWIFT_CLASS(SWIFT_NAME) SWIFT_RUNTIME_NAME(SWIFT_NAME) __attribute__((objc_subclassing_restricted)) SWIFT_CLASS_EXTRA
#  define SWIFT_CLASS_NAMED(SWIFT_NAME) __attribute__((objc_subclassing_restricted)) SWIFT_COMPILE_NAME(SWIFT_NAME) SWIFT_CLASS_EXTRA
# else
#  define SWIFT_CLASS(SWIFT_NAME) SWIFT_RUNTIME_NAME(SWIFT_NAME) SWIFT_CLASS_EXTRA
#  define SWIFT_CLASS_NAMED(SWIFT_NAME) SWIFT_COMPILE_NAME(SWIFT_NAME) SWIFT_CLASS_EXTRA
# endif
#endif
#if !defined(SWIFT_RESILIENT_CLASS)
# if __has_attribute(objc_class_stub)
#  define SWIFT_RESILIENT_CLASS(SWIFT_NAME) SWIFT_CLASS(SWIFT_NAME) __attribute__((objc_class_stub))
#  define SWIFT_RESILIENT_CLASS_NAMED(SWIFT_NAME) __attribute__((objc_class_stub)) SWIFT_CLASS_NAMED(SWIFT_NAME)
# else
#  define SWIFT_RESILIENT_CLASS(SWIFT_NAME) SWIFT_CLASS(SWIFT_NAME)
#  define SWIFT_RESILIENT_CLASS_NAMED(SWIFT_NAME) SWIFT_CLASS_NAMED(SWIFT_NAME)
# endif
#endif

#if !defined(SWIFT_PROTOCOL)
# define SWIFT_PROTOCOL(SWIFT_NAME) SWIFT_RUNTIME_NAME(SWIFT_NAME) SWIFT_PROTOCOL_EXTRA
# define SWIFT_PROTOCOL_NAMED(SWIFT_NAME) SWIFT_COMPILE_NAME(SWIFT_NAME) SWIFT_PROTOCOL_EXTRA
#endif

#if !defined(SWIFT_EXTENSION)
# define SWIFT_EXTENSION(M) SWIFT_PASTE(M##_Swift_, __LINE__)
#endif

#if !defined(OBJC_DESIGNATED_INITIALIZER)
# if __has_attribute(objc_designated_initializer)
#  define OBJC_DESIGNATED_INITIALIZER __attribute__((objc_designated_initializer))
# else
#  define OBJC_DESIGNATED_INITIALIZER
# endif
#endif
#if !defined(SWIFT_ENUM_ATTR)
# if defined(__has_attribute) && __has_attribute(enum_extensibility)
#  define SWIFT_ENUM_ATTR(_extensibility) __attribute__((enum_extensibility(_extensibility)))
# else
#  define SWIFT_ENUM_ATTR(_extensibility)
# endif
#endif
#if !defined(SWIFT_ENUM)
# define SWIFT_ENUM(_type, _name, _extensibility) enum _name : _type _name; enum SWIFT_ENUM_ATTR(_extensibility) SWIFT_ENUM_EXTRA _name : _type
# if __has_feature(generalized_swift_name)
#  define SWIFT_ENUM_NAMED(_type, _name, SWIFT_NAME, _extensibility) enum _name : _type _name SWIFT_COMPILE_NAME(SWIFT_NAME); enum SWIFT_COMPILE_NAME(SWIFT_NAME) SWIFT_ENUM_ATTR(_extensibility) SWIFT_ENUM_EXTRA _name : _type
# else
#  define SWIFT_ENUM_NAMED(_type, _name, SWIFT_NAME, _extensibility) SWIFT_ENUM(_type, _name, _extensibility)
# endif
#endif
#if !defined(SWIFT_UNAVAILABLE)
# define SWIFT_UNAVAILABLE __attribute__((unavailable))
#endif
#if !defined(SWIFT_UNAVAILABLE_MSG)
# define SWIFT_UNAVAILABLE_MSG(msg) __attribute__((unavailable(msg)))
#endif
#if !defined(SWIFT_AVAILABILITY)
# define SWIFT_AVAILABILITY(plat, ...) __attribute__((availability(plat, __VA_ARGS__)))
#endif
#if !defined(SWIFT_WEAK_IMPORT)
# define SWIFT_WEAK_IMPORT __attribute__((weak_import))
#endif
#if !defined(SWIFT_DEPRECATED)
# define SWIFT_DEPRECATED __attribute__((deprecated))
#endif
#if !defined(SWIFT_DEPRECATED_MSG)
# define SWIFT_DEPRECATED_MSG(...) __attribute__((deprecated(__VA_ARGS__)))
#endif
#if __has_feature(attribute_diagnose_if_objc)
# define SWIFT_DEPRECATED_OBJC(Msg) __attribute__((diagnose_if(1, Msg, "warning")))
#else
# define SWIFT_DEPRECATED_OBJC(Msg) SWIFT_DEPRECATED_MSG(Msg)
#endif
#if defined(__OBJC__)
#if !defined(IBSegueAction)
# define IBSegueAction
#endif
#endif
#if !defined(SWIFT_EXTERN)
# if defined(__cplusplus)
#  define SWIFT_EXTERN extern "C"
# else
#  define SWIFT_EXTERN extern
# endif
#endif
#if !defined(SWIFT_CALL)
# define SWIFT_CALL __attribute__((swiftcall))
#endif
#if defined(__cplusplus)
#if !defined(SWIFT_NOEXCEPT)
# define SWIFT_NOEXCEPT noexcept
#endif
#else
#if !defined(SWIFT_NOEXCEPT)
# define SWIFT_NOEXCEPT 
#endif
#endif
#if defined(__cplusplus)
#if !defined(SWIFT_CXX_INT_DEFINED)
#define SWIFT_CXX_INT_DEFINED
namespace swift {
using Int = ptrdiff_t;
using UInt = size_t;
}
#endif
#endif
#if defined(__OBJC__)
#if __has_feature(modules)
#if __has_warning("-Watimport-in-framework-header")
#pragma clang diagnostic ignored "-Watimport-in-framework-header"
#endif
@import CoreFoundation;
@import Foundation;
@import ObjectiveC;
@import UIKit;
#endif

#endif
#pragma clang diagnostic ignored "-Wproperty-attribute-mismatch"
#pragma clang diagnostic ignored "-Wduplicate-method-arg"
#if __has_warning("-Wpragma-clang-attribute")
# pragma clang diagnostic ignored "-Wpragma-clang-attribute"
#endif
#pragma clang diagnostic ignored "-Wunknown-pragmas"
#pragma clang diagnostic ignored "-Wnullability"
#pragma clang diagnostic ignored "-Wdollar-in-identifier-extension"

#if __has_attribute(external_source_symbol)
# pragma push_macro("any")
# undef any
# pragma clang attribute push(__attribute__((external_source_symbol(language="Swift", defined_in="PyrusServiceDesk",generated_declaration))), apply_to=any(function,enum,objc_interface,objc_category,objc_protocol))
# pragma pop_macro("any")
#endif

#if defined(__OBJC__)
@class NSString;
@protocol FileChooserDelegate;

/// Interface with label(String) and chooserDelegate(FileChooserDelegate).
SWIFT_PROTOCOL("_TtP16PyrusServiceDesk11FileChooser_")
@protocol FileChooser
/// Is name that need show to user
@property (nonatomic, copy) NSString * _Nonnull label;
/// Protocol to send status messages of completion.
@property (nonatomic, weak) id <FileChooserDelegate> _Nullable chooserDelegate;
@end

@class NSData;
@class NSURL;

/// Protocol to send status messages of completion.
SWIFT_PROTOCOL("_TtP16PyrusServiceDesk19FileChooserDelegate_")
@protocol FileChooserDelegate
/// Send succes status
/// \param data Data that need to send attachment to chat. If empty or nil will show error alert.
///
- (void)didEndWithSuccess:(NSData * _Nullable)data url:(NSURL * _Nullable)url;
/// Send cancel status. Will do nothing. Just close.
- (void)didEndWithCancel;
@end


/// The protocol for log some events from PyrusServiceDesk
SWIFT_PROTOCOL("_TtP16PyrusServiceDesk9LogEvents_")
@protocol LogEvents
/// The callback that PyrusServiceDesk was closed
- (void)logPyrusServiceDeskWithEvent:(NSString * _Nonnull)event;
@end





/// The protocol for sending a notification that a new message has arrived
SWIFT_PROTOCOL("_TtP16PyrusServiceDesk18NewReplySubscriber_")
@protocol NewReplySubscriber
/// The new message was send
/// \param hasUnreadComments Is there a new messages in chat
///
/// \param lastCommentText The text of last unread message.  Returns nil if there is no new messages or text in it.
///
/// \param lastCommentAttachmentsCount Total number of attachments in the message.  Returns 0 if there is no new messages or attachments in it.
///
/// \param lastCommentAttachments The list of attachments’ names. Returns nil if there is no new messages, or atttachments in it.
///
/// \param utcTime The date of last unread message in(utc). Returns 0 if there is no new messages.
///
- (void)onNewReplyWithHasUnreadComments:(BOOL)hasUnreadComments lastCommentText:(NSString * _Nullable)lastCommentText lastCommentAttachmentsCount:(NSInteger)lastCommentAttachmentsCount lastCommentAttachments:(NSArray<NSString *> * _Nullable)lastCommentAttachments utcTime:(double)utcTime;
@end


/// The protocol for getting a notification that PyrusServiceDesk was closed
SWIFT_PROTOCOL("_TtP16PyrusServiceDesk14OnStopCallback_")
@protocol OnStopCallback
/// The callback that PyrusServiceDesk was closed
- (void)onStop;
@end

@class NSCoder;

/// The showed
SWIFT_CLASS("_TtC16PyrusServiceDesk11PSDInfoView")
@interface PSDInfoView : UIView
- (void)removeFromSuperview;
- (nonnull instancetype)initWithFrame:(CGRect)frame OBJC_DESIGNATED_INITIALIZER;
- (nullable instancetype)initWithCoder:(NSCoder * _Nonnull)coder OBJC_DESIGNATED_INITIALIZER;
@end

@class UIViewController;
@class ServiceDeskConfiguration;
@class UINavigationController;

SWIFT_CLASS("_TtC16PyrusServiceDesk16PyrusServiceDesk")
@interface PyrusServiceDesk : NSObject
/// Send device id to server
/// \param token String with device id
///
/// \param completion Error. Not nil if success. See error.localizedDescription to understand why its happened
///
+ (void)setPushToken:(NSString * _Nullable)token completion:(void (^ _Nonnull)(NSError * _Nullable))completion;
/// Show chat
/// \param viewController ViewController that must present chat
///
/// \param onStopCallback OnStopCallback object or nil. OnStopCallback is object for getting a notification that PyrusServiceDesk was closed.
///
+ (void)startOn:(UIViewController * _Nonnull)viewController onStopCallback:(id <OnStopCallback> _Nullable)onStopCallback;
/// Show chat
/// \param viewController ViewController that must present chat
///
/// \param configuration ServiceDeskConfiguration object or nil. ServiceDeskConfiguration is object that create custom interface: theme color,welcome message, image for support’s avatar and chat title for navigation bar title. If nil, the default design will be used.
///
/// \param onStopCallback OnStopCallback object or nil. OnStopCallback is object for getting a notification that PyrusServiceDesk was closed.
///
+ (void)startOn:(UIViewController * _Nonnull)viewController configuration:(ServiceDeskConfiguration * _Nullable)configuration onStopCallback:(id <OnStopCallback> _Nullable)onStopCallback;
/// Show chat
/// \param viewController ViewController that must present chat
///
/// \param configuration ServiceDeskConfiguration object or nil. ServiceDeskConfiguration is object that create custom interface: theme color,welcome message, image for support’s avatar and chat title for navigation bar title. If nil, the default design will be used.
///
/// \param completion The block to execute after the presentation finishes. This block has no return value and takes no parameters. You may specify nil for this parameter.
///
/// \param onStopCallback OnStopCallback object or nil. OnStopCallback is object for getting a notification that PyrusServiceDesk was closed.
///
+ (void)startOn:(UIViewController * _Nonnull)viewController configuration:(ServiceDeskConfiguration * _Nullable)configuration completion:(void (^ _Nullable)(void))completion onStopCallback:(id <OnStopCallback> _Nullable)onStopCallback;
/// Show chat
/// \param configuration ServiceDeskConfiguration object or nil. ServiceDeskConfiguration is object that create custom interface: theme color,welcome message, image for support’s avatar and chat title for navigation bar title. If nil, the default design will be used.
///
+ (UINavigationController * _Nullable)startWith:(ServiceDeskConfiguration * _Nullable)configuration SWIFT_WARN_UNUSED_RESULT;
/// Close PyrusServiceDesk
+ (void)stop;
SWIFT_CLASS_PROPERTY(@property (nonatomic, class, copy) void (^ _Nullable onAuthorizationFailed)(void);)
+ (void (^ _Nullable)(void))onAuthorizationFailed SWIFT_WARN_UNUSED_RESULT;
+ (void)setOnAuthorizationFailed:(void (^ _Nullable)(void))value;
/// Subscribe [subscriber] for notifications that new messages from support have appeared in the chat.
+ (void)subscribeToReplies:(id <NewReplySubscriber> _Nullable)subscriber;
/// Unsubscribe [subscriber] from alerts for new messages from chat support.
+ (void)unsubscribeFromReplies:(id <NewReplySubscriber> _Nullable)subscriber;
+ (void)subscribeToGogEvents:(id <LogEvents> _Nonnull)subscriber;
/// Init PyrusServiceDesk with new clientId.
/// \param clientId clientId using for all requests. If clientId not setted PyrusServiceDesk Controller will not be created
///
/// \param domain Base domain for network requests. If the [domain] is null, the default pyrus.com will be used.
///
/// \param loggingEnabled If true, then the library will write logs, and they can be sent as a file to chat by clicking the “Send Library Logs” button in the menu under the “+” sign. 
///
+ (void)createWith:(NSString * _Nullable)clientId domain:(NSString * _Nullable)domain loggingEnabled:(BOOL)loggingEnabled;
/// Init PyrusServiceDesk with new clientId.
/// \param clientId clientId using for all requests. If clientId not setted PyrusServiceDesk Controller will not be created
///
/// \param reset If true, user will be reseted
///
/// \param domain Base domain for network requests. If the [domain] is null, the default pyrus.com will be used.
///
/// \param loggingEnabled If true, then the library will write logs, and they can be sent as a file to chat by clicking the “Send Library Logs” button in the menu under the “+” sign. 
///
+ (void)createWith:(NSString * _Nullable)clientId reset:(BOOL)reset domain:(NSString * _Nullable)domain loggingEnabled:(BOOL)loggingEnabled;
/// Init PyrusServiceDesk with new clientId.
/// \param clientId clientId using for all requests. If clientId not setted PyrusServiceDesk Controller will not be created
///
/// \param userId userId of the user who is initializing service desk
///
/// \param securityKey security key of the user for safe initialization
/// ///- parameter domain: Base domain for network requests. If the [domain] is null, the default pyrus.com will be used.
///
/// \param loggingEnabled If true, then the library will write logs, and they can be sent as a file to chat by clicking the “Send Library Logs” button in the menu under the “+” sign. 
///
+ (void)createWith:(NSString * _Nullable)clientId userId:(NSString * _Nullable)userId securityKey:(NSString * _Nullable)securityKey domain:(NSString * _Nullable)domain loggingEnabled:(BOOL)loggingEnabled;
+ (void)refreshOnError:(void (^ _Nullable)(NSError * _Nullable))onError;
/// Scrolls chat to bottom, starts refreshing chat and shows fake message from support is psd is open.
+ (void)refreshFromPushWithMessageId:(NSInteger)messageId;
+ (void)present:(UIViewController * _Nonnull)viewController animated:(BOOL)animated completion:(void (^ _Nullable)(void))completion;
/// Save viewController with FileChooser interface. Use to add custom row in attachment-add-menu
/// \param chooser (FileChooser & UIViewController) to present.
///
+ (void)registerFileChooser:(UIViewController <FileChooser> * _Nullable)chooser;
- (nonnull instancetype)init OBJC_DESIGNATED_INITIALIZER;
@end


@interface PyrusServiceDesk (SWIFT_EXTENSION(PyrusServiceDesk))
/// Активирует поддержку фич от бека
/// Для настройки флагов воспользуйся [ссылкой](https://dev.pyrus.com/admin/api-flags
+ (NSString * _Nonnull)apiSign SWIFT_WARN_UNUSED_RESULT;
@end


SWIFT_CLASS("_TtC16PyrusServiceDesk24ServiceDeskConfiguration")
@interface ServiceDeskConfiguration : NSObject
- (nonnull instancetype)init SWIFT_UNAVAILABLE;
+ (nonnull instancetype)new SWIFT_UNAVAILABLE_MSG("-init is unavailable");
@end

@class UIColor;
@class UIImage;
@class UIBarButtonItem;

SWIFT_CLASS_NAMED("Builder")
@interface ServiceDeskConfigurationBuilder : NSObject
- (ServiceDeskConfigurationBuilder * _Nonnull)setChatTitle:(NSString * _Nullable)chatTitle;
- (ServiceDeskConfigurationBuilder * _Nonnull)setThemeColor:(UIColor * _Nullable)themeColor;
- (ServiceDeskConfigurationBuilder * _Nonnull)setWelcomeMessage:(NSString * _Nullable)welcomeMessage;
- (ServiceDeskConfigurationBuilder * _Nonnull)setAvatarForSupport:(UIImage * _Nullable)avatarForSupport;
- (ServiceDeskConfigurationBuilder * _Nonnull)setUserName:(NSString * _Nullable)userName;
- (ServiceDeskConfigurationBuilder * _Nonnull)setChatTitleView:(UIView * _Nullable)chatTitleView;
- (ServiceDeskConfigurationBuilder * _Nonnull)setCustomRightBarButtonItem:(UIBarButtonItem * _Nullable)customRightBarButtonItem;
- (ServiceDeskConfigurationBuilder * _Nonnull)setCustomLeftBarButtonItem:(UIBarButtonItem * _Nullable)customLeftBarButtonItem;
- (ServiceDeskConfigurationBuilder * _Nonnull)setInfoView:(PSDInfoView * _Nullable)infoView;
- (ServiceDeskConfigurationBuilder * _Nonnull)setStatusBarStyle:(UIStatusBarStyle)barStyle darkBarStyle:(UIStatusBarStyle)darkBarStyle;
- (ServiceDeskConfigurationBuilder * _Nonnull)setKeyboardAppearance:(UIKeyboardAppearance)keyboardAppearance darkKeyboardAppearance:(UIKeyboardAppearance)darkKeyboardAppearance;
- (ServiceDeskConfigurationBuilder * _Nonnull)setKeyboardColor:(UIColor * _Nonnull)color;
- (ServiceDeskConfigurationBuilder * _Nonnull)setFontName:(NSString * _Nullable)fontName;
- (ServiceDeskConfigurationBuilder * _Nonnull)setUserTextColor:(UIColor * _Nullable)color;
- (ServiceDeskConfigurationBuilder * _Nonnull)setUserMessageBackgroundColor:(UIColor * _Nullable)color;
- (ServiceDeskConfigurationBuilder * _Nonnull)setSupportTextColor:(UIColor * _Nullable)color;
- (ServiceDeskConfigurationBuilder * _Nonnull)setSupportMessageBackgroundColor:(UIColor * _Nullable)color;
- (ServiceDeskConfigurationBuilder * _Nonnull)setChatTitleColor:(UIColor * _Nullable)color;
- (ServiceDeskConfigurationBuilder * _Nonnull)setToolbarStyle:(UIBarStyle)barStyle darkBarStyle:(UIBarStyle)darkBarStyle;
- (ServiceDeskConfigurationBuilder * _Nonnull)setToolbarColor:(UIColor * _Nullable)color;
- (ServiceDeskConfigurationBuilder * _Nonnull)setToolbarButtonColor:(UIColor * _Nullable)color;
- (ServiceDeskConfigurationBuilder * _Nonnull)setBackgroundColor:(UIColor * _Nullable)color;
- (ServiceDeskConfigurationBuilder * _Nonnull)setAttachmentMenuTextColor:(UIColor * _Nullable)color;
- (ServiceDeskConfigurationBuilder * _Nonnull)setAttachmentMenuButtonColor:(UIColor * _Nullable)color;
- (ServiceDeskConfigurationBuilder * _Nonnull)setSendButtonColor:(UIColor * _Nullable)color;
- (ServiceDeskConfiguration * _Nonnull)build SWIFT_WARN_UNUSED_RESULT;
- (nonnull instancetype)init OBJC_DESIGNATED_INITIALIZER;
@end
































#endif
#if defined(__cplusplus)
#endif
#if __has_attribute(external_source_symbol)
# pragma clang attribute pop
#endif
#pragma clang diagnostic pop
#endif

#else
#error unsupported Swift architecture
#endif