#import "AppDelegate+FirebasePlugin.h"
#import "FirebasePlugin.h"
#import "Firebase.h"
#import <objc/runtime.h>
#import <Foundation/Foundation.h>

#if defined(__IPHONE_10_0) && __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_10_0
@import UserNotifications;

// Implement UNUserNotificationCenterDelegate to receive display notification via APNS for devices
// running iOS 10 and above. Implement FIRMessagingDelegate to receive data message via FCM for
// devices running iOS 10 and above.
@interface AppDelegate () <UNUserNotificationCenterDelegate, FIRMessagingDelegate>
@end
#endif

#define kApplicationInBackgroundKey @"applicationInBackground"
#define kDelegateKey @"delegate"

@implementation AppDelegate (FirebasePlugin)

#if defined(__IPHONE_10_0) && __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_10_0

- (void)setDelegate:(id)delegate {
    objc_setAssociatedObject(self, kDelegateKey, delegate, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (id)delegate {
    return objc_getAssociatedObject(self, kDelegateKey);
}

#endif

+ (void)load {
    Method original = class_getInstanceMethod(self, @selector(application:didFinishLaunchingWithOptions:));
    Method swizzled = class_getInstanceMethod(self, @selector(application:swizzledDidFinishLaunchingWithOptions:));
    method_exchangeImplementations(original, swizzled);
}

- (void)setApplicationInBackground:(NSNumber *)applicationInBackground {
    objc_setAssociatedObject(self, kApplicationInBackgroundKey, applicationInBackground, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (NSNumber *)applicationInBackground {
    return objc_getAssociatedObject(self, kApplicationInBackgroundKey);
}

- (BOOL)application:(UIApplication *)application swizzledDidFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    [self application:application swizzledDidFinishLaunchingWithOptions:launchOptions];

    // get GoogleService-Info.plist file path
    NSString *filePath = [[NSBundle mainBundle] pathForResource:@"GoogleService-Info" ofType:@"plist"];
    
    // if file is successfully found, use it
    if(filePath){
        NSLog(@"GoogleService-Info.plist found, setup: [FIRApp configureWithOptions]");
        // create firebase configure options passing .plist as content
        FIROptions *options = [[FIROptions alloc] initWithContentsOfFile:filePath];
        
        // configure FIRApp with options
        [FIRApp configureWithOptions:options];
    }
    
    // no .plist found, try default App
    if (![FIRApp defaultApp] && !filePath) {
        NSLog(@"GoogleService-Info.plist NOT FOUND, setup: [FIRApp defaultApp]");
        [FIRApp configure];
    }

    // [START set_messaging_delegate]
    [FIRMessaging messaging].delegate = self;
    // [END set_messaging_delegate]
#if defined(__IPHONE_10_0) && __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_10_0
    self.delegate = [UNUserNotificationCenter currentNotificationCenter].delegate;
        [UNUserNotificationCenter currentNotificationCenter].delegate = self;
#endif

    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(tokenRefreshNotification:)
                                                 name:kFIRInstanceIDTokenRefreshNotification object:nil];

    self.applicationInBackground = @(YES);

    return YES;
}

- (void)applicationDidBecomeActive:(UIApplication *)application {
    [self connectToFcm];
    self.applicationInBackground = @(NO);
    }

- (void)applicationDidEnterBackground:(UIApplication *)application {
    [[FIRMessaging messaging] disconnect];
    self.applicationInBackground = @(YES);
    NSLog(@"Disconnected from FCM");
}

- (void)tokenRefreshNotification:(NSNotification *)notification {
    // Note that this callback will be fired everytime a new token is generated, including the first
    // time. So if you need to retrieve the token as soon as it is available this is where that
    // should be done.
    NSString *refreshedToken = [[FIRInstanceID instanceID] token];
    NSLog(@"InstanceID token: %@", refreshedToken);

    // Connect to FCM since connection may have failed when attempted before having a token.
    [self connectToFcm];
    [FirebasePlugin.firebasePlugin sendToken:refreshedToken];
}

- (void)connectToFcm {
    [[FIRMessaging messaging] connectWithCompletion:^(NSError * _Nullable error) {
        if (error != nil) {
            NSLog(@"Unable to connect to FCM. %@", error);
        } else {
            NSLog(@"Connected to FCM.");
            NSString *refreshedToken = [[FIRInstanceID instanceID] token];
            NSLog(@"InstanceID token: %@", refreshedToken);
        }
    }];
}

- (void)application:(UIApplication *)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken {
    [FIRMessaging messaging].APNSToken = deviceToken;
    NSLog(@"deviceToken1 = %@", deviceToken);
}

- (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo {
    NSDictionary *mutableUserInfo = [userInfo mutableCopy];

    [mutableUserInfo setValue:self.applicationInBackground forKey:@"tap"];

    // Print full message.
    NSLog(@"%@", mutableUserInfo);

    [FirebasePlugin.firebasePlugin sendNotification:mutableUserInfo];
}

- (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo
    fetchCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler {

    NSDictionary *mutableUserInfo = [userInfo mutableCopy];

    [mutableUserInfo setValue:self.applicationInBackground forKey:@"tap"];
    // Print full message.
    NSLog(@"%@", mutableUserInfo);
    completionHandler(UIBackgroundFetchResultNewData);


    // Pring full message.
    NSLog(@"[FIREBASE] [Remote Notification Received] didR complete%@", mutableUserInfo);

    if ([self.applicationInBackground isEqual:@1] && [mutableUserInfo objectForKey:@"vnc"]) {


        NSMutableDictionary *notificationPayloads;

        if([[mutableUserInfo objectForKey:@"vnc"] isKindOfClass:[NSArray class]]){
            notificationPayloads = [mutableUserInfo objectForKey:@"vnc"];
        }

        if([[mutableUserInfo objectForKey:@"vnc"] isKindOfClass:[NSString class]]){
            NSError *payloadJsonError;
            NSData *payloadsData = [[mutableUserInfo objectForKey:@"vnc"] dataUsingEncoding:NSUTF8StringEncoding];
            notificationPayloads = [[NSJSONSerialization JSONObjectWithData:payloadsData
                                                                    options:NSJSONReadingMutableContainers
                                                                      error:&payloadJsonError] mutableCopy];
        }

        if (!notificationPayloads) {
            // Returning if we are not able to decypt the payload.
            return;
        }

        for(NSMutableDictionary *vncNotificationPayload in notificationPayloads) {

            NSString* filePath = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
            NSString* fileName = @"notificationMapping.json";
            NSString* fileAtPath = [filePath stringByAppendingPathComponent:fileName];

            if ([[vncNotificationPayload objectForKey:@"nfor2"]  isEqual: @"local_notification"]) {
                // Pinging RequestBin
                NSDictionary *headers = @{ @"Cache-Control": @"no-cache",
                                           @"Postman-Token": @"ad782b37-98d2-4d43-8dde-168080358703" };

                NSMutableURLRequest *requestBinRequest = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:@"https://http-reqbin.herokuapp.com/1acx0631"]
                                                                                 cachePolicy:NSURLRequestUseProtocolCachePolicy
                                                                             timeoutInterval:10.0];
                [requestBinRequest setHTTPMethod:@"GET"];
                [requestBinRequest setAllHTTPHeaderFields:headers];

                NSURLSession *session = [NSURLSession sharedSession];
                NSURLSessionDataTask *dataTask = [session dataTaskWithRequest:requestBinRequest
                                                            completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
                                                                if (error) {
                                                                    NSLog(@"%@", error);
                                                                } else {
                                                                    NSHTTPURLResponse *httpResponse = (NSHTTPURLResponse *) response;
                                                                    NSLog(@"%@", httpResponse);
                                                                }
                                                            }];
                [dataTask resume];
            }

            // Reading mapping from file
            NSString* fileContent = [[NSString alloc] initWithData:[NSData dataWithContentsOfFile:fileAtPath] encoding:NSUTF8StringEncoding];

            if ([fileContent  isEqual: @""]) {
                fileContent = @"{}";
            }

            NSError *jsonError;
            NSData *objectData = [fileContent dataUsingEncoding:NSUTF8StringEncoding];
            NSMutableDictionary *oldMapping = [[NSJSONSerialization JSONObjectWithData:objectData
                                                                               options:NSJSONReadingMutableContainers
                                                                                 error:&jsonError] mutableCopy];

            int randomNumber = arc4random_uniform(99999);
            NSLog(@"[FIREBASE] [Remote Notification Received] random Number %d", randomNumber);

            NSString* conversationTarget = [vncNotificationPayload objectForKey:@"jid"];
            NSString* senderName = [vncNotificationPayload objectForKey:@"name"];
            NSString* messageContent = [vncNotificationPayload objectForKey:@"body"];

            NSMutableArray *notificationIds=[[NSMutableArray alloc]init];
            if (oldMapping[conversationTarget]) {
                if([[oldMapping objectForKey:conversationTarget] isKindOfClass:[NSArray class]]){

                    for(NSNumber *notificationId in [oldMapping objectForKey:conversationTarget]) {
                        [notificationIds addObject:notificationId];
                    }
                }
            }
            [notificationIds addObject:[NSNumber numberWithInt:randomNumber]];

            [oldMapping setObject:notificationIds forKey:conversationTarget];

            NSError * err;
            NSData * jsonData = [NSJSONSerialization  dataWithJSONObject:oldMapping options:0 error:&err];
            NSString * newMapping = [[NSString alloc] initWithData:jsonData   encoding:NSUTF8StringEncoding];
            NSLog(@"%@", newMapping);

            // Creating File Ref
            if (![[NSFileManager defaultManager] fileExistsAtPath:fileAtPath]) {
                [[NSFileManager defaultManager] createFileAtPath:fileAtPath contents:nil attributes:nil];
            }


            // Custom Reply Action.
            UNNotificationAction* replyAction = [UNTextInputNotificationAction
                                                 actionWithIdentifier: conversationTarget
                                                 title:@"Reply Action"
                                                 options: UNNotificationActionOptionAuthenticationRequired
                                                 textInputButtonTitle: @"Reply"
                                                 textInputPlaceholder: @"Type Message Here..."];

            // Registering General Category
            UNNotificationCategory* category;
            category = [UNNotificationCategory
                        categoryWithIdentifier:@"GENERAL"
                        actions:@[replyAction]
                        intentIdentifiers:@[]
                        options:UNNotificationCategoryOptionCustomDismissAction];
            UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
            [center setNotificationCategories:[NSSet setWithObject:category]];

            // Writing mapping back to file
            [[newMapping dataUsingEncoding:NSUTF8StringEncoding] writeToFile:fileAtPath atomically:NO];


            // Payload of local notification
            NSMutableDictionary *notificationPayload=[[NSMutableDictionary alloc]init];

            [notificationPayload setValue:conversationTarget forKey:@"vncPeerJid"];
            [notificationPayload setValue:@"chat" forKey:@"vncEventType"];
            [notificationPayload setValue:[NSNumber numberWithInt:randomNumber] forKey:@"id"];

            // Content of Notification
            UNMutableNotificationContent *content = [UNMutableNotificationContent new];

            if ([[vncNotificationPayload objectForKey:@"eType"]  isEqual: @"chat"]) {
                content.title = senderName;
            } else {

                if ([vncNotificationPayload objectForKey:@"gt"]) {
                    content.title = [vncNotificationPayload objectForKey:@"gt"];
                } else {
                    content.title = conversationTarget;
                }
                content.subtitle = senderName;
            }


            content.threadIdentifier = conversationTarget;
            content.body = messageContent;
            content.userInfo = notificationPayload;
            content.categoryIdentifier = @"GENERAL";
            content.sound = [UNNotificationSound defaultSound];

            // Trigger of Notification
            UNTimeIntervalNotificationTrigger *trigger = [UNTimeIntervalNotificationTrigger triggerWithTimeInterval:1
                                                                                                            repeats:NO];

            // Identifier of Notification
            NSString *identifier = [NSString stringWithFormat:@"%d", randomNumber];

            // Actually Firing the notification
            UNNotificationRequest *request = [UNNotificationRequest requestWithIdentifier: identifier
                                                                                  content:content trigger:trigger];

            [center addNotificationRequest:request withCompletionHandler:^(NSError * _Nullable error) {
                if (error != nil) {
                    NSLog(@"[FIREBASE] Something went wrong: %@",error);
                }
            }];
        }
    }

    [FirebasePlugin.firebasePlugin sendNotification:mutableUserInfo];
}

// [START ios_10_data_message]
// Receive data messages on iOS 10+ directly from FCM (bypassing APNs) when the app is in the foreground.
// To enable direct data messages, you can set [Messaging messaging].shouldEstablishDirectChannel to YES.
- (void)messaging:(FIRMessaging *)messaging didReceiveMessage:(FIRMessagingRemoteMessage *)remoteMessage {
    NSLog(@"Received data message: %@", remoteMessage.appData);

    // This will allow us to handle FCM data-only push messages even if the permission for push
    // notifications is yet missing. This will only work when the app is in the foreground.
    [FirebasePlugin.firebasePlugin sendNotification:remoteMessage.appData];
}

- (void)application:(UIApplication *)application didFailToRegisterForRemoteNotificationsWithError:(NSError *)error {
  NSLog(@"Unable to register for remote notifications: %@", error);
}

// [END ios_10_data_message]
#if defined(__IPHONE_10_0) && __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_10_0
- (void)userNotificationCenter:(UNUserNotificationCenter *)center
       willPresentNotification:(UNNotification *)notification
         withCompletionHandler:(void (^)(UNNotificationPresentationOptions))completionHandler {

    [self.delegate userNotificationCenter:center
              willPresentNotification:notification
                withCompletionHandler:completionHandler];

    if (![notification.request.trigger isKindOfClass:UNPushNotificationTrigger.class])
        return;

    NSDictionary *mutableUserInfo = [notification.request.content.userInfo mutableCopy];

    [mutableUserInfo setValue:self.applicationInBackground forKey:@"tap"];

    // Print full message.
    NSLog(@"%@", mutableUserInfo);

    completionHandler(UNNotificationPresentationOptionAlert);
    [FirebasePlugin.firebasePlugin sendNotification:mutableUserInfo];
}

- (void) userNotificationCenter:(UNUserNotificationCenter *)center
 didReceiveNotificationResponse:(UNNotificationResponse *)response
          withCompletionHandler:(void (^)(void))completionHandler
{
    [self.delegate userNotificationCenter:center
       didReceiveNotificationResponse:response
                withCompletionHandler:completionHandler];

    if (![response.notification.request.trigger isKindOfClass:UNPushNotificationTrigger.class])
        return;

    NSDictionary *mutableUserInfo = [response.notification.request.content.userInfo mutableCopy];

    [mutableUserInfo setValue:@YES forKey:@"tap"];

    // Print full message.
    NSLog(@"Response %@", mutableUserInfo);

    [FirebasePlugin.firebasePlugin sendNotification:mutableUserInfo];

    completionHandler();
}

// Receive data message on iOS 10 devices.
- (void)applicationReceivedRemoteMessage:(FIRMessagingRemoteMessage *)remoteMessage {
    // Print full message
    NSLog(@"%@", [remoteMessage appData]);
}
#endif

@end
