use reqwest::StatusCode;

use crate::{
    spotify::error::SpotifyApiError,
    types::{
        requests::{StartPlaybackOffset, StartPlaybackRequest, TransferPlaybackRequest},
        responses::DevicesResponse,
    },
};

use super::{
    check_response_json, SpotifyClient, REQUEST_TIMEOUT, SPOTIFY_API_URL,
};

impl SpotifyClient {
    /// Sends an authorized request against the Web API, transparently
    /// refreshing the OAuth token once on 401 and retrying.
    pub(crate) async fn send_authorized(
        &self,
        method_name: &str,
        build_request: impl Fn(&str) -> reqwest::RequestBuilder,
    ) -> Result<StatusCode, SpotifyApiError> {
        let token = self.load_token().await?;
        let token = token.ok_or_else(|| {
            SpotifyApiError::Generic("No account token present!".to_string())
        })?;

        let res = build_request(&token.access_token)
            .timeout(REQUEST_TIMEOUT)
            .send()
            .await?;

        let res = if res.status() == StatusCode::UNAUTHORIZED {
            let new_token = self.refresh_token(&token).await?;
            build_request(&new_token.access_token)
                .timeout(REQUEST_TIMEOUT)
                .send()
                .await?
        } else {
            res
        };

        let status = res.status();
        if !status.is_success() {
            let body = res.text().await.unwrap_or_default();
            return Err(SpotifyApiError::Generic(format!(
                "{method_name} failed with status {}: {body}",
                status.as_str()
            )));
        }

        Ok(status)
    }

    /// Builds a player endpoint URL ending with an open query string,
    /// so extra parameters can be appended directly.
    fn player_url(path: &str, device_id: &Option<String>) -> String {
        match device_id {
            Some(id) => format!("{SPOTIFY_API_URL}/v1/me/player/{path}?device_id={id}&"),
            None => format!("{SPOTIFY_API_URL}/v1/me/player/{path}?"),
        }
    }

    pub async fn get_devices(&self) -> Result<DevicesResponse, SpotifyApiError> {
        let token = self.load_token().await?;
        let token = token.ok_or_else(|| {
            SpotifyApiError::Generic("No account token present!".to_string())
        })?;

        let url = format!("{}/v1/me/player/devices", SPOTIFY_API_URL);

        let res = self
            .client
            .get(&url)
            .bearer_auth(&token.access_token)
            .timeout(REQUEST_TIMEOUT)
            .send()
            .await?;

        if res.status() == StatusCode::UNAUTHORIZED {
            let new_token = self.refresh_token(&token).await?;
            let res = self
                .client
                .get(&url)
                .bearer_auth(new_token.access_token)
                .timeout(REQUEST_TIMEOUT)
                .send()
                .await?;
            let data = check_response_json::<DevicesResponse>("get_devices", res).await?;
            return Ok(data);
        }

        let data = check_response_json::<DevicesResponse>("get_devices", res).await?;

        Ok(data)
    }

    pub async fn transfer_playback(
        &self,
        device_id: String,
    ) -> Result<StatusCode, SpotifyApiError> {
        let body = TransferPlaybackRequest {
            device_ids: vec![device_id],
        };

        self.send_authorized("transfer_playback", |token| {
            self.client
                .put(format!("{SPOTIFY_API_URL}/v1/me/player"))
                .bearer_auth(token)
                .json(&body)
        })
        .await
    }

    /// Starts playback on the given (or active) device.
    /// Either `context_uri` (album/playlist/artist) or `uris` (tracks) must be set.
    pub async fn start_playback(
        &self,
        device_id: Option<String>,
        context_uri: Option<String>,
        uris: Option<Vec<String>>,
        offset_uri: Option<String>,
        position_ms: Option<u32>,
    ) -> Result<StatusCode, SpotifyApiError> {
        if context_uri.is_none() && uris.is_none() {
            return Err(SpotifyApiError::Generic(
                "start_playback requires either context_uri or uris".to_string(),
            ));
        }

        let offset = offset_uri.map(|uri| StartPlaybackOffset {
            uri: Some(uri),
        });

        let body = StartPlaybackRequest {
            context_uri,
            uris,
            offset,
            position_ms,
        };

        let url = Self::player_url("play", &device_id);
        self.send_authorized("start_playback", |token| {
            self.client.put(&url).bearer_auth(token).json(&body)
        })
        .await
    }

    pub async fn pause_playback(
        &self,
        device_id: Option<String>,
    ) -> Result<StatusCode, SpotifyApiError> {
        let url = Self::player_url("pause", &device_id);
        self.send_authorized("pause_playback", |token| {
            self.client.put(&url).bearer_auth(token)
        })
        .await
    }

    pub async fn next_track(
        &self,
        device_id: Option<String>,
    ) -> Result<StatusCode, SpotifyApiError> {
        let url = Self::player_url("next", &device_id);
        self.send_authorized("next_track", |token| {
            self.client.post(&url).bearer_auth(token)
        })
        .await
    }

    pub async fn previous_track(
        &self,
        device_id: Option<String>,
    ) -> Result<StatusCode, SpotifyApiError> {
        let url = Self::player_url("previous", &device_id);
        self.send_authorized("previous_track", |token| {
            self.client.post(&url).bearer_auth(token)
        })
        .await
    }

    pub async fn seek_playback(
        &self,
        position_ms: u32,
        device_id: Option<String>,
    ) -> Result<StatusCode, SpotifyApiError> {
        let mut url = Self::player_url("seek", &device_id);
        url.push_str(&format!("position_ms={position_ms}"));
        self.send_authorized("seek_playback", |token| {
            self.client.put(&url).bearer_auth(token)
        })
        .await
    }

    pub async fn set_volume(
        &self,
        volume_percent: u8,
        device_id: Option<String>,
    ) -> Result<StatusCode, SpotifyApiError> {
        if volume_percent > 100 {
            return Err(SpotifyApiError::Generic(
                "volume_percent must be within 0..=100".to_string(),
            ));
        }

        let mut url = Self::player_url("volume", &device_id);
        url.push_str(&format!("volume_percent={volume_percent}"));
        self.send_authorized("set_volume", |token| {
            self.client.put(&url).bearer_auth(token)
        })
        .await
    }

    /// state: "track", "context" or "off"
    pub async fn set_repeat(
        &self,
        state: String,
        device_id: Option<String>,
    ) -> Result<StatusCode, SpotifyApiError> {
        let mut url = Self::player_url("repeat", &device_id);
        url.push_str(&format!("state={state}"));
        self.send_authorized("set_repeat", |token| {
            self.client.put(&url).bearer_auth(token)
        })
        .await
    }

    pub async fn set_shuffle(
        &self,
        state: bool,
        device_id: Option<String>,
    ) -> Result<StatusCode, SpotifyApiError> {
        let mut url = Self::player_url("shuffle", &device_id);
        url.push_str(&format!("state={state}"));
        self.send_authorized("set_shuffle", |token| {
            self.client.put(&url).bearer_auth(token)
        })
        .await
    }

    pub async fn add_to_queue(
        &self,
        uri: String,
        device_id: Option<String>,
    ) -> Result<StatusCode, SpotifyApiError> {
        let mut url = Self::player_url("queue", &device_id);
        url.push_str(&format!("uri={uri}"));
        self.send_authorized("add_to_queue", |token| {
            self.client.post(&url).bearer_auth(token)
        })
        .await
    }
}
