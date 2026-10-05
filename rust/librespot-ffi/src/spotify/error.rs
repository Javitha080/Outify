use thiserror::Error;

#[derive(Error, Debug)]
pub enum SpotifyApiError {
    #[error("Token is none")]
    NoToken,

    #[error("Spotify Error: {0}")]
    ApiError(String),

    #[error("HTTP {status}: {message}")]
    Http { status: String, message: String },

    #[error("JSON error: {0}")]
    Json(#[from] serde_json::Error),

    #[error("HTTP request failed: {0}")]
    Reqwest(#[from] reqwest::Error),

    #[error("Librespot error: {0}")]
    Librespot(#[from] librespot_core::Error),

    #[error("IO error: {0}")]
    IO(#[from] std::io::Error),

    #[error("{0}")]
    Generic(String),
}
