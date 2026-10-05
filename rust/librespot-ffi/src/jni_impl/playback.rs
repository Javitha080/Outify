use std::sync::Mutex;

use jni::{
    JNIEnv,
    objects::{GlobalRef, JClass, JObject},
};

pub static PLAYER_EVENT_LISTENER: Mutex<Option<GlobalRef>> = Mutex::new(None);

// Registers the track update callback and stores its GlobalRef
#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_playback_AudioEngine_registerPlayerEventListener(
    env: JNIEnv,
    _this: JClass,
    callback: JObject,
) {
    let global = match env.new_global_ref(callback) {
        Ok(g) => g,
        Err(e) => {
            error!("jni new_global_ref failed for player event listener: {e}");
            return;
        }
    };

    let mut guard = PLAYER_EVENT_LISTENER.lock().unwrap();
    *guard = Some(global);
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_cc_tomko_outify_playback_AudioEngine_unregisterPlayerEventListener(
    _env: JNIEnv,
    _this: JClass,
) {
    let mut guard = PLAYER_EVENT_LISTENER.lock().unwrap();
    if let Some(global) = guard.take() {
        drop(global);
    }
}
