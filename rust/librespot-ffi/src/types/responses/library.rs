use serde::Deserialize;

#[derive(Debug, Deserialize)]
pub struct SavedItemsResponse<T> {
    pub items: Vec<SavedItem<T>>,
}

#[derive(Debug, Deserialize)]
pub struct SavedItem<T> {
    #[serde(alias = "album", alias = "track")]
    pub item: T,
}

#[derive(Debug, Deserialize)]
pub struct Uri {
    pub uri: String,
}
