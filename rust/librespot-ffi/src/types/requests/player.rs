use serde::Serialize;

#[derive(Serialize, Default)]
pub struct StartPlaybackRequest {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_uri: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub uris: Option<Vec<String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub offset: Option<StartPlaybackOffset>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub position_ms: Option<u32>,
}

#[derive(Serialize)]
pub struct StartPlaybackOffset {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub uri: Option<String>,
}
