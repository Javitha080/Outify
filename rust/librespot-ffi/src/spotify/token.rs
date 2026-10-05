use std::time::SystemTime;

use serde::{Deserialize, Serialize};

#[derive(Clone, Serialize, Deserialize)]
pub struct WebApiToken {
    pub access_token: String,
    pub refresh_token: String,
    pub expires_at: u64,
    pub scope: String,
}

impl WebApiToken {
    fn now_secs() -> u64 {
        SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_secs())
            .unwrap_or(0)
    }

    pub fn new(access_token: String, refresh_token: String, expires_in: u64, scope: String) -> Self {
        let now = Self::now_secs();
        Self {
            access_token,
            refresh_token,
            expires_at: now.saturating_add(expires_in),
            scope,
        }
    }

    pub fn is_expired(&self) -> bool {
        Self::now_secs() >= self.expires_at
    }

    pub fn from(token: TokenResponse, old_refresh_token: Option<&str>) -> Self {
        let now = Self::now_secs();

        let refresh_token = token
            .refresh_token
            .or_else(|| old_refresh_token.map(String::from))
            .unwrap_or_default();

        Self {
            access_token: token.access_token,
            refresh_token,
            expires_at: now + token.expires_in,
            scope: token.scope,
        }
    }
}

#[derive(Deserialize)]
pub struct TokenResponse {
    pub access_token: String,
    pub expires_in: u64,
    #[serde(default)]
    pub refresh_token: Option<String>,
    #[serde(default)]
    pub scope: String,
}
