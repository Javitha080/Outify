use std::path::PathBuf;

use jni::{
    JNIEnv,
    objects::{JObject, JString},
};

pub(crate) fn get_android_dir(env: &mut JNIEnv, context: &JObject, method: &str) -> Option<PathBuf> {
    let dir_object: JObject = env
        .call_method(context, method, "()Ljava/io/File;", &[])
        .ok()?
        .l()
        .ok()?;

    let path_jstring: JString = env
        .call_method(dir_object, "getAbsolutePath", "()Ljava/lang/String;", &[])
        .ok()?
        .l()
        .ok()?
        .into();

    let path: String = env.get_string(&path_jstring).ok()?.into();
    Some(PathBuf::from(path))
}
