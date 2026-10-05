use jni::{
    JNIEnv,
    objects::{JClass, JObject},
    sys::jboolean,
};

use crate::session::with_session;

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_Session_initializeSession(
    env: JNIEnv,
    _this: JClass,
    callback: JObject,
) -> jboolean {
    let rt = match crate::TOKIO_RUNTIME.get() {
        Some(r) => r,
        None => return 0,
    };

    let global_callback = match env.new_global_ref(callback) {
        Ok(c) => c,
        Err(_) => return 0,
    };

    crate::session::set_session_callback(global_callback.clone());

    let handle = rt.handle().clone();
    let jvm = crate::JVM.get().unwrap();

    handle.spawn(async move {
        crate::session::initialize_session().await;
        let mut env = match jvm.attach_current_thread() {
            Ok(env) => env,
            Err(e) => {
                error!("jvm attach_current_thread failed for session init: {e}");
                return;
            }
        };

        env.call_method(global_callback.as_obj(), "onInitialized", "()V", &[])
            .ok();
    });

    1
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_Session_shutdown(
    _env: JNIEnv,
    _this: JClass,
) -> jboolean {
    let _ = with_session(|session| session.shutdown());
    1
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_core_Session_unregisterSessionCallback(
    _env: JNIEnv,
    _this: JClass,
) {
    crate::session::unregister_session_callback();
}
