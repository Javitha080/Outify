use std::str::FromStr;

use reqwest::StatusCode;

use crate::{
    spotify::error::SpotifyApiError,
    types::responses::library::{SavedItemsResponse, Uri},
};

use super::{REQUEST_TIMEOUT, SPOTIFY_API_URL, SpotifyClient};

impl SpotifyClient {
    pub async fn save_items(&self, uris: Vec<String>) -> Result<StatusCode, SpotifyApiError> {
        let token = self.load_token().await?;
        let token = token
            .ok_or_else(|| SpotifyApiError::Generic("No account token present!".to_string()))?;

        let ids = uris.join(",");

        let res = self
            .client
            .put(format!("{}/v1/me/library", SPOTIFY_API_URL))
            .query(&[("uris", ids)])
            .header(reqwest::header::CONTENT_LENGTH, "0")
            .bearer_auth(token.access_token)
            .timeout(REQUEST_TIMEOUT)
            .send()
            .await?;

        if !res.status().is_success() {
            let status = res.status().as_str().to_string();
            let body = res.text().await.unwrap_or_default();
            return Err(SpotifyApiError::Generic(format!(
                "save_items failed with status {status}: {body}"
            )));
        }

        Ok(res.status())
    }

    pub async fn delete_items(&self, uris: Vec<String>) -> Result<StatusCode, SpotifyApiError> {
        let token = self.load_token().await?;
        let token = token
            .ok_or_else(|| SpotifyApiError::Generic("No account token present!".to_string()))?;

        let ids = uris.join(",");

        let res = self
            .client
            .delete(format!("{}/v1/me/library", SPOTIFY_API_URL))
            .query(&[("uris", ids)])
            .header(reqwest::header::CONTENT_LENGTH, "0")
            .bearer_auth(token.access_token)
            .timeout(REQUEST_TIMEOUT)
            .send()
            .await?;

        if !res.status().is_success() {
            let status = res.status().as_str().to_string();
            let body = res.text().await.unwrap_or_default();
            return Err(SpotifyApiError::Generic(format!(
                "delete_items failed with status {status}: {body}"
            )));
        }

        Ok(res.status())
    }

    /// Get tracks/albums saved in library
    pub async fn get_saved(
        &self,
        item: SavedItemType,
    ) -> Result<SavedItemsResponse<Uri>, SpotifyApiError> {
        let token = self.load_token().await?;
        let token = token
            .ok_or_else(|| SpotifyApiError::Generic("No account token present!".to_string()))?;

        let res = self
            .client
            .get(format!("{}/v1/me/{}", SPOTIFY_API_URL, item.as_str()))
            .bearer_auth(token.access_token)
            .timeout(REQUEST_TIMEOUT)
            .send()
            .await?
            .json::<SavedItemsResponse<Uri>>()
            .await;

        match res {
            Ok(items) => Ok(items),
            Err(e) => {
                return Err(SpotifyApiError::Generic(format!(
                    "get_saved failed with error: {e}"
                )));
            }
        }
    }
}

pub enum SavedItemType {
    Tracks,
    Albums,
}

impl SavedItemType {
    pub fn as_str(&self) -> &'static str {
        match self {
            SavedItemType::Tracks => "tracks",
            SavedItemType::Albums => "albums",
        }
    }
}

impl FromStr for SavedItemType {
    type Err = SpotifyApiError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        match s {
            "tracks" => Ok(Self::Tracks),
            "albums" => Ok(Self::Albums),
            other => Err(SpotifyApiError::Generic(format!(
                "Invalid SavedItemType: {other}"
            ))),
        }
    }
}
